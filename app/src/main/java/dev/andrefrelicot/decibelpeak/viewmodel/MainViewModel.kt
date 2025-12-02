package dev.andrefrelicot.decibelpeak.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.andrefrelicot.decibelpeak.audio.AudioProcessor
import dev.andrefrelicot.decibelpeak.audio.AudioRecorder
import dev.andrefrelicot.decibelpeak.model.TimestampedDbValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val audioRecorder = AudioRecorder()
    private val audioProcessor = AudioProcessor()
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _decibelLevel = MutableStateFlow(0.0)
    val decibelLevel: StateFlow<Double> = _decibelLevel.asStateFlow()

    private val _waveformSamples = MutableStateFlow<List<Float>>(emptyList())
    val waveformSamples: StateFlow<List<Float>> = _waveformSamples.asStateFlow()

    private val _frequencyBands = MutableStateFlow<List<Float>>(List(64) { 0f })
    val frequencyBands: StateFlow<List<Float>> = _frequencyBands.asStateFlow()

    private val _waterfallData = MutableStateFlow<List<List<Float>>>(emptyList())
    val waterfallData: StateFlow<List<List<Float>>> = _waterfallData.asStateFlow()

    private val _dbHistory = MutableStateFlow<List<Double>>(emptyList())
    val dbHistory: StateFlow<List<Double>> = _dbHistory.asStateFlow()

    // Timestamped dB history for smooth curve scrolling (matching iOS)
    private val _timestampedDbHistory = MutableStateFlow<List<TimestampedDbValue>>(emptyList())
    val timestampedDbHistory: StateFlow<List<TimestampedDbValue>> = _timestampedDbHistory.asStateFlow()

    // Selected visualization index (persists across orientation changes)
    private val _selectedVisualization = MutableStateFlow(0)
    val selectedVisualization: StateFlow<Int> = _selectedVisualization.asStateFlow()

    fun setSelectedVisualization(index: Int) {
        _selectedVisualization.value = index
    }

    // Time-based throttling for refresh rate independence (matching iOS intervals)
    private var lastLevelUpdateTime = 0L      // 33ms = ~30 FPS (iOS levelTimer)
    private var lastDbHistoryUpdateTime = 0L  // 100ms = 10 FPS (iOS dbHistoryTimer)
    private var lastWaterfallUpdateTime = 0L  // 67ms = ~15 FPS (iOS waterfall)
    private var lastSpectrumUpdateTime = 0L   // 33ms = ~30 FPS

    // iOS timing constants (in milliseconds)
    companion object {
        private const val LEVEL_UPDATE_INTERVAL = 33L    // ~30 FPS
        private const val DB_HISTORY_INTERVAL = 100L     // 10 FPS
        private const val WATERFALL_INTERVAL = 67L       // ~15 FPS
        private const val SPECTRUM_INTERVAL = 33L        // ~30 FPS

        // dB Curve smooth scrolling constants (matching iOS)
        private const val TIME_WINDOW_SECONDS = 10.0          // 10 seconds of visible data
        private const val BUFFER_TIME_SECONDS = 1.5           // Extra buffer for offscreen
        private const val APPEARANCE_DELAY_MS = 75_000_000L   // 75ms in nanoseconds
    }

    // Smoothing for waterfall (matching iOS: 70% old + 30% new)
    private var lastWaterfallBands = FloatArray(64) { 0f }

    // Smoothing for waveform samples (matching iOS animation behavior)
    // iOS uses withAnimation(.easeInOut(duration: 0.1)) + .animation(.easeInOut(duration: 0.05))
    // We simulate this with exponential smoothing
    private var lastWaveformSamples = FloatArray(102) { 0f }  // ~102 samples from 1024/10

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        recordingJob = viewModelScope.launch {
            audioRecorder.startRecording().collect { buffer ->
                val currentTime = System.currentTimeMillis()
                val db = audioProcessor.calculateDecibel(buffer)
                val fft = audioProcessor.calculateFFT(buffer)

                // Decibel level + Waveform: 30 FPS (33ms) - matches iOS levelTimer
                if (currentTime - lastLevelUpdateTime >= LEVEL_UPDATE_INTERVAL) {
                    lastLevelUpdateTime = currentTime
                    _decibelLevel.value = db

                    // Downsample waveform for display
                    val downsampled = buffer.filterIndexed { index, _ -> index % 10 == 0 }

                    // Apply smoothing to waveform samples (matching iOS animation behavior)
                    // iOS uses ~150ms total animation time, we use 0.3 smoothing factor (~100ms equivalent)
                    val smoothingFactor = 0.3f
                    val smoothedSamples = downsampled.mapIndexed { i, newValue ->
                        val oldValue = if (i < lastWaveformSamples.size) lastWaveformSamples[i] else 0f
                        val smoothed = oldValue * (1f - smoothingFactor) + newValue * smoothingFactor
                        if (i < lastWaveformSamples.size) {
                            lastWaveformSamples[i] = smoothed
                        }
                        smoothed
                    }
                    _waveformSamples.value = smoothedSamples
                }

                // dB History: 10 FPS (100ms) - matches iOS dbHistoryTimer
                if (currentTime - lastDbHistoryUpdateTime >= DB_HISTORY_INTERVAL) {
                    lastDbHistoryUpdateTime = currentTime

                    // Legacy simple history (for compatibility)
                    val currentHistory = _dbHistory.value.toMutableList()
                    if (currentHistory.size >= 100) {
                        currentHistory.removeAt(0)
                    }
                    currentHistory.add(db)
                    _dbHistory.value = currentHistory

                    // Timestamped history for smooth curve scrolling (matching iOS)
                    val nanoTime = System.nanoTime()
                    val newPoint = TimestampedDbValue(
                        value = db,
                        timestamp = nanoTime,
                        appearanceTime = nanoTime + APPEARANCE_DELAY_MS
                    )

                    val updatedTimestampedHistory = _timestampedDbHistory.value.toMutableList()
                    updatedTimestampedHistory.add(newPoint)

                    // Remove old points outside the visible time window + buffer
                    val cutoffTime = nanoTime - ((TIME_WINDOW_SECONDS + BUFFER_TIME_SECONDS) * 1_000_000_000L).toLong()
                    updatedTimestampedHistory.removeAll { it.timestamp < cutoffTime }

                    _timestampedDbHistory.value = updatedTimestampedHistory
                }

                // Spectrum/FFT bands: 30 FPS (33ms)
                if (currentTime - lastSpectrumUpdateTime >= SPECTRUM_INTERVAL) {
                    lastSpectrumUpdateTime = currentTime
                    _frequencyBands.value = fft
                }

                // Waterfall: 15 FPS (67ms) - matches iOS waterfall throttle
                if (currentTime - lastWaterfallUpdateTime >= WATERFALL_INTERVAL) {
                    lastWaterfallUpdateTime = currentTime

                    // Apply smoothing like iOS: 70% old + 30% new
                    val smoothingFactor = 0.3f
                    val smoothedBands = fft.mapIndexed { i, newValue ->
                        val smoothed = lastWaterfallBands[i] * (1f - smoothingFactor) + newValue * smoothingFactor
                        lastWaterfallBands[i] = smoothed
                        smoothed
                    }

                    val currentWaterfall = _waterfallData.value.toMutableList()
                    if (currentWaterfall.size >= 80) {
                        currentWaterfall.removeAt(currentWaterfall.lastIndex)
                    }
                    currentWaterfall.add(0, smoothedBands)
                    _waterfallData.value = currentWaterfall
                }
            }
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        _decibelLevel.value = 0.0
        _waveformSamples.value = emptyList()
        _frequencyBands.value = List(64) { 0f }
        _dbHistory.value = emptyList()
        _timestampedDbHistory.value = emptyList()
        _waterfallData.value = emptyList()

        // Reset all timing and smoothing state
        lastWaterfallBands = FloatArray(64) { 0f }
        lastWaveformSamples = FloatArray(102) { 0f }
        lastLevelUpdateTime = 0L
        lastDbHistoryUpdateTime = 0L
        lastWaterfallUpdateTime = 0L
        lastSpectrumUpdateTime = 0L
        audioProcessor.resetSmoothing()
    }
}

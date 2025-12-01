package com.example.decibelpeak.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decibelpeak.audio.AudioProcessor
import com.example.decibelpeak.audio.AudioRecorder
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
    }

    // Smoothing for waterfall (matching iOS: 70% old + 30% new)
    private var lastWaterfallBands = FloatArray(64) { 0f }

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
                    val downsampled = buffer.filterIndexed { index, _ -> index % 10 == 0 }.toList()
                    _waveformSamples.value = downsampled
                }

                // dB History: 10 FPS (100ms) - matches iOS dbHistoryTimer
                if (currentTime - lastDbHistoryUpdateTime >= DB_HISTORY_INTERVAL) {
                    lastDbHistoryUpdateTime = currentTime
                    val currentHistory = _dbHistory.value.toMutableList()
                    if (currentHistory.size >= 100) {
                        currentHistory.removeAt(0)
                    }
                    currentHistory.add(db)
                    _dbHistory.value = currentHistory
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
        _waterfallData.value = emptyList()

        // Reset all timing and smoothing state
        lastWaterfallBands = FloatArray(64) { 0f }
        lastLevelUpdateTime = 0L
        lastDbHistoryUpdateTime = 0L
        lastWaterfallUpdateTime = 0L
        lastSpectrumUpdateTime = 0L
    }
}

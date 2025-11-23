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
                val db = audioProcessor.calculateDecibel(buffer)
                _decibelLevel.value = db
                
                // Update history (keep last 100 points)
                val currentHistory = _dbHistory.value.toMutableList()
                if (currentHistory.size >= 100) {
                    currentHistory.removeAt(0)
                }
                currentHistory.add(db)
                _dbHistory.value = currentHistory
                
                // Downsample for waveform
                val downsampled = buffer.filterIndexed { index, _ -> index % 10 == 0 }.toList()
                _waveformSamples.value = downsampled

                val fft = audioProcessor.calculateFFT(buffer)
                _frequencyBands.value = fft

                // Update waterfall
                val currentWaterfall = _waterfallData.value.toMutableList()
                if (currentWaterfall.size >= 80) {
                    currentWaterfall.removeAt(currentWaterfall.lastIndex)
                }
                currentWaterfall.add(0, fft)
                _waterfallData.value = currentWaterfall
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
    }
}

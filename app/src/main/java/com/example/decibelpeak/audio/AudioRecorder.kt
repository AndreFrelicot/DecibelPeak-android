package com.example.decibelpeak.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class AudioRecorder {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<FloatArray> = flow {
        // Use UNPROCESSED for raw audio without AGC (automatic gain control)
        // This provides full dynamic range like iOS
        // Falls back to VOICE_RECOGNITION (minimal processing) on older devices
        val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }

        val recorder = AudioRecord(
            audioSource,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            return@flow
        }

        recorder.startRecording()
        val buffer = FloatArray(1024) // Match iOS buffer size

        try {
            while (kotlin.coroutines.coroutineContext.isActive) {
                val readCount = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (readCount > 0) {
                    emit(buffer.copyOf())
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}

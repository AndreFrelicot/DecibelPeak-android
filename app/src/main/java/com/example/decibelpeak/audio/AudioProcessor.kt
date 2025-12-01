package com.example.decibelpeak.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class AudioProcessor {
    private val fftSize = 1024
    private val window = FloatArray(fftSize) { i ->
        0.5f * (1 - cos(2.0 * PI * i / (fftSize - 1))).toFloat() // Hanning window
    }

    fun calculateDecibel(buffer: FloatArray): Double {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample * sample
        }
        val rms = sqrt(sum / buffer.size)
        val db = 20 * log10(rms.coerceAtLeast(0.00001))
        return db + 100 // Calibration offset to match iOS
    }

    fun calculateFFT(buffer: FloatArray): List<Float> {
        // Zero-pad or truncate to fftSize
        val input = FloatArray(fftSize)
        val length = minOf(buffer.size, fftSize)
        for (i in 0 until length) {
            input[i] = buffer[i] * window[i]
        }

        val real = input.copyOf()
        val imag = FloatArray(fftSize)

        fft(real, imag)

        val magnitudes = FloatArray(fftSize / 2)
        for (i in magnitudes.indices) {
            val r = real[i]
            val im = imag[i]
            magnitudes[i] = sqrt(r * r + im * im)
        }

        // Match iOS: Convert to dB scale and normalize to 0-1 range
        val minDB = -60f
        val maxDB = 0f
        val dbRange = maxDB - minDB

        return magnitudes.take(64).map { magnitude ->
            // Convert to dB (matching iOS)
            val db = 20f * log10(magnitude.coerceAtLeast(0.000001f))
            // Clamp to range and normalize to 0-1
            val clampedDb = db.coerceIn(minDB, maxDB)
            (clampedDb - minDB) / dbRange
        }
    }

    // Simple Cooley-Tukey FFT implementation
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        val half = n / 2
        val evenReal = FloatArray(half)
        val evenImag = FloatArray(half)
        val oddReal = FloatArray(half)
        val oddImag = FloatArray(half)

        for (i in 0 until half) {
            evenReal[i] = real[2 * i]
            evenImag[i] = imag[2 * i]
            oddReal[i] = real[2 * i + 1]
            oddImag[i] = imag[2 * i + 1]
        }

        fft(evenReal, evenImag)
        fft(oddReal, oddImag)

        for (k in 0 until half) {
            val angle = -2.0 * PI * k / n
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            
            val tReal = cosA * oddReal[k] - sinA * oddImag[k]
            val tImag = sinA * oddReal[k] + cosA * oddImag[k]
            
            real[k] = evenReal[k] + tReal
            imag[k] = evenImag[k] + tImag
            real[k + half] = evenReal[k] - tReal
            imag[k + half] = evenImag[k] - tImag
        }
    }
}

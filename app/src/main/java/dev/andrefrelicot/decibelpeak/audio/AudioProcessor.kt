package dev.andrefrelicot.decibelpeak.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

class AudioProcessor {
    private val fftSize = 1024
    private val sampleRate = 44100f
    private val bandCount = 64
    private val window = FloatArray(fftSize) { i ->
        0.5f * (1 - cos(2.0 * PI * i / (fftSize - 1))).toFloat() // Hanning window
    }

    // Pre-calculate logarithmic frequency bands (matching iOS: 20Hz to 20kHz)
    private val logFrequencies: FloatArray = FloatArray(bandCount) { i ->
        val minFreq = 20f
        val maxFreq = 20000f
        val logMin = log10(minFreq)
        val logMax = log10(maxFreq)
        val logFreq = logMin + (i.toFloat() / (bandCount - 1)) * (logMax - logMin)
        10f.pow(logFreq)
    }

    // Smoothed dB value (matching iOS: 80% old + 20% new)
    private var smoothedDb: Double = 0.0

    fun calculateDecibel(buffer: FloatArray): Double {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample * sample
        }
        val rms = sqrt(sum / buffer.size)
        val db = 20 * log10(rms.coerceAtLeast(0.00001))
        val calibratedDb = db + 110 // Calibration offset to match iOS

        // Apply exponential smoothing (increased for smoother display: 92% old + 8% new)
        smoothedDb = smoothedDb * 0.92 + calibratedDb * 0.08
        return smoothedDb
    }

    fun resetSmoothing() {
        smoothedDb = 0.0
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

        // Calculate magnitudes for all bins
        val magnitudeCount = fftSize / 2
        val magnitudes = FloatArray(magnitudeCount)
        for (i in magnitudes.indices) {
            val r = real[i]
            val im = imag[i]
            magnitudes[i] = sqrt(r * r + im * im)
        }

        // Match iOS: Convert to dB scale and normalize to 0-1 range
        val minDB = -60f
        val maxDB = 0f
        val dbRange = maxDB - minDB

        val dbMagnitudes = FloatArray(magnitudeCount) { i ->
            val db = 20f * log10(magnitudes[i].coerceAtLeast(0.000001f))
            val clampedDb = db.coerceIn(minDB, maxDB)
            (clampedDb - minDB) / dbRange
        }

        // Match iOS: Map to logarithmic frequency bands (20Hz - 20kHz)
        val nyquistFrequency = sampleRate / 2f
        val frequencyResolution = nyquistFrequency / magnitudeCount

        return logFrequencies.map { frequency ->
            val binIndex = (frequency / frequencyResolution).roundToInt()
            if (binIndex < magnitudeCount) {
                // Average surrounding bins for smoother representation (matching iOS)
                val startBin = maxOf(0, binIndex - 1)
                val endBin = minOf(magnitudeCount - 1, binIndex + 1)
                var sum = 0f
                var count = 0
                for (bin in startBin..endBin) {
                    sum += dbMagnitudes[bin]
                    count++
                }
                if (count > 0) sum / count else 0f
            } else {
                0f
            }
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

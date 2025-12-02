package dev.andrefrelicot.decibelpeak.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages calibration offset storage using SharedPreferences.
 * Range: -20.0 to +20.0 dB with 0.5 dB steps
 * Default: 0.0 dB
 */
class CalibrationManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "decibel_peak_prefs"
        private const val KEY_CALIBRATION_OFFSET = "calibration_offset"

        const val MIN_OFFSET = -20.0
        const val MAX_OFFSET = 20.0
        const val STEP = 0.5
        const val DEFAULT_OFFSET = 0.0
    }

    /**
     * Get the current calibration offset.
     * Returns 0.0 if no value has been saved.
     */
    fun getCalibrationOffset(): Double {
        return prefs.getFloat(KEY_CALIBRATION_OFFSET, DEFAULT_OFFSET.toFloat()).toDouble()
    }

    /**
     * Save the calibration offset.
     * Value is clamped to valid range and rounded to nearest step.
     */
    fun setCalibrationOffset(offset: Double) {
        // Round to nearest 0.5 dB step
        val rounded = (offset / STEP).let { kotlin.math.round(it) * STEP }
        // Clamp to valid range
        val clamped = rounded.coerceIn(MIN_OFFSET, MAX_OFFSET)

        prefs.edit()
            .putFloat(KEY_CALIBRATION_OFFSET, clamped.toFloat())
            .apply()
    }
}

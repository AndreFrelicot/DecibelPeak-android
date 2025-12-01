package com.example.decibelpeak.model

/**
 * Timestamped dB value for smooth curve scrolling (matching iOS implementation)
 * @param value The decibel value
 * @param timestamp When the data was recorded (System.nanoTime() based)
 * @param appearanceTime When the data should start appearing visually (with delay for smooth entry)
 */
data class TimestampedDbValue(
    val value: Double,
    val timestamp: Long,      // nanoseconds from System.nanoTime()
    val appearanceTime: Long  // nanoseconds - timestamp + appearance delay
)

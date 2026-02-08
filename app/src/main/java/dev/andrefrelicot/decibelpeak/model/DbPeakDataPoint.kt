package dev.andrefrelicot.decibelpeak.model

data class DbPeakDataPoint(
    val value: Double,
    val timeMillis: Long  // System.currentTimeMillis()
)

package dev.andrefrelicot.decibelpeak

import dev.andrefrelicot.decibelpeak.viewmodel.MainViewModel
import org.junit.Test

import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun equivalentDecibelLevelUsesEnergyAverage() {
        val powerSum = MainViewModel.linearPower(60.0)!! + MainViewModel.linearPower(70.0)!!
        val average = MainViewModel.equivalentDecibelLevel(powerSum, 2)!!

        assertEquals(67.4036, average, 0.001)
    }

    @Test
    fun equivalentDecibelLevelRejectsInvalidInputs() {
        assertNull(MainViewModel.linearPower(Double.NaN))
        assertNull(MainViewModel.equivalentDecibelLevel(0.0, 2))
        assertNull(MainViewModel.equivalentDecibelLevel(1.0, 0))
    }
}

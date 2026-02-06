package com.example.warehouse.ui.viewmodel

import com.example.warehouse.util.MuntinCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MuntinViewModelTest {

    @Test
    fun `calculate updates result state`() {
        // Given
        val viewModel = MuntinViewModel()
        
        // When
        viewModel.calculate(
            sashWidth = 1000,
            sashHeight = 1000,
            profileHeight = 70,
            beadHeight = 20,
            beadAngle = 45.0,
            muntinWidth = 26,
            muntinGap = 1.0,
            overlap = 0.0,
            verticalFields = 2,
            horizontalFields = 2,
            isHalvingJoint = false,
            externalOffset = 0.0
        )

        // Then
        val result = viewModel.result.value
        assertNotNull(result)
        // Check if values are roughly as expected (logic detailed in MuntinCalculatorTest)
        // Just verify it's not null and has correct structure counts
        assertEquals(1, result?.verticalCount)
        assertEquals(2, result?.horizontalSegments?.size)
    }
}

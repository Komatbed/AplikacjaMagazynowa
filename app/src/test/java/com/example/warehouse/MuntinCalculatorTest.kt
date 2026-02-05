package com.example.warehouse

import com.example.warehouse.util.MuntinCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class MuntinCalculatorTest {

    @Test
    fun calculateRectangularGrid_SimpleCross_ReturnsCorrectLengths() {
        // Arrange
        val request = MuntinCalculator.MuntinRequest(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfileHeightMm = 70,
            beadHeightMm = 20,
            beadAngleDeg = 45.0,
            muntinWidthMm = 26,
            muntinGapMm = 1.0,
            overlapBeadMm = 0.0,
            isHalvingJoint = false
        )

        // Act
        // 2x2 fields -> 1 vertical line, 1 horizontal line
        val result = MuntinCalculator.calculateRectangularGrid(request, 2, 2)

        // Assert
        // Glass Opening = 1000 - 140 = 860
        // Bead Projection = 20 / tan(45) = 20
        // Visible Glass = 860 - 40 = 820
        
        // Vertical (Continuous):
        // Length = Visible(820) + Overlap(0) - 2*Gap(1) = 818
        assertEquals(818.0, result.verticalMuntinLength, 0.1)
        assertEquals(1, result.verticalCount)
        
        // Horizontal (Cut):
        // Total Visible Width = 820
        // Muntin Width = 26
        // Remaining Width = 820 - 26 = 794
        // Segment Width = 794 / 2 = 397
        // Left Segment: 397 + Overlap(0) - Gap(1) = 396
        // Right Segment: 397 + Overlap(0) - Gap(1) = 396
        
        assertEquals(2, result.horizontalSegments.size)
        assertEquals(396.0, result.horizontalSegments[0], 0.1)
    }

    @Test
    fun calculateRectangularGrid_HalvingJoint_ReturnsFullLengths() {
        // Arrange
        val request = MuntinCalculator.MuntinRequest(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfileHeightMm = 70,
            beadHeightMm = 20,
            beadAngleDeg = 45.0,
            muntinWidthMm = 26,
            muntinGapMm = 1.0,
            overlapBeadMm = 0.0,
            isHalvingJoint = true
        )

        // Act
        val result = MuntinCalculator.calculateRectangularGrid(request, 2, 2)

        // Assert
        // Vertical should be same: 818
        assertEquals(818.0, result.verticalSegments[0], 0.1)
        
        // Horizontal should ALSO be full length (818)
        assertEquals(818.0, result.horizontalSegments[0], 0.1)
    }
}

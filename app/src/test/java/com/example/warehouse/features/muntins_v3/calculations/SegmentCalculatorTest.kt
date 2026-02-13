package com.example.warehouse.features.muntins_v3.calculations

import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentCalculatorTest {

    private val WIDTH = 26.0

    @Test
    fun `calculate straight cut 90 degrees`() {
        // Segment from (0,0) to (100,0) - Horizontal
        val segment = Segment("1", Node(0.0, 0.0), Node(100.0, 0.0), WIDTH)
        
        // Boundaries are vertical lines at x=10 and x=90
        val startBoundary = Pair(Node(10.0, -50.0), Node(10.0, 50.0))
        val endBoundary = Pair(Node(90.0, -50.0), Node(90.0, 50.0))

        val result = SegmentCalculator.calculateRealLength(
            segment,
            startBoundary,
            endBoundary,
            clearanceStart = 1.0,
            clearanceEnd = 1.0
        )

        // Axis Intersection:
        // Start: (10, 0)
        // End: (90, 0)
        // Axis Dist = 80
        
        // Angles:
        // Horizontal (0 deg) vs Vertical (90 deg) -> Angle is 90 deg.
        // Correction = 0.

        // Final = 80 + 0 + 0 - 2 = 78.0
        
        assertEquals(78.0, result.finalLength, 0.01)
        assertEquals(90.0, result.cutAngleStart, 0.01)
        assertEquals(90.0, result.cutAngleEnd, 0.01)
    }

    @Test
    fun `calculate 45 degree cut`() {
        // Segment from (0,0) to (100,0) - Horizontal
        val segment = Segment("1", Node(0.0, 0.0), Node(100.0, 0.0), WIDTH) // width 26
        
        // Boundaries are lines at 45 degrees
        // Start Boundary passing through (10,0) with angle 45
        // y - 0 = 1 * (x - 10) => y = x - 10
        // Points: (10, 0) and (20, 10)
        val startBoundary = Pair(Node(10.0, 0.0), Node(20.0, 10.0))
        
        // End Boundary passing through (90,0) with angle -45 (135)
        // y - 0 = -1 * (x - 90) => y = -x + 90
        // Points: (90, 0) and (100, -10)
        val endBoundary = Pair(Node(90.0, 0.0), Node(100.0, -10.0))

        val result = SegmentCalculator.calculateRealLength(
            segment,
            startBoundary,
            endBoundary,
            clearanceStart = 1.0,
            clearanceEnd = 1.0
        )

        // Axis Intersection:
        // Start: (10, 0)
        // End: (90, 0)
        // Axis Dist = 80
        
        // Angles:
        // Segment: 0 deg
        // Start Bound: 45 deg -> Intersection Angle 45
        // End Bound: 135 deg -> Intersection Angle 45 (180-135)

        // Correction for 45 deg:
        // (26 / 2) / tan(45) = 13 / 1 = 13.0
        
        // Final = 80 + 13 + 13 - 2 = 104.0
        
        assertEquals(104.0, result.finalLength, 0.01)
        assertEquals(45.0, result.cutAngleStart, 0.01)
        assertEquals(45.0, result.cutAngleEnd, 0.01)
    }
}

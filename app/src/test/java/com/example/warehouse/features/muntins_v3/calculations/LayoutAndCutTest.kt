package com.example.warehouse.features.muntins_v3.calculations

import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutAndCutTest {

    @Test
    fun `test 2x2 grid cuts`() {
        val glassWidth = 1000.0
        val glassHeight = 1000.0
        val muntinWidth = 26.0
        
        // Generate 2x2 grid (1 vertical, 1 horizontal split into 2)
        val segments = LayoutEngine.generateGrid(
            width = glassWidth,
            height = glassHeight,
            rows = 2,
            cols = 2,
            muntinWidth = muntinWidth
        )

        // Verify segments count: 1 vertical + 2 horizontals = 3 segments
        assertEquals(3, segments.size)

        // Calculate cuts
        val cutList = CutListCalculator.calculateCutList(segments, glassWidth, glassHeight)

        // Verify cuts
        // 1 Vertical: 1000 - 1 - 1 = 998.0
        // 2 Horizontals: 500 - 1 - (13+1) = 485.0
        
        assertEquals(2, cutList.size) // 2 groups
        
        val verticalCut = cutList.find { it.length == 998.0 }
        val horizontalCut = cutList.find { it.length == 485.0 }
        
        assertEquals(1, verticalCut?.count)
        assertEquals(2, horizontalCut?.count)
    }

    @Test
    fun `test 3x3 grid cuts`() {
        val glassWidth = 1000.0
        val glassHeight = 1000.0
        val muntinWidth = 26.0
        
        val segments = LayoutEngine.generateGrid(
            width = glassWidth,
            height = glassHeight,
            rows = 3,
            cols = 3,
            muntinWidth = muntinWidth
        )
        
        assertEquals(8, segments.size)
        
        val cutList = CutListCalculator.calculateCutList(segments, glassWidth, glassHeight)
        
        // Verticals: 1000 - 2 = 998.0. Count: 2.
        val verticalCuts = cutList.find { it.length == 998.0 }
        assertEquals(2, verticalCuts?.count)
        
        // Horizontals:
        // Side sections: 333.333 - 15.0 = 318.3
        // Middle sections: 333.333 - 28.0 = 305.3
        
        val sideCuts = cutList.find { kotlin.math.abs(it.length - 318.3) < 0.1 }
        val middleCuts = cutList.find { kotlin.math.abs(it.length - 305.3) < 0.1 }

        assertEquals(4, sideCuts?.count)
        assertEquals(2, middleCuts?.count)
    }

    @Test
    fun `test loose end handling`() {
        val glassWidth = 1000.0
        val glassHeight = 1000.0
        val muntinWidth = 26.0
        
        // 1. Generate 2x2 grid
        val segments = LayoutEngine.generateGrid(
            width = glassWidth,
            height = glassHeight,
            rows = 2,
            cols = 2,
            muntinWidth = muntinWidth
        ).toMutableList()

        // 2. Identify and remove vertical segment
        // Vertical segment is the one with startNode.y == 0 and endNode.y == 1000
        val vertical = segments.find { kotlin.math.abs(it.startNode.y - 0.0) < 0.001 && kotlin.math.abs(it.endNode.y - 1000.0) < 0.001 }
        
        if (vertical != null) {
            segments.remove(vertical)
        } else {
            println("Vertical segment not found!")
            segments.forEach { println("Seg: ${it.startNode} -> ${it.endNode}") }
        }

        assertEquals(2, segments.size)

        // 3. Calculate cuts for remaining horizontals
        val cutList = CutListCalculator.calculateCutList(segments, glassWidth, glassHeight)

        // Expected:
        // Length = 500 - 1.0 (glass edge) - 0.0 (loose end) = 499.0
        
        assertEquals(1, cutList.size)
        val cut = cutList.first()
        assertEquals(499.0, cut.length, 0.1)
        assertEquals(2, cut.count)
    }
}

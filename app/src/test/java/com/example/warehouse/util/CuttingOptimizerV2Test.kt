package com.example.warehouse.util

import com.example.warehouse.model.CutItemV2
import com.example.warehouse.model.Axis
import org.junit.Assert.assertEquals
import org.junit.Test

class CuttingOptimizerV2Test {

    private fun createItem(length: Double, id: String = "1", profile: String = "ProfileA"): CutItemV2 {
        return CutItemV2(
            sashNo = 1,
            slatNo = 1,
            muntinNo = id.toIntOrNull() ?: 1,
            axis = Axis.HORIZONTAL,
            lengthMm = length,
            leftAngleDeg = 90.0,
            rightAngleDeg = 90.0,
            qty = 1,
            profileName = profile,
            description = "Test Item $id",
            notes = ""
        )
    }

    @Test
    fun `optimize multiple items fitting in one bar`() {
        val items = listOf(
            createItem(2000.0),
            createItem(2000.0),
            createItem(1000.0)
        )
        // 2000+3 + 2000+3 + 1000+3 = 5009 <= 6000
        val result = CuttingOptimizer.optimize(items, stockLengthMm = 6000.0, sawWidthMm = 3.0)
        
        assertEquals(1, result.bars.size)
        assertEquals(3, result.bars[0].cuts.size)
        // Remaining: 6000 - 5009 = 991
        assertEquals(991.0, result.bars[0].remainingMm, 0.001)
    }

    @Test
    fun `optimize items requiring multiple bars`() {
        val items = listOf(
            createItem(4000.0),
            createItem(4000.0)
        )
        // 4000+3 = 4003. Can fit one per bar.
        val result = CuttingOptimizer.optimize(items, stockLengthMm = 6000.0, sawWidthMm = 3.0)
        
        assertEquals(2, result.bars.size)
        assertEquals(1, result.bars[0].cuts.size)
        assertEquals(1, result.bars[1].cuts.size)
    }

    @Test
    fun `optimize with mixed profiles`() {
        val items = listOf(
            createItem(1000.0, profile = "A"),
            createItem(1000.0, profile = "B")
        )
        val result = CuttingOptimizer.optimize(items, stockLengthMm = 6000.0, sawWidthMm = 3.0)
        
        assertEquals(2, result.bars.size)
    }
}

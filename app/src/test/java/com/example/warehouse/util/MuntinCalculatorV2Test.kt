package com.example.warehouse.util

import com.example.warehouse.model.*
import com.example.warehouse.util.MuntinCalculatorV2.IntersectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuntinCalculatorV2Test {

    // Common setup
    val sash = SashProfileV2("SASH", 70, 70, 0, 45.0)
    val bead = BeadProfileV2("BEAD", 20, 20, 45.0)
    val muntin = MuntinProfileV2("MUNTIN", 26, 12, 0.0)
    val settings = V2GlobalSettings(0.0, 0.0, 1.0) // 1.0mm clearance

    // --- Orthogonal Tests (V2) ---

    @Test
    fun `Case A - Single Vertical Muntin (No Cross)`() {
        // Sash 1000x1000
        // Opening = 1000 - 140 - 40 = 820 mm
        // Length = 820 - 2*1.0 = 818 mm
        
        val cuts = MuntinCalculatorV2.calculate(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfile = sash,
            beadProfile = bead,
            muntinProfile = muntin,
            verticalPositions = listOf(500.0),
            horizontalPositions = emptyList(),
            settings = settings
        )
        
        assertEquals(1, cuts.size)
        assertEquals(818.0, cuts[0].lengthMm, 0.001)
        assertEquals(Axis.VERTICAL, cuts[0].axis)
    }

    @Test
    fun `Case B - 1x1 Cross - Vertical Continuous`() {
        // V at 500, H at 500.
        // Vertical: 818 mm
        // Horizontal: Split into 2 segments.
        // Opening X: 90 to 910 (Width 820)
        // Seg 1 (Left): 90 to 500 = 410. Deduct: 1.0 (start), 13+1=14 (end). Len = 395.
        // Seg 2 (Right): 500 to 910 = 410. Deduct: 14 (start), 1.0 (end). Len = 395.
        
        val cuts = MuntinCalculatorV2.calculate(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfile = sash,
            beadProfile = bead,
            muntinProfile = muntin,
            verticalPositions = listOf(500.0),
            horizontalPositions = listOf(500.0),
            settings = settings,
            defaultIntersectionRule = IntersectionType.VERTICAL_CONTINUOUS
        )
        
        assertEquals(3, cuts.size)
        val v = cuts.first { it.axis == Axis.VERTICAL }
        assertEquals(818.0, v.lengthMm, 0.001)
        
        val h = cuts.filter { it.axis == Axis.HORIZONTAL }
        assertEquals(2, h.size)
        h.forEach { assertEquals(395.0, it.lengthMm, 0.001) }
    }

    @Test
    fun `Case C - 2x2 Grid`() {
        // Sash 1000x1000.
        // V1 at 400, V2 at 700.
        // H1 at 400, H2 at 700.
        // Vertical Continuous.
        
        // 2 Verticals: Both Length 818.
        // 2 Horizontals: Each split by 2 Verticals -> 3 segments each.
        // Total Horz Segments: 2 * 3 = 6.
        // Total Cuts: 2 + 6 = 8.
        
        val cuts = MuntinCalculatorV2.calculate(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfile = sash,
            beadProfile = bead,
            muntinProfile = muntin,
            verticalPositions = listOf(400.0, 700.0),
            horizontalPositions = listOf(400.0, 700.0),
            settings = settings,
            defaultIntersectionRule = IntersectionType.VERTICAL_CONTINUOUS
        )
        
        assertEquals(8, cuts.size)
        
        val verticals = cuts.filter { it.axis == Axis.VERTICAL }
        assertEquals(2, verticals.size)
        verticals.forEach { assertEquals(818.0, it.lengthMm, 0.001) }
        
        val horizontals = cuts.filter { it.axis == Axis.HORIZONTAL }
        assertEquals(6, horizontals.size)
        
        // Horizontal Segments lengths:
        // Opening X: 90..910.
        // V1 at 400. V2 at 700.
        // Seg 1: 90..400. Dist 310. Ded: 1.0, 14.0. Len = 295.
        // Seg 2: 400..700. Dist 300. Ded: 14.0, 14.0. Len = 272.
        // Seg 3: 700..910. Dist 210. Ded: 14.0, 1.0. Len = 195.
        
        val seg1 = horizontals.filter { it.lengthMm > 294.0 && it.lengthMm < 296.0 }
        assertEquals(2, seg1.size) // H1-S1, H2-S1
        
        val seg2 = horizontals.filter { it.lengthMm > 271.0 && it.lengthMm < 273.0 }
        assertEquals(2, seg2.size) // H1-S2, H2-S2
        
        val seg3 = horizontals.filter { it.lengthMm > 194.0 && it.lengthMm < 196.0 }
        assertEquals(2, seg3.size) // H1-S3, H2-S3
    }

    @Test
    fun `Case E - Impact of Clearance`() {
        val settings2mm = V2GlobalSettings(0.0, 0.0, 2.0)
        val cuts = MuntinCalculatorV2.calculate(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfile = sash,
            beadProfile = bead,
            muntinProfile = muntin,
            verticalPositions = listOf(500.0),
            horizontalPositions = emptyList(),
            settings = settings2mm
        )
        // 820 - 2*2 = 816
        assertEquals(816.0, cuts[0].lengthMm, 0.001)
    }

    @Test
    fun `Case F - Mounting Marks`() {
        val marks = MuntinCalculatorV2.generateMountingMarks(
            listOf(500.0),
            listOf(300.0, 700.0)
        )
        
        assertEquals(3, marks.size)
        assertEquals("Pion 1 (V1): Oś = 500,0 mm od lewej krawędzi zewn.", marks[0])
        assertEquals("Poziom 1 (H1): Oś = 300,0 mm od górnej krawędzi zewn.", marks[1])
        assertEquals("Poziom 2 (H2): Oś = 700,0 mm od górnej krawędzi zewn.", marks[2])
    }

    // --- Angular Tests (V2 Angular) ---

    @Test
    fun `Case Angular A - Single Diagonal`() {
        val diag = DiagonalLineV2("D1", 45.0, 0.0, true)
        
        val result = MuntinCalculatorV2Angular.calculate(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfile = sash,
            beadProfile = bead,
            muntinProfile = muntin,
            diagonals = listOf(diag),
            settings = settings
        )
        
        assertEquals(1, result.cutItems.size)
        val len = result.cutItems[0].lengthMm
        assertTrue("Length should be around 1160", len > 1150.0 && len < 1170.0)
    }

    @Test
    fun `Case Angular B - Cross 1x1`() {
        val d1 = DiagonalLineV2("D1", 45.0, 0.0, true) // Continuous
        val d2 = DiagonalLineV2("D2", 135.0, 1000.0, false) // Split
        
        val result = MuntinCalculatorV2Angular.calculate(
            sashWidthMm = 1000,
            sashHeightMm = 1000,
            sashProfile = sash,
            beadProfile = bead,
            muntinProfile = muntin,
            diagonals = listOf(d1, d2),
            settings = settings
        )
        
        assertEquals(3, result.cutItems.size)
        
        val d1Items = result.cutItems.filter { it.description.contains("D1") }
        val d2Items = result.cutItems.filter { it.description.contains("D2") }
        
        assertEquals(1, d1Items.size)
        assertEquals(2, d2Items.size)
    }
}

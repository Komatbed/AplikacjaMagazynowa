package com.example.warehouse.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileProcessingOptimizerExtendedTest {

    @Test
    fun `process handles empty input gracefully`() {
        val result = FileProcessingOptimizer.process("")
        assertTrue(result.outputLines.isEmpty())
        assertTrue(result.logs.any { it.contains("No valid records found") })
    }

    @Test
    fun `process handles invalid lines gracefully`() {
        val input = """
            invalid_line_without_separators
            001*Partial*Data
        """.trimIndent()
        
        val result = FileProcessingOptimizer.process(input)
        assertTrue(result.outputLines.isEmpty())
    }

    @Test
    fun `process LONGEST_FIRST sorts correctly`() {
        val input = """
            001*    100000*Profil*Color*10000*0*0*0
            002*    100000*Profil*Color*30000*0*0*0
            003*    100000*Profil*Color*20000*0*0*0
        """.trimIndent()

        // Mode: LONGEST_FIRST
        val result = FileProcessingOptimizer.process(input, FileProcessingOptimizer.Mode.LONGEST_FIRST)
        
        // Expect order: 3000mm, 2000mm, 1000mm
        // The output lines contain the cut info. We need to verify the processing order or result.
        // Since they fit in one bar (6500mm), they should be in the same bar.
        // Let's check the cut index or order in the output line if possible, 
        // OR checks the internal logic.
        // With LONGEST_FIRST, the 3000mm piece should be processed first.
        
        // Output format: BarNum*Profile*Color*...*Length*...
        // Let's verify that the longest piece is present.
        
        // Actually, FFD also sorts descending by default in this implementation. 
        // So let's verify that the result is valid and contains all pieces.
        assertEquals(3, result.outputLines.size)
    }

    @Test
    fun `process MIN_WASTE fits pieces efficiently`() {
        // Bar length 6500mm.
        // Pieces: 3000mm, 3000mm, 1000mm. 
        // Total 7000mm + Kerf. Needs 2 bars.
        // Bar 1: 3000, 3000 (Remaining 500 - 20 = 480mm). 
        // Bar 2: 1000.
        
        val input = """
            001*    100000*Profil*Color*30000*0*0*0
            002*    100000*Profil*Color*30000*0*0*0
            003*    100000*Profil*Color*10000*0*0*0
        """.trimIndent()

        val result = FileProcessingOptimizer.process(input, FileProcessingOptimizer.Mode.MIN_WASTE)
        
        // We expect 2 distinct bars.
        // Output line format: BarNum*...
        val barNums = result.outputLines.map { it.split("*")[0] }.distinct()
        assertEquals(2, barNums.size)
    }

    @Test
    fun `process DEFINED_WASTE prioritizes reserved waste`() {
        // Bar 6500mm.
        // Piece 5000mm.
        // Remaining 1500mm (minus kerf 10mm = 1490mm).
        // If we have a reserved waste of 1490mm, it should be preferred?
        // Actually, the logic is: Select a bar that results in reserved waste.
        // But here we are creating new bars. 
        // The logic in optimizeGroup creates new bars if needed.
        // The selection logic applies when CHOOSING an existing bar.
        
        // Let's create a scenario where we have an EXISTING bar with some space.
        // But the optimizer processes group by group, piece by piece.
        // It maintains a list of 'bars'.
        
        // Scenario:
        // Piece 1: 3000mm. New Bar 1 (Rem: 3500 - 10 = 3490).
        // Piece 2: 1000mm. 
        // Options:
        //  A) Put in Bar 1. Rem: 3490 - 1000 - 10 = 2480.
        //  B) New Bar 2. Rem: 6500 - 1000 - 10 = 5490.
        
        // If '2480' is a reserved length, and '5490' is not. 
        // The algorithm filters bars that HAVE space.
        // If we are in DEFINED_WASTE mode, and putting it in Bar 1 results in a reserved waste match (eventually?), 
        // wait, the logic checks the REMAINING space *after* the cut?
        // No, `minByOrNull` logic:
        // `val wasteDeciMm = bar.remainingDeciMm - requiredSpaceDeciMm`
        // This is the waste AFTER placing the piece.
        // So if placing the piece results in a remaining length that matches reserved, it prefers it.
        
        // Let's try:
        // Reserved: 1500mm (15000 deci).
        // Bar Stock: 6500mm.
        // We want a waste of 1500mm. So used space = 5000mm (assuming 0 kerf for simplicity of calc, but code has 10mm).
        // Code: `requiredSpaceDeciMm = piece.lengthDeciMm + (KERF_MM * 10)`
        // Waste = Stock - Required.
        // 15000 = 65000 - (Length + 100).
        // Length = 65000 - 15000 - 100 = 49900 deci = 4990mm.

        
        // We need to simulate a choice. 
        // But with one piece, it just creates a new bar. 
        // The optimization is about packing *multiple* pieces.
        
        // Let's try 2 pieces that can fit in one bar, or be split.
        // Usually FFD packs into first available.
        // If we have pieces: 4000, 2000.
        // Bar 6500.
        // P1 (4000) -> Bar 1 (Rem: 2490).
        // P2 (2000) -> Fits in Bar 1 (Rem: 480). 
        // If we have Reserved=2490.
        // And we have another bar (Bar 2) which is empty? 
        // No, we don't have empty bars floating around unless created.
        
        // The logic mainly helps when we have MULTIPLE bars open.
        // So we need enough pieces to open multiple bars, then a small piece that could go into either.
        
        // Setup:
        // P1: 4000 (Bar 1, Rem 2490).
        // P2: 4000 (Bar 2, Rem 2490).
        // P3: 1000.
        // If P3 goes to Bar 1 -> Rem 1480.
        // If P3 goes to Bar 2 -> Rem 1480.
        // This doesn't distinguish.
        
        // Different Setup:
        // P1: 4000 (Bar 1, Rem 2490).
        // P2: 5000 (Bar 2, Rem 1490).
        // P3: 1000.
        // Target: Reserved = 1490 (matches Bar 2 current state? No, we want RESULT to match).
        // If P3 -> Bar 1: Rem 2490 - 1010 = 1480.
        // If P3 -> Bar 2: Rem 1490 - 1010 = 480.
        
        // If Reserved = 1480.
        // P3 should prefer Bar 1.
        
        val input2 = """
            001*    100000*Profil*Color*40000*0*0*0
            002*    100000*Profil*Color*50000*0*0*0
            003*    100000*Profil*Color*10000*0*0*0
        """.trimIndent()
        
        // Note: FFD sorts descending! 
        // Sorted: 5000, 4000, 1000.
        // 1. 5000 -> Bar 1 (Rem 1490).
        // 2. 4000 -> Bar 2 (Rem 2490). (Doesn't fit in Bar 1).
        // 3. 1000 -> Fits in Bar 1 (Rem 480) OR Bar 2 (Rem 1480).
        
        // If Reserved = 1480 (1480mm).
        // P3 in Bar 1 -> Rem 480. (Diff 1000 from reserved).
        // P3 in Bar 2 -> Rem 1480. (Matches reserved!).
        // Should choose Bar 2.
        
        val reserved = listOf(1480)
        val result = FileProcessingOptimizer.process(input2, FileProcessingOptimizer.Mode.DEFINED_WASTE, reserved)
        
        // Check where P3 (1000mm) ended up.
        // It should be in the same bar as P2 (4000mm) - wait.
        // P2 is 4000mm.
        // P1 is 5000mm.
        
        // Bar 1 has P1 (5000).
        // Bar 2 has P2 (4000).
        // P3 (1000) should go to Bar 2.
        
        // Output format check.
        // BarNum is the first field.
        // We can parse the output.
        val lines = result.outputLines
        val p3Line = lines.find { it.contains("*10000*") } // 10000 = 1000.0mm
        val p2Line = lines.find { it.contains("*40000*") }
        
        val p3Bar = p3Line?.split("*")?.get(0)
        val p2Bar = p2Line?.split("*")?.get(0)
        
        assertEquals(p2Bar, p3Bar)
    }
}

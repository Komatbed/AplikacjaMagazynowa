package com.example.warehouse.util

import org.junit.Test
import org.junit.Assert.*

class FileProcessingOptimizerTest {

    @Test
    fun `test basic processing`() {
        val input = """
            001*    101290*Ciemnoszary*6500*09020*45*45*00000
            002*    101290*Ciemnoszary*6500*20000*90*90*00000
            003*    101290*Ciemnoszary*6500*20000*90*90*00000
            004*    101290*Ciemnoszary*6500*20000*90*90*00000
        """.trimIndent()

        val result = FileProcessingOptimizer.process(input, FileProcessingOptimizer.Mode.MIN_WASTE)

        println("Logs: ${result.logs}")
        result.outputLines.forEach { println(it) }

        // Assertions
        assertFalse(result.outputLines.isEmpty())
        
        // Check for waste comment
        val hasWasteComment = result.outputLines.any { it.contains("odpad=") }
        assertTrue("Should have waste comment", hasWasteComment)
    }

    @Test
    fun `test defined waste mode prefers reserved length`() {
        // Scenario:
        // Stock 6500mm (65000)
        // Kerf 10mm (100)
        
        // Piece 1: Leaves ~2000mm. 
        // 65000 - (44900 + 100) = 20000.
        // Piece 2: Leaves ~1500mm.
        // 65000 - (49900 + 100) = 15000.
        
        // Piece 3: 4900 + 100 = 5000.
        // If in Bar 1: 20000 - 5000 = 15000 (1500mm).
        // If in Bar 2: 15000 - 5000 = 10000 (1000mm).
        
        // Reserved: 1500mm.
        // MIN_WASTE would choose Bar 2 (1000 < 1500).
        // DEFINED_WASTE should choose Bar 1 (matches 1500).
        
        val input = """
            1*P1*C1*6500*44900*0*0*0
            2*P1*C1*6500*49900*0*0*0
            3*P1*C1*6500*04900*0*0*0
        """.trimIndent()
        
        // Test MIN_WASTE first
        val resMin = FileProcessingOptimizer.process(input, FileProcessingOptimizer.Mode.MIN_WASTE)
        
        // Check MIN_WASTE
        // Bar 1 should have 2 cuts. Bar 2 should have 1 cut.
        val barCountsMin = resMin.outputLines.map { it.split("*")[0] }.groupingBy { it }.eachCount()
        
        // Test DEFINED_WASTE
        val reserved = listOf(1500)
        val resDef = FileProcessingOptimizer.process(input, FileProcessingOptimizer.Mode.DEFINED_WASTE, reserved)
        
        val barCountsDef = resDef.outputLines.map { it.split("*")[0] }.groupingBy { it }.eachCount()
        
        println("MIN_WASTE counts: $barCountsMin")
        println("DEFINED_WASTE counts: $barCountsDef")
        
        // Verify Logic
        // With Min Waste, piece 3 goes to Bar 2 (1000 < 1500).
        // Bar 1 (44900) -> Rem 20000.
        // Bar 2 (49900) -> Rem 15000.
        // Piece 3 (5000). 
        // Bar 1 -> Rem 15000.
        // Bar 2 -> Rem 10000.
        // Min Waste picks Bar 2 (10000 remaining).
        // So Bar 2 has 2 pieces. Bar 1 has 1 piece.
        // Since sorting puts longest first:
        // 1. 49900 (Bar 1). Rem 15000.
        // 2. 44900 (Bar 2). Rem 20000.
        // 3. 4900.
        //    Bar 1 -> 10000.
        //    Bar 2 -> 15000.
        //    Min Waste picks Bar 1 (10000).
        //    Defined Waste (reserved 1500) picks Bar 2 (15000 is reserved).
        
        // Wait, FFD sorting order matters.
        // Input: 44900, 49900, 04900.
        // Sorted: 49900, 44900, 04900.
        // Bar 1: 49900 (Rem 15000).
        // Bar 2: 44900 (Rem 20000).
        // Piece 3: 4900.
        // Into Bar 1 -> Rem 10000.
        // Into Bar 2 -> Rem 15000.
        
        // MIN_WASTE prefers Bar 1 (10000 < 15000).
        // So Bar 1 has 2 pieces. Bar 2 has 1 piece.
        assertEquals(2, barCountsMin["001"]?.toInt())
        assertEquals(1, barCountsMin["002"]?.toInt())
        
        // DEFINED_WASTE prefers Bar 2 (15000 is reserved).
        // So Bar 2 has 2 pieces. Bar 1 has 1 piece.
        assertEquals(1, barCountsDef["001"]?.toInt())
        assertEquals(2, barCountsDef["002"]?.toInt())
    }

    @Test
    fun `test sorting and optimization`() {
        val input = """
            1*P1*C1*6500*40000*0*0*0
            2*P1*C1*6500*40000*0*0*0
        """.trimIndent()
        
        val result = FileProcessingOptimizer.process(input)
        assertEquals(2, result.outputLines.size)
        assertTrue(result.summary.contains("Used 2 bars"))
    }
}

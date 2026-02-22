package com.example.warehouse.features.muntins_v3.calculations

import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AssemblyInstructionsTest {

    @Test
    fun `test instructions sorting order`() {
        val glassWidth = 1000.0
        val glassHeight = 1000.0
        
        // Create 3 segments:
        // 1. Long vertical (1000mm)
        // 2. Short horizontal (487mm)
        // 3. Medium horizontal (500mm - hypothetical)
        
        val longSeg = Segment(
            id = UUID.randomUUID().toString(),
            startNode = Node(500.0, 0.0),
            endNode = Node(500.0, 1000.0),
            width = 26.0
        )
        
        val shortSeg = Segment(
            id = UUID.randomUUID().toString(),
            startNode = Node(0.0, 500.0),
            endNode = Node(487.0, 500.0),
            width = 26.0
        )

        val segments = listOf(shortSeg, longSeg) // Unsorted input

        val instructions = AssemblyInstructionCalculator.generateInstructions(segments, glassWidth, glassHeight)

        assertEquals(2, instructions.size)
        
        // Check order: Longest first
        assertEquals(longSeg.id, instructions[0].segment.id)
        assertEquals(shortSeg.id, instructions[1].segment.id)
        
        // Check indices
        assertEquals(1, instructions[0].orderIndex)
        assertEquals(2, instructions[1].orderIndex)
    }

    @Test
    fun `test position descriptions`() {
        val glassWidth = 1000.0
        val glassHeight = 1000.0
        
        val vertical = Segment(
            id = "v1",
            startNode = Node(300.0, 0.0),
            endNode = Node(300.0, 1000.0),
            width = 26.0
        )
        
        val horizontal = Segment(
            id = "h1",
            startNode = Node(0.0, 600.0),
            endNode = Node(1000.0, 600.0),
            width = 26.0
        )

        val instructions = AssemblyInstructionCalculator.generateInstructions(listOf(vertical, horizontal), glassWidth, glassHeight)
        
        // Find vertical instruction
        val vInstr = instructions.find { it.segment.id == "v1" }!!
        assertEquals("Oś: 30.0 mm od lewej krawędzi skrzydła", vInstr.positionLabel)
        assertTrue(vInstr.description.contains("PION"))

        // Find horizontal instruction
        val hInstr = instructions.find { it.segment.id == "h1" }!!
        // Y=600 from top means 400 from bottom (40.0 in current scaling)
        assertEquals("Oś: 40.0 mm od dolnej krawędzi skrzydła", hInstr.positionLabel)
        assertTrue(hInstr.description.contains("POZIOM"))
    }
}

package com.example.warehouse.features.muntins_v3.calculations

import com.example.warehouse.features.muntins_v3.geometry.Segment
import kotlin.math.abs
import kotlin.math.roundToInt

data class AssemblyStep(
    val orderIndex: Int,
    val segment: Segment,
    val length: Double,
    val description: String,
    val positionLabel: String
)

object AssemblyInstructionCalculator {

    fun generateInstructions(
        segments: List<Segment>,
        glassWidth: Double,
        glassHeight: Double,
        presetType: String? = null
    ): List<AssemblyStep> {
        if (presetType == "DIAMOND") {
            return generateDiamondInstructions(segments, glassWidth, glassHeight)
        }

        // 1. Calculate lengths for all segments first to sort them
        val segmentsWithLengths: List<Pair<Segment, Double>> = segments.map { segment ->
            // Re-use logic from CutListCalculator to get real length
            // We need boundaries. For instruction purposes, we can assume
            // the same boundary logic applies.
            // However, to avoid circular dependency or code duplication, 
            // we might ideally want CutListCalculator to return detailed info per segment.
            // For now, we will approximate or re-calculate.
            
            // Let's use a simplified length for sorting (geometric length is close enough for sorting order)
            val dx = segment.endNode.x - segment.startNode.x
            val dy = segment.endNode.y - segment.startNode.y
            val geoLength = kotlin.math.sqrt(dx * dx + dy * dy)
            segment to geoLength
        }

        // 2. Sort: Longest first
        // TODO: Refine logic if "Through bars" need priority over just "Longest"
        val sortedSegments = segmentsWithLengths.sortedByDescending { it.second }

        // 3. Generate Steps
        return sortedSegments.mapIndexed { index, pair ->
            val segment = pair.first
            val length = pair.second
            AssemblyStep(
                orderIndex = index + 1,
                segment = segment,
                length = (length * 10.0).roundToInt() / 10.0, // Display precision
                description = determineType(segment, glassWidth, glassHeight),
                positionLabel = formatPosition(segment, glassWidth, glassHeight)
            )
        }
    }

    private fun generateDiamondInstructions(
        segments: List<Segment>,
        width: Double,
        height: Double
    ): List<AssemblyStep> {
        val midX = width / 2.0
        val midY = height / 2.0
        
        // Sort segments to match expected order: Top-Right, Bottom-Right, Bottom-Left, Top-Left
        // Center of segment determines quadrant
        val ordered = segments.sortedBy { segment ->
            val cx = (segment.startNode.x + segment.endNode.x) / 2.0
            val cy = (segment.startNode.y + segment.endNode.y) / 2.0
            
            when {
                cx >= midX && cy < midY -> 1 // Top-Right
                cx >= midX && cy >= midY -> 2 // Bottom-Right
                cx < midX && cy >= midY -> 3 // Bottom-Left
                else -> 4 // Top-Left
            }
        }
        
        return ordered.mapIndexed { index, segment ->
             val dx = segment.endNode.x - segment.startNode.x
             val dy = segment.endNode.y - segment.startNode.y
             val length = kotlin.math.sqrt(dx * dx + dy * dy)
             
             val desc = when(index) {
                 0 -> "1. Prawy Górny"
                 1 -> "2. Prawy Dolny"
                 2 -> "3. Lewy Dolny"
                 3 -> "4. Lewy Górny"
                 else -> "Segment dodatkowy"
             }
             
             AssemblyStep(
                 orderIndex = index + 1,
                 segment = segment,
                 length = (length * 10.0).roundToInt() / 10.0,
                 description = desc,
                 positionLabel = "Zacznij od środka: X=${(midX*10).roundToInt()/10.0}, Y=${(midY*10).roundToInt()/10.0} (od góry)"
             )
        }
    }

    private fun determineType(segment: Segment, w: Double, h: Double): String {
        val isVertical = abs(segment.startNode.x - segment.endNode.x) < 0.1
        val isHorizontal = abs(segment.startNode.y - segment.endNode.y) < 0.1
        
        return when {
            isVertical -> "PION (Vertical)"
            isHorizontal -> "POZIOM (Horizontal)"
            else -> "SKOS (Angled)"
        }
    }

    private fun formatPosition(segment: Segment, w: Double, h: Double): String {
        val isVertical = abs(segment.startNode.x - segment.endNode.x) < 0.1
        val isHorizontal = abs(segment.startNode.y - segment.endNode.y) < 0.1

        return when {
            isVertical -> {
                // Distance from left
                val x = (segment.startNode.x * 10.0).roundToInt() / 10.0
                "Pozycja X: $x mm od lewej"
            }
            isHorizontal -> {
                // Distance from top (Canvas Y=0 is top) or bottom?
                // Spec says "643 mm od dołu" (from bottom).
                // Our Y=0 is top. So distance from bottom is Height - Y.
                val yFromTop = segment.startNode.y
                val yFromBottom = h - yFromTop
                val yDisplay = (yFromBottom * 10.0).roundToInt() / 10.0
                "Pozycja Y: $yDisplay mm od dołu"
            }
            else -> {
                // Angled
                val startX = (segment.startNode.x * 10.0).roundToInt() / 10.0
                val startY = ((h - segment.startNode.y) * 10.0).roundToInt() / 10.0
                "Start: ($startX, $startY) od lewej/dołu"
            }
        }
    }
}

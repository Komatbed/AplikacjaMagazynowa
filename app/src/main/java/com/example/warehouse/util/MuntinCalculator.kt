package com.example.warehouse.util

import kotlin.math.tan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.pow

object MuntinCalculator {

    data class MuntinRequest(
        val sashWidthMm: Int,
        val sashHeightMm: Int,
        val sashProfileHeightMm: Int,
        val beadHeightMm: Int,
        val beadAngleDeg: Double,
        val muntinWidthMm: Int,
        val muntinGapMm: Double = 1.0, // Gap at each connection
        val overlapBeadMm: Double = 0.0, // How much it overlaps the bead (nachodzenie)
        val isHalvingJoint: Boolean = false, // If true, muntins cross (overlap). If false, one is cut.
        val externalOffsetMm: Double = 0.0 // Offset for external muntins (e.g. +2mm or -2mm relative to internal)
    )

    data class MuntinResult(
        val verticalMuntinLength: Double,
        val horizontalMuntinLength: Double,
        val verticalCount: Int,
        val horizontalCount: Int,
        val verticalSegments: List<Double>, // If not continuous
        val horizontalSegments: List<Double>, // If not continuous
        val externalVerticalMuntinLength: Double,
        val externalHorizontalMuntinLength: Double,
        val externalVerticalSegments: List<Double>,
        val externalHorizontalSegments: List<Double>
    )

    /**
     * Calculates muntins for a standard rectangular grid.
     */
    fun calculateRectangularGrid(
        request: MuntinRequest,
        verticalFields: Int,   // Number of fields horizontally (so verticalFields - 1 lines)
        horizontalFields: Int  // Number of fields vertically (so horizontalFields - 1 lines)
    ): MuntinResult {
        
        // --- Internal Calculation ---
        val internalResult = calculateSingleSide(request, verticalFields, horizontalFields, 0.0)
        
        // --- External Calculation ---
        // Apply offset to lengths? 
        // User said: "narazie jako wynik działan poprzednich +- ustalony offset"
        // If offset is positive, external is longer? Or just shifted?
        // Usually external muntin might be shorter or longer depending on profile geometry.
        // Let's assume the offset is added to the FINAL LENGTHS.
        
        val externalOffset = request.externalOffsetMm
        
        return MuntinResult(
            verticalMuntinLength = internalResult.verticalMuntinLength,
            horizontalMuntinLength = internalResult.horizontalMuntinLength,
            verticalCount = internalResult.verticalCount,
            horizontalCount = internalResult.horizontalCount,
            verticalSegments = internalResult.verticalSegments,
            horizontalSegments = internalResult.horizontalSegments,
            
            externalVerticalMuntinLength = if (internalResult.verticalMuntinLength > 0) internalResult.verticalMuntinLength + externalOffset else 0.0,
            externalHorizontalMuntinLength = if (internalResult.horizontalMuntinLength > 0) internalResult.horizontalMuntinLength + externalOffset else 0.0,
            externalVerticalSegments = internalResult.verticalSegments.map { it + externalOffset },
            externalHorizontalSegments = internalResult.horizontalSegments.map { it + externalOffset }
        )
    }

    private fun calculateSingleSide(
        request: MuntinRequest,
        verticalFields: Int,
        horizontalFields: Int,
        lengthOffset: Double
    ): MuntinResult {
        // 1. Calculate Visible Glass Dimensions
        // Visible Width = SashWidth - 2 * (ProfileHeight - (BeadHeight/tan(alpha))?)
        // Actually, usually:
        // Glass Opening = SashOuter - 2 * ProfileHeight.
        // Visible Glass = Glass Opening - 2 * BeadProjection.
        
        // Bead Projection calculation:
        // Assuming 90deg corner for profile/bead interaction is simplified.
        // Projection = BeadHeight / tan(Angle).
        val beadProjection = if (request.beadAngleDeg > 0) {
            request.beadHeightMm / tan(Math.toRadians(request.beadAngleDeg))
        } else 0.0
        
        val glassWidth = request.sashWidthMm - 2 * request.sashProfileHeightMm
        val glassHeight = request.sashHeightMm - 2 * request.sashProfileHeightMm
        
        val visibleWidth = glassWidth - 2 * beadProjection
        val visibleHeight = glassHeight - 2 * beadProjection
        
        // Effective Lengths (including overlap on beads)
        // If overlap > 0, the muntin is longer than visible glass.
        // But it can't be longer than Glass Opening usually (unless it sits on profile).
        // Let's assume it sits on glass, so max length = GlassWidth.
        
        // Length = Visible + 2 * Overlap.
        val baseVerticalLen = visibleHeight + 2 * request.overlapBeadMm - 2 * request.muntinGapMm
        val baseHorizontalLen = visibleWidth + 2 * request.overlapBeadMm - 2 * request.muntinGapMm
        
        val numVerticalLines = if (verticalFields > 1) verticalFields - 1 else 0
        val numHorizontalLines = if (horizontalFields > 1) horizontalFields - 1 else 0
        
        val vSegments = mutableListOf<Double>()
        val hSegments = mutableListOf<Double>()
        
        if (request.isHalvingJoint) {
            // Both are full length (crossing each other)
            // Just subtract gaps if they apply to the frame connection.
            // Usually halving joint means they interlock, so length is full span.
            
            repeat(numVerticalLines) { vSegments.add(baseVerticalLen + lengthOffset) }
            repeat(numHorizontalLines) { hSegments.add(baseHorizontalLen + lengthOffset) }
            
        } else {
            // Butt Joint.
            // Usually Vertical is continuous (dominating), Horizontal is cut.
            // Or vice versa. Let's assume Vertical is continuous.
            
            // Verticals are full length
            repeat(numVerticalLines) { vSegments.add(baseVerticalLen + lengthOffset) }
            
            // Horizontals are cut into segments.
            // Number of segments per horizontal line = verticalFields.
            // Total width available for segments = baseHorizontalLen - (numVerticalLines * MuntinWidth).
            // Segment Length = Total / verticalFields.
            
            // Wait, baseHorizontalLen includes overlap on beads.
            // The inner segments don't overlap beads.
            // Only end segments overlap beads.
            
            // Let's refine segment calculation.
            // Total Visible Width is divided.
            // Segment Length = (VisibleWidth - (numVerticalLines * MuntinWidth)) / verticalFields.
            // Each segment needs gaps?
            // Yes, 1mm gap on each connection.
            
            val totalMuntinWidth = numVerticalLines * request.muntinWidthMm
            // gaps at each muntin side + frame sides?
            // Actually, gap is usually per cut.
            
            // Let's do it per segment type.
            
            if (numHorizontalLines > 0) {
                // We have horizontal lines. Each line consists of 'verticalFields' segments.
                // 1. End segments (touch frame + muntin).
                // 2. Middle segments (touch muntin + muntin).
                
                // Segment pure length (without gaps/overlaps)
                val rawSegmentW = (visibleWidth - totalMuntinWidth) / verticalFields
                
                repeat(numHorizontalLines) {
                    // For each line:
                    // Left End Segment
                    val leftLen = rawSegmentW + request.overlapBeadMm - request.muntinGapMm + lengthOffset // Overlap frame, gap at muntin
                    hSegments.add(leftLen)
                    
                    // Middle Segments
                    if (verticalFields > 2) {
                        val middleLen = rawSegmentW - 2 * request.muntinGapMm + lengthOffset // Gap at both ends
                        repeat(verticalFields - 2) { hSegments.add(middleLen) }
                    }
                    
                    // Right End Segment
                    if (verticalFields >= 2) {
                         val rightLen = rawSegmentW + request.overlapBeadMm - request.muntinGapMm + lengthOffset
                         hSegments.add(rightLen)
                    } else {
                        // Special case: 1 field? No, then numVerticalLines=0.
                        // If verticalFields=1, then no verticals.
                    }
                }
            }
        }

        return MuntinResult(
            verticalMuntinLength = if (vSegments.isNotEmpty()) vSegments[0] else 0.0, // Representative
            horizontalMuntinLength = if (hSegments.isNotEmpty()) hSegments[0] else 0.0,
            verticalCount = numVerticalLines,
            horizontalCount = numHorizontalLines,
            verticalSegments = vSegments,
            horizontalSegments = hSegments,
            externalVerticalMuntinLength = 0.0,
            externalHorizontalMuntinLength = 0.0,
            externalVerticalSegments = emptyList(),
            externalHorizontalSegments = emptyList()
        )
    }
}

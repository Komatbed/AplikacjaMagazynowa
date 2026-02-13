package com.example.warehouse.features.muntins_v3.calculations

import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import com.example.warehouse.features.muntins_v3.calculations.MuntinV3Calculations.GeometryUtils
import kotlin.math.*

object SegmentCalculator {

    data class CutResult(
        val finalLength: Double,
        val cutAngleStart: Double,
        val cutAngleEnd: Double,
        val startPoint: Node,
        val endPoint: Node
    )

    /**
     * Calculates the "Production Length" (Real Cut Length) for a segment.
     * 
     * Logic:
     * 1. Determine the axis line of the segment.
     * 2. Determine the "boundary" lines at both ends (e.g., the edge of the frame/bead OR the edge of a crossing muntin).
     * 3. Calculate intersections of the segment axis with these boundary lines. -> These are the "Axis Endpoints" trimmed.
     * 4. BUT, we need "EDGE TO EDGE". This usually means the longest dimension of the trapezoid formed by the cut.
     * 
     * Simplified approach for V3 "Master Prompt":
     * L_final = edge_distance + angle_correction_start + angle_correction_end - clearance_total
     * 
     * @param segment The segment to calculate (axis).
     * @param startBoundaryLine The line (p1, p2) representing the edge limit at the start.
     * @param endBoundaryLine The line (p1, p2) representing the edge limit at the end.
     * @param clearanceTotal Gap deduction (e.g. 1mm per joint -> 2mm total? Prompt says "1 mm na każdy punkt styku").
     */
    fun calculateRealLength(
        segment: Segment,
        startBoundaryLine: Pair<Node, Node>,
        endBoundaryLine: Pair<Node, Node>,
        clearanceStart: Double = 1.0,
        clearanceEnd: Double = 1.0
    ): CutResult {
        // 1. Find intersection of Axis with Start Boundary
        val startInt = GeometryUtils.getLineIntersection(
            segment.startNode, segment.endNode,
            startBoundaryLine.first, startBoundaryLine.second
        ) ?: segment.startNode // Fallback if no intersection (shouldn't happen in valid layout)

        // 2. Find intersection of Axis with End Boundary
        val endInt = GeometryUtils.getLineIntersection(
            segment.startNode, segment.endNode,
            endBoundaryLine.first, endBoundaryLine.second
        ) ?: segment.endNode

        // 3. Calculate basic edge-to-edge distance (along axis)
        // Wait, "edge_distance" in prompt might mean distance between the boundary planes.
        val axisLength = GeometryUtils.distance(startInt, endInt)

        // 4. Calculate Angles
        // Angle between Segment Axis and Boundary Line
        val angleStart = calculateIntersectionAngle(segment.startNode, segment.endNode, startBoundaryLine.first, startBoundaryLine.second)
        val angleEnd = calculateIntersectionAngle(segment.startNode, segment.endNode, endBoundaryLine.first, endBoundaryLine.second)

        // 5. Calculate Angle Corrections
        // If cut is not 90 degrees, the "Longest" side is longer than the axis length.
        // Correction = (width / 2) * cot(angle) ? OR (width / 2) * tan(90 - angle)?
        // If angle is 90, correction is 0.
        // If angle is 45, correction is width/2.
        
        // We need the acute angle of intersection to calculate the extra length needed for the tip.
        // length_correction = (muntin_thickness / 2) / tan(angle_radians)
        // Wait, "muntin_thickness" in prompt is "width" of the profile face?
        // Let's assume segment.width is the face width.
        
        val corrStart = calculateAngleCorrection(segment.width, angleStart)
        val corrEnd = calculateAngleCorrection(segment.width, angleEnd)

        // 6. Final Length
        val finalLength = axisLength + corrStart + corrEnd - (clearanceStart + clearanceEnd)

        return CutResult(
            finalLength = max(0.0, finalLength),
            cutAngleStart = angleStart,
            cutAngleEnd = angleEnd,
            startPoint = startInt,
            endPoint = endInt
        )
    }

    private fun calculateIntersectionAngle(p1: Node, p2: Node, b1: Node, b2: Node): Double {
        // Vector 1 (Segment)
        val dx1 = p2.x - p1.x
        val dy1 = p2.y - p1.y
        
        // Vector 2 (Boundary)
        val dx2 = b2.x - b1.x
        val dy2 = b2.y - b1.y

        val angle1 = atan2(dy1, dx1)
        val angle2 = atan2(dy2, dx2)

        var diffDeg = Math.toDegrees(abs(angle1 - angle2))
        // Normalize to 0-180
        if (diffDeg > 180) diffDeg = 360 - diffDeg
        // We usually want the angle between the line and the cut face, typically <= 90
        if (diffDeg > 90) diffDeg = 180 - diffDeg
        
        return diffDeg
    }

    /**
     * Calculates the extra length to add to the axis length to get the "Longest" tip.
     * @param width Width of the muntin profile.
     * @param angleDeg Angle of intersection (0-90).
     */
    private fun calculateAngleCorrection(width: Double, angleDeg: Double): Double {
        if (angleDeg < 1.0) return 0.0 // Parallel or degenerate
        if (angleDeg >= 89.9) return 0.0 // 90 degrees, no correction (square cut)

        val angleRad = Math.toRadians(angleDeg)
        // Triangle logic:
        // half_width = width / 2
        // correction = half_width / tan(angle)  -> This gives the projection along the axis
        // correction = half_width * cot(angle)
        return (width / 2.0) / tan(angleRad)
    }
}

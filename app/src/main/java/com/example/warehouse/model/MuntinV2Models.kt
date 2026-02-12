package com.example.warehouse.model

data class SashProfileV2(
    val profileNo: String,
    val widthMm: Int,
    val heightMm: Int,
    val outerConstructionAngleDeg: Double // Angle of the outer sash corner (usually 45 or 90)
)

data class BeadProfileV2(
    val profileNo: String,
    val widthMm: Int,
    val heightMm: Int,
    val innerBeadAngleDeg: Double // Angle of the bead miter (usually 45)
)

data class MuntinProfileV2(
    val profileNo: String,
    val widthMm: Int,
    val heightMm: Int,
    val wallAngleDeg: Double // Angle of the muntin wall (for geometric corrections)
)

data class V2GlobalSettings(
    val sawCorrectionMm: Double = 0.0,
    val windowCorrectionMm: Double = 0.0,
    val assemblyClearanceMm: Double = 1.0 // Default 1.0mm per joint side
)

data class CutItemV2(
    val sashNo: Int = 1,
    val slatNo: Int? = null, // e.g. 1, 2, 3...
    val muntinNo: Int? = null, // e.g. 1, 2, 3...
    val axis: Axis,
    val lengthMm: Double,
    val leftAngleDeg: Double,
    val rightAngleDeg: Double,
    val qty: Int = 1,
    val profileName: String,
    val description: String,
    val notes: String = ""
)

enum class Axis {
    VERTICAL, HORIZONTAL
}

data class MuntinNode(
    val id: String,
    val axis: Axis,
    val positionMm: Double, // Distance from Top (Horizontal) or Left (Vertical)
    val isContinuous: Boolean = false // If true, this muntin runs through intersections
)

// --- Angular / Slanted Mode Models ---

data class AngularModeConfigV2(
    val allowedAnglesDeg: List<Double> = listOf(15.0, 22.5, 30.0, 45.0, 60.0, 75.0, 90.0),
    val customAngleEnabled: Boolean = true
)

data class DiagonalLineV2(
    val lineId: String,
    val angleDeg: Double,
    val offsetRefMm: Double, // Offset from top-left corner along top edge (for positive angle) or bottom edge? 
                             // Prompt says "offsetRefMm". Let's assume X-intercept or specific reference point. 
                             // For simplicity in UI: "Distance from Top-Left along Top Edge" (X-axis) for lines starting at Top.
                             // Or we can define line by Point + Angle.
                             // Let's assume standard form: y = mx + c.
                             // But for UI "offsetRefMm" usually means starting point on the frame.
    val isContinuous: Boolean = false,
    val profileNo: String? = null
)

data class SpiderPatternV2(
    val centerX: Double,
    val centerY: Double,
    val armCount: Int,
    val startAngleDeg: Double,
    val ringCount: Int,
    val ringSpacingMm: Double
)

data class ArchPatternV2(
    // Option A: Radius
    val radiusMm: Double? = null,
    // Option B: Chord + Sagitta
    val chordMm: Double? = null,
    val sagittaMm: Double? = null,
    
    val arcStartDeg: Double,
    val arcEndDeg: Double,
    val divisionCount: Int,
    val withStraightJoins: Boolean
)

data class MountMarkV2(
    val itemId: String,
    val referenceEdge: String, // e.g. "Left", "Top", "Right", "Bottom"
    val offsetMm: Double, // Distance from the reference edge
    val axisDescription: String // e.g. "Center of Muntin"
)

package com.example.warehouse.features.muntins_v3.geometry

data class Node(
    val x: Double,
    val y: Double
)

enum class IntersectionType {
    BEAD_EDGE,
    MUNTIN_EDGE,
    T_JOINT
}

data class Intersection(
    val type: IntersectionType,
    // Additional properties might be needed for calculation (e.g., related segments)
)

data class Segment(
    val id: String,
    val startNode: Node,
    val endNode: Node,
    val width: Double,
    val angleStart: Double = 90.0, // Angle relative to something? Or the cut angle? 
    // Prompt says: "Każdy koniec: cut_angle source: BEAD_FACE / MUNTIN_FACE"
    // And "angleStart", "angleEnd" in Segment definition.
    val angleEnd: Double = 90.0,
    
    // Additional properties for logic
    val profileId: Long? = null
)

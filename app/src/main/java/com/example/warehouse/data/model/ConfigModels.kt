package com.example.warehouse.data.model

data class ProfileDefinition(
    val id: String? = null,
    val code: String,
    val description: String = "",
    val heightMm: Int = 0,
    val widthMm: Int = 0,
    val beadHeightMm: Int = 0,
    val beadAngle: Double = 0.0,
    val standardLengthMm: Int = 6500,
    val system: String = "",
    val manufacturer: String = "",
    val type: String = "OTHER"
)

data class ColorDefinition(
    val id: String? = null,
    val code: String,
    val description: String = "",
    val name: String = "",
    val paletteCode: String = "",
    val vekaCode: String = "",
    val type: String = "smooth",
    val foilManufacturer: String = ""
)

data class MuntinsV3ProfileConfig(
    val name: String,
    val glassOffsetX: Double,
    val glassOffsetY: Double,
    val outerConstructionAngleDeg: Double = 90.0
)

data class MuntinsV3BeadConfig(
    val name: String,
    val angleFace: Double,
    val effectiveGlassOffset: Double
)

data class MuntinsV3MuntinConfig(
    val name: String,
    val width: Double,
    val thickness: Double,
    val wallAngleDeg: Double = 90.0
)

data class MuntinsV3Config(
    val profiles: List<MuntinsV3ProfileConfig> = emptyList(),
    val beads: List<MuntinsV3BeadConfig> = emptyList(),
    val muntins: List<MuntinsV3MuntinConfig> = emptyList()
)

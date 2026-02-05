package com.example.warehouse.data.model

data class ProfileDefinition(
    val id: String? = null,
    val code: String,
    val description: String = "",
    val heightMm: Int = 0,
    val widthMm: Int = 0,
    val beadHeightMm: Int = 0,
    val beadAngle: Double = 0.0,
    val standardLengthMm: Int = 6500
)

data class ColorDefinition(
    val id: String? = null,
    val code: String,
    val description: String = "",
    val name: String = "",
    val paletteCode: String = "",
    val vekaCode: String = ""
)

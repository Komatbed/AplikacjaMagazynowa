package com.example.warehouse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_definitions")
data class ProfileEntity(
    @PrimaryKey
    val code: String, 
    
    @ColumnInfo(name = "id")
    val id: String, // Store UUID as String

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "height_mm")
    val heightMm: Int = 0,

    @ColumnInfo(name = "width_mm")
    val widthMm: Int = 0,

    @ColumnInfo(name = "bead_height_mm")
    val beadHeightMm: Int = 0,

    @ColumnInfo(name = "bead_angle")
    val beadAngle: Double = 0.0,
    
    @ColumnInfo(name = "standard_length_mm")
    val standardLengthMm: Int = 6500,

    @ColumnInfo(name = "system")
    val system: String = "",

    @ColumnInfo(name = "manufacturer")
    val manufacturer: String = "",

    @ColumnInfo(name = "type")
    val type: String = "OTHER"
)

@Entity(tableName = "color_definitions")
data class ColorEntity(
    @PrimaryKey
    val code: String,
    
    @ColumnInfo(name = "id")
    val id: String, // Store UUID as String

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "palette_code")
    val paletteCode: String = "",

    @ColumnInfo(name = "veka_code")
    val vekaCode: String = "",

    @ColumnInfo(name = "type")
    val type: String = "smooth",

    @ColumnInfo(name = "foil_manufacturer")
    val foilManufacturer: String = ""
)

@Entity(tableName = "core_color_rules")
data class CoreColorRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "ext_color_code")
    val extColorCode: String,

    @ColumnInfo(name = "core_color_code")
    val coreColorCode: String
)

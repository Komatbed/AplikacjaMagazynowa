package com.example.warehouse.features.muntins_v3.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "v3_profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "glass_offset_x")
    val glassOffsetX: Double,
    
    @ColumnInfo(name = "glass_offset_y")
    val glassOffsetY: Double,
    
    @ColumnInfo(name = "outer_construction_angle_deg")
    val outerConstructionAngleDeg: Double = 90.0
)

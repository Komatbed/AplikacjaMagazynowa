package com.example.warehouse.features.muntins_v3.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "v3_glass_beads")
data class GlassBeadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "angle_face")
    val angleFace: Double, // 10.0 - 40.0 degrees
    
    @ColumnInfo(name = "effective_glass_offset")
    val effectiveGlassOffset: Double
)

package com.example.warehouse.features.muntins_v3.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(
    tableName = "v3_projects",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = GlassBeadEntity::class,
            parentColumns = ["id"],
            childColumns = ["bead_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        androidx.room.Index(value = ["profile_id"]),
        androidx.room.Index(value = ["bead_id"])
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "frame_width")
    val frameWidth: Double,
    
    @ColumnInfo(name = "frame_height")
    val frameHeight: Double,
    
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    
    @ColumnInfo(name = "bead_id")
    val beadId: Long,
    
    @ColumnInfo(name = "name")
    val name: String = "New Project",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "manual_correction")
    val manualCorrection: Double = 0.0,

    @ColumnInfo(name = "comp_top")
    val compTop: Double = 0.0,

    @ColumnInfo(name = "comp_bottom")
    val compBottom: Double = 0.0,

    @ColumnInfo(name = "comp_left")
    val compLeft: Double = 0.0,

    @ColumnInfo(name = "comp_right")
    val compRight: Double = 0.0
)

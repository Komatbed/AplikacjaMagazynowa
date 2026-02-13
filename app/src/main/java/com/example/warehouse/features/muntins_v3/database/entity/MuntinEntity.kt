package com.example.warehouse.features.muntins_v3.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "v3_muntins")
data class MuntinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "width")
    val width: Double,
    
    @ColumnInfo(name = "thickness")
    val thickness: Double
)

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
    val description: String
)

@Entity(tableName = "color_definitions")
data class ColorEntity(
    @PrimaryKey
    val code: String,
    
    @ColumnInfo(name = "id")
    val id: String, // Store UUID as String

    @ColumnInfo(name = "description")
    val description: String
)

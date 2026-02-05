package com.example.warehouse.model

import jakarta.persistence.*

@Entity
@Table(name = "locations")
data class Location(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "row_number", nullable = false)
    val rowNumber: Int,

    @Column(name = "palette_number", nullable = false)
    val paletteNumber: Int,

    @Column(name = "label", nullable = false, unique = true, length = 10)
    val label: String, // e.g. "01A"

    @Column(name = "is_waste_palette")
    val isWastePalette: Boolean = false
)

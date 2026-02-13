package com.example.warehouse.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.util.UUID

@Entity
@Table(name = "profile_definitions")
data class ProfileDefinition(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @field:NotBlank(message = "Kod profilu jest wymagany")
    @Column(name = "code", nullable = false, unique = true)
    val code: String,

    @Column(name = "description")
    val description: String = "",

    // Dimensions for calculations (e.g. Muntins, Cutting)
    @Column(name = "height_mm")
    val heightMm: Int = 0,

    @Column(name = "width_mm")
    val widthMm: Int = 0,

    @Column(name = "bead_height_mm")
    val beadHeightMm: Int = 0,

    @Column(name = "bead_angle")
    val beadAngle: Double = 0.0,
    
    @Column(name = "standard_length_mm")
    val standardLengthMm: Int = 6500,

    @Column(name = "system")
    val system: String = "", // e.g. "Softline 82", "Veka 70"

    @Column(name = "manufacturer")
    val manufacturer: String = "", // e.g. "Veka", "Aluplast"

    @Column(name = "type")
    val type: String = "OTHER" // SASH, BEAD, MUNTIN, FRAME, OTHER
)

@Entity
@Table(name = "color_definitions")
data class ColorDefinition(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @field:NotBlank(message = "Kod koloru jest wymagany")
    @Column(name = "code", nullable = false, unique = true)
    val code: String, // e.g. RAL9016

    @Column(name = "description")
    val description: String = "",

    @Column(name = "name")
    val name: String = "",

    @Column(name = "palette_code")
    val paletteCode: String = "", // Numer z kolornika

    @Column(name = "veka_code")
    val vekaCode: String = "", // Numer wewnętrzny Veka

    @Column(name = "type")
    val type: String = "smooth", // wood, smooth, mat

    @Column(name = "foil_manufacturer")
    val foilManufacturer: String = "" // e.g. "Renolit", "Hornschuch"
)

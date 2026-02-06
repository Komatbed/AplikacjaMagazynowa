package com.example.warehouse.data.model

data class CatalogCategory(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int? = null // Optional drawable resource ID
)

data class CatalogProduct(
    val id: String,
    val categoryId: String,
    val name: String,
    val shortDescription: String,
    val fullDescription: String,
    val usage: String, // Zastosowanie
    val technicalDetails: String? = null,
    val imageUrl: String? = null // Placeholder for image URL or resource name
)

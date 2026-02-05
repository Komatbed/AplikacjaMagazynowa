package com.example.warehouse.model

data class OcrResult(
    val rawText: String,
    val parsedData: ParsedData?,
    val error: String? = null
)

data class ParsedData(
    val producer: String?,
    val profileCode: String?,
    val lengthMm: Int?,
    val color: String?
)

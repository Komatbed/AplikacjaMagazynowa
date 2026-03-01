package com.example.warehouse.util

import android.content.Context
import android.net.Uri
import com.example.warehouse.model.OcrResult
import com.example.warehouse.model.ParsedData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException

class OcrProcessor(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient()
    
    private val colorMap: Map<String, String> by lazy {
        try {
            val jsonString = context.assets.open("initial_data/color_abbreviations.json")
                .bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(jsonString, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun processImage(imageFile: File, onResult: (OcrResult) -> Unit) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        } catch (e: IOException) {
            onResult(OcrResult("", null, "Nie udało się wczytać zdjęcia: ${e.message}"))
            return
        }

        // 1. Try Barcode Scanning first
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    // Process first barcode found
                    val barcode = barcodes.first()
                    val rawValue = barcode.rawValue ?: ""
                    val parsedData = parseQrContent(rawValue)
                    onResult(OcrResult(rawValue, parsedData))
                } else {
                    // 2. Fallback to Text Recognition
                    processText(image, onResult)
                }
            }
            .addOnFailureListener {
                // If barcode fails, try text recognition anyway
                processText(image, onResult)
            }
    }

    private fun processText(image: InputImage, onResult: (OcrResult) -> Unit) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                val parsedData = parseText(rawText)
                onResult(OcrResult(rawText, parsedData))
            }
            .addOnFailureListener { e ->
                onResult(OcrResult("", null, "Błąd OCR/QR: ${e.message}"))
            }
    }

    private fun parseQrContent(content: String): ParsedData {
        // Format: ID=ZLTX3413ID4DG;P=103341;C=Złoty Dąb;L=1500mm;D=2026-02-27
        val parts = content.split(";")
        var producer: String? = null
        var profileCode: String? = null
        var lengthMm: Int? = null
        var color: String? = null

        for (part in parts) {
            val keyValue = part.split("=")
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim()
                when (key) {
                    "P" -> profileCode = value
                    "C" -> {
                        // Check if value is in map (key in map)
                        // If "Złoty Dąb" is in map, use value "ZLT"
                        // If not found, use original value
                        color = colorMap[value] ?: value
                    }
                    "L" -> {
                        // Remove "mm" and parse int
                        val lengthStr = value.replace("mm", "", ignoreCase = true).trim()
                        lengthMm = lengthStr.toIntOrNull()
                    }
                }
            }
        }
        
        return ParsedData(producer, profileCode, lengthMm, color)
    }

    private fun parseText(text: String): ParsedData {
        var producer: String? = null
        var profileCode: String? = null
        var lengthMm: Int? = null
        var color: String? = null

        val lines = text.split("\n")
        
        // 1. Detect Producer
        val producers = listOf("ALUPLAST", "VEKA", "SALAMANDER", "SCHUCO")
        for (line in lines) {
            val upperLine = line.uppercase()
            for (p in producers) {
                if (upperLine.contains(p)) {
                    producer = p
                    break
                }
            }
            if (producer != null) break
        }

        // 2. Detect Profile Code
        val profileRegex = Regex("\\b(\\d{6}|\\d{5}|\\d{2}\\.\\d{3})\\b")
        val profileMatch = profileRegex.find(text)
        if (profileMatch != null) {
            profileCode = profileMatch.value
        }

        // 3. Detect Length
        val lengthRegex = Regex("\\b(\\d{3,4})\\s*(mm|MM)?\\b")
        val lengthMatches = lengthRegex.findAll(text)
        for (match in lengthMatches) {
            val value = match.groupValues[1].toIntOrNull()
            if (value != null && value in 300..7000) {
                if (lengthMm == null || value > lengthMm) {
                    lengthMm = value
                }
            }
        }

        // 4. Detect Color using Map
        // Iterate through map keys ("Złoty Dąb", "Orzech", etc.)
        // If found in text, use the mapped value ("ZLT", "ORZ")
        for (line in lines) {
             for ((key, abbreviation) in colorMap) {
                 if (line.contains(key, ignoreCase = true)) {
                     color = abbreviation
                     break
                 }
             }
             if (color != null) break
        }

        return ParsedData(producer, profileCode, lengthMm, color)
    }
}

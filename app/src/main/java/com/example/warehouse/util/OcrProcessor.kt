package com.example.warehouse.util

import android.content.Context
import android.net.Uri
import com.example.warehouse.model.OcrResult
import com.example.warehouse.model.ParsedData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException

class OcrProcessor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processImage(imageFile: File, onResult: (OcrResult) -> Unit) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        } catch (e: IOException) {
            onResult(OcrResult("", null, "Nie udało się wczytać zdjęcia: ${e.message}"))
            return
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                val parsedData = parseText(rawText)
                onResult(OcrResult(rawText, parsedData))
            }
            .addOnFailureListener { e ->
                onResult(OcrResult("", null, "Błąd OCR: ${e.message}"))
            }
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
        // Regex for Aluplast (6 digits) and Veka (5 digits or 2.3 format)
        // We prioritize looking for lines containing "Art" or "Profil" if possible, but global search is good for now
        val profileRegex = Regex("\\b(\\d{6}|\\d{5}|\\d{2}\\.\\d{3})\\b")
        val profileMatch = profileRegex.find(text)
        if (profileMatch != null) {
            profileCode = profileMatch.value
        }

        // 3. Detect Length
        // Look for 3-4 digits followed optionally by mm, usually in range 300-6500
        val lengthRegex = Regex("\\b(\\d{3,4})\\s*(mm|MM)?\\b")
        val lengthMatches = lengthRegex.findAll(text)
        for (match in lengthMatches) {
            val value = match.groupValues[1].toIntOrNull()
            if (value != null && value in 300..7000) {
                // Heuristic: If we find multiple, usually length is the largest dimension on the label
                // but sometimes date (2023) can be confused. 
                // However, dates usually have separators. 
                // Let's assume the largest number in this range is length for now.
                if (lengthMm == null || value > lengthMm) {
                    lengthMm = value
                }
            }
        }

        // 4. Detect Color (Simplified)
        val colorKeywords = listOf("Złoty Dąb", "Orzech", "Antracyt", "Biały", "ZD", "AP05", "AP23", "Winchester")
        for (line in lines) {
             for (c in colorKeywords) {
                 if (line.contains(c, ignoreCase = true)) {
                     color = c
                     break
                 }
             }
             if (color != null) break
        }

        return ParsedData(producer, profileCode, lengthMm, color)
    }
}

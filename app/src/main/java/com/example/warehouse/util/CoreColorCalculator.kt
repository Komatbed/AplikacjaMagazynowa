package com.example.warehouse.util

object CoreColorCalculator {
    private const val COLOR_WHITE = "biały"
    private const val COLOR_RAL9016 = "9016"
    private const val COLOR_RAL9001 = "9001"
    private const val COLOR_BIALY = "biały"
    private const val COLOR_KREMOWY = "kremowy"

    fun calculate(
        extColorCode: String,
        intColorCode: String,
        rules: Map<String, String>
    ): String {
        val ext = extColorCode.lowercase()
        val int = intColorCode.lowercase()

        // Rule 1: If internal is RAL 9001 (cream), core is cream
        if (int.contains(COLOR_RAL9001)) {
            return COLOR_KREMOWY
        }
        
        // Rule 2: If any side is white (9016), core is white
        if (isWhite(ext) || isWhite(int)) {
            return COLOR_WHITE
        }

        // Rule 3: Lookup external color in map
        // Normalize keys in map to lowercase just in case
        val normalizedRules = rules.mapKeys { it.key.lowercase() }
        val mapped = normalizedRules[ext]
        return translate(mapped) ?: COLOR_WHITE
    }

    private fun isWhite(code: String): Boolean {
        return code.contains("white") || 
               code.contains(COLOR_RAL9016) || 
               code.contains(COLOR_BIALY)
    }
    
    private fun translate(code: String?): String? {
        if (code == null) return null
        return when (code.lowercase()) {
            "white", "biały", "bialy", COLOR_RAL9016 -> COLOR_WHITE
            "brown", "brąz", "braz" -> "brąz"
            "caramel", "karmel" -> "karmel"
            "anthracite", "antracyt" -> "antracyt"
            "grey", "gray", "szary" -> "szary"
            "black", "czarny" -> "czarny"
            "cream", "krem", "kremowy", COLOR_RAL9001 -> COLOR_KREMOWY
            else -> code
        }
    }
}

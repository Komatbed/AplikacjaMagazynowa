package com.example.warehouse.util

object CoreColorCalculator {
    private const val COLOR_WHITE = "white"
    private const val COLOR_RAL9016 = "9016"
    private const val COLOR_BIALY = "biały"

    fun calculate(
        extColorCode: String,
        intColorCode: String,
        rules: Map<String, String>
    ): String {
        val ext = extColorCode.lowercase()
        val int = intColorCode.lowercase()

        // Rule 1: If any side is white, core is white
        if (isWhite(ext) || isWhite(int)) {
            return COLOR_WHITE
        }

        // Rule 2: Lookup external color in map
        // Normalize keys in map to lowercase just in case
        val normalizedRules = rules.mapKeys { it.key.lowercase() }
        
        return normalizedRules[ext] ?: COLOR_WHITE
    }

    private fun isWhite(code: String): Boolean {
        return code.contains(COLOR_WHITE) || 
               code.contains(COLOR_RAL9016) || 
               code.contains(COLOR_BIALY)
    }
}

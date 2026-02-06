package com.example.warehouse.util

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Advanced Optimization Module for processing .ct500txt files to .dcxtxt.
 * 
 * Features:
 * - Parses custom CSV/TXT format
 * - Implements FFD (First Fit Decreasing) optimization
 * - Supports 3 modes (Min Waste, Defined Waste, Longest First)
 * - Handles waste management (>500mm = Warehouse)
 * - Generates machine-compatible output
 */
object FileProcessingOptimizer {

    data class InputRecord(
        val originalLine: String,
        val id: String,
        val profileCode: String,
        val color: String,
        val lengthDeciMm: Int, // 0.1 mm
        val angleL: String,
        val angleR: String
    )

    data class OptimizationResult(
        val outputLines: List<String>,
        val summary: String,
        val logs: List<String>
    )

    enum class Mode {
        MIN_WASTE,      // Try to fit tightly
        DEFINED_WASTE,  // Keep waste > threshold
        LONGEST_FIRST   // Prioritize longest pieces
    }

    private const val KERF_MM = 10
    private const val STOCK_LEN_MM = 6500
    private const val MIN_USEFUL_WASTE_MM = 500

    fun process(
        inputContent: String,
        mode: Mode = Mode.MIN_WASTE,
        reservedWasteLengths: List<Int> = emptyList()
    ): OptimizationResult {
        val logs = mutableListOf<String>()
        val outputLines = mutableListOf<String>()
        
        logs.add("Start processing. Mode: $mode")
        if (reservedWasteLengths.isNotEmpty()) {
            logs.add("Reserved Waste Lengths: $reservedWasteLengths mm")
        }

        // 1. Parse Input
        val records = parseInput(inputContent, logs)
        if (records.isEmpty()) {
            logs.add("No valid records found.")
            return OptimizationResult(emptyList(), "No records", logs)
        }

        // 2. Group by Profile + Color
        val groups = records.groupBy { "${it.profileCode}|${it.color}" }
        logs.add("Found ${groups.size} groups (Profile+Color).")

        // 3. Process Each Group
        groups.forEach { (key, groupRecords) ->
            val (profile, color) = key.split("|")
            logs.add("Optimizing Group: $profile ($color) - ${groupRecords.size} pieces")

            // Sort based on Mode
            val sortedPieces = when (mode) {
                Mode.LONGEST_FIRST -> groupRecords.sortedByDescending { it.lengthDeciMm }
                else -> groupRecords.sortedByDescending { it.lengthDeciMm } // FFD defaults to Descending
            }

            // Optimize Group
            val cutPlan = optimizeGroup(sortedPieces, logs, mode, reservedWasteLengths)
            
            // Generate Output for Group
            cutPlan.forEach { bar ->
                totalBars++
                bar.cuts.forEach { cut ->
                    totalPieces++
                    // BarNum*Profile*Color*Order*Rack*Pos*Comment*Length*A1*A2*Cut1*CutNum*CutNum*Empty
                    // Note: Length in output is 0.1mm
                    val line = buildOutputLine(bar.barIndex, profile, color, cut)
                    outputLines.add(line)
                }

                // Add Waste Entry to LAST piece
                val wasteDeciMm = bar.remainingDeciMm
                val wasteMm = wasteDeciMm / 10
                if (wasteMm > MIN_USEFUL_WASTE_MM && bar.cuts.isNotEmpty()) {
                    val lastCut = bar.cuts.last()
                    val wasteStr = String.format(Locale.US, "%05d", wasteDeciMm)
                    lastCut.wasteComment = "odpad=$wasteStr"
                }
            }
        }

        val summary = "Processed $totalPieces pieces. Used $totalBars bars."
        logs.add("Finished. $summary")
        
        return OptimizationResult(outputLines, summary, logs)
    }

    private fun parseInput(content: String, logs: MutableList<String>): List<InputRecord> {
        val list = mutableListOf<InputRecord>()
        content.lines().forEachIndexed { index, line ->
            if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
            try {
                // Format: 001*    101290*Ciemnoszary*6500*09020*45*45*00000
                val parts = line.split("*")
                if (parts.size >= 7) {
                    val id = parts[0].trim()
                    val profile = parts[1].trim()
                    val color = parts[2].trim()
                    // parts[3] is 6500 (Stock?)
                    val lenStr = parts[4].trim()
                    val lenDeciMm = lenStr.toIntOrNull() ?: 0
                    val angL = parts[5].trim()
                    val angR = parts[6].trim()
                    
                    if (lenDeciMm > 0) {
                        list.add(InputRecord(line, id, profile, color, lenDeciMm, angL, angR))
                    }
                }
            } catch (e: Exception) {
                logs.add("Error parsing line ${index + 1}: ${e.message}")
            }
        }
        return list
    }

    private data class Bar(
        val barIndex: Int,
        val cuts: MutableList<CutAssignment>,
        var remainingDeciMm: Int
    )

    private data class CutAssignment(
        val record: InputRecord,
        val cutIndex: Int,
        var wasteComment: String = ""
    )

    private fun optimizeGroup(
        pieces: List<InputRecord>, 
        logs: MutableList<String>,
        mode: Mode,
        reservedWasteLengths: List<Int>
    ): List<Bar> {
        val bars = mutableListOf<Bar>()
        var barCounter = 1

        // FFD Logic
        for (piece in pieces) {
            val requiredSpaceDeciMm = piece.lengthDeciMm + (KERF_MM * 10) // 10mm kerf in 0.1mm units = 100 units
            
            // Try to fit in existing bars
            var placed = false
            
            // Strategy for finding bar
            val candidateBar = when(mode) {
                Mode.MIN_WASTE -> bars.filter { it.remainingDeciMm >= requiredSpaceDeciMm }
                    .minByOrNull { it.remainingDeciMm - requiredSpaceDeciMm } // Best Fit
                Mode.DEFINED_WASTE -> bars.filter { it.remainingDeciMm >= requiredSpaceDeciMm }
                    .minByOrNull { bar -> 
                        val wasteDeciMm = bar.remainingDeciMm - requiredSpaceDeciMm
                        val wasteMm = wasteDeciMm / 10
                        
                        // Check if matches reserved (tolerance +/- 10mm)
                        val matchesReserved = reservedWasteLengths.any { 
                             kotlin.math.abs(it - wasteMm) <= 10 
                        }
                        
                        if (matchesReserved) {
                             // Prefer this! Treat waste as "0 cost" (or very low)
                             // To differentiate between multiple reserved matches, we can use waste size (smaller reserved is better? or larger?)
                             // Usually larger reserved piece is better? Or just any.
                             // Let's say any reserved is Cost 0.
                             0
                        } else {
                             // Normal waste penalty. 
                             // We want to avoid non-reserved waste.
                             // Cost = waste + large_offset
                             wasteDeciMm + 100000 
                        }
                    }
                else -> bars.firstOrNull { it.remainingDeciMm >= requiredSpaceDeciMm } // First Fit
            }

            if (candidateBar != null) {
                candidateBar.cuts.add(CutAssignment(piece, candidateBar.cuts.size + 1))
                candidateBar.remainingDeciMm -= requiredSpaceDeciMm
                placed = true
            } else {
                // New Bar
                val newBar = Bar(barCounter++, mutableListOf(), (STOCK_LEN_MM * 10)) // 6500mm * 10
                if (newBar.remainingDeciMm >= requiredSpaceDeciMm) {
                    newBar.cuts.add(CutAssignment(piece, 1))
                    newBar.remainingDeciMm -= requiredSpaceDeciMm
                    bars.add(newBar)
                    placed = true
                } else {
                    logs.add("ERROR: Piece too long for stock! ${piece.lengthDeciMm / 10}mm > ${STOCK_LEN_MM}mm")
                }
            }
        }

        // Post-processing: Calculate Waste and add comments
        bars.forEach { bar ->
            // Remaining space is the waste at the END of the bar.
            // (KERF was deducted for each piece).
            // Actually, we deducted KERF for every piece. 
            // If the last piece doesn't need a trailing cut (end of bar), we might have extra 10mm?
            // User said "-10 mm na każde cięcie". I'll stick to simple logic: consume 10mm per piece.
            
            val wasteDeciMm = bar.remainingDeciMm
            val wasteMm = wasteDeciMm / 10
            
            if (wasteMm > MIN_USEFUL_WASTE_MM) {
                // Add waste info to the LAST cut
                if (bar.cuts.isNotEmpty()) {
                    val lastCut = bar.cuts.last()
                    // Format: "odpad=06530" (5 digits)
                    val wasteStr = String.format(Locale.US, "%05d", wasteDeciMm)
                    lastCut.wasteComment = "odpad=$wasteStr"
                }
            }
        }

        return bars
    }

    private fun buildOutputLine(
        barIdx: Int, 
        profile: String, 
        color: String, 
        cut: CutAssignment
    ): String {
        // Format:
        // numer sztangi*numer profila*kolor*zlecenie(CHOLANDIA)*stojak(R-XX)*pozycja(pusty)*komentaż(napisz "odpad=wymiar")*wymiar*kąt1*kąt2*cięcie(1)*numer cięcia(kolejne numery)*numer cięcia(kolejne numery)*(nie wiem co to(puste))
        
        val sb = StringBuilder()
        sb.append(String.format(Locale.US, "%03d", barIdx)).append("*") // 001
        sb.append(profile).append("*") // Profile
        sb.append(color).append("*") // Color
        sb.append("ZLECENIE").append("*") // Zlecenie placeholder
        sb.append("R-01").append("*") // Stojak placeholder
        sb.append("").append("*") // Pozycja (empty)
        
        // Comment
        var comment = "000000000000000" // Default filler
        if (cut.wasteComment.isNotEmpty()) {
             comment = "  ${cut.wasteComment}  "
        }
        sb.append(comment).append("*")
        
        // Length
        sb.append(String.format(Locale.US, "%05d", cut.record.lengthDeciMm)).append("*")
        
        // Angles
        sb.append(cut.record.angleL).append("*")
        sb.append(cut.record.angleR).append("*")
        
        // Cut flag
        sb.append("1").append("*")
        
        // Cut Num
        val cutNumStr = String.format(Locale.US, "%04d", cut.cutIndex)
        sb.append(cutNumStr).append("*")
        sb.append(cutNumStr).append("*")
        
        // Empty
        sb.append("0")
        
        return sb.toString()
    }
}

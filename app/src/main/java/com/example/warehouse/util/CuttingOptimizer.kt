package com.example.warehouse.util

import com.example.warehouse.data.model.CutPlanResponse
import com.example.warehouse.data.model.CutStepDto
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.model.CutItemV2

enum class OptimizationMode(val displayName: String) {
    MIN_WASTE("Minimalny odpad"),
    PRIORITIZE_WASTE("Priorytet odpadów"),
    MIN_CUTS("Minimalna liczba cięć")
}

object CuttingOptimizer {

    // --- V2 Logic ---

    data class OptimizationResult(
        val stockLengthMm: Double,
        val wasteMm: Double,
        val wastePercentage: Double,
        val bars: List<StockBar>
    )

    data class StockBar(
        val id: Int,
        val cuts: List<CutItemV2>,
        val remainingMm: Double
    )

    fun optimize(
        items: List<CutItemV2>,
        stockLengthMm: Double = 6000.0,
        sawWidthMm: Double = 3.0
    ): OptimizationResult {
        // Simple First Fit Decreasing algorithm
        val byProfile = items.groupBy { it.profileName }
        val allBars = mutableListOf<StockBar>()
        var globalId = 1
        
        byProfile.forEach { (_, profileItems) ->
            // Sort by length descending
            val sortedItems = profileItems.sortedByDescending { it.lengthMm }
            
            val bars = mutableListOf<StockBar>()
            
            sortedItems.forEach { item ->
                // Try to fit in existing bars
                var placed = false
                for (i in bars.indices) {
                    val bar = bars[i]
                    val required = item.lengthMm + sawWidthMm
                    
                    if (bar.remainingMm >= required) {
                        // Add to this bar
                        val newCuts = bar.cuts + item
                        val newRemaining = bar.remainingMm - required 
                        bars[i] = bar.copy(cuts = newCuts, remainingMm = newRemaining)
                        placed = true
                        break
                    }
                }
                
                if (!placed) {
                    // Start new bar
                    val required = item.lengthMm + sawWidthMm
                    if (required <= stockLengthMm) {
                        bars.add(StockBar(globalId++, listOf(item), stockLengthMm - required))
                    } else {
                        // Item too long
                        bars.add(StockBar(globalId++, listOf(item), -1.0)) 
                    }
                }
            }
            allBars.addAll(bars)
        }
        
        val totalStockLen = allBars.filter { it.remainingMm >= 0 }.size * stockLengthMm
        val usedLen = items.sumOf { it.lengthMm }
        val waste = totalStockLen - usedLen
        val wastePct = if (totalStockLen > 0) (waste / totalStockLen) * 100 else 0.0
        
        return OptimizationResult(
            stockLengthMm = stockLengthMm,
            wasteMm = waste,
            wastePercentage = wastePct,
            bars = allBars
        )
    }

    // --- V1 / Inventory Logic ---

    fun calculate(
        requiredPieces: List<Int>,
        availableWaste: List<InventoryItemDto>,
        mode: OptimizationMode,
        usefulWasteMinLength: Int
    ): CutPlanResponse {
        val stockLength = 6000
        val sawWidth = 3
        
        // Convert pieces to list of lengths
        val pieces = requiredPieces.sortedDescending().toMutableList()
        val steps = mutableListOf<CutStepDto>()
        
        // Helper class to track usage of a source
        class Source(
            val id: String?, // null for new bar
            val length: Int,
            val isWaste: Boolean,
            val location: String?
        ) {
            val cuts = mutableListOf<Int>()
            var remaining = length
            
            fun canFit(len: Int) = remaining >= (len + (if(cuts.isEmpty()) 0 else sawWidth))
            
            fun addCut(len: Int) {
                val cost = len + (if(cuts.isEmpty()) 0 else sawWidth)
                remaining -= cost
                cuts.add(len)
            }
        }
        
        val wasteSources = availableWaste
            .filter { it.lengthMm >= usefulWasteMinLength }
            .map { Source(it.id, it.lengthMm, true, it.location.label) }
            .toMutableList()
        
        val newBarSources = mutableListOf<Source>()
        
        // Process pieces
        pieces.forEach { pieceLen ->
            // Filter candidates
            val candidates = if (mode == OptimizationMode.PRIORITIZE_WASTE) {
                 // Check waste first, then open bars
                 wasteSources.filter { it.canFit(pieceLen) } + newBarSources.filter { it.canFit(pieceLen) }
            } else {
                 // Check open bars first (to minimize new bars count), then waste
                 newBarSources.filter { it.canFit(pieceLen) } + wasteSources.filter { it.canFit(pieceLen) }
            }
            
            // Best Fit: Choose the one with minimal remaining space after cut
            val bestFit = candidates.minByOrNull { it.remaining - pieceLen }
            
            if (bestFit != null) {
                bestFit.addCut(pieceLen)
            } else {
                // Open new bar
                val newBar = Source(null, stockLength, false, null)
                if (newBar.canFit(pieceLen)) {
                    newBar.addCut(pieceLen)
                    newBarSources.add(newBar)
                }
                // If too big, ignore or handle error (not handled here)
            }
        }
        
        // Convert to Steps
        (wasteSources.filter { it.cuts.isNotEmpty() } + newBarSources).forEach { src ->
            steps.add(CutStepDto(
                sourceItemId = src.id,
                sourceLengthMm = src.length,
                cuts = src.cuts,
                remainingWasteMm = src.remaining,
                isNewBar = !src.isWaste,
                locationLabel = src.location
            ))
        }
        
        val totalStockUsed = newBarSources.size * stockLength
        val totalInput = steps.sumOf { it.sourceLengthMm }
        val totalCutsLen = requiredPieces.sum()
        val totalWaste = steps.sumOf { it.remainingWasteMm }
        
        val efficiency = if (totalInput > 0) (totalCutsLen.toDouble() / totalInput) * 100.0 else 0.0
        
        return CutPlanResponse(
            totalStockUsed = totalStockUsed,
            steps = steps,
            totalWasteMm = totalWaste,
            efficiency = efficiency
        )
    }
}

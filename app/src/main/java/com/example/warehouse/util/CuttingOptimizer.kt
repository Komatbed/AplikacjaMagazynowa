package com.example.warehouse.util

import com.example.warehouse.data.model.CutPlanResponse
import com.example.warehouse.data.model.CutStepDto
import com.example.warehouse.data.model.InventoryItemDto

enum class OptimizationMode(val displayName: String) {
    MIN_WASTE("Minimalny Odpad"),
    LONGEST_FIRST("Najpierw Długie"),
    PREFER_USEFUL_WASTE("Zostawiaj Użyteczne Odpady"),
    USE_ALL_SCRAPS("Wykorzystaj Wszystko")
}

object CuttingOptimizer {

    private const val SAW_BLADE_WIDTH = 4 // mm
    private const val STANDARD_BAR_LENGTH = 6500 // mm

    fun calculate(
        requiredPieces: List<Int>,
        availableWaste: List<InventoryItemDto>,
        mode: OptimizationMode,
        usefulWasteMinLength: Int = 1000
    ): CutPlanResponse {
        
        // 1. Prepare required pieces (sorted based on mode)
        val sortedPieces = when (mode) {
            OptimizationMode.LONGEST_FIRST -> requiredPieces.sortedDescending()
            else -> requiredPieces.sortedDescending() // Default is also Longest First for packing efficiency
        }.toMutableList()

        // 2. Prepare stock
        // Filter waste that is too short for ANY piece? No, might fit smaller ones.
        // Sort waste?
        val sortedWaste = when (mode) {
            OptimizationMode.USE_ALL_SCRAPS -> availableWaste.sortedBy { it.lengthMm } // Use smallest possible waste first? Or largest? usually smallest that fits.
            else -> availableWaste.sortedBy { it.lengthMm }
        }.map { StockItem(it.id, it.lengthMm, it.location.label, false) }.toMutableList()

        var totalStockUsed = 0

        // 3. Packing Algorithm
        // We will modify sortedPieces as we satisfy them.
        
        // Phase 1: Try to cut from Waste

        // For each piece, find best stock
        // Changing logic: We should fill stock items one by one? 
        // Or for each piece find best stock?
        // Bin Packing: For each Item, place in First Bin that fits.
        
        // Better for "Cut List": Process requirements one by one?
        // Actually, FFD (First Fit Decreasing) means take largest item, put in first bin that fits.
        
        // We need to construct "Bins". 
        // Existing Waste are initial Bins.
        // New Bars are infinite potential Bins.

        // We will simulate Bins.
        val bins = sortedWaste.map { Bin(it) }.toMutableList()
        
        // Add potential new bars (dynamic)
        
        // Helper to get a new bar bin
        fun createNewBarBin(): Bin {
            val bin = Bin(StockItem(null, STANDARD_BAR_LENGTH, "Nowa Sztanga", true))
            bins.add(bin)
            return bin
        }

        for (piece in sortedPieces) {
            var bestBin: Bin?
            
            // Find candidate bins
            val candidates = bins.filter { bin -> 
                val currentUsed = bin.usedLength + (if (bin.cuts.isNotEmpty()) SAW_BLADE_WIDTH else 0)
                val remaining = bin.stock.length - currentUsed
                remaining >= piece
            }

            if (candidates.isEmpty()) {
                // Create new bar
                bestBin = createNewBarBin()
            } else {
                // Select best bin based on Strategy
                bestBin = when (mode) {
                    OptimizationMode.MIN_WASTE -> {
                        // Best Fit: Select bin where remaining space is smallest (tightest fit)
                        candidates.minByOrNull { bin ->
                             val currentUsed = bin.usedLength + (if (bin.cuts.isNotEmpty()) SAW_BLADE_WIDTH else 0)
                             bin.stock.length - currentUsed - piece
                        }
                    }
                    OptimizationMode.PREFER_USEFUL_WASTE -> {
                         // Try to find a bin where remainder is >= useful OR remainder is very small (~0)
                         // Prioritize "Very Small Waste" first, then "Useful Waste". Avoid "Useless Waste".
                         candidates.sortedWith(compareBy { bin ->
                             val currentUsed = bin.usedLength + (if (bin.cuts.isNotEmpty()) SAW_BLADE_WIDTH else 0)
                             val remainder = bin.stock.length - currentUsed - piece
                             
                             when {
                                 remainder < 100 -> 0 // Excellent (almost no waste)
                                 remainder >= usefulWasteMinLength -> 1 // Good (useful offcut)
                                 else -> 2 // Bad (useless offcut)
                             }
                         }).firstOrNull()
                    }
                    OptimizationMode.USE_ALL_SCRAPS -> {
                        // Prefer Existing Waste over New Bars
                        // Within Waste: Best Fit
                        val wasteCandidates = candidates.filter { !it.stock.isNew }
                        if (wasteCandidates.isNotEmpty()) {
                             wasteCandidates.minByOrNull { bin ->
                                 val currentUsed = bin.usedLength + (if (bin.cuts.isNotEmpty()) SAW_BLADE_WIDTH else 0)
                                 bin.stock.length - currentUsed - piece
                             }
                        } else {
                            // If only new bars available
                            candidates.firstOrNull() // Just take the first one (should be only one open new bar usually if we optimize well, but here we might have multiple if using FFD logic fully)
                             // Actually for New Bars we should reuse the last opened one if possible to minimize opened bars.
                             // But createNewBarBin adds to 'bins'.
                             // Let's prefer existing open new bars.
                             candidates.firstOrNull()
                        }
                    }
                    else -> candidates.firstOrNull() // First Fit
                }
                
                if (bestBin == null) {
                     bestBin = createNewBarBin()
                }
            }
            
            // Assign piece
            bestBin.cuts.add(piece)
            bestBin.usedLength += piece + (if (bestBin.cuts.size > 1) SAW_BLADE_WIDTH else 0)
        }

        // 4. Generate Result
        val usedBins = bins.filter { it.cuts.isNotEmpty() }
        var totalWaste = 0
        
        val cutSteps = usedBins.map { bin ->
            val remaining = bin.stock.length - bin.usedLength
            totalWaste += remaining
            if (bin.stock.isNew) totalStockUsed++
            
            CutStepDto(
                sourceItemId = bin.stock.id,
                sourceLengthMm = bin.stock.length,
                cuts = bin.cuts,
                remainingWasteMm = remaining,
                isNewBar = bin.stock.isNew,
                locationLabel = bin.stock.location
            )
        }
        
        // Calculate Efficiency
        val totalMaterial = cutSteps.sumOf { it.sourceLengthMm }
        val efficiency = if (totalMaterial > 0) {
             val utilized = totalMaterial - totalWaste
             (utilized.toDouble() / totalMaterial.toDouble()) * 100
        } else 0.0

        return CutPlanResponse(
            totalStockUsed = totalStockUsed,
            steps = cutSteps,
            totalWasteMm = totalWaste,
            efficiency = efficiency
        )
    }

    private data class StockItem(val id: String?, val length: Int, val location: String, val isNew: Boolean)
    private class Bin(val stock: StockItem) {
        val cuts = mutableListOf<Int>()
        var usedLength = 0
    }
}

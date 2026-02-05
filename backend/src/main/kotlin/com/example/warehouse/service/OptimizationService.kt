package com.example.warehouse.service

import com.example.warehouse.model.*
import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.ProfileDefinitionRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OptimizationService(
    private val inventoryRepository: InventoryItemRepository,
    private val profileRepository: ProfileDefinitionRepository
) {

    fun optimizeCuts(request: OptimizationRequest): CutPlan {
        // 1. Get Profile Standard Length
        val profileDef = profileRepository.findByCode(request.profileCode)
        val standardLength = profileDef?.standardLengthMm ?: 6500

        // 2. Fetch Available Inventory (Wastes)
        // Note: Assuming we have a custom query for this or filtering in memory
        val allItems = inventoryRepository.findAll().filter {
            it.profileCode == request.profileCode &&
            it.internalColor == request.internalColor &&
            it.externalColor == request.externalColor &&
            (request.coreColor == null || it.coreColor == request.coreColor) &&
            it.status == ItemStatus.AVAILABLE
        }

        // Separate wastes (anything less than standard length, or explicitly marked)
        // For simplicity, we treat everything in inventory as "Stock" to be used.
        // We prefer using shorter pieces (Waste) before full bars if configured.
        
        val stockList = allItems.filter { item ->
            // Exclude items that match reserved waste lengths
            // Tolerance: +/- 1mm to be safe
            val isReserved = request.reserveWasteLengths.any { reservedLen ->
                kotlin.math.abs(item.lengthMm - reservedLen) <= 1
            }
            !isReserved
        }.map { 
            StockItem(it.id.toString(), it.lengthMm, it.location.label, false) 
        }.toMutableList()

        // 3. Algorithm: Best Fit Decreasing with Grouping and Waste Targeting
        val required = request.requiredPieces.sortedDescending()
        val bins = mutableListOf<Bin>()
        val usedStock = mutableSetOf<String>() // IDs of used real stock

        for (piece in required) {
            var bestBin: Bin? = null
            var bestFitScore = Double.MAX_VALUE
            
            // 1. Try to fit in already opened Bins
            for (bin in bins) {
                if (bin.remaining >= piece) {
                    val potentialWaste = bin.remaining - piece
                    // Scoring: 
                    // Lower is better.
                    
                    // Check if waste matches reserved lengths
                    val isReservedWaste = request.reserveWasteLengths.any { 
                        kotlin.math.abs(potentialWaste - it) <= 5 // 5mm tolerance for creation
                    }

                    val score = when {
                        isReservedWaste -> potentialWaste - 10000.0 // Huge bonus for reserved waste
                        potentialWaste in 50..250 -> potentialWaste - 1000.0 // Bonus for small useful waste
                        else -> potentialWaste.toDouble()
                    }
                    
                    if (score < bestFitScore) {
                        bestFitScore = score
                        bestBin = bin
                    }
                }
            }
            
            // 2. If no fit in existing bins, look for unused Stock (Waste)
            if (bestBin == null) {
                var bestStock: StockItem? = null
                var bestStockScore = Double.MAX_VALUE
                
                for (stock in stockList) {
                    if (stock.id !in usedStock && stock.length >= piece) {
                        val potentialWaste = stock.length - piece
                        
                        var score = potentialWaste.toDouble()
                        
                        if (potentialWaste == 0) {
                            score = -1_000_000.0
                        } else if (potentialWaste in request.reserveWasteLengths) {
                            score = -500_000.0
                        } else if (potentialWaste in 50..250) {
                            score = -2_000.0 + potentialWaste
                        } else if (potentialWaste < 50) {
                            score = 10_000.0 + potentialWaste
                        }
                        
                        if (score < bestStockScore) {
                            bestStockScore = score
                            bestStock = stock
                        }
                    }
                }
                
                if (bestStock != null) {
                    val newBin = Bin(
                        id = bestStock.id,
                        originalLength = bestStock.length,
                        remaining = bestStock.length,
                        isNewBar = false,
                        location = bestStock.location
                    )
                    bins.add(newBin)
                    usedStock.add(bestStock.id)
                    bestBin = newBin
                }
            }
            
            // 3. If still null, use New Bar
            if (bestBin == null) {
                val newBin = Bin(
                    id = UUID.randomUUID().toString(), // Virtual ID
                    originalLength = standardLength,
                    remaining = standardLength,
                    isNewBar = true,
                    location = "MAGAZYN" // General storage
                )
                bins.add(newBin)
                bestBin = newBin
            }
            
            // Execute Cut
            bestBin!!.remaining -= piece
            bestBin.cuts.add(piece)
        }
        
        // 4. Post-processing: Sort cuts within each bin
        // "sort details by length from longest... place similar ones next to each other"
        bins.forEach { bin ->
            bin.cuts.sortDescending()
        }
        
        // Convert Bins to CutSteps
        
        // Convert Bins to CutSteps
        var totalWaste = 0
        val cutSteps = bins.map { bin ->
            totalWaste += bin.remaining
            CutStep(
                sourceItemId = if (bin.isNewBar) null else bin.id,
                sourceLengthMm = bin.originalLength,
                cuts = bin.cuts,
                remainingWasteMm = bin.remaining,
                isNewBar = bin.isNewBar,
                locationLabel = bin.location
            )
        }
        
        // Calculate Efficiency
        val totalMaterial = bins.sumOf { it.originalLength }
        val usedMaterial = totalMaterial - totalWaste
        val efficiency = if (totalMaterial > 0) (usedMaterial.toDouble() / totalMaterial) * 100 else 0.0

        return CutPlan(
            totalStockUsed = bins.size,
            steps = cutSteps,
            totalWasteMm = totalWaste,
            efficiency = efficiency
        )
    }

    private data class StockItem(val id: String, val length: Int, val location: String, val used: Boolean)
    
    private class Bin(
        val id: String,
        val originalLength: Int,
        var remaining: Int,
        val isNewBar: Boolean,
        val location: String,
        val cuts: MutableList<Int> = mutableListOf()
    )
}

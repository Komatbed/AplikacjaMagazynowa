package com.example.warehouse.features.muntins_v3.calculations

import kotlin.math.ceil

data class OptimizationResult(
    val bars: List<Bar>,
    val totalWaste: Double,
    val wastePercentage: Double,
    val totalBarsUsed: Int
)

data class Bar(
    val id: Int,
    val totalLength: Double,
    val usedLength: Double,
    val waste: Double,
    val cuts: List<Cut>
)

data class Cut(
    val length: Double,
    val angleStart: Double,
    val angleEnd: Double,
    val description: String
)

object BarOptimizer {

    fun optimize(
        cutItems: List<CutItem>, // From CutListCalculator
        barLength: Double = 6000.0,
        sawKerf: Double = 4.0,
        sashCount: Int = 1
    ): OptimizationResult {
        // Flatten the list of items based on count and sashCount
        val allCuts = mutableListOf<Cut>()
        
        cutItems.forEach { item ->
            val totalCount = item.count * sashCount
            for (i in 0 until totalCount) {
                allCuts.add(
                    Cut(
                        length = item.length,
                        angleStart = item.angleStart,
                        angleEnd = item.angleEnd,
                        description = "L:${item.length}mm (${item.angleStart}°/${item.angleEnd}°)"
                    )
                )
            }
        }

        // Sort by length descending (Best Fit Decreasing)
        allCuts.sortByDescending { it.length }

        val bars = mutableListOf<BarState>()

        for (cut in allCuts) {
            val requiredLength = cut.length + sawKerf
            
            // Find best fit bar
            // Best fit: minimum remaining space after adding this cut
            var bestBarIndex = -1
            var minWaste = Double.MAX_VALUE

            for (i in bars.indices) {
                val remaining = bars[i].remainingLength
                if (remaining >= requiredLength) {
                    val newRemaining = remaining - requiredLength
                    if (newRemaining < minWaste) {
                        minWaste = newRemaining
                        bestBarIndex = i
                    }
                }
            }

            if (bestBarIndex != -1) {
                // Add to existing bar
                bars[bestBarIndex].cuts.add(cut)
                bars[bestBarIndex].remainingLength -= requiredLength
                bars[bestBarIndex].usedLength += requiredLength
            } else {
                // Create new bar
                if (barLength < requiredLength) {
                    // Item too long for bar - technically impossible but handled gracefully
                    // Create a "special" bar just for this item or mark as error
                    // For now, put in a new bar and mark negative remaining (overflow)
                    val bar = BarState(barLength)
                    bar.cuts.add(cut)
                    bar.remainingLength -= requiredLength // Will be negative
                    bar.usedLength += requiredLength
                    bars.add(bar)
                } else {
                    val bar = BarState(barLength)
                    bar.cuts.add(cut)
                    bar.remainingLength -= requiredLength
                    bar.usedLength += requiredLength
                    bars.add(bar)
                }
            }
        }

        // Convert to result
        val resultBars = bars.mapIndexed { index, state ->
            Bar(
                id = index + 1,
                totalLength = barLength,
                usedLength = state.usedLength,
                waste = if (state.remainingLength < 0) 0.0 else state.remainingLength,
                cuts = state.cuts
            )
        }

        val totalWaste = resultBars.sumOf { it.waste }
        val totalUsed = resultBars.sumOf { it.usedLength }
        val totalCapacity = resultBars.size * barLength
        val wastePercentage = if (totalCapacity > 0) (totalWaste / totalCapacity) * 100 else 0.0

        return OptimizationResult(
            bars = resultBars,
            totalWaste = totalWaste,
            wastePercentage = wastePercentage,
            totalBarsUsed = resultBars.size
        )
    }

    private class BarState(val totalLength: Double) {
        var remainingLength: Double = totalLength
        var usedLength: Double = 0.0
        val cuts = mutableListOf<Cut>()
    }
}

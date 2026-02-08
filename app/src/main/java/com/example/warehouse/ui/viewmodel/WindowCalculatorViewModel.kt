package com.example.warehouse.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class WindowConfig(
    val widthMm: Int = 1000,
    val heightMm: Int = 1000,
    val sashCount: Int = 1, // 1 or 2
    val hasMullion: Boolean = false, // True = Fixed Mullion (Słupek stały), False = Flying Mullion (Słupek ruchomy) if sashCount=2
    val profileSystem: ProfileSystem = ProfileSystem.SYSTEM_70,
    val glazingType: GlazingType = GlazingType.TRIPLE // 2 or 3 pane
)

enum class ProfileSystem(val label: String, val frameWidth: Int, val sashWidth: Int, val overlap: Int, val mullionWidth: Int, val glassDeduction: Int) {
    SYSTEM_70("System 70mm", 70, 80, 20, 84, 96), // 96mm deduction from sash outer to glass? (80-20-gap)*2? No. usually sash outer - glass = ~80-90mm. Let's assume 90.
    SYSTEM_80("System 80mm", 80, 85, 20, 94, 106)
}

enum class GlazingType(val label: String, val weightPerM2: Double, val costPerM2: Double) {
    DOUBLE("2-szybowy (4/16/4)", 20.0, 150.0),
    TRIPLE("3-szybowy (4/12/4/12/4)", 30.0, 250.0)
}

data class CalculationResult(
    val sashWidth: Int = 0,
    val sashHeight: Int = 0,
    val glassWidth: Int = 0,
    val glassHeight: Int = 0,
    val glassWeightKg: Double = 0.0,
    val profileWeightKg: Double = 0.0, // Estimated
    val estimatedCost: Double = 0.0,
    val mullionLength: Int = 0,
    val glazingBeadHorizontal: Int = 0,
    val glazingBeadVertical: Int = 0
)

class WindowCalculatorViewModel : ViewModel() {

    private val _config = MutableStateFlow(WindowConfig())
    val config: StateFlow<WindowConfig> = _config.asStateFlow()

    private val _result = MutableStateFlow(CalculationResult())
    val result: StateFlow<CalculationResult> = _result.asStateFlow()

    fun updateWidth(width: String) {
        val w = width.toIntOrNull() ?: 0
        _config.value = _config.value.copy(widthMm = w)
        calculate()
    }

    fun updateHeight(height: String) {
        val h = height.toIntOrNull() ?: 0
        _config.value = _config.value.copy(heightMm = h)
        calculate()
    }

    fun updateSashCount(count: Int) {
        _config.value = _config.value.copy(sashCount = count.coerceIn(1, 2))
        calculate()
    }

    fun toggleMullion(hasMullion: Boolean) {
        _config.value = _config.value.copy(hasMullion = hasMullion)
        calculate()
    }

    fun updateSystem(system: ProfileSystem) {
        _config.value = _config.value.copy(profileSystem = system)
        calculate()
    }

    fun updateGlazing(glazing: GlazingType) {
        _config.value = _config.value.copy(glazingType = glazing)
        calculate()
    }

    private fun calculate() {
        val c = _config.value
        if (c.widthMm == 0 || c.heightMm == 0) return

        val sys = c.profileSystem
        
        // Height Calculations
        val frameClearH = c.heightMm - 2 * sys.frameWidth
        val sashOuterH = frameClearH + 2 * sys.overlap
        val glassH = sashOuterH - sys.glassDeduction
        
        val sashOuterW: Int
        var mullionLen = 0

        if (c.sashCount == 1) {
            // Single Sash
            val frameClearW = c.widthMm - 2 * sys.frameWidth
            sashOuterW = frameClearW + 2 * sys.overlap
        } else {
            // 2 Sash
            if (c.hasMullion) {
                // Fixed Mullion
                mullionLen = frameClearH // Usually mullion connects to frame inner
                val frameClearW = c.widthMm - 2 * sys.frameWidth - sys.mullionWidth
                sashOuterW = (frameClearW / 2) + 2 * sys.overlap
            } else {
                // Flying Mullion (Słupek ruchomy)
                // One sash is Master, one is Slave (with flying mullion profile attached)
                // Simplified: Total Width - 2*Frame. 
                // Overlap logic is complex for flying mullion.
                // Usually: (FrameClearW + OverlapCentral) / 2?
                // Let's approximate: SashW = (Width - 2*Frame + CentralOverlap) / 2
                // Central overlap is usually ~10-20mm effectively?
                // Let's assume symmetric sashes for simplicity calculation
                val frameClearW = c.widthMm - 2 * sys.frameWidth
                sashOuterW = (frameClearW / 2) + sys.overlap // Approximate
            }
        }

        val glassW = sashOuterW - sys.glassDeduction

        // Weight
        val glassArea = (glassW / 1000.0) * (glassH / 1000.0) * c.sashCount
        val glassWeight = glassArea * c.glazingType.weightPerM2
        
        // Profile Weight (Estimate: 3kg/m for Frame, 2.5kg/m for Sash)
        val framePerimeter = 2 * (c.widthMm + c.heightMm) / 1000.0
        val sashPerimeter = 2 * (sashOuterW + sashOuterH) / 1000.0 * c.sashCount
        val profileWeight = (framePerimeter * 3.0) + (sashPerimeter * 2.5) + (if(c.hasMullion) mullionLen/1000.0 * 3.0 else 0.0)

        // Cost
        val glassCost = glassArea * c.glazingType.costPerM2
        val profileCost = (framePerimeter + sashPerimeter) * 40.0 // Arbitrary 40 PLN/m
        val hardwareCost = c.sashCount * 150.0 // 150 PLN per sash
        val totalCost = glassCost + profileCost + hardwareCost

        // Beads (Listewki)
        // Usually width of glass + tolerance? Or width of Sash inner?
        // Bead length ~ Glass W/H
        val beadH = glassH
        val beadV = glassW

        _result.value = CalculationResult(
            sashWidth = sashOuterW,
            sashHeight = sashOuterH,
            glassWidth = glassW,
            glassHeight = glassH,
            glassWeightKg = (glassWeight * 10).roundToInt() / 10.0,
            profileWeightKg = (profileWeight * 10).roundToInt() / 10.0,
            estimatedCost = (totalCost * 100).roundToInt() / 100.0,
            mullionLength = mullionLen,
            glazingBeadHorizontal = beadV, // Top/Bottom beads
            glazingBeadVertical = beadH    // Left/Right beads
        )
    }
}

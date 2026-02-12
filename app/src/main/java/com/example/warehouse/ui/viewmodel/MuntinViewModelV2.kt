package com.example.warehouse.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.warehouse.data.local.V2ProfileCatalog
import com.example.warehouse.model.*
import com.example.warehouse.util.MuntinCalculatorV2
import com.example.warehouse.util.MuntinCalculatorV2Angular
import com.example.warehouse.util.MuntinCalculatorV2.IntersectionType
import java.util.UUID

class MuntinViewModelV2 : ViewModel() {

    // --- State ---
    data class MuntinV2UiState(
        val sashWidth: String = "1000",
        val sashHeight: String = "1000",
        val selectedSashProfileIndex: Int = 0,
        val selectedBeadProfileIndex: Int = 0,
        val selectedMuntinProfileIndex: Int = 0,
        val assemblyClearance: String = "1.0",
        val sawCorrection: String = "0.0",
        val windowCorrection: String = "0.0",
        val intersectionRule: IntersectionType = IntersectionType.VERTICAL_CONTINUOUS,
        
        // Orthogonal Mode
        val verticalMuntins: List<Double> = emptyList(),
        val horizontalMuntins: List<Double> = emptyList(),
        
        // Angular Mode
        val isAngularMode: Boolean = false,
        val angularConfig: AngularModeConfigV2 = AngularModeConfigV2(),
        val diagonals: List<DiagonalLineV2> = emptyList(),
        val spiderPattern: SpiderPatternV2? = null,
        val archPattern: ArchPatternV2? = null,
        
        val cutList: List<CutItemV2> = emptyList(),
        val mountingMarks: List<String> = emptyList(), // For display
        val mountingMarksV2: List<MountMarkV2> = emptyList(), // Structured
        
        // Visualization
        val debugSegments: List<MuntinCalculatorV2Angular.Segment> = emptyList()
    )

    private val _uiState = mutableStateOf(MuntinV2UiState())
    val uiState: State<MuntinV2UiState> = _uiState

    // --- Catalogs ---
    val sashProfiles = V2ProfileCatalog.sashProfiles
    val beadProfiles = V2ProfileCatalog.beadProfiles
    val muntinProfiles = V2ProfileCatalog.muntinProfiles

    // --- Actions ---

    fun setMode(isAngular: Boolean) {
        _uiState.value = _uiState.value.copy(isAngularMode = isAngular)
        calculate()
    }

    fun updateSashDimensions(w: String, h: String) {
        _uiState.value = _uiState.value.copy(sashWidth = w, sashHeight = h)
        calculate()
    }

    fun selectSashProfile(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSashProfileIndex = index)
        calculate()
    }

    fun selectBeadProfile(index: Int) {
        _uiState.value = _uiState.value.copy(selectedBeadProfileIndex = index)
        calculate()
    }

    fun selectMuntinProfile(index: Int) {
        _uiState.value = _uiState.value.copy(selectedMuntinProfileIndex = index)
        calculate()
    }
    
    fun updateSettings(clearance: String, saw: String, win: String, rule: IntersectionType) {
        _uiState.value = _uiState.value.copy(
            assemblyClearance = clearance,
            sawCorrection = saw,
            windowCorrection = win,
            intersectionRule = rule
        )
        calculate()
    }

    fun addVerticalMuntin() {
        val w = _uiState.value.sashWidth.toIntOrNull() ?: 1000
        val list = _uiState.value.verticalMuntins.toMutableList()
        if (list.isEmpty()) {
            list.add(w / 2.0)
        } else {
             list.add((w / (list.size + 2)).toDouble() * (list.size + 1))
        }
        _uiState.value = _uiState.value.copy(verticalMuntins = list.sorted())
        calculate()
    }
    
    fun removeVerticalMuntin(index: Int) {
        if (index in _uiState.value.verticalMuntins.indices) {
            val list = _uiState.value.verticalMuntins.toMutableList()
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(verticalMuntins = list)
            calculate()
        }
    }
    
    fun addHorizontalMuntin() {
        val h = _uiState.value.sashHeight.toIntOrNull() ?: 1000
        val list = _uiState.value.horizontalMuntins.toMutableList()
        if (list.isEmpty()) {
            list.add(h / 2.0)
        } else {
             list.add((h / (list.size + 2)).toDouble() * (list.size + 1))
        }
        _uiState.value = _uiState.value.copy(horizontalMuntins = list.sorted())
        calculate()
    }
    
    fun removeHorizontalMuntin(index: Int) {
        if (index in _uiState.value.horizontalMuntins.indices) {
            val list = _uiState.value.horizontalMuntins.toMutableList()
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(horizontalMuntins = list)
            calculate()
        }
    }

    // --- Angular Actions ---

    fun addDiagonal(angle: Double) {
        val w = _uiState.value.sashWidth.toIntOrNull() ?: 1000
        val offset = w / 2.0
        val line = DiagonalLineV2(
            lineId = UUID.randomUUID().toString().take(4),
            angleDeg = angle,
            offsetRefMm = offset,
            isContinuous = false
        )
        _uiState.value = _uiState.value.copy(diagonals = _uiState.value.diagonals + line)
        calculate()
    }

    fun removeDiagonal(id: String) {
        _uiState.value = _uiState.value.copy(diagonals = _uiState.value.diagonals.filter { it.lineId != id })
        calculate()
    }
    
    fun setSpiderPattern(active: Boolean) {
        if (active) {
            val w = _uiState.value.sashWidth.toIntOrNull() ?: 1000
            val h = _uiState.value.sashHeight.toIntOrNull() ?: 1000
            val frame = 82 + 20 
            val cx = (frame + (w - 2*frame)/2).toDouble()
            val cy = (frame + (h - 2*frame)/2).toDouble()
            
            val spider = SpiderPatternV2(
                centerX = cx,
                centerY = cy,
                armCount = 8,
                startAngleDeg = 0.0,
                ringCount = 1,
                ringSpacingMm = 150.0
            )
            _uiState.value = _uiState.value.copy(spiderPattern = spider)
        } else {
            _uiState.value = _uiState.value.copy(spiderPattern = null)
        }
        calculate()
    }

    fun setArchPattern(active: Boolean) {
        if (active) {
            val w = _uiState.value.sashWidth.toIntOrNull() ?: 1000
            val h = _uiState.value.sashHeight.toIntOrNull() ?: 1000
            val frame = 82 + 20 
            
            val arch = ArchPatternV2(
                radiusMm = (w - 2*frame) / 2.0,
                arcStartDeg = 180.0,
                arcEndDeg = 360.0,
                divisionCount = 3,
                withStraightJoins = true
            )
            _uiState.value = _uiState.value.copy(archPattern = arch)
        } else {
            _uiState.value = _uiState.value.copy(archPattern = null)
        }
        calculate()
    }

    private fun calculate() {
        val s = _uiState.value
        val w = s.sashWidth.toIntOrNull() ?: 1000
        val h = s.sashHeight.toIntOrNull() ?: 1000
        
        val sashP = sashProfiles.getOrElse(s.selectedSashProfileIndex) { sashProfiles.first() }
        val beadP = beadProfiles.getOrElse(s.selectedBeadProfileIndex) { beadProfiles.first() }
        val muntinP = muntinProfiles.getOrElse(s.selectedMuntinProfileIndex) { muntinProfiles.first() }
        
        val settings = V2GlobalSettings(
            sawCorrectionMm = s.sawCorrection.toDoubleOrNull() ?: 0.0,
            windowCorrectionMm = s.windowCorrection.toDoubleOrNull() ?: 0.0,
            assemblyClearanceMm = s.assemblyClearance.toDoubleOrNull() ?: 1.0
        )

        if (s.isAngularMode) {
            val result = MuntinCalculatorV2Angular.calculate(
                sashWidthMm = w,
                sashHeightMm = h,
                sashProfile = sashP,
                beadProfile = beadP,
                muntinProfile = muntinP,
                diagonals = s.diagonals,
                spider = s.spiderPattern,
                arch = s.archPattern,
                settings = settings
            )
            _uiState.value = s.copy(
                cutList = result.cutItems,
                mountingMarksV2 = result.mountingMarks,
                mountingMarks = result.mountingMarks.map { "${it.itemId}: ${it.axisDescription}" },
                debugSegments = result.debugSegments
            )
        } else {
            // Orthogonal Mode (Existing)
            val cuts = MuntinCalculatorV2.calculate(
                sashWidthMm = w,
                sashHeightMm = h,
                sashProfile = sashP,
                beadProfile = beadP,
                muntinProfile = muntinP,
                verticalPositions = s.verticalMuntins,
                horizontalPositions = s.horizontalMuntins,
                settings = settings,
                defaultIntersectionRule = s.intersectionRule
            )
            
            val marks = cuts.map { "Muntin ${it.muntinNo}: L=${it.lengthMm}" } 
            
            _uiState.value = s.copy(
                cutList = cuts,
                mountingMarks = marks,
                debugSegments = emptyList() 
            )
        }
    }
}

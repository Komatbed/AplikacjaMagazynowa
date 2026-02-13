package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.V2ProfileCatalog
import com.example.warehouse.data.model.*
import com.example.warehouse.model.*
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.data.repository.ConfigRepository
import com.example.warehouse.util.MuntinCalculatorV2
import com.example.warehouse.util.MuntinCalculatorV2Angular
import com.example.warehouse.util.MuntinCalculatorV2.IntersectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

import com.example.warehouse.util.CuttingOptimizer

class MuntinViewModelV2 @JvmOverloads constructor(
    application: Application,
    configRepo: ConfigRepository? = null
) : AndroidViewModel(application) {

    private val configRepository = configRepo ?: ConfigRepository(application)


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
        val debugSegments: List<MuntinCalculatorV2Angular.Segment> = emptyList(),
        
        // Optimization
        val optimizationResult: CuttingOptimizer.OptimizationResult? = null
    )

    private val _uiState = mutableStateOf(MuntinV2UiState())
    val uiState: State<MuntinV2UiState> = _uiState

    // --- Catalogs ---
    // Loaded from Repository
    private val _sashProfiles = MutableStateFlow<List<SashProfileV2>>(emptyList())
    val sashProfiles: StateFlow<List<SashProfileV2>> = _sashProfiles

    private val _beadProfiles = MutableStateFlow<List<BeadProfileV2>>(emptyList())
    val beadProfiles: StateFlow<List<BeadProfileV2>> = _beadProfiles

    private val _muntinProfiles = MutableStateFlow<List<MuntinProfileV2>>(emptyList())
    val muntinProfiles: StateFlow<List<MuntinProfileV2>> = _muntinProfiles

    private val _allProfiles = MutableStateFlow<List<ProfileDefinition>>(emptyList())
    val allProfiles: StateFlow<List<ProfileDefinition>> = _allProfiles

    init {
        viewModelScope.launch {
            configRepository.getProfilesFlow().collectLatest { entities ->
                // Map Entity to Definition
                val definitions = entities.map { entity ->
                    ProfileDefinition(
                        id = entity.id,
                        code = entity.code,
                        description = entity.description,
                        heightMm = entity.heightMm,
                        widthMm = entity.widthMm,
                        beadHeightMm = entity.beadHeightMm,
                        beadAngle = entity.beadAngle,
                        standardLengthMm = entity.standardLengthMm,
                        system = entity.system,
                        manufacturer = entity.manufacturer,
                        type = entity.type
                    )
                }
                _allProfiles.value = definitions

                // Filter by Type and Map to V2 Models
                val sashes = definitions.filter { it.type == "SASH" }
                    .map { 
                        SashProfileV2(
                            profileNo = it.code,
                            widthMm = it.widthMm,
                            heightMm = it.heightMm,
                            rebateHeightMm = it.beadHeightMm,
                            outerConstructionAngleDeg = 45.0 // Default or from data if available
                        )
                    }
                    .ifEmpty { V2ProfileCatalog.sashProfiles }

                val beads = definitions.filter { it.type == "BEAD" }
                    .map {
                        BeadProfileV2(
                            profileNo = it.code,
                            widthMm = it.widthMm,
                            heightMm = if (it.beadHeightMm > 0) it.beadHeightMm else it.heightMm, // Use beadHeightMm if set
                            innerBeadAngleDeg = if (it.beadAngle > 0) it.beadAngle else 45.0
                        )
                    }
                    .ifEmpty { V2ProfileCatalog.beadProfiles }

                val muntins = definitions.filter { it.type == "MUNTIN" }
                    .map {
                        MuntinProfileV2(
                            profileNo = it.code,
                            widthMm = it.widthMm,
                            heightMm = it.heightMm,
                            wallAngleDeg = 0.0
                        )
                    }
                    .ifEmpty { V2ProfileCatalog.muntinProfiles }

                _sashProfiles.value = sashes
                _beadProfiles.value = beads
                _muntinProfiles.value = muntins

                // Reset selection if out of bounds or invalid
                // But keep it simple for now, calculate() handles index out of bounds safely
                calculate()
            }
        }
    }


    // --- Interaction ---
    fun handleCanvasClick(relX: Float, relY: Float, scale: Float) {
        val s = _uiState.value
        val w = s.sashWidth.toDoubleOrNull() ?: 1000.0
        val h = s.sashHeight.toDoubleOrNull() ?: 1000.0
        
        val clickX = relX / scale
        val clickY = relY / scale
        
        val threshold = 40.0 // mm threshold for removing

        // Check Verticals
        val clickedVertical = s.verticalMuntins.withIndex().minByOrNull { abs(it.value - clickX) }
        if (clickedVertical != null && abs(clickedVertical.value - clickX) < threshold) {
            removeVerticalMuntin(clickedVertical.index)
            return
        }

        // Check Horizontals
        val clickedHorizontal = s.horizontalMuntins.withIndex().minByOrNull { abs(it.value - clickY) }
        if (clickedHorizontal != null && abs(clickedHorizontal.value - clickY) < threshold) {
            removeHorizontalMuntin(clickedHorizontal.index)
            return
        }

        // Add New
        // Determine if Vertical or Horizontal based on proximity to center of fields?
        // Or simple logic: if clicked closer to top/bottom edges -> vertical? No.
        // V1 logic: "distToVCenter < distToHCenter" -> Vertical.
        // Here we don't have a grid, we have free space.
        
        // Let's use simple logic:
        // If click is closer to an existing Vertical than Horizontal? No.
        
        // Let's default to:
        // If we are "adding", we add a line at that position.
        // But do we add Vertical or Horizontal?
        // Maybe check which dimension is larger? Or just guess based on aspect ratio?
        // Better: Check where the click is relative to existing grid.
        // Or simply: Add BOTH? No.
        
        // V1 Logic revisited:
        // distToVCenter = abs((relX % vStep) - (vStep / 2))
        // It calculated which "center" was closer.
        
        // Here we can try to snap to "Halves".
        // If I click, I probably want to add a line splitting a space.
        // Find the "space" I clicked in.
        
        val sortedV = (listOf(0.0) + s.verticalMuntins + listOf(w)).sorted()
        val sortedH = (listOf(0.0) + s.horizontalMuntins + listOf(h)).sorted()
        
        // Find which vertical interval I am in
        val vIntervalIndex = sortedV.zipWithNext().indexOfFirst { (start, end) -> clickX >= start && clickX <= end }
        val hIntervalIndex = sortedH.zipWithNext().indexOfFirst { (start, end) -> clickY >= start && clickY <= end }
        
        if (vIntervalIndex != -1 && hIntervalIndex != -1) {
            val vStart = sortedV[vIntervalIndex]
            val vEnd = sortedV[vIntervalIndex+1]
            val hStart = sortedH[hIntervalIndex]
            val hEnd = sortedH[hIntervalIndex+1]
            
            val vCenter = (vStart + vEnd) / 2
            val hCenter = (hStart + hEnd) / 2
            
            val distV = abs(clickX - vCenter)
            val distH = abs(clickY - hCenter)
            
            // Normalize distances by interval size to be fair?
            val normDistV = distV / (vEnd - vStart)
            val normDistH = distH / (hEnd - hStart)
            
            if (normDistV < normDistH) {
                // Closer to vertical center -> Add Vertical
                addVerticalMuntin()
            } else {
                // Closer to horizontal center -> Add Horizontal
                addHorizontalMuntin()
            }
            // calculate() is called inside add methods
        }
    }

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
        val currentCount = _uiState.value.verticalMuntins.size
        val newCount = currentCount + 1
        
        val list = mutableListOf<Double>()
        val step = w.toDouble() / (newCount + 1)
        for (i in 1..newCount) {
            list.add(step * i)
        }
        
        _uiState.value = _uiState.value.copy(verticalMuntins = list)
        calculate()
    }
    
    fun removeVerticalMuntin(index: Int) {
        val currentCount = _uiState.value.verticalMuntins.size
        if (currentCount > 0) {
            val newCount = currentCount - 1
            val w = _uiState.value.sashWidth.toIntOrNull() ?: 1000
            
            val list = mutableListOf<Double>()
            if (newCount > 0) {
                val step = w.toDouble() / (newCount + 1)
                for (i in 1..newCount) {
                    list.add(step * i)
                }
            }
            
            _uiState.value = _uiState.value.copy(verticalMuntins = list)
            calculate()
        }
    }
    
    fun addHorizontalMuntin() {
        val h = _uiState.value.sashHeight.toIntOrNull() ?: 1000
        val currentCount = _uiState.value.horizontalMuntins.size
        val newCount = currentCount + 1
        
        val list = mutableListOf<Double>()
        val step = h.toDouble() / (newCount + 1)
        for (i in 1..newCount) {
            list.add(step * i)
        }
        
        _uiState.value = _uiState.value.copy(horizontalMuntins = list)
        calculate()
    }
    
    fun removeHorizontalMuntin(index: Int) {
        val currentCount = _uiState.value.horizontalMuntins.size
        if (currentCount > 0) {
            val newCount = currentCount - 1
            val h = _uiState.value.sashHeight.toIntOrNull() ?: 1000
            
            val list = mutableListOf<Double>()
            if (newCount > 0) {
                val step = h.toDouble() / (newCount + 1)
                for (i in 1..newCount) {
                    list.add(step * i)
                }
            }
            
            _uiState.value = _uiState.value.copy(horizontalMuntins = list)
            calculate()
        }
    }

    fun setQuickGrid(verticalLines: Int, horizontalLines: Int) {
        val w = _uiState.value.sashWidth.toIntOrNull() ?: 1000
        val h = _uiState.value.sashHeight.toIntOrNull() ?: 1000
        
        val vList = mutableListOf<Double>()
        if (verticalLines > 0) {
            val step = w.toDouble() / (verticalLines + 1)
            for (i in 1..verticalLines) {
                vList.add(step * i)
            }
        }
        
        val hList = mutableListOf<Double>()
        if (horizontalLines > 0) {
            val step = h.toDouble() / (horizontalLines + 1)
            for (i in 1..horizontalLines) {
                hList.add(step * i)
            }
        }
        
        _uiState.value = _uiState.value.copy(
            verticalMuntins = vList,
            horizontalMuntins = hList
        )
        calculate()
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

    fun setArchPattern(active: Boolean, withRays: Boolean = false) {
        if (active) {
            val w = _uiState.value.sashWidth.toIntOrNull() ?: 1000
            val frame = 82 + 20 
            
            val arch = ArchPatternV2(
                radiusMm = (w - 2*frame) / 2.0,
                arcStartDeg = 180.0,
                arcEndDeg = 360.0,
                divisionCount = if (withRays) 3 else 0,
                withStraightJoins = true
            )
            _uiState.value = _uiState.value.copy(archPattern = arch)
        } else {
            _uiState.value = _uiState.value.copy(archPattern = null)
        }
        calculate()
    }

    // --- Config Management ---
    fun refreshConfig() {
        viewModelScope.launch {
            configRepository.forceReloadAndSync()
        }
    }

    fun addProfile(profile: ProfileDefinition) {
        viewModelScope.launch {
            configRepository.addProfile(profile)
        }
    }

    fun updateProfile(profile: ProfileDefinition) {
        viewModelScope.launch {
            configRepository.updateProfile(profile)
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            configRepository.deleteProfile(id)
        }
    }

    fun addProfile(
        type: String,
        code: String,
        height: Int,
        width: Int,
        beadHeight: Int,
        beadAngle: Double
    ) {
        viewModelScope.launch {
            val profile = ProfileDefinition(
                code = code,
                type = type,
                heightMm = height,
                widthMm = width,
                beadHeightMm = beadHeight,
                beadAngle = beadAngle,
                system = "Custom", // Default or add input
                description = "Custom $type $code"
            )
            configRepository.addProfile(profile)
        }
    }

    // --- Optimization ---
    fun runOptimization(stockLengthMm: Double, sawWidthMm: Double) {
        val cutList = _uiState.value.cutList
        if (cutList.isNotEmpty()) {
            val result = CuttingOptimizer.optimize(
                items = cutList,
                stockLengthMm = stockLengthMm,
                sawWidthMm = sawWidthMm
            )
            _uiState.value = _uiState.value.copy(optimizationResult = result)
        }
    }

    private fun calculate() {
        val s = _uiState.value
        val w = s.sashWidth.toIntOrNull() ?: 1000
        val h = s.sashHeight.toIntOrNull() ?: 1000
        
        val sList = _sashProfiles.value
        val bList = _beadProfiles.value
        val mList = _muntinProfiles.value

        if (sList.isEmpty() || bList.isEmpty() || mList.isEmpty()) return

        val sashP = sList.getOrElse(s.selectedSashProfileIndex) { sList.first() }
        val beadP = bList.getOrElse(s.selectedBeadProfileIndex) { bList.first() }
        val muntinP = mList.getOrElse(s.selectedMuntinProfileIndex) { mList.first() }

        
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

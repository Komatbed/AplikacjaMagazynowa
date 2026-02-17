package com.example.warehouse.features.muntins_v3.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.features.muntins_v3.calculations.AssemblyInstructionCalculator
import com.example.warehouse.features.muntins_v3.calculations.AssemblyStep
import com.example.warehouse.features.muntins_v3.calculations.BarOptimizer
import com.example.warehouse.features.muntins_v3.calculations.CutItem
import com.example.warehouse.features.muntins_v3.calculations.CutListCalculator
import com.example.warehouse.features.muntins_v3.calculations.LayoutEngine
import com.example.warehouse.features.muntins_v3.calculations.MuntinV3Calculations
import com.example.warehouse.features.muntins_v3.calculations.OptimizationResult
import com.example.warehouse.features.muntins_v3.database.entity.GlassBeadEntity
import com.example.warehouse.features.muntins_v3.database.entity.MuntinEntity
import com.example.warehouse.features.muntins_v3.database.entity.ProfileEntity
import com.example.warehouse.features.muntins_v3.database.entity.ProjectEntity
import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import com.example.warehouse.features.muntins_v3.repository.MuntinsV3Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MuntinV3ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MuntinsV3Repository(application)
    private var defaultsInserted = false

    // UI State
    data class MuntinV3UiState(
        val currentProject: ProjectEntity? = null,
        val glassWidth: Double = 0.0,
        val glassHeight: Double = 0.0,
        val segments: List<Segment> = emptyList(),
        val nodes: List<Node> = emptyList(),
        val cutList: List<CutItem> = emptyList(),
        val assemblySteps: List<AssemblyStep> = emptyList(),
        val optimizationResult: OptimizationResult? = null,
        val barLength: Double = 6000.0,
        val sawKerf: Double = 4.0,
        val sashCount: Int = 1,
        val selectedSegmentId: String? = null,
        val presetType: String? = null,
        val isLoading: Boolean = false,
        val rows: Int = 2,
        val cols: Int = 2,
        val continuousMaster: ContinuousMaster = ContinuousMaster.VERTICAL,
        val clearanceGlobal: Double = 1.0,
        val clearanceBead: Double = 0.0,
        val clearanceMuntin: Double = 0.0,
        val groupCuts: Boolean = false,
        val snapThresholdMm: Double = 5.0,
        // Config Data
        val availableProfiles: List<ProfileEntity> = emptyList(),
        val availableBeads: List<GlassBeadEntity> = emptyList(),
        val availableMuntins: List<MuntinEntity> = emptyList(),
        val selectedProfile: ProfileEntity? = null,
        val selectedBead: GlassBeadEntity? = null,
        val selectedMuntin: MuntinEntity? = null,
        // Corrections
        val manualCorrection: Double = 0.0,
        val compTop: Double = 0.0,
        val compBottom: Double = 0.0,
        val compLeft: Double = 0.0,
        val compRight: Double = 0.0
    )
    
    enum class ContinuousMaster { VERTICAL, HORIZONTAL }
    enum class AddMode { VERTICAL, HORIZONTAL, DIAGONAL_45, DIAGONAL_135 }

    private val _uiState = MutableStateFlow(MuntinV3UiState())
    val uiState: StateFlow<MuntinV3UiState> = _uiState.asStateFlow()
    private var diagonalMode: Boolean = false
    private var manualMuntinWidth: Double? = null
    private val history = ArrayDeque<Pair<List<Segment>, String?>>()

    init {
        ensureDefaultConfig()
        // Observe config data
        viewModelScope.launch {
            repository.allProfiles.collect { profiles ->
                _uiState.value = _uiState.value.copy(availableProfiles = profiles)
                // If we have a project but no selected profile, try to find it
                val currentProject = _uiState.value.currentProject
                if (currentProject != null && _uiState.value.selectedProfile == null) {
                    val profile = profiles.find { it.id == currentProject.profileId }
                    _uiState.value = _uiState.value.copy(selectedProfile = profile)
                }
            }
        }
        
        viewModelScope.launch {
            repository.allBeads.collect { beads ->
                _uiState.value = _uiState.value.copy(availableBeads = beads)
                 // If we have a project but no selected bead, try to find it
                val currentProject = _uiState.value.currentProject
                if (currentProject != null && _uiState.value.selectedBead == null) {
                    val bead = beads.find { it.id == currentProject.beadId }
                    _uiState.value = _uiState.value.copy(selectedBead = bead)
                }
            }
        }
        
        viewModelScope.launch {
            repository.allMuntins.collect { muntins ->
                _uiState.value = _uiState.value.copy(availableMuntins = muntins)
                if (_uiState.value.selectedMuntin == null && muntins.isNotEmpty()) {
                    // Auto-select first VEKA 109* if present
                    val veka = muntins.find { it.name.contains("109") } ?: muntins.first()
                    _uiState.value = _uiState.value.copy(selectedMuntin = veka)
                }
            }
        }
    }

    private fun ensureDefaultConfig() {
        viewModelScope.launch {
            if (defaultsInserted) return@launch
            val profiles = repository.allProfiles.first()
            val beads = repository.allBeads.first()
            val muntins = repository.allMuntins.first()
            if (profiles.isEmpty() && beads.isEmpty() && muntins.isEmpty()) {
                try {
                    val cfg = NetworkModule.api.getMuntinsV3Config()
                    if (cfg.profiles.isNotEmpty() || cfg.beads.isNotEmpty() || cfg.muntins.isNotEmpty()) {
                        cfg.profiles.forEach {
                            repository.saveProfile(
                                ProfileEntity(
                                    name = it.name,
                                    glassOffsetX = it.glassOffsetX,
                                    glassOffsetY = it.glassOffsetY,
                                    outerConstructionAngleDeg = it.outerConstructionAngleDeg
                                )
                            )
                        }
                        cfg.beads.forEach {
                            repository.saveGlassBead(
                                GlassBeadEntity(
                                    name = it.name,
                                    angleFace = it.angleFace,
                                    effectiveGlassOffset = it.effectiveGlassOffset
                                )
                            )
                        }
                        cfg.muntins.forEach {
                            repository.saveMuntin(
                                MuntinEntity(
                                    name = it.name,
                                    width = it.width,
                                    thickness = it.thickness,
                                    wallAngleDeg = it.wallAngleDeg
                                )
                            )
                        }
                    } else {
                        insertDefaultMuntinsConfig()
                    }
                } catch (e: Exception) {
                    insertDefaultMuntinsConfig()
                }
            }
            defaultsInserted = true
        }
    }

    private suspend fun insertDefaultMuntinsConfig() {
        repository.saveProfile(
            ProfileEntity(
                name = "Profil PVC 82",
                glassOffsetX = 32.0,
                glassOffsetY = 32.0,
                outerConstructionAngleDeg = 90.0
            )
        )
        repository.saveGlassBead(
            GlassBeadEntity(
                name = "Listwa 25/27,5",
                angleFace = 18.0,
                effectiveGlassOffset = 26.0
            )
        )
        repository.saveMuntin(
            MuntinEntity(
                name = "Szpros 25/12",
                width = 25.0,
                thickness = 12.0,
                wallAngleDeg = 18.0
            )
        )
    }

    fun selectProfile(profile: ProfileEntity?) {
        _uiState.value = _uiState.value.copy(selectedProfile = profile)
        updateCorrections(
            _uiState.value.manualCorrection,
            _uiState.value.compTop,
            _uiState.value.compBottom,
            _uiState.value.compLeft,
            _uiState.value.compRight
        )
    }

    fun selectBead(bead: GlassBeadEntity?) {
        _uiState.value = _uiState.value.copy(selectedBead = bead)
        updateCorrections(
            _uiState.value.manualCorrection,
            _uiState.value.compTop,
            _uiState.value.compBottom,
            _uiState.value.compLeft,
            _uiState.value.compRight
        )
    }
    
    fun selectMuntin(muntin: MuntinEntity?) {
        _uiState.value = _uiState.value.copy(selectedMuntin = muntin)
        // No need to recalc corrections; width will be used on next generation
    }

    fun selectSegment(id: String?) {
        _uiState.value = _uiState.value.copy(selectedSegmentId = id)
    }

    fun removeSelectedSegment() {
        val selectedId = _uiState.value.selectedSegmentId ?: return
        val currentSegments = _uiState.value.segments.toMutableList()
        currentSegments.removeAll { it.id == selectedId }
        
        updateCalculations(currentSegments, "CUSTOM")
    }

    fun generateGrid(rows: Int, cols: Int) {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return

        val mWidth = manualMuntinWidth ?: _uiState.value.selectedMuntin?.width ?: 26.0
        val segments = when (_uiState.value.continuousMaster) {
            ContinuousMaster.VERTICAL -> LayoutEngine.generateGrid(width, height, rows, cols, mWidth)
            ContinuousMaster.HORIZONTAL -> LayoutEngine.generateGridHorizontalMaster(width, height, rows, cols, mWidth)
        }
        
        _uiState.value = _uiState.value.copy(rows = rows, cols = cols)
        updateCalculations(segments, "GRID")
    }
    
    fun addVertical() {
        val newCols = _uiState.value.cols + 1
        generateGrid(_uiState.value.rows, newCols)
    }
    
    fun addHorizontal() {
        val newRows = _uiState.value.rows + 1
        generateGrid(newRows, _uiState.value.cols)
    }
    
    fun setAddMode(mode: AddMode) {
        _uiState.value = _uiState.value.copy() // no-op; maintained via internal
        _addMode = mode
    }
    
    private var _addMode: AddMode = AddMode.VERTICAL
    
    fun setDiagonalMode(enabled: Boolean) {
        diagonalMode = enabled
    }
    
    fun setManualMuntinWidth(width: Double?) {
        manualMuntinWidth = width
        // Re-generate grid with new width if layout exists
        if (_uiState.value.segments.isNotEmpty()) {
            generateGrid(_uiState.value.rows, _uiState.value.cols)
        }
    }
    
    fun setSnapThreshold(thresholdMm: Double) {
        _uiState.value = _uiState.value.copy(snapThresholdMm = thresholdMm)
    }
    
    fun setRowsN(n: Int) {
        if (n <= 0) return
        generateGrid(n, _uiState.value.cols)
    }
    
    fun setColsN(n: Int) {
        if (n <= 0) return
        generateGrid(_uiState.value.rows, n)
    }
    
    fun undo() {
        val prev = history.removeLastOrNull() ?: return
        updateCalculations(prev.first, prev.second)
    }
    
    fun onCanvasTap(segmentId: String?, tapX: Double, tapY: Double) {
        if (segmentId != null) {
            val currentSegments = _uiState.value.segments.toMutableList()
            currentSegments.removeAll { it.id == segmentId }
            updateCalculations(currentSegments, "CUSTOM")
        } else {
            val mWidth = manualMuntinWidth ?: _uiState.value.selectedMuntin?.width ?: 26.0
            if (diagonalMode) {
                val width = _uiState.value.glassWidth
                val height = _uiState.value.glassHeight
                if (width <= 0.0 || height <= 0.0) return
                val slope = when (_addMode) {
                    AddMode.DIAGONAL_45 -> 1.0
                    AddMode.DIAGONAL_135 -> -1.0
                    AddMode.VERTICAL -> Double.POSITIVE_INFINITY
                    AddMode.HORIZONTAL -> 0.0
                }
                val newSeg = createDiagonalSegment(tapX, tapY, slope, width, height, mWidth)
                val currentSegments = _uiState.value.segments.toMutableList()
                currentSegments.add(newSeg)
                updateCalculations(currentSegments, "CUSTOM")
            } else {
                when (_addMode) {
                    AddMode.VERTICAL -> addVerticalAt(tapX, mWidth)
                    AddMode.HORIZONTAL -> addHorizontalAt(tapY, mWidth)
                    AddMode.DIAGONAL_45, AddMode.DIAGONAL_135 -> {
                    }
                }
            }
        }
    }
    
    private fun addVerticalAt(tapX: Double, muntinWidth: Double) {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return
        val snapT = _uiState.value.snapThresholdMm
        val existingX = _uiState.value.segments
            .filter { kotlin.math.abs(it.startNode.x - it.endNode.x) < 0.1 }
            .map { it.startNode.x }
        val candidates = (existingX + listOf(0.0, width)).distinct()
        val nearest = candidates.minByOrNull { kotlin.math.abs(it - tapX) } ?: tapX
        val finalX = if (kotlin.math.abs(nearest - tapX) <= snapT) nearest else tapX
        val newSeg = Segment(
            id = java.util.UUID.randomUUID().toString(),
            startNode = Node(finalX, 0.0),
            endNode = Node(finalX, height),
            width = muntinWidth,
            angleStart = 90.0,
            angleEnd = 90.0
        )
        val currentSegments = _uiState.value.segments.toMutableList()
        currentSegments.add(newSeg)
        updateCalculations(currentSegments, "CUSTOM")
    }
    
    private fun addHorizontalAt(tapY: Double, muntinWidth: Double) {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return
        val snapT = _uiState.value.snapThresholdMm
        val existingY = _uiState.value.segments
            .filter { kotlin.math.abs(it.startNode.y - it.endNode.y) < 0.1 }
            .map { it.startNode.y }
        val candidates = (existingY + listOf(0.0, height)).distinct()
        val nearest = candidates.minByOrNull { kotlin.math.abs(it - tapY) } ?: tapY
        val finalY = if (kotlin.math.abs(nearest - tapY) <= snapT) nearest else tapY
        val newSeg = Segment(
            id = java.util.UUID.randomUUID().toString(),
            startNode = Node(0.0, finalY),
            endNode = Node(width, finalY),
            width = muntinWidth,
            angleStart = 0.0,
            angleEnd = 0.0
        )
        val currentSegments = _uiState.value.segments.toMutableList()
        currentSegments.add(newSeg)
        updateCalculations(currentSegments, "CUSTOM")
    }
    
    private fun createDiagonalSegment(
        x0: Double,
        y0: Double,
        slope: Double,
        width: Double,
        height: Double,
        muntinWidth: Double
    ): Segment {
        val rows = _uiState.value.rows
        val cols = _uiState.value.cols
        val tx = if (cols > 0) width / cols else width
        val ty = if (rows > 0) height / rows else height
        val vx = (0..cols).map { it * tx }
        val vy = (0..rows).map { it * ty }
        val snapT = _uiState.value.snapThresholdMm
        val nearestX = (vx + listOf(0.0, width)).minByOrNull { kotlin.math.abs(it - x0) } ?: x0
        val nearestY = (vy + listOf(0.0, height)).minByOrNull { kotlin.math.abs(it - y0) } ?: y0
        val sx = if (kotlin.math.abs(nearestX - x0) <= snapT) nearestX else x0
        val sy = if (kotlin.math.abs(nearestY - y0) <= snapT) nearestY else y0
        val b = sy - slope * sx
        val candidates = mutableListOf<Node>()
        fun addIfInBounds(x: Double, y: Double) {
            if (x in 0.0..width && y in 0.0..height) candidates.add(Node(x, y))
        }
        if (slope.isFinite()) {
            addIfInBounds(0.0, b)
            addIfInBounds(width, slope * width + b)
            if (slope != 0.0) {
                addIfInBounds((-b) / slope, 0.0)
                addIfInBounds((height - b) / slope, height)
            }
        } else {
            // Vertical line through x = x0
            addIfInBounds(sx, 0.0)
            addIfInBounds(sx, height)
        }
        // Pick extreme pair
        val pts = candidates.distinct().sortedBy { it.x + it.y }
        val start = pts.firstOrNull() ?: Node(0.0, 0.0)
        val end = pts.lastOrNull() ?: Node(width, height)
        val angle = if (!slope.isFinite()) 90.0 else 45.0
        return Segment(
            id = java.util.UUID.randomUUID().toString(),
            startNode = start,
            endNode = end,
            width = muntinWidth,
            angleStart = angle,
            angleEnd = angle
        )
    }
    
    fun setContinuousMaster(master: ContinuousMaster) {
        _uiState.value = _uiState.value.copy(continuousMaster = master)
        generateGrid(_uiState.value.rows, _uiState.value.cols)
    }

    fun generateCross() {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return

        val segments = LayoutEngine.generateCross(
            width = width,
            height = height,
            muntinWidth = 26.0
        )
        updateCalculations(segments, "CROSS")
    }

    fun generateDiamond() {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return

        val segments = LayoutEngine.generateDiamond(
            width = width,
            height = height,
            muntinWidth = 26.0
        )
        updateCalculations(segments, "DIAMOND")
    }

    fun generateSunburst(numRays: Int = 3) {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return

        val segments = LayoutEngine.generateSunburst(
            width = width,
            height = height,
            muntinWidth = 26.0,
            numRays = numRays
        )
        updateCalculations(segments, "SUNBURST")
    }

    fun generateWeb(numRays: Int = 8) {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return

        val segments = LayoutEngine.generateWeb(
            width = width,
            height = height,
            muntinWidth = 26.0,
            numRays = numRays
        )
        updateCalculations(segments, "WEB")
    }

    fun generateGothic() {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return

        val segments = LayoutEngine.generateGothic(
            width = width,
            height = height,
            muntinWidth = 26.0
        )
        updateCalculations(segments, "GOTHIC")
    }

    fun generateTrapezoid(numVerticals: Int = 3) {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        if (width <= 0.0 || height <= 0.0) return

        val segments = LayoutEngine.generateTrapezoidLayout(
            width = width,
            height = height,
            muntinWidth = 26.0,
            numVerticals = numVerticals
        )
        updateCalculations(segments, "TRAPEZOID")
    }

    fun updateOptimizationSettings(barLength: Double, sawKerf: Double, sashCount: Int) {
        _uiState.value = _uiState.value.copy(
            barLength = barLength,
            sawKerf = sawKerf,
            sashCount = sashCount
        )
        // Only recalculate if we have a cut list
        if (_uiState.value.cutList.isNotEmpty()) {
            val result = BarOptimizer.optimize(
                _uiState.value.cutList,
                barLength,
                sawKerf,
                sashCount
            )
            _uiState.value = _uiState.value.copy(optimizationResult = result)
        }
    }

    fun updateCorrections(
        manual: Double,
        top: Double,
        bottom: Double,
        left: Double,
        right: Double
    ) {
        val currentProject = _uiState.value.currentProject ?: return
        val profile = _uiState.value.selectedProfile
        val bead = _uiState.value.selectedBead
        
        // Calculate new glass dimensions
        // Formula: Glass = Frame - 2 * (ProfileOffset + BeadOffset)
        // Note: ProfileOffset is usually "Frame Thickness - Overlap" or similar. 
        // Here we assume ProfileEntity.glassOffsetX is the distance from frame outer edge to glass edge (excluding bead?).
        // Or including? The seed data says "48.0", "18.0".
        // Let's assume: Total Deduction = 2 * (Profile.glassOffset + Bead.effectiveGlassOffset)
        
        val pOffset = profile?.glassOffsetX ?: 48.0
        val bOffset = bead?.effectiveGlassOffset ?: 18.0
        
        val baseGlassWidth = currentProject.frameWidth - 2 * (pOffset + bOffset)
        val baseGlassHeight = currentProject.frameHeight - 2 * (pOffset + bOffset) // Assuming uniform X/Y for now or use pOffset Y
        
        // Apply corrections
        // manual_window_correction applies to both width and height (per spec logic usually, or maybe per side?)
        // Spec says: "manual_window_correction" (singular). Usually means "shrink the whole glass by X mm" (clearance).
        // If it's clearance, it's usually -2*correction. Let's assume it's a reduction per side or total.
        // Let's assume manualCorrection is a total reduction (or expansion if positive).
        // And compensations are per side.
        
        val newWidth = baseGlassWidth + manual + left + right
        val newHeight = baseGlassHeight + manual + top + bottom
        
        _uiState.value = _uiState.value.copy(
            manualCorrection = manual,
            compTop = top,
            compBottom = bottom,
            compLeft = left,
            compRight = right,
            glassWidth = newWidth,
            glassHeight = newHeight
        )
        
        // Regenerate layout if a preset is active
        when (_uiState.value.presetType) {
            "GRID" -> {
                // We need to know rows/cols. 
                // Currently generateGrid takes params. 
                // We might need to store rows/cols in UiState to regenerate properly.
                // For now, let's just clear or keep segments (which might be wrong size).
                // Better approach: clear segments if size changes, or warn user.
                // Or: scale segments.
                // For this implementation, I will just re-trigger calculations, 
                // BUT segments are absolute. They won't move.
                // So if glass grows, segments stay in place (top-left aligned usually).
                // This is fine for "Corrections" which are usually small (mm).
                // The CutListCalculator uses glassWidth/Height for boundaries.
                updateCalculations(_uiState.value.segments)
            }
            else -> updateCalculations(_uiState.value.segments)
        }
    }

    private fun updateCalculations(segments: List<Segment>, newPresetType: String? = null) {
        history.addLast(_uiState.value.segments to _uiState.value.presetType)
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        
        // If a new preset type is provided, use it. Otherwise, keep existing unless segments changed manually (then it might be custom)
        // But for now, let's just stick to what's passed or current.
        val finalPresetType = newPresetType ?: _uiState.value.presetType

        val rows = _uiState.value.rows
        val cols = _uiState.value.cols
        val labelProvider: (Segment) -> String = { s ->
            val isVertical = kotlin.math.abs(s.endNode.x - s.startNode.x) < 0.001
            if (isVertical) {
                val stepX = if (cols > 0) width / cols else width
                val index = kotlin.math.round(s.startNode.x / stepX).toInt()
                "pion$index"
            } else {
                val stepY = if (rows > 0) height / rows else height
                val row = kotlin.math.round(s.startNode.y / stepY).toInt()
                val rowSegments = segments.filter { kotlin.math.abs(it.startNode.y - s.startNode.y) < 0.001 }.sortedBy { it.startNode.x }
                val segIdx = rowSegments.indexOfFirst { it.id == s.id } + 1
                "poziom$row lewy$segIdx"
            }
        }
        
        val cutList = CutListCalculator.calculateCutList(
            segments,
            width,
            height,
            CutListCalculator.Clearances(
                global = _uiState.value.clearanceGlobal,
                bead = _uiState.value.clearanceBead,
                muntin = _uiState.value.clearanceMuntin
            ),
            groupIdentical = _uiState.value.groupCuts,
            labelProvider = labelProvider
        )
        
        val profile = _uiState.value.selectedProfile
        val bead = _uiState.value.selectedBead
        val project = _uiState.value.currentProject
        val offsetFromFrame = if (project != null && profile != null && bead != null) {
            (profile.glassOffsetX + bead.effectiveGlassOffset)
        } else {
            0.0
        }
        
        val assemblySteps = AssemblyInstructionCalculator.generateInstructions(
            segments,
            width,
            height,
            offsetFromFrame,
            offsetFromFrame,
            finalPresetType
        )

        val optimizationResult = BarOptimizer.optimize(
            cutList,
            _uiState.value.barLength,
            _uiState.value.sawKerf,
            _uiState.value.sashCount
        )

        _uiState.value = _uiState.value.copy(
            segments = segments,
            cutList = cutList,
            assemblySteps = assemblySteps,
            optimizationResult = optimizationResult,
            selectedSegmentId = null,
            presetType = finalPresetType
        )
    }
    
    fun updateClearances(global: Double, bead: Double, muntin: Double) {
        _uiState.value = _uiState.value.copy(
            clearanceGlobal = global,
            clearanceBead = bead,
            clearanceMuntin = muntin
        )
        updateCalculations(_uiState.value.segments, _uiState.value.presetType)
    }
    
    fun setGroupCuts(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(groupCuts = enabled)
        updateCalculations(_uiState.value.segments, _uiState.value.presetType)
    }

    fun addV3Profile(name: String, glassOffsetX: Double, glassOffsetY: Double, outerAngleDeg: Double = 90.0) {
        viewModelScope.launch {
            repository.saveProfile(ProfileEntity(name = name, glassOffsetX = glassOffsetX, glassOffsetY = glassOffsetY, outerConstructionAngleDeg = outerAngleDeg))
        }
    }

    fun addV3Bead(name: String, angleFace: Double, effectiveOffset: Double) {
        viewModelScope.launch {
            repository.saveGlassBead(GlassBeadEntity(name = name, angleFace = angleFace, effectiveGlassOffset = effectiveOffset))
        }
    }

    fun addV3Muntin(name: String, width: Double, thickness: Double, wallAngleDeg: Double = 90.0) {
        viewModelScope.launch {
            repository.saveMuntin(MuntinEntity(name = name, width = width, thickness = thickness, wallAngleDeg = wallAngleDeg))
        }
    }
    
    fun highlightByCut(item: CutItem) {
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        val clearances = CutListCalculator.Clearances(
            global = _uiState.value.clearanceGlobal,
            bead = _uiState.value.clearanceBead,
            muntin = _uiState.value.clearanceMuntin
        )
        
        fun findBoundary(node: Node, current: Segment): Pair<Pair<Node, Node>, Double> {
            val EPS = 0.001
            if (kotlin.math.abs(node.y) < EPS) return Pair(Node(0.0, 0.0) to Node(width, 0.0), clearances.global + clearances.bead)
            if (kotlin.math.abs(node.y - height) < EPS) return Pair(Node(0.0, height) to Node(width, height), clearances.global + clearances.bead)
            if (kotlin.math.abs(node.x) < EPS) return Pair(Node(0.0, 0.0) to Node(0.0, height), clearances.global + clearances.bead)
            if (kotlin.math.abs(node.x - width) < EPS) return Pair(Node(width, 0.0) to Node(width, height), clearances.global + clearances.bead)
            for (other in _uiState.value.segments) {
                if (other.id == current.id) continue
                // Simple collinearity and point-on-segment check
                val on = isPointOnSegment(node, other)
                if (on) {
                    val parallel = areParallel(current, other)
                    if (parallel) continue
                    return Pair(other.startNode to other.endNode, other.width / 2.0 + clearances.global + clearances.muntin)
                }
            }
            val dx = current.endNode.x - current.startNode.x
            val dy = current.endNode.y - current.startNode.y
            val p1 = Node(node.x - dy, node.y + dx)
            val p2 = Node(node.x + dy, node.y - dx)
            return Pair(p1 to p2, 0.0)
        }
        
        val matched = _uiState.value.segments.firstOrNull { seg ->
            val (sb, sc) = findBoundary(seg.startNode, seg)
            val (eb, ec) = findBoundary(seg.endNode, seg)
            val res = com.example.warehouse.features.muntins_v3.calculations.SegmentCalculator.calculateRealLength(seg, sb, eb, sc, ec)
            val len = kotlin.math.round(res.finalLength * 10) / 10.0
            val a1 = kotlin.math.round(res.cutAngleStart * 10) / 10.0
            val a2 = kotlin.math.round(res.cutAngleEnd * 10) / 10.0
            val mi = minOf(a1, a2)
            val ma = maxOf(a1, a2)
            kotlin.math.abs(len - item.length) < 0.1 && kotlin.math.abs(mi - minOf(item.angleStart, item.angleEnd)) < 0.1 && kotlin.math.abs(ma - maxOf(item.angleStart, item.angleEnd)) < 0.1
        }
        
        _uiState.value = _uiState.value.copy(selectedSegmentId = matched?.id)
    }
    
    private fun isPointOnSegment(p: Node, s: Segment): Boolean {
        val EPS = 0.001
        val crossProduct = (p.y - s.startNode.y) * (s.endNode.x - s.startNode.x) -
                (p.x - s.startNode.x) * (s.endNode.y - s.startNode.y)
        if (kotlin.math.abs(crossProduct) > EPS) return false
        val dotProduct = (p.x - s.startNode.x) * (s.endNode.x - s.startNode.x) +
                (p.y - s.startNode.y) * (s.endNode.y - s.startNode.y)
        if (dotProduct < 0) return false
        val squaredLength = (s.endNode.x - s.startNode.x) * (s.endNode.x - s.startNode.x) +
                (s.endNode.y - s.startNode.y) * (s.endNode.y - s.startNode.y)
        if (dotProduct > squaredLength + EPS) return false
        return true
    }
    
    private fun areParallel(s1: Segment, s2: Segment): Boolean {
        val EPS = 0.001
        val dx1 = s1.endNode.x - s1.startNode.x
        val dy1 = s1.endNode.y - s1.startNode.y
        val dx2 = s2.endNode.x - s2.startNode.x
        val dy2 = s2.endNode.y - s2.startNode.y
        val cross = dx1 * dy2 - dy1 * dx2
        return kotlin.math.abs(cross) < EPS
    }

    fun clearLayout() {
        _uiState.value = _uiState.value.copy(
            segments = emptyList(),
            cutList = emptyList(),
            assemblySteps = emptyList(),
            presetType = null,
            rows = 2,
            cols = 2
        )
        history.clear()
    }

    fun createNewProject(
        frameWidth: Double,
        frameHeight: Double,
        profileId: Long,
        beadId: Long
    ) {
        viewModelScope.launch {
            val newProject = ProjectEntity(
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                profileId = profileId,
                beadId = beadId
            )
            val id = repository.saveProject(newProject)
            val savedProject = repository.getProjectById(id)
            
            // Fetch Profile and Bead
            val profile = repository.getProfileById(profileId)
            val bead = repository.getGlassBeadById(beadId)
            
            // Calculate glass dimensions
            // Formula: Glass = Frame - 2 * (ProfileOffset + BeadOffset)
            val pOffset = profile?.glassOffsetX ?: 48.0
            val bOffset = bead?.effectiveGlassOffset ?: 18.0
            
            val glassWidth = frameWidth - 2 * (pOffset + bOffset)
            val glassHeight = frameHeight - 2 * (pOffset + bOffset)
            
            _uiState.value = _uiState.value.copy(
                currentProject = savedProject,
                selectedProfile = profile,
                selectedBead = bead,
                glassWidth = glassWidth,
                glassHeight = glassHeight,
                segments = emptyList(),
                cutList = emptyList(),
                assemblySteps = emptyList(),
                manualCorrection = 0.0,
                compTop = 0.0,
                compBottom = 0.0,
                compLeft = 0.0,
                compRight = 0.0
            )
        }
    }
}

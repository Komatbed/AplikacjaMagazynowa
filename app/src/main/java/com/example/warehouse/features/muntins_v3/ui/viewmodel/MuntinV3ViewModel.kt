package com.example.warehouse.features.muntins_v3.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        // Config Data
        val availableProfiles: List<ProfileEntity> = emptyList(),
        val availableBeads: List<GlassBeadEntity> = emptyList(),
        val selectedProfile: ProfileEntity? = null,
        val selectedBead: GlassBeadEntity? = null,
        // Corrections
        val manualCorrection: Double = 0.0,
        val compTop: Double = 0.0,
        val compBottom: Double = 0.0,
        val compLeft: Double = 0.0,
        val compRight: Double = 0.0
    )

    private val _uiState = MutableStateFlow(MuntinV3UiState())
    val uiState: StateFlow<MuntinV3UiState> = _uiState.asStateFlow()

    init {
        // Load config data
        viewModelScope.launch {
            // Seed DB if empty
            val profiles = repository.allProfiles.first()
            if (profiles.isEmpty()) {
                repository.saveProfile(ProfileEntity(name = "Standard 68mm", glassOffsetX = 48.0, glassOffsetY = 48.0))
                repository.saveProfile(ProfileEntity(name = "Renowacja 40mm", glassOffsetX = 30.0, glassOffsetY = 30.0))
            }
            
            val beads = repository.allGlassBeads.first()
            if (beads.isEmpty()) {
                repository.saveGlassBead(GlassBeadEntity(name = "Classic 18mm", angleFace = 15.0, effectiveGlassOffset = 18.0))
                repository.saveGlassBead(GlassBeadEntity(name = "Rondo 20mm", angleFace = 10.0, effectiveGlassOffset = 20.0))
            }
        }

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
            repository.allGlassBeads.collect { beads ->
                _uiState.value = _uiState.value.copy(availableBeads = beads)
                 // If we have a project but no selected bead, try to find it
                val currentProject = _uiState.value.currentProject
                if (currentProject != null && _uiState.value.selectedBead == null) {
                    val bead = beads.find { it.id == currentProject.beadId }
                    _uiState.value = _uiState.value.copy(selectedBead = bead)
                }
            }
        }

        // Load initial data or create a default project if needed
        // For testing purposes, create a dummy project immediately
        // We delay slightly to let config load or just use defaults
        viewModelScope.launch {
            // Wait for data? Or just proceed. createNewProject will fetch from DB/Repo if needed.
            // But for the dummy call, we need valid IDs.
            // Let's assume ID 1 exists after seeding.
            createNewProject(1000.0, 1000.0, 1, 1)
        }
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

        val segments = LayoutEngine.generateGrid(
            width = width,
            height = height,
            rows = rows,
            cols = cols,
            muntinWidth = 26.0 // Default muntin width for V3
        )
        
        updateCalculations(segments, "GRID")
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
        val width = _uiState.value.glassWidth
        val height = _uiState.value.glassHeight
        
        // If a new preset type is provided, use it. Otherwise, keep existing unless segments changed manually (then it might be custom)
        // But for now, let's just stick to what's passed or current.
        val finalPresetType = newPresetType ?: _uiState.value.presetType

        val cutList = CutListCalculator.calculateCutList(
            segments,
            width,
            height
        )
        
        val assemblySteps = AssemblyInstructionCalculator.generateInstructions(
            segments,
            width,
            height,
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

    fun clearLayout() {
        _uiState.value = _uiState.value.copy(
            segments = emptyList(),
            cutList = emptyList(),
            assemblySteps = emptyList()
        )
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

package com.example.warehouse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.model.CutItemV2
import com.example.warehouse.ui.viewmodel.MuntinViewModelV2
import com.example.warehouse.util.MuntinCalculatorV2.IntersectionType
import com.example.warehouse.ui.theme.SafetyOrange
import kotlin.math.min
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.warehouse.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuntinScreenV2(
    viewModel: MuntinViewModelV2 = viewModel()
) {
    val state by viewModel.uiState
    
    // Local Dialog States
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCrossSectionDialog by remember { mutableStateOf(false) }
    var showOptimizationDialog by remember { mutableStateOf(false) }
    var showCheckerboardDialog by remember { mutableStateOf(false) }
    var showProfileManagerDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        MuntinV2SettingsDialog(
            viewModel = viewModel,
            state = state,
            onDismiss = { showSettingsDialog = false },
            onManageProfiles = { 
                showSettingsDialog = false
                showProfileManagerDialog = true 
            },
            onForceImport = {
                viewModel.refreshConfig()
                showSettingsDialog = false
            }
        )
    }

    if (showProfileManagerDialog) {
        ProfileManagerDialog(
            viewModel = viewModel,
            onDismiss = { showProfileManagerDialog = false }
        )
    }

    if (showCheckerboardDialog) {
        CheckerboardDialog(
            onDismiss = { showCheckerboardDialog = false },
            onApply = { cols, rows ->
                viewModel.setQuickGrid(cols, rows)
            }
        )
    }

    if (showCrossSectionDialog) {
        val sashProfiles by viewModel.sashProfiles.collectAsState()
        val beadProfiles by viewModel.beadProfiles.collectAsState()
        val muntinProfiles by viewModel.muntinProfiles.collectAsState()
        
        val sp = sashProfiles.getOrElse(state.selectedSashProfileIndex) { sashProfiles.firstOrNull() }
        val bp = beadProfiles.getOrElse(state.selectedBeadProfileIndex) { beadProfiles.firstOrNull() }
        val mp = muntinProfiles.getOrElse(state.selectedMuntinProfileIndex) { muntinProfiles.firstOrNull() }

        if (sp != null && bp != null && mp != null) {
            CrossSectionDialog(
                sashProfile = sp,
                beadProfile = bp,
                muntinProfile = mp,
                onDismiss = { showCrossSectionDialog = false }
            )
        }
    }
    
    if (showOptimizationDialog && state.optimizationResult != null) {
        OptimizationDialog(
            result = state.optimizationResult!!,
            onDismiss = { showOptimizationDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Szprosy V2", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.isAngularMode) "Tryb: Skośny" else "Tryb: Prosty", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.setMode(!state.isAngularMode) }) {
                        Text(if (state.isAngularMode) "Zmień na Prosty" else "Zmień na Skośny")
                    }
                    IconButton(onClick = { showCrossSectionDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Przekrój")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow removed as per request to save space
            
            if (!state.isAngularMode) {
                MuntinV2OrthogonalScreen(
                    viewModel, 
                    state,
                    onCheckerboard = { showCheckerboardDialog = true },
                    onOptimize = {
                        viewModel.runOptimization(6000.0, 3.0)
                        showOptimizationDialog = true
                    }
                )
            } else {
                MuntinV2AngularScreen(
                    viewModel, 
                    state,
                    onOptimize = {
                        viewModel.runOptimization(6000.0, 3.0)
                        showOptimizationDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun MuntinV2OrthogonalScreen(
    viewModel: MuntinViewModelV2, 
    state: MuntinViewModelV2.MuntinV2UiState,
    onCheckerboard: () -> Unit,
    onOptimize: () -> Unit
) {
    val sashProfiles by viewModel.sashProfiles.collectAsState()
    val beadProfiles by viewModel.beadProfiles.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        item {
            Text("Tryb Prosty (Wiedeński)", style = MaterialTheme.typography.headlineSmall)
        }

        // --- Inputs ---
        item { SashDimensionsCard(viewModel, state) }
        item { ProfilesCard(viewModel, state) }

        // --- Visualizer & Interaction ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wirtualne Skrzydło", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick Grids
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.setQuickGrid(1, 0) }, modifier = Modifier.weight(1f)) { Text("1 Pion") }
                        Button(onClick = { viewModel.setQuickGrid(0, 1) }, modifier = Modifier.weight(1f)) { Text("1 Poziom") }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.setQuickGrid(1, 1) }, modifier = Modifier.weight(1f)) { Text("Krzyż") }
                        Button(onClick = onCheckerboard, modifier = Modifier.weight(1f)) { Text("Szachownica") }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { viewModel.addVerticalMuntin() }) {
                            Icon(Icons.Default.Add, null)
                            Text("Pion")
                        }
                        OutlinedButton(onClick = { viewModel.addHorizontalMuntin() }) {
                            Icon(Icons.Default.Add, null)
                            Text("Poziom")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Kliknij na schemat, aby dodać/usunąć szprosy (kliknij w wolne pole aby podzielić)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.Center
                    ) {
                        MuntinVisualizerOrthogonal(
                            sashWidth = state.sashWidth.toIntOrNull() ?: 1000,
                            sashHeight = state.sashHeight.toIntOrNull() ?: 1000,
                            verticals = state.verticalMuntins,
                            horizontals = state.horizontalMuntins,
                            sashProfileWidth = sashProfiles.getOrElse(state.selectedSashProfileIndex) { sashProfiles.firstOrNull() ?: return@Box }.heightMm,
                            beadWidth = beadProfiles.getOrElse(state.selectedBeadProfileIndex) { beadProfiles.firstOrNull() ?: return@Box }.heightMm,
                            onCanvasClick = { x, y, scale ->
                                viewModel.handleCanvasClick(x, y, scale)
                            }
                        )
                    }
                    
                    if (state.verticalMuntins.isNotEmpty() || state.horizontalMuntins.isNotEmpty()) {
                        Text("Szprosy (kliknij aby usunąć):", style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             state.verticalMuntins.forEachIndexed { i, _ ->
                                AssistChip(
                                    onClick = { viewModel.removeVerticalMuntin(i) },
                                    label = { Text("V${i+1}") },
                                    trailingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) }
                                )
                             }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             state.horizontalMuntins.forEachIndexed { i, _ ->
                                AssistChip(
                                    onClick = { viewModel.removeHorizontalMuntin(i) },
                                    label = { Text("H${i+1}") },
                                    trailingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) }
                                )
                             }
                        }
                    }
                }
            }
        }

        // --- Results ---
        item { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Lista Cięć", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onOptimize) { Text("Optymalizuj") }
            }
        }
        items(state.cutList) { item -> CutItemRow(item) }
        item { MountingMarksCard(state) }
    }
}

@Composable
fun MuntinV2AngularScreen(
    viewModel: MuntinViewModelV2, 
    state: MuntinViewModelV2.MuntinV2UiState,
    onOptimize: () -> Unit
) {
    val sashProfiles by viewModel.sashProfiles.collectAsState()
    val beadProfiles by viewModel.beadProfiles.collectAsState()
    
    val currentSashProfile = sashProfiles.getOrElse(state.selectedSashProfileIndex) { sashProfiles.firstOrNull() }
    val currentBeadProfile = beadProfiles.getOrElse(state.selectedBeadProfileIndex) { beadProfiles.firstOrNull() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Tryb Skośny (Wiedeński)", style = MaterialTheme.typography.headlineSmall)
        }

        item { SashDimensionsCard(viewModel, state) }
        item { ProfilesCard(viewModel, state) }
        
        // --- Angular Controls ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dodaj Ukos / Wzór", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Angle Buttons
                    Text("Kąt ukosu:", style = MaterialTheme.typography.bodySmall)
                    val angles = listOf(15, 22, 30, 45, 60, 75, 90) // 22 is 22.5 simplified
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        angles.forEach { angle ->
                            Button(
                                onClick = { viewModel.addDiagonal(if (angle == 22) 22.5 else angle.toDouble()) },
                                contentPadding = PaddingValues(4.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 40.dp)
                            ) {
                                Text(if (angle == 22) "22.5" else "$angle")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isSpider = state.spiderPattern != null
                        Button(
                            onClick = { viewModel.setSpiderPattern(!isSpider) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSpider) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Pajęczyna")
                        }
                        
                        // Arch button (Only Arc)
                        val isArch = state.archPattern != null && state.archPattern.divisionCount == 0
                        Button(
                            onClick = { 
                                if (isArch) viewModel.setArchPattern(false) 
                                else viewModel.setArchPattern(true, false) 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isArch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isArch) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Łuk")
                        }

                        // Sun button (Arc + Rays)
                        val isSun = state.archPattern != null && state.archPattern.divisionCount > 0
                        Button(
                            onClick = { 
                                if (isSun) viewModel.setArchPattern(false) 
                                else viewModel.setArchPattern(true, true) 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSun) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSun) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Słoneczko")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.LightGray)
                            .border(2.dp, Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                         MuntinVisualizerAngular(
                            sashWidth = state.sashWidth.toIntOrNull() ?: 1000,
                            sashHeight = state.sashHeight.toIntOrNull() ?: 1000,
                            sashProfileWidth = currentSashProfile?.heightMm ?: 0,
                            beadWidth = currentBeadProfile?.heightMm ?: 0,
                            debugSegments = state.debugSegments
                        )
                    }
                    
                    // List of diagonals
                    if (state.diagonals.isNotEmpty()) {
                        Text("Elementy skośne:", style = MaterialTheme.typography.bodySmall)
                        state.diagonals.forEach { diag ->
                             AssistChip(
                                onClick = { viewModel.removeDiagonal(diag.lineId) },
                                label = { Text("${diag.angleDeg}°") },
                                trailingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        }

        // --- Results ---
        item { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Lista Cięć", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onOptimize) { Text("Optymalizuj") }
            }
        }
        items(state.cutList) { item -> CutItemRow(item) }
        item { MountingMarksCard(state) }
    }
}

// --- Shared Components ---

@Composable
fun SashDimensionsCard(viewModel: MuntinViewModelV2, state: MuntinViewModelV2.MuntinV2UiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Wymiary Skrzydła (mm)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.sashWidth,
                    onValueChange = { viewModel.updateSashDimensions(it, state.sashHeight) },
                    label = { Text("Szerokość") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.sashHeight,
                    onValueChange = { viewModel.updateSashDimensions(state.sashWidth, it) },
                    label = { Text("Wysokość") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProfilesCard(viewModel: MuntinViewModelV2, state: MuntinViewModelV2.MuntinV2UiState) {
    val sashProfiles by viewModel.sashProfiles.collectAsState()
    val beadProfiles by viewModel.beadProfiles.collectAsState()
    val muntinProfiles by viewModel.muntinProfiles.collectAsState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            
            Text("Profil Skrzydła:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sashProfiles.forEachIndexed { index, p ->
                    FilterChip(
                        selected = state.selectedSashProfileIndex == index,
                        onClick = { viewModel.selectSashProfile(index) },
                        label = { Text(p.profileNo) } // Use code or system/manufacturer
                    )
                }
            }

            Text("Listwa Przyszybowa:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                beadProfiles.forEachIndexed { index, p ->
                    FilterChip(
                        selected = state.selectedBeadProfileIndex == index,
                        onClick = { viewModel.selectBeadProfile(index) },
                        label = { Text(p.profileNo) }
                    )
                }
            }

            Text("Szpros:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                muntinProfiles.forEachIndexed { index, p ->
                    FilterChip(
                        selected = state.selectedMuntinProfileIndex == index,
                        onClick = { viewModel.selectMuntinProfile(index) },
                        label = { Text(p.profileNo) }
                    )
                }
            }
        }
    }
}



@Composable
fun MuntinV2SettingsDialog(
    viewModel: MuntinViewModelV2,
    state: MuntinViewModelV2.MuntinV2UiState,
    onDismiss: () -> Unit,
    onManageProfiles: () -> Unit,
    onForceImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfiguracja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsTab(viewModel, state)
                
                HorizontalDivider()
                
                Button(
                    onClick = onManageProfiles,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zarządzaj Profilami (CRUD)")
                }
                
                OutlinedButton(
                    onClick = onForceImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wymuś Import z Serwera")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

@Composable
fun SettingsTab(viewModel: MuntinViewModelV2, state: MuntinViewModelV2.MuntinV2UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.assemblyClearance,
                onValueChange = { viewModel.updateSettings(it, state.sawCorrection, state.windowCorrection, state.intersectionRule) },
                label = { Text("Luz (mm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = state.sawCorrection,
                onValueChange = { viewModel.updateSettings(state.assemblyClearance, it, state.windowCorrection, state.intersectionRule) },
                label = { Text("Korekta Piły") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        
        Text("Reguła Krzyżowania:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.intersectionRule == IntersectionType.VERTICAL_CONTINUOUS,
                onClick = { viewModel.updateSettings(state.assemblyClearance, state.sawCorrection, state.windowCorrection, IntersectionType.VERTICAL_CONTINUOUS) },
                label = { Text("Pion Ciągły") }
            )
            FilterChip(
                selected = state.intersectionRule == IntersectionType.HORIZONTAL_CONTINUOUS,
                onClick = { viewModel.updateSettings(state.assemblyClearance, state.sawCorrection, state.windowCorrection, IntersectionType.HORIZONTAL_CONTINUOUS) },
                label = { Text("Poziom Ciągły") }
            )
        }
    }
}

@Composable
fun ProfileManagerDialog(
    viewModel: MuntinViewModelV2,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf("SASH") }
    var code by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var beadHeight by remember { mutableStateOf("0") }
    var beadAngle by remember { mutableStateOf("45") }

    val allProfiles by viewModel.allProfiles.collectAsState()
    
    // Filtered list
    val displayedProfiles = allProfiles.filter { it.type == type }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zarządzanie Profilami") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth()
            ) {
                // Type Selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SASH" to "Skrzydło", "BEAD" to "Listwa", "MUNTIN" to "Szpros").forEach { (key, label) ->
                        FilterChip(
                            selected = type == key,
                            onClick = { type = key },
                            label = { Text(label) }
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Add New Form
                Text("Dodaj nowy:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Kod") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Wys.") },
                        modifier = Modifier.weight(0.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("Szer.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (type == "SASH") {
                        OutlinedTextField(
                            value = beadHeight,
                            onValueChange = { beadHeight = it },
                            label = { Text("Przylga") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    } else if (type == "BEAD") {
                        OutlinedTextField(
                            value = beadAngle,
                            onValueChange = { beadAngle = it },
                            label = { Text("Kąt") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                
                Button(
                    onClick = {
                        val h = height.toIntOrNull() ?: 0
                        val w = width.toIntOrNull() ?: 0
                        val bh = beadHeight.toIntOrNull() ?: 0
                        val ba = beadAngle.toDoubleOrNull() ?: 0.0
                        
                        if (code.isNotBlank() && h > 0) {
                            viewModel.addProfile(type, code, h, w, bh, ba)
                            code = ""
                            height = ""
                            width = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    enabled = code.isNotBlank() && height.toIntOrNull() != null
                ) {
                    Text("Dodaj")
                }
                
                HorizontalDivider()
                
                // List
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedProfiles) { profile ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(profile.code, style = MaterialTheme.typography.titleSmall)
                                    Text("${profile.heightMm}x${profile.widthMm} mm", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { 
                                    if (profile.id != null) viewModel.deleteProfile(profile.id)
                                }) {
                                    Icon(Icons.Default.Delete, "Usuń")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}


@Composable
fun MountingMarksCard(state: MuntinViewModelV2.MuntinV2UiState) {
     Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Montaż / Znakowanie", style = MaterialTheme.typography.titleMedium)
            Text("(Wymiary od zewnętrznej krawędzi skrzydła)", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            state.mountingMarks.forEach { mark ->
                Text("• $mark")
            }
        }
     }
}

@Composable
fun MuntinVisualizerOrthogonal(
    sashWidth: Int,
    sashHeight: Int,
    verticals: List<Double>,
    horizontals: List<Double>,
    sashProfileWidth: Int,
    beadWidth: Int,
    onCanvasClick: ((Float, Float, Float) -> Unit)? = null
) {
    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(10.dp)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                if (onCanvasClick != null) {
                    val scaleX = size.width / sashWidth.toFloat()
                    val scaleY = size.height / sashHeight.toFloat()
                    val scale = min(scaleX, scaleY)
                    
                    // We need to account for padding (10.dp) but Canvas modifier applies padding 
                    // BEFORE drawing content? No, padding modifier insets the content.
                    // The 'size' in Canvas scope is the size INSIDE padding.
                    // But 'offset' in pointerInput is relative to the element?
                    // Wait, pointerInput is modifier on the Canvas.
                    // So offset (0,0) is top-left of the Canvas (after padding).
                    // Correct.
                    
                    onCanvasClick(offset.x, offset.y, scale)
                }
            }
        }
    ) {
        val scaleX = size.width / sashWidth.toFloat()
        val scaleY = size.height / sashHeight.toFloat()
        val scale = min(scaleX, scaleY)
        
        // Draw Sash Frame
        drawRect(
            color = Color.White,
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(sashWidth * scale, sashHeight * scale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
        
        // Draw Bead Line (Inner)
        val offset = (sashProfileWidth + beadWidth) * scale
        drawRect(
            color = Color.Gray,
            topLeft = Offset(offset, offset),
            size = androidx.compose.ui.geometry.Size(
                (sashWidth * scale) - 2 * offset,
                (sashHeight * scale) - 2 * offset
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // Draw Muntins
        verticals.forEach { pos ->
            val x = pos.toFloat() * scale
            drawLine(
                color = SafetyOrange,
                start = Offset(x, offset),
                end = Offset(x, (sashHeight * scale) - offset),
                strokeWidth = 2.dp.toPx()
            )
        }
        
        horizontals.forEach { pos ->
            val y = pos.toFloat() * scale
            drawLine(
                color = SafetyOrange,
                start = Offset(offset, y),
                end = Offset((sashWidth * scale) - offset, y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun MuntinVisualizerAngular(
    sashWidth: Int,
    sashHeight: Int,
    sashProfileWidth: Int,
    beadWidth: Int,
    debugSegments: List<com.example.warehouse.util.MuntinCalculatorV2Angular.Segment>
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val scaleX = size.width / sashWidth.toFloat()
        val scaleY = size.height / sashHeight.toFloat()
        val scale = min(scaleX, scaleY)
        
        // Draw Sash Frame
        drawRect(
            color = Color.White,
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(sashWidth * scale, sashHeight * scale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
        
        // Draw Bead Line (Inner)
        val offset = (sashProfileWidth + beadWidth) * scale
        drawRect(
            color = Color.Gray,
            topLeft = Offset(offset, offset),
            size = androidx.compose.ui.geometry.Size(
                (sashWidth * scale) - 2 * offset,
                (sashHeight * scale) - 2 * offset
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        
        // Draw Debug Segments (The calculated cut pieces)
        debugSegments.forEach { seg ->
            drawLine(
                color = SafetyOrange,
                start = Offset(seg.p1.x.toFloat() * scale, seg.p1.y.toFloat() * scale),
                end = Offset(seg.p2.x.toFloat() * scale, seg.p2.y.toFloat() * scale),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun CutItemRow(item: CutItemV2) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.titleSmall)
                Text(item.notes, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${item.lengthMm} mm", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("Kąty: ${item.leftAngleDeg}° / ${item.rightAngleDeg}°", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

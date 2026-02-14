package com.example.warehouse.features.muntins_v3.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.warehouse.ui.components.DropdownField
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.features.muntins_v3.ui.canvas.MuntinCanvas
import com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MuntinV3Screen(
    viewModel: MuntinV3ViewModel = viewModel(),
    onBackClick: () -> Unit,
    isDiagonalMode: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    viewModel.setDiagonalMode(isDiagonalMode)
    val settingsViewModel: com.example.warehouse.ui.viewmodel.SettingsViewModel = viewModel()
    val manualInputEnabled by settingsViewModel.muntinManualInput.collectAsState()
    var showPresetsMenu by remember { mutableStateOf(false) }
    var showOptimizationSettings by remember { mutableStateOf(false) }
    var showCorrectionsDialog by remember { mutableStateOf(false) }
    var showPrintPreview by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var showAddBeadDialog by remember { mutableStateOf(false) }
    var showAddMuntinDialog by remember { mutableStateOf(false) }
    var configExpanded by remember { mutableStateOf(true) }
    val isConfigured = state.currentProject != null &&
            state.selectedProfile != null &&
            state.selectedBead != null &&
            state.glassWidth > 0.0 &&
            state.glassHeight > 0.0
    LaunchedEffect(isConfigured) {
        if (isConfigured) configExpanded = false
    }

    if (showPrintPreview) {
        MuntinV3PrintPreviewScreen(
            state = state,
            onBack = { showPrintPreview = false }
        )
        return
    }

    if (showCorrectionsDialog) {
        var manual by remember { mutableStateOf(state.manualCorrection.toString()) }
        var top by remember { mutableStateOf(state.compTop.toString()) }
        var bottom by remember { mutableStateOf(state.compBottom.toString()) }
        var left by remember { mutableStateOf(state.compLeft.toString()) }
        var right by remember { mutableStateOf(state.compRight.toString()) }

        AlertDialog(
            onDismissRequest = { showCorrectionsDialog = false },
            title = { Text("Korekty Wymiarów (mm)") },
            text = {
                Column {
                    Text("Korekta Skrzydła (+/-)", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = manual,
                        onValueChange = { manual = it },
                        label = { Text("Manual Correction") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Kompensacja Krawędzi", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedTextField(
                            value = left,
                            onValueChange = { left = it },
                            label = { Text("Lewa") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = right,
                            onValueChange = { right = it },
                            label = { Text("Prawa") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedTextField(
                            value = top,
                            onValueChange = { top = it },
                            label = { Text("Góra") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        )
                        OutlinedTextField(
                            value = bottom,
                            onValueChange = { bottom = it },
                            label = { Text("Dół") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateCorrections(
                        manual.toDoubleOrNull() ?: 0.0,
                        top.toDoubleOrNull() ?: 0.0,
                        bottom.toDoubleOrNull() ?: 0.0,
                        left.toDoubleOrNull() ?: 0.0,
                        right.toDoubleOrNull() ?: 0.0
                    )
                    showCorrectionsDialog = false
                }) {
                    Text("Zastosuj")
                }
            },
            dismissButton = {
                Button(onClick = { showCorrectionsDialog = false }) { Text("Anuluj") }
            }
        )
    }

    if (showAddProfileDialog) {
        var name by remember { mutableStateOf("") }
        var gx by remember { mutableStateOf("48") }
        var gy by remember { mutableStateOf("48") }
        var ang by remember { mutableStateOf("90") }
        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            title = { Text("Dodaj Profil (V3)") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = gx, onValueChange = { gx = it }, label = { Text("Glass Offset X") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = gy, onValueChange = { gy = it }, label = { Text("Glass Offset Y") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ang, onValueChange = { ang = it }, label = { Text("Kąt zewnętrzny (deg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nx = gx.toDoubleOrNull() ?: 48.0
                    val ny = gy.toDoubleOrNull() ?: 48.0
                    val a = ang.toDoubleOrNull() ?: 90.0
                    if (name.isNotBlank()) viewModel.addV3Profile(name, nx, ny, a)
                    showAddProfileDialog = false
                }) { Text("ZAPISZ") }
            },
            dismissButton = { Button(onClick = { showAddProfileDialog = false }) { Text("ANULUJ") } }
        )
    }

    if (showAddBeadDialog) {
        var name by remember { mutableStateOf("") }
        var ang by remember { mutableStateOf("20") }
        var offs by remember { mutableStateOf("18") }
        AlertDialog(
            onDismissRequest = { showAddBeadDialog = false },
            title = { Text("Dodaj Listwę (V3)") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ang, onValueChange = { ang = it }, label = { Text("Kąt twarzy (deg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = offs, onValueChange = { offs = it }, label = { Text("Efektywny offset szkła") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val a = ang.toDoubleOrNull() ?: 20.0
                    val e = offs.toDoubleOrNull() ?: 18.0
                    if (name.isNotBlank()) viewModel.addV3Bead(name, a, e)
                    showAddBeadDialog = false
                }) { Text("ZAPISZ") }
            },
            dismissButton = { Button(onClick = { showAddBeadDialog = false }) { Text("ANULUJ") } }
        )
    }

    if (showAddMuntinDialog) {
        var name by remember { mutableStateOf("") }
        var width by remember { mutableStateOf("26") }
        var thick by remember { mutableStateOf("18") }
        var ang by remember { mutableStateOf("90") }
        AlertDialog(
            onDismissRequest = { showAddMuntinDialog = false },
            title = { Text("Dodaj Szpros (V3)") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("Szerokość (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = thick, onValueChange = { thick = it }, label = { Text("Grubość (mm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ang, onValueChange = { ang = it }, label = { Text("Kąt ścianki (deg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val w = width.toDoubleOrNull() ?: 26.0
                    val t = thick.toDoubleOrNull() ?: 18.0
                    val a = ang.toDoubleOrNull() ?: 90.0
                    if (name.isNotBlank()) viewModel.addV3Muntin(name, w, t, a)
                    showAddMuntinDialog = false
                }) { Text("ZAPISZ") }
            },
            dismissButton = { Button(onClick = { showAddMuntinDialog = false }) { Text("ANULUJ") } }
        )
    }

    if (showOptimizationSettings) {
        var barLength by remember { mutableStateOf(state.barLength.toString()) }
        var sawKerf by remember { mutableStateOf(state.sawKerf.toString()) }
        var sashCount by remember { mutableStateOf(state.sashCount.toString()) }

        AlertDialog(
            onDismissRequest = { showOptimizationSettings = false },
            title = { Text("Ustawienia Optymalizacji") },
            text = {
                Column {
                    OutlinedTextField(
                        value = barLength,
                        onValueChange = { barLength = it },
                        label = { Text("Długość sztangi (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = sawKerf,
                        onValueChange = { sawKerf = it },
                        label = { Text("Rzaz piły (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = sashCount,
                        onValueChange = { sashCount = it },
                        label = { Text("Liczba skrzydeł") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateOptimizationSettings(
                        barLength.toDoubleOrNull() ?: 6000.0,
                        sawKerf.toDoubleOrNull() ?: 4.0,
                        sashCount.toIntOrNull() ?: 1
                    )
                    showOptimizationSettings = false
                }) {
                    Text("Zastosuj")
                }
            },
            dismissButton = {
                Button(onClick = { showOptimizationSettings = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val modeTitle = if (isDiagonalMode) "SZPROSY SKOŚNE" else "SZPROSY PROSTE"
                    Text(text = "$modeTitle - ${state.currentProject?.name ?: "Brak Projektu"}", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wstecz"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPresetsMenu = true }) {
                        Icon(Icons.Default.Star, contentDescription = "Presets")
                    }
                    DropdownMenu(
                        expanded = showPresetsMenu,
                        onDismissRequest = { showPresetsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cross (Krzyż 45°)") },
                            onClick = {
                                viewModel.generateCross()
                                showPresetsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Diamond (Romb)") },
                            onClick = {
                                viewModel.generateDiamond()
                                showPresetsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sunburst (3 promienie)") },
                            onClick = {
                                viewModel.generateSunburst(3)
                                showPresetsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sunburst (5 promieni)") },
                            onClick = {
                                viewModel.generateSunburst(5)
                                showPresetsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pajęczyna (8 promieni)") },
                            onClick = {
                                viewModel.generateWeb(8)
                                showPresetsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Gotyk (Łuk ostry)") },
                            onClick = {
                                viewModel.generateGothic()
                                showPresetsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Trapez (Wachlarz)") },
                            onClick = {
                                viewModel.generateTrapezoid(3)
                                showPresetsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Zapisz Preset") },
                            onClick = { /* Save preset */ showPresetsMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Wczytaj Preset") },
                            onClick = { /* Load preset */ showPresetsMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Dodaj Profil (V3)") },
                            onClick = { showAddProfileDialog = true; showPresetsMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Dodaj Listwę (V3)") },
                            onClick = { showAddBeadDialog = true; showPresetsMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Dodaj Szpros (V3)") },
                            onClick = { showAddMuntinDialog = true; showPresetsMenu = false }
                        )
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ustawienia początkowe")
                        TextButton(onClick = { configExpanded = !configExpanded }) {
                            Text(if (configExpanded) "Zwiń" else "Rozwiń")
                        }
                    }
                    if (configExpanded) {
                        val profileOptions = state.availableProfiles.map { it.name }
                        val beadOptions = state.availableBeads.map { it.name }
                        val muntinOptions = state.availableMuntins.map { it.name }
                        var fwText by remember { mutableStateOf("1200") }
                        var fhText by remember { mutableStateOf("800") }
                        DropdownField(
                            label = "Profil Skrzydła",
                            options = profileOptions,
                            selectedOption = state.selectedProfile?.name ?: "",
                            onOptionSelected = { name ->
                                val p = state.availableProfiles.find { it.name == name }
                                viewModel.selectProfile(p)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            label = "Listwa Przyszybowa",
                            options = beadOptions,
                            selectedOption = state.selectedBead?.name ?: "",
                            onOptionSelected = { name ->
                                val b = state.availableBeads.find { it.name == name }
                                viewModel.selectBead(b)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            label = "Szpros",
                            options = muntinOptions,
                            selectedOption = state.selectedMuntin?.name ?: "",
                            onOptionSelected = { name ->
                                val m = state.availableMuntins.find { it.name == name }
                                viewModel.selectMuntin(m)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = fwText,
                                onValueChange = { fwText = it },
                                label = { Text("Szerokość ramy (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = fhText,
                                onValueChange = { fhText = it },
                                label = { Text("Wysokość ramy (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = {
                                    val fw = fwText.toDoubleOrNull() ?: return@Button
                                    val fh = fhText.toDoubleOrNull() ?: return@Button
                                    val p = state.selectedProfile ?: return@Button
                                    val b = state.selectedBead ?: return@Button
                                    viewModel.createNewProject(fw, fh, p.id, b.id)
                                }
                            ) { Text("Utwórz wirtualne skrzydło") }
                            if (state.availableProfiles.isEmpty() || state.availableBeads.isEmpty() || state.availableMuntins.isEmpty()) {
                                Text("Brak danych konfiguracyjnych V3", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            // Main Canvas Area (Top Half)
            MuntinCanvas(
                glassWidth = state.glassWidth,
                glassHeight = state.glassHeight,
                segments = state.segments,
                selectedSegmentId = state.selectedSegmentId,
                onTap = { id, point -> viewModel.onCanvasTap(id, point.x.toDouble(), point.y.toDouble()) },
                clearanceGlobal = state.clearanceGlobal,
                clearanceBead = state.clearanceBead,
                clearanceMuntin = state.clearanceMuntin,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            
            HorizontalDivider()

            if (!isConfigured) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.ime)
                ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val profileOptions = state.availableProfiles.map { it.name }
                        val beadOptions = state.availableBeads.map { it.name }
                        val muntinOptions = state.availableMuntins.map { it.name }
                        var fwText by remember { mutableStateOf("1200") }
                        var fhText by remember { mutableStateOf("800") }
                        DropdownField(
                            label = "Profil Skrzydła",
                            options = profileOptions,
                            selectedOption = state.selectedProfile?.name ?: "",
                            onOptionSelected = { name ->
                                val p = state.availableProfiles.find { it.name == name }
                                viewModel.selectProfile(p)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            label = "Listwa Przyszybowa",
                            options = beadOptions,
                            selectedOption = state.selectedBead?.name ?: "",
                            onOptionSelected = { name ->
                                val b = state.availableBeads.find { it.name == name }
                                viewModel.selectBead(b)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownField(
                            label = "Szpros",
                            options = muntinOptions,
                            selectedOption = state.selectedMuntin?.name ?: "",
                            onOptionSelected = { name ->
                                val m = state.availableMuntins.find { it.name == name }
                                viewModel.selectMuntin(m)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = fwText,
                                onValueChange = { fwText = it },
                                label = { Text("Szerokość ramy (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = fhText,
                                onValueChange = { fhText = it },
                                label = { Text("Wysokość ramy (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = {
                                    val fw = fwText.toDoubleOrNull() ?: return@Button
                                    val fh = fhText.toDoubleOrNull() ?: return@Button
                                    val p = state.selectedProfile ?: return@Button
                                    val b = state.selectedBead ?: return@Button
                                    viewModel.createNewProject(fw, fh, p.id, b.id)
                                }
                            ) { Text("Utwórz wirtualne skrzydło") }
                            if (state.availableProfiles.isEmpty() || state.availableBeads.isEmpty() || state.availableMuntins.isEmpty()) {
                                Text("Brak danych konfiguracyjnych V3", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Profil Skrzydła")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.availableProfiles.take(6).forEach { p ->
                                Button(
                                    onClick = { viewModel.selectProfile(p) },
                                    enabled = state.selectedProfile?.id != p.id
                                ) { Text(p.name) }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Listwa Przyszybowa")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.availableBeads.take(6).forEach { b ->
                                Button(
                                    onClick = { viewModel.selectBead(b) },
                                    enabled = state.selectedBead?.id != b.id
                                ) { Text(b.name) }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Szpros (VEKA 109*)")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.availableMuntins.take(6).forEach { m ->
                                Button(
                                    onClick = { viewModel.selectMuntin(m) },
                                    enabled = state.selectedMuntin?.id != m.id
                                ) { Text(m.name) }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(text = "Segments: ${state.segments.size}")
                    
                    if (state.selectedSegmentId != null) {
                        Button(
                            onClick = { viewModel.removeSelectedSegment() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Selected")
                        }
                    }
                }
                
                if (manualInputEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Parametry ręczne szprosa", style = MaterialTheme.typography.titleSmall)
                            var manualWidth by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = manualWidth,
                                onValueChange = {
                                    manualWidth = it
                                    viewModel.setManualMuntinWidth(it.toDoubleOrNull())
                                },
                                label = { Text("Szerokość szprosa (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { viewModel.addVertical() }) { Text("+ Pion") }
                    Button(onClick = { viewModel.addHorizontal() }) { Text("+ Poziom") }
                    Button(onClick = { viewModel.clearLayout() }) { Text("Wyczyść") }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!isDiagonalMode) {
                        Button(onClick = { viewModel.setAddMode(com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.AddMode.VERTICAL) }) { Text("Tryb dodawania: Pion") }
                        Button(onClick = { viewModel.setAddMode(com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.AddMode.HORIZONTAL) }) { Text("Tryb dodawania: Poziom") }
                    } else {
                        Button(onClick = { viewModel.setAddMode(com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.AddMode.DIAGONAL_45) }) { Text("Tryb dodawania: 45°") }
                        Button(onClick = { viewModel.setAddMode(com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.AddMode.DIAGONAL_135) }) { Text("Tryb dodawania: 135°") }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var nText by remember { mutableStateOf("${state.cols}") }
                    OutlinedTextField(
                        value = nText,
                        onValueChange = { nText = it },
                        label = { Text("N (podziały)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        val n = nText.toIntOrNull() ?: return@Button
                        viewModel.setColsN(n)
                    }) { Text("Piony N") }
                    Button(onClick = {
                        val n = nText.toIntOrNull() ?: return@Button
                        viewModel.setRowsN(n)
                    }) { Text("Poziomy N") }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.setContinuousMaster(com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.ContinuousMaster.VERTICAL) },
                        enabled = state.continuousMaster != com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.ContinuousMaster.VERTICAL
                    ) { Text("Ciągły: Pion") }
                    Button(
                        onClick = { viewModel.setContinuousMaster(com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.ContinuousMaster.HORIZONTAL) },
                        enabled = state.continuousMaster != com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel.ContinuousMaster.HORIZONTAL
                    ) { Text("Ciągły: Poziom") }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var snap by remember { mutableStateOf("${state.snapThresholdMm}") }
                    OutlinedTextField(
                        value = snap,
                        onValueChange = { snap = it },
                        label = { Text("Próg snap (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        val t = snap.toDoubleOrNull() ?: return@Button
                        viewModel.setSnapThreshold(t)
                    }) { Text("Ustaw próg") }
                    Button(onClick = { viewModel.undo() }) { Text("Cofnij") }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(
                            checked = state.groupCuts,
                            onCheckedChange = { viewModel.setGroupCuts(it) }
                        )
                        Text("Grupuj identyczne cięcia")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Luz globalny (mm)")
                        OutlinedTextField(
                            value = state.clearanceGlobal.toString(),
                            onValueChange = { s ->
                                val v = s.toDoubleOrNull() ?: state.clearanceGlobal
                                viewModel.updateClearances(v, state.clearanceBead, state.clearanceMuntin)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Column {
                        Text("Korekta luzu: Szpros-Listwa (mm)")
                        OutlinedTextField(
                            value = state.clearanceBead.toString(),
                            onValueChange = { s ->
                                val v = s.toDoubleOrNull() ?: state.clearanceBead
                                viewModel.updateClearances(state.clearanceGlobal, v, state.clearanceMuntin)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Column {
                        Text("Korekta luzu: Szpros-Szpros (mm)")
                        OutlinedTextField(
                            value = state.clearanceMuntin.toString(),
                            onValueChange = { s ->
                                val v = s.toDoubleOrNull() ?: state.clearanceMuntin
                                viewModel.updateClearances(state.clearanceGlobal, state.clearanceBead, v)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { viewModel.generateGrid(2, 2) }) { Text("2x2") }
                    Button(onClick = { viewModel.generateGrid(3, 3) }) { Text("3x3") }
                }
            }
            }
            
            HorizontalDivider()

            // Info Lists (Bottom Half)
            LazyColumn(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
            ) {
                // Section 1: Cut List
                stickyHeader {
                    HeaderTitle("TABELA CIĘĆ")
                }
                
                items(state.cutList) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                            .clickable { viewModel.highlightByCut(item) },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.angleStart}° → ${item.length} mm → ${item.angleEnd}°", style = MaterialTheme.typography.bodyMedium)
                        if (item.description.isNotEmpty()) {
                            Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Section 2: Assembly Instructions
                stickyHeader {
                    HeaderTitle("INSTRUKCJA MONTAŻU (Kolejność klejenia)")
                }

                items(state.assemblySteps) { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${step.orderIndex}. ${step.description}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = step.positionLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "${step.length} mm",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Section 3: Optimization
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OPTYMALIZACJA SZTANG",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(onClick = { showOptimizationSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                if (state.optimizationResult != null) {
                    val result = state.optimizationResult!!
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Zużyto sztang: ${result.totalBarsUsed} (L=${state.barLength.toInt()}mm)", style = MaterialTheme.typography.titleSmall)
                            Text("Odpad całkowity: ${String.format("%.1f", result.wastePercentage)}%", style = MaterialTheme.typography.bodyMedium)
                            Text("Rzaz: ${state.sawKerf}mm | Ilość skrzydeł: ${state.sashCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    items(result.bars) { bar ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sztanga #${bar.id}", style = MaterialTheme.typography.titleSmall)
                                    Text("Odpad: ${String.format("%.1f", bar.waste)} mm", color = MaterialTheme.colorScheme.error)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                bar.cuts.forEach { cut ->
                                    Text("• ${cut.description}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text("Brak danych do optymalizacji", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

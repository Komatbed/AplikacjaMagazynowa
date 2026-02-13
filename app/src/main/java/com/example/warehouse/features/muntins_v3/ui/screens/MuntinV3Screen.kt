package com.example.warehouse.features.muntins_v3.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.features.muntins_v3.ui.canvas.MuntinCanvas
import com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MuntinV3Screen(
    viewModel: MuntinV3ViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showPresetsMenu by remember { mutableStateOf(false) }
    var showOptimizationSettings by remember { mutableStateOf(false) }
    var showCorrectionsDialog by remember { mutableStateOf(false) }
    var showPrintPreview by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                    Text(
                        text = "Szprosy V3 - ${state.currentProject?.name ?: "No Project"}",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showPrintPreview = true }) {
                        Icon(Icons.Default.Print, contentDescription = "Print/Export")
                    }
                    IconButton(onClick = { showCorrectionsDialog = true }) {
                        Icon(Icons.Default.Build, contentDescription = "Corrections")
                    }
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
            // Main Canvas Area (Top Half)
            MuntinCanvas(
                glassWidth = state.glassWidth,
                glassHeight = state.glassHeight,
                segments = state.segments,
                selectedSegmentId = state.selectedSegmentId,
                onTap = { viewModel.selectSegment(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            
            HorizontalDivider()

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { viewModel.generateGrid(2, 2) }) {
                        Text("2x2")
                    }
                    Button(onClick = { viewModel.generateGrid(3, 3) }) {
                        Text("3x3")
                    }
                    Button(onClick = { viewModel.clearLayout() }) {
                        Text("Clear")
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.count}x", style = MaterialTheme.typography.bodyMedium)
                        Text("L: ${item.length} mm", style = MaterialTheme.typography.bodyMedium)
                        Text("Ang: ${item.angleStart}° / ${item.angleEnd}°", style = MaterialTheme.typography.bodyMedium)
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

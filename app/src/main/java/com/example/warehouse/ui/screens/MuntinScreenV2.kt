package com.example.warehouse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.model.CutItemV2
import com.example.warehouse.ui.viewmodel.MuntinViewModelV2
import com.example.warehouse.util.MuntinCalculatorV2.IntersectionType
import kotlin.math.min

@Composable
fun MuntinScreenV2(
    viewModel: MuntinViewModelV2 = viewModel()
) {
    val state by viewModel.uiState
    
    // Tab State local to this screen
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Prosty", "Skośny")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { 
                        selectedTab = index 
                        viewModel.setMode(index == 1) // 1 = Angular
                    },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> MuntinV2OrthogonalScreen(viewModel, state)
            1 -> MuntinV2AngularScreen(viewModel, state)
        }
    }
}

@Composable
fun MuntinV2OrthogonalScreen(viewModel: MuntinViewModelV2, state: MuntinViewModelV2.MuntinV2UiState) {
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
        item { SettingsCard(viewModel, state) }

        // --- Visualizer & Interaction ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wirtualne Skrzydło", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { viewModel.addVerticalMuntin() }) {
                            Icon(Icons.Default.Add, null)
                            Text("Pion")
                        }
                        Button(onClick = { viewModel.addHorizontalMuntin() }) {
                            Icon(Icons.Default.Add, null)
                            Text("Poziom")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.LightGray)
                            .border(2.dp, Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        MuntinVisualizerOrthogonal(
                            sashWidth = state.sashWidth.toIntOrNull() ?: 1000,
                            sashHeight = state.sashHeight.toIntOrNull() ?: 1000,
                            verticals = state.verticalMuntins,
                            horizontals = state.horizontalMuntins,
                            sashProfileWidth = viewModel.sashProfiles[state.selectedSashProfileIndex].heightMm,
                            beadWidth = viewModel.beadProfiles[state.selectedBeadProfileIndex].heightMm
                        )
                    }
                    
                    if (state.verticalMuntins.isNotEmpty() || state.horizontalMuntins.isNotEmpty()) {
                        Text("Szprosy (kliknij aby usunąć):", style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             state.verticalMuntins.forEachIndexed { i, pos ->
                                AssistChip(
                                    onClick = { viewModel.removeVerticalMuntin(i) },
                                    label = { Text("V${i+1}") },
                                    trailingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) }
                                )
                             }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             state.horizontalMuntins.forEachIndexed { i, pos ->
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
        item { Text("Lista Cięć", style = MaterialTheme.typography.titleMedium) }
        items(state.cutList) { item -> CutItemRow(item) }
        item { MountingMarksCard(state) }
    }
}

@Composable
fun MuntinV2AngularScreen(viewModel: MuntinViewModelV2, state: MuntinViewModelV2.MuntinV2UiState) {
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
        item { SettingsCard(viewModel, state) }
        
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
                        Button(onClick = { viewModel.setSpiderPattern(state.spiderPattern == null) }) {
                            Text(if (state.spiderPattern == null) "Pajęczyna" else "Usuń Pajęczynę")
                        }
                        // Arch button
                        Button(onClick = { viewModel.setArchPattern(state.archPattern == null) }) {
                            Text(if (state.archPattern == null) "Łuk" else "Usuń Łuk")
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
                            sashProfileWidth = viewModel.sashProfiles[state.selectedSashProfileIndex].heightMm,
                            beadWidth = viewModel.beadProfiles[state.selectedBeadProfileIndex].heightMm,
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
        item { Text("Lista Cięć", style = MaterialTheme.typography.titleMedium) }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Profile", style = MaterialTheme.typography.titleMedium)
            
            Text("Profil Skrzydła:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.sashProfiles.forEachIndexed { index, p ->
                    FilterChip(
                        selected = state.selectedSashProfileIndex == index,
                        onClick = { viewModel.selectSashProfile(index) },
                        label = { Text(p.profileNo) }
                    )
                }
            }

            Text("Listwa Przyszybowa:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.beadProfiles.forEachIndexed { index, p ->
                    FilterChip(
                        selected = state.selectedBeadProfileIndex == index,
                        onClick = { viewModel.selectBeadProfile(index) },
                        label = { Text(p.profileNo) }
                    )
                }
            }

            Text("Szpros:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.muntinProfiles.forEachIndexed { index, p ->
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
fun SettingsCard(viewModel: MuntinViewModelV2, state: MuntinViewModelV2.MuntinV2UiState) {
     Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ustawienia & Reguły", style = MaterialTheme.typography.titleMedium)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.assemblyClearance,
                    onValueChange = { viewModel.updateSettings(it, state.sawCorrection, state.windowCorrection, state.intersectionRule) },
                    label = { Text("Luz (mm)") },
                    modifier = Modifier.weight(1f)
                )
                 OutlinedTextField(
                    value = state.sawCorrection,
                    onValueChange = { viewModel.updateSettings(state.assemblyClearance, it, state.windowCorrection, state.intersectionRule) },
                    label = { Text("Korekta Piły") },
                    modifier = Modifier.weight(1f)
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
    beadWidth: Int
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val scaleX = size.width / sashWidth.toFloat()
        val scaleY = size.height / sashHeight.toFloat()
        val scale = min(scaleX, scaleY)
        
        // Draw Sash Frame
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(sashWidth * scale, sashHeight * scale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        
        // Draw Bead Line (Inner)
        val offset = (sashProfileWidth + beadWidth) * scale
        drawRect(
            color = Color.Blue,
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
                color = Color.Red,
                start = Offset(x, offset),
                end = Offset(x, (sashHeight * scale) - offset),
                strokeWidth = 2.dp.toPx()
            )
        }
        
        horizontals.forEach { pos ->
            val y = pos.toFloat() * scale
            drawLine(
                color = Color.Red,
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
            color = Color.DarkGray,
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(sashWidth * scale, sashHeight * scale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        
        // Draw Bead Line (Inner)
        val offset = (sashProfileWidth + beadWidth) * scale
        drawRect(
            color = Color.Blue,
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
                color = Color.Red,
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

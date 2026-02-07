package com.example.warehouse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.theme.DarkGrey
import com.example.warehouse.ui.viewmodel.WindowCalculatorViewModel
import com.example.warehouse.ui.viewmodel.ProfileSystem
import com.example.warehouse.ui.viewmodel.GlazingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowCalculatorScreen(
    onBackClick: () -> Unit,
    viewModel: WindowCalculatorViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val result by viewModel.result.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KALKULATOR OKIEN", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = SafetyOrange
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Wymiary zewnętrzne ramy (mm)", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = config.widthMm.toString(),
                            onValueChange = { viewModel.updateWidth(it) },
                            label = { Text("Szerokość") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SafetyOrange,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        OutlinedTextField(
                            value = config.heightMm.toString(),
                            onValueChange = { viewModel.updateHeight(it) },
                            label = { Text("Wysokość") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SafetyOrange,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Konfiguracja", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                    
                    // Profile System
                    Text("System: ${config.profileSystem.label}", color = Color.White)
                    Row {
                        ProfileSystem.values().forEach { sys ->
                            SelectableButton(
                                text = sys.label,
                                selected = config.profileSystem == sys,
                                onClick = { viewModel.updateSystem(sys) }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }

                    // Glazing
                    Text("Pakiet szybowy: ${config.glazingType.label}", color = Color.White)
                    Row {
                        GlazingType.values().forEach { g ->
                            SelectableButton(
                                text = if(g == GlazingType.DOUBLE) "2-szyby" else "3-szyby",
                                selected = config.glazingType == g,
                                onClick = { viewModel.updateGlazing(g) }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }

                    // Sash Count
                    Text("Ilość skrzydeł:", color = Color.White)
                    Row {
                        SelectableButton("1 Skrzydło", config.sashCount == 1) { viewModel.updateSashCount(1) }
                        Spacer(Modifier.width(8.dp))
                        SelectableButton("2 Skrzydła", config.sashCount == 2) { viewModel.updateSashCount(2) }
                    }

                    if (config.sashCount == 2) {
                        Text("Typ słupka:", color = Color.White)
                        Row {
                            SelectableButton("Stały", config.hasMullion) { viewModel.toggleMullion(true) }
                            Spacer(Modifier.width(8.dp))
                            SelectableButton("Ruchomy", !config.hasMullion) { viewModel.toggleMullion(false) }
                        }
                    }
                }
            }

            HorizontalDivider(color = SafetyOrange)

            // Results Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wyniki", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                    Spacer(Modifier.height(8.dp))
                    
                    ResultRow("Wymiar skrzydła", "${result.sashWidth} x ${result.sashHeight} mm")
                    ResultRow("Wymiar pakietu szybowego", "${result.glassWidth} x ${result.glassHeight} mm")
                    ResultRow("Waga pakietu", "${result.glassWeightKg} kg")
                    ResultRow("Szacunkowa waga okna", "${(result.glassWeightKg + result.profileWeightKg)} kg")
                    ResultRow("Listewki (Poziom/Pion)", "${result.glazingBeadHorizontal} / ${result.glazingBeadVertical} mm")
                    if (config.hasMullion) {
                        ResultRow("Długość słupka", "${result.mullionLength} mm")
                    }
                    HorizontalDivider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow("Szacunkowy Koszt", "${result.estimatedCost} PLN", isHighlight = true)
                }
            }

            // Sketch
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            ) {
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    WindowSketch(config)
                }
            }
        }
    }
}

@Composable
fun SelectableButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) SafetyOrange else Color.Gray
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ResultRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = if (isHighlight) SafetyOrange else Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun WindowSketch(config: com.example.warehouse.ui.viewmodel.WindowConfig) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Scale factors
        val scaleW = w / (config.widthMm.toFloat() + 200) // padding
        val scaleH = h / (config.heightMm.toFloat() + 200)
        val scale = minOf(scaleW, scaleH)

        val drawW = config.widthMm * scale
        val drawH = config.heightMm * scale
        
        val offsetX = (w - drawW) / 2
        val offsetY = (h - drawH) / 2

        // Draw Frame
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(offsetX, offsetY),
            size = Size(drawW, drawH),
            style = Stroke(width = 8.dp.toPx())
        )

        // Draw Sash(es)
        val sys = config.profileSystem
        val frameW = sys.frameWidth * scale
        
        if (config.sashCount == 1) {
            val sashW = drawW - 2 * frameW
            val sashH = drawH - 2 * frameW
            drawRect(
                color = SafetyOrange,
                topLeft = Offset(offsetX + frameW, offsetY + frameW),
                size = Size(sashW, sashH),
                style = Stroke(width = 4.dp.toPx())
            )
            // Handle
            drawCircle(
                color = Color.Black,
                center = Offset(offsetX + frameW + sashW - 20, offsetY + drawH / 2),
                radius = 10f
            )
        } else {
            // 2 Sashes
            val halfW = (drawW - 2 * frameW) / 2 // Simplified visualization
            // Sash 1
            drawRect(
                color = SafetyOrange,
                topLeft = Offset(offsetX + frameW, offsetY + frameW),
                size = Size(halfW, drawH - 2*frameW),
                style = Stroke(width = 4.dp.toPx())
            )
            // Sash 2
            drawRect(
                color = SafetyOrange,
                topLeft = Offset(offsetX + frameW + halfW, offsetY + frameW),
                size = Size(halfW, drawH - 2*frameW),
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Mullion
            drawLine(
                color = if(config.hasMullion) Color.Black else SafetyOrange,
                start = Offset(offsetX + frameW + halfW, offsetY + frameW),
                end = Offset(offsetX + frameW + halfW, offsetY + drawH - frameW),
                strokeWidth = if(config.hasMullion) 8f else 4f
            )
        }
    }
}

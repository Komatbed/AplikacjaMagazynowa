package com.example.warehouse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.MuntinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuntinScreen(
    onBackClick: () -> Unit,
    viewModel: MuntinViewModel = viewModel()
) {
    // Inputs
    var sashWidth by remember { mutableStateOf("1000") }
    var sashHeight by remember { mutableStateOf("1000") }
    var profileHeight by remember { mutableStateOf("70") }
    var beadHeight by remember { mutableStateOf("20") }
    var beadAngle by remember { mutableStateOf("45") }
    var muntinWidth by remember { mutableStateOf("26") }
    
    // Grid Configuration
    var verticalFields by remember { mutableStateOf("2") }
    var horizontalFields by remember { mutableStateOf("2") }
    
    var isHalvingJoint by remember { mutableStateOf(false) }
    var externalOffset by remember { mutableStateOf("0") }
    
    // Advanced (defaults)
    var gap by remember { mutableStateOf("1.0") }
    var overlap by remember { mutableStateOf("0.0") }
    
    val result by viewModel.result
    
    // Trigger calculation
    LaunchedEffect(sashWidth, sashHeight, profileHeight, beadHeight, beadAngle, muntinWidth, verticalFields, horizontalFields, isHalvingJoint, externalOffset, gap, overlap) {
        val w = sashWidth.toIntOrNull() ?: 0
        val h = sashHeight.toIntOrNull() ?: 0
        val ph = profileHeight.toIntOrNull() ?: 0
        val bh = beadHeight.toIntOrNull() ?: 0
        val ba = beadAngle.toDoubleOrNull() ?: 0.0
        val mw = muntinWidth.toIntOrNull() ?: 0
        val vf = verticalFields.toIntOrNull() ?: 1
        val hf = horizontalFields.toIntOrNull() ?: 1
        val g = gap.toDoubleOrNull() ?: 0.0
        val ov = overlap.toDoubleOrNull() ?: 0.0
        val eo = externalOffset.toDoubleOrNull() ?: 0.0

        if (w > 0 && h > 0) {
            viewModel.calculate(w, h, ph, bh, ba, mw, g, ov, vf, hf, isHalvingJoint, eo)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KALKULATOR SZPROSÓW", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
        ) {
            // Type Selection
            Text("Typ Szprosu", color = SafetyOrange, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { verticalFields = "2"; horizontalFields = "1" }, modifier = Modifier.weight(1f)) { Text("1 Pion") }
                Button(onClick = { verticalFields = "1"; horizontalFields = "2" }, modifier = Modifier.weight(1f)) { Text("1 Poziom") }
                Button(onClick = { verticalFields = "2"; horizontalFields = "2" }, modifier = Modifier.weight(1f)) { Text("Krzyż") }
                Button(onClick = { verticalFields = "1"; horizontalFields = "1" }, modifier = Modifier.weight(1f)) { Text("Niestand.") }
            }
            Text("Kliknij na schemat, aby dodać/usunąć szprosy", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            // Interactive Schema
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 16.dp)
                    .border(1.dp, Color.Gray)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val w = sashWidth.toFloatOrNull() ?: 1000f
                            val h = sashHeight.toFloatOrNull() ?: 1000f
                            val vf = verticalFields.toIntOrNull() ?: 1
                            val hf = horizontalFields.toIntOrNull() ?: 1
                            
                            val aspectRatio = if (h > 0) w / h else 1f
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            
                            var drawW: Float = canvasW
                            var drawH: Float = canvasW / aspectRatio
                            
                            if (drawH > canvasH) {
                                drawH = canvasH
                                drawW = canvasH * aspectRatio
                            }
                            
                            val offsetX = (canvasW - drawW) / 2
                            val offsetY = (canvasH - drawH) / 2
                            
                            // Check bounds
                            if (offset.x >= offsetX && offset.x <= offsetX + drawW &&
                                offset.y >= offsetY && offset.y <= offsetY + drawH) {
                                
                                val relX = offset.x - offsetX
                                val relY = offset.y - offsetY
                                
                                // Determine click type (vertical or horizontal split)
                                // If click is near a vertical line, remove it.
                                // If click is in empty space, add line.
                                
                                // Vertical logic
                                val vStep = drawW / vf
                                var actionTaken = false
                                
                                // Check removal of vertical lines
                                for (i in 1 until vf) {
                                    val x = vStep * i
                                    if (kotlin.math.abs(relX - x) < 40) { // 40px threshold
                                        if (vf > 1) {
                                            verticalFields = (vf - 1).toString()
                                            actionTaken = true
                                        }
                                        break
                                    }
                                }
                                
                                if (!actionTaken) {
                                    // Check removal of horizontal lines
                                    val hStep = drawH / hf
                                    for (i in 1 until hf) {
                                        val y = hStep * i
                                        if (kotlin.math.abs(relY - y) < 40) {
                                            if (hf > 1) {
                                                horizontalFields = (hf - 1).toString()
                                                actionTaken = true
                                            }
                                            break
                                        }
                                    }
                                    
                                    if (!actionTaken) {
                                        // Add logic
                                        // Calculate distance to center of current cell
                                        // We want to see if we are splitting the width or height
                                        
                                        val distToVCenter = kotlin.math.abs((relX % vStep) - (vStep / 2))
                                        val distToHCenter = kotlin.math.abs((relY % hStep) - (hStep / 2))
                                        
                                        // If we are closer to vertical center, it implies we want to draw a vertical line there
                                        if (distToVCenter < distToHCenter) {
                                            verticalFields = (vf + 1).toString()
                                        } else {
                                            horizontalFields = (hf + 1).toString()
                                        }
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val w = sashWidth.toFloatOrNull() ?: 1000f
                    val h = sashHeight.toFloatOrNull() ?: 1000f
                    val vf = verticalFields.toIntOrNull() ?: 1
                    val hf = horizontalFields.toIntOrNull() ?: 1
                    
                    val aspectRatio = if (h > 0) w / h else 1f
                    
                    // Fit rect in canvas
                    val canvasW = size.width
                    val canvasH = size.height
                    
                    var drawW = canvasW
                    var drawH = canvasW / aspectRatio
                    
                    if (drawH > canvasH) {
                        drawH = canvasH
                        drawW = canvasH * aspectRatio
                    }
                    
                    val offsetX = (canvasW - drawW) / 2
                    val offsetY = (canvasH - drawH) / 2
                    
                    // Draw Sash Frame
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(drawW, drawH),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    
                    // Draw Vertical Muntins
                    if (vf > 1) {
                        val step = drawW / vf
                        for (i in 1 until vf) {
                            drawLine(
                                color = SafetyOrange,
                                start = Offset(offsetX + step * i, offsetY),
                                end = Offset(offsetX + step * i, offsetY + drawH),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                    
                    // Draw Horizontal Muntins
                    if (hf > 1) {
                        val step = drawH / hf
                        for (i in 1 until hf) {
                            drawLine(
                                color = SafetyOrange,
                                start = Offset(offsetX, offsetY + step * i),
                                end = Offset(offsetX + drawW, offsetY + step * i),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Inputs Row 1

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sashWidth, onValueChange = { if(it.all {c -> c.isDigit()}) sashWidth = it },
                    label = { Text("Szer. Skrzydła") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = sashHeight, onValueChange = { if(it.all {c -> c.isDigit()}) sashHeight = it },
                    label = { Text("Wys. Skrzydła") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // Grid Inputs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = verticalFields, onValueChange = { if(it.all {c -> c.isDigit()}) verticalFields = it },
                    label = { Text("Pola Pion") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = horizontalFields, onValueChange = { if(it.all {c -> c.isDigit()}) horizontalFields = it },
                    label = { Text("Pola Poziom") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // Profile Inputs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = muntinWidth, onValueChange = { if(it.all {c -> c.isDigit()}) muntinWidth = it },
                    label = { Text("Szer. Szprosa") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = externalOffset, onValueChange = { externalOffset = it },
                    label = { Text("Offset Zewn.") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isHalvingJoint, onCheckedChange = { isHalvingJoint = it })
                Text("Łączenie na wpust (Krzyżowe)", color = Color.White)
            }

            // Results
            result?.let { res ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("WYNIKI WEWNĘTRZNE", color = SafetyOrange, style = MaterialTheme.typography.titleMedium)
                        Text("Pionowe: ${res.verticalCount} szt. x ${String.format("%.1f", res.verticalMuntinLength)} mm", color = Color.White)
                        if (res.verticalSegments.isNotEmpty()) {
                            Text("Segmenty: ${res.verticalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text("Poziome: ${res.horizontalCount} szt. x ${String.format("%.1f", res.horizontalMuntinLength)} mm", color = Color.White)
                        if (res.horizontalSegments.isNotEmpty()) {
                            Text("Segmenty: ${res.horizontalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        Text("WYNIKI ZEWNĘTRZNE (Offset: $externalOffset)", color = SafetyOrange, style = MaterialTheme.typography.titleMedium)
                        Text("Pionowe: ${res.verticalCount} szt. x ${String.format("%.1f", res.externalVerticalMuntinLength)} mm", color = Color.White)
                        if (res.externalVerticalSegments.isNotEmpty()) {
                            Text("Segmenty: ${res.externalVerticalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text("Poziome: ${res.horizontalCount} szt. x ${String.format("%.1f", res.externalHorizontalMuntinLength)} mm", color = Color.White)
                        if (res.externalHorizontalSegments.isNotEmpty()) {
                            Text("Segmenty: ${res.externalHorizontalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

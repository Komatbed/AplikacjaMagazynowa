package com.example.warehouse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.warehouse.model.BeadProfileV2
import com.example.warehouse.model.MuntinProfileV2
import com.example.warehouse.model.SashProfileV2
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.util.CuttingOptimizer

@Composable
fun CrossSectionDialog(
    sashProfile: SashProfileV2,
    beadProfile: BeadProfileV2,
    muntinProfile: MuntinProfileV2,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Przekrój B-B (Schemat)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CrossSectionView(sashProfile, beadProfile, muntinProfile)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Legenda: Szary=Skrzydło, Niebieski=Listwa, Pomarańczowy=Szpros", style = MaterialTheme.typography.bodySmall)
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
fun CrossSectionView(
    sashProfile: SashProfileV2,
    beadProfile: BeadProfileV2,
    muntinProfile: MuntinProfileV2
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val scale = 3.0f // Zoom factor

        // Dimensions
        val sashH = sashProfile.heightMm * scale // Depth
        val sashW = sashProfile.widthMm * scale // Face Height
        val beadH = beadProfile.heightMm * scale // Depth
        val beadW = beadProfile.widthMm * scale // Face Height
        val muntinH = muntinProfile.heightMm * scale // Depth
        val muntinW = muntinProfile.widthMm * scale // Face Height
        
        val glassThick = 24.0f * scale // Assume 24mm glass package
        
        // Glass Line (Vertical)
        drawRect(
            color = Color(0xFFE0F7FA), // Light Cyan
            topLeft = Offset(cx - glassThick/2, cy - 150*scale),
            size = androidx.compose.ui.geometry.Size(glassThick, 300*scale)
        )
        drawLine(
            color = Color.Cyan,
            start = Offset(cx, cy - 150*scale),
            end = Offset(cx, cy + 150*scale),
            strokeWidth = 2.dp.toPx()
        )
        
        // Draw Sash (Left Side - Outside)
        // Top Sash
        // Sash Main Body
        drawRect(
            color = Color.Gray,
            topLeft = Offset(cx - glassThick/2 - sashH, cy - 150*scale),
            size = androidx.compose.ui.geometry.Size(sashH, sashW)
        )
        // Bottom Sash
        drawRect(
            color = Color.Gray,
            topLeft = Offset(cx - glassThick/2 - sashH, cy + 150*scale - sashW),
            size = androidx.compose.ui.geometry.Size(sashH, sashW)
        )
        
        // Draw Bead (Right Side - Inside)
        // Top Bead
        drawRect(
            color = Color.Blue,
            // Bead height (face) matches sash rebate?
            // Let's align Bead top to Sash Top? No, Bead is usually shorter.
            // Align Bead "glass edge" to Sash "glass edge".
            // Let's just draw it at the glass surface.
            topLeft = Offset(cx + glassThick/2, cy - 150*scale),
            size = androidx.compose.ui.geometry.Size(beadH, beadW)
        )
        // Bottom Bead
        drawRect(
            color = Color.Blue,
            topLeft = Offset(cx + glassThick/2, cy + 150*scale - beadW),
            size = androidx.compose.ui.geometry.Size(beadH, beadW)
        )
        
        // Muntin (On Glass)
        // Outside (Left)
        drawRect(
            color = SafetyOrange,
            topLeft = Offset(cx - glassThick/2 - muntinH, cy - muntinW/2),
            size = androidx.compose.ui.geometry.Size(muntinH, muntinW)
        )
        // Inside (Right)
         drawRect(
            color = SafetyOrange,
            topLeft = Offset(cx + glassThick/2, cy - muntinW/2),
            size = androidx.compose.ui.geometry.Size(muntinH, muntinW)
        )
        
        // Spacer inside glass (Aluminium/Black)
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(cx - 6*scale, cy - muntinW/2),
            size = androidx.compose.ui.geometry.Size(12*scale, muntinW)
        )
    }
}

@Composable
fun CheckerboardDialog(
    onDismiss: () -> Unit,
    onApply: (Int, Int) -> Unit
) {
    var rows by remember { mutableStateOf("2") }
    var cols by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Szachownica (Siatka)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rows,
                    onValueChange = { rows = it },
                    label = { Text("Liczba pól w pionie (wiersze)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = cols,
                    onValueChange = { cols = it },
                    label = { Text("Liczba pól w poziomie (kolumny)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = rows.toIntOrNull() ?: 0
                val c = cols.toIntOrNull() ?: 0
                if (r > 0 && c > 0) {
                    onApply(c - 1, r - 1) // Lines = Cells - 1
                }
                onDismiss()
            }) {
                Text("Zastosuj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun OptimizationDialog(
    result: CuttingOptimizer.OptimizationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wynik Optymalizacji") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Długość sztangi: ${result.stockLengthMm} mm")
                Text("Odpad: ${"%.1f".format(result.wasteMm)} mm (${"%.1f".format(result.wastePercentage)}%)")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rozkrój:", style = MaterialTheme.typography.titleSmall)
                result.bars.forEach { bar ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Sztanga #${bar.id} (Reszta: ${"%.1f".format(bar.remainingMm)} mm)", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            bar.cuts.forEach { cut ->
                                Text("- ${cut.lengthMm.toInt()} mm (${cut.description})")
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

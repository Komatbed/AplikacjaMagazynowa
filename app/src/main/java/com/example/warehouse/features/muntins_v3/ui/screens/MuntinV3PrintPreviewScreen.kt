package com.example.warehouse.features.muntins_v3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.warehouse.features.muntins_v3.ui.canvas.StaticMuntinCanvas
import com.example.warehouse.features.muntins_v3.ui.viewmodel.MuntinV3ViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuntinV3PrintPreviewScreen(
    state: MuntinV3ViewModel.MuntinV3UiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podgląd Wydruku / Raport") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isExporting) return@IconButton
                        isExporting = true
                        try {
                            val pdf = PdfDocument()
                            val mmToPt = { mm: Double -> ((mm / 25.4) * 72.0).roundToInt() }
                            val pageWidth = mmToPt(100.0)
                            val pageHeight = mmToPt(40.0)
                            val textPaint = Paint().apply {
                                isAntiAlias = true
                                textSize = 14f
                            }
                            val smallPaint = Paint().apply {
                                isAntiAlias = true
                                textSize = 10f
                            }
                            state.cutList.forEachIndexed { index, item ->
                                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                                val page = pdf.startPage(pageInfo)
                                val canvas = page.canvas
                                
                                // Border
                                val borderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f }
                                canvas.drawRect(4f, 4f, pageWidth - 4f, pageHeight - 4f, borderPaint)
                                
                                // Content: kąt1 → wymiar → kąt2
                                val header = "${String.format("%.1f", item.angleStart)}° → ${String.format("%.1f", item.length)} mm → ${String.format("%.1f", item.angleEnd)}°"
                                canvas.drawText(header, 12f, 20f, textPaint)
                                
                                // Description and project
                                val desc = if (item.description.isNotEmpty()) item.description else "-"
                                canvas.drawText("Opis: $desc", 12f, 32f, smallPaint)
                                val proj = state.currentProject?.name ?: "-"
                                canvas.drawText("Projekt: $proj", 12f, 44f, smallPaint)
                                
                                pdf.finishPage(page)
                            }
                            
                            val outFile = File(context.cacheDir, "szprosy_labels_${System.currentTimeMillis()}.pdf")
                            pdf.writeTo(outFile.outputStream())
                            pdf.close()
                            
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outFile)
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, "Udostępnij / Drukuj PDF"))
                        } catch (e: Exception) {
                            // swallow
                        } finally {
                            isExporting = false
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share/Print")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "KARTA PRODUKCYJNA - SZPROSY",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Project Info Table
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Projekt:", state.currentProject?.name ?: "-")
                    InfoRow("Data:", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Profil:", state.selectedProfile?.name ?: "Domyślny")
                    InfoRow("Listwa:", state.selectedBead?.name ?: "Domyślna")
                    InfoRow("Wymiar ramy:", "${state.currentProject?.frameWidth?.toInt()} x ${state.currentProject?.frameHeight?.toInt()} mm")
                    InfoRow("Wymiar szyby (Obliczony):", "${state.glassWidth.toInt()} x ${state.glassHeight.toInt()} mm")
                    if (state.manualCorrection != 0.0) {
                        InfoRow("Korekta ręczna:", "${state.manualCorrection} mm", Color.Red)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Visual
            Text("RYSUNEK POGLĄDOWY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Fixed height for visual
                    .background(Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
            ) {
                StaticMuntinCanvas(
                    glassWidth = state.glassWidth,
                    glassHeight = state.glassHeight,
                    segments = state.segments,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Optimization / Cut List
            Text("LISTA CIĘĆ I OPTYMALIZACJA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.optimizationResult != null) {
                val result = state.optimizationResult
                Text("Sztanga: ${state.barLength.toInt()}mm | Rzaz: ${state.sawKerf}mm | Ilość skrzydeł: ${state.sashCount}")
                Text("Całkowite zużycie: ${result.totalBarsUsed} szt. (Odpad: ${String.format("%.1f", result.wastePercentage)}%)")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                result.bars.forEach { bar ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Sztanga #${bar.id} (Odpad: ${String.format("%.1f", bar.waste)}mm)", fontWeight = FontWeight.Bold)
                            bar.cuts.forEach { cut ->
                                Text(" • ${cut.description}", fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                Text("Brak danych optymalizacji.")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Assembly Instructions
            Text("INSTRUKCJA MONTAŻU", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            state.assemblySteps.forEach { step ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${step.orderIndex}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                    Column {
                        Text(step.description, fontWeight = FontWeight.Bold)
                        Text("Długość: ${step.length}mm", fontSize = 14.sp)
                        Text(step.positionLabel, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, fontWeight = FontWeight.Bold, color = color)
    }
}

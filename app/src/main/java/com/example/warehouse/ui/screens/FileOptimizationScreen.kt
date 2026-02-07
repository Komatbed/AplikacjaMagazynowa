package com.example.warehouse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.FileOptimizationViewModel
import com.example.warehouse.util.FileProcessingOptimizer
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileOptimizationScreen(
    onBackClick: () -> Unit,
    viewModel: FileOptimizationViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val outputContent by viewModel.outputContent.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // File Picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processFile(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Optymalizacja Plikowa (CT500 -> DCX)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wróć", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Controls
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Wybierz Tryb", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMode == FileProcessingOptimizer.Mode.MIN_WASTE,
                            onClick = { viewModel.setMode(FileProcessingOptimizer.Mode.MIN_WASTE) },
                            label = { Text("Min Odpad") }
                        )
                        FilterChip(
                            selected = selectedMode == FileProcessingOptimizer.Mode.LONGEST_FIRST,
                            onClick = { viewModel.setMode(FileProcessingOptimizer.Mode.LONGEST_FIRST) },
                            label = { Text("Najpierw Długie") }
                        )
                        FilterChip(
                            selected = selectedMode == FileProcessingOptimizer.Mode.DEFINED_WASTE,
                            onClick = { viewModel.setMode(FileProcessingOptimizer.Mode.DEFINED_WASTE) },
                            label = { Text("Odpady") }
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. Wczytaj Plik", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { launcher.launch("*/*") }, // Allow any text file
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WYBIERZ PLIK .CT500TXT")
                    }
                }
            }

            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SafetyOrange)
            }

            // Results
            if (outputContent.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wynik (DCX)", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                            Row {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(outputContent))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopiuj", tint = Color.White)
                                }
                                IconButton(onClick = {
                                    // Share Intent
                                    viewModel.saveOutput(context)
                                    val file = File(context.cacheDir, "optimized.dcxtxt")
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Udostępnij DCX"))
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Udostępnij", tint = Color.White)
                                }
                            }
                        }
                        
                        HorizontalDivider(color = Color.Gray)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.DarkGray)
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(outputContent, color = Color.Green, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
            } else if (logs.isNotEmpty()) {
                // Show logs if no output yet
                 Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Logi", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        LazyColumn {
                            items(logs) { log ->
                                Text(log, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

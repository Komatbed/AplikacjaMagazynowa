package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.HardwarePickerViewModel
import com.example.warehouse.util.FittingSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwarePickerScreen(
    onBackClick: () -> Unit,
    viewModel: HardwarePickerViewModel = viewModel()
) {
    val components by viewModel.components.collectAsState()
    val error by viewModel.error.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    var ffb by remember { mutableStateOf("") }
    var ffh by remember { mutableStateOf("") }
    var selectedSystem by remember { mutableStateOf(FittingSystem.ACTIVPILOT_CONCEPT) }
    var expandedProfile by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ASYSTENT OKUĆ", color = SafetyOrange) },
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
            // Presets Row
            if (presets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ulubione (Presety):",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { preset ->
                            AssistChip(
                                onClick = { viewModel.loadPreset(preset) },
                                label = { Text(preset.name) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SafetyOrange
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Configuration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Parametry Skrzydła (We Wrębie)", style = MaterialTheme.typography.titleMedium, color = Color.White)

                    // Profile Selection
                    ExposedDropdownMenuBox(
                        expanded = expandedProfile,
                        onExpandedChange = { expandedProfile = !expandedProfile }
                    ) {
                        OutlinedTextField(
                            value = selectedProfile?.code ?: "Wybierz profil (Opcjonalne)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("System Profilowy") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProfile) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedProfile,
                            onDismissRequest = { expandedProfile = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Brak (Uniwersalny)") },
                                onClick = {
                                    viewModel.selectProfile(null)
                                    expandedProfile = false
                                }
                            )
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text("${profile.code} (${profile.system})") },
                                    onClick = {
                                        viewModel.selectProfile(profile)
                                        expandedProfile = false
                                    }
                                )
                            }
                        }
                    }

                    // System Selection
                    Column {
                        Text("System Okuć:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedSystem == FittingSystem.ACTIVPILOT_CONCEPT,
                                onClick = { selectedSystem = FittingSystem.ACTIVPILOT_CONCEPT }
                            )
                            Text("activPilot Concept (Standard)", color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedSystem == FittingSystem.ACTIVPILOT_SELECT,
                                onClick = { selectedSystem = FittingSystem.ACTIVPILOT_SELECT }
                            )
                            Text("activPilot Select (Ukryte)", color = Color.White)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ffb,
                            onValueChange = { ffb = it },
                            label = { Text("Szerokość (FFB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ffh,
                            onValueChange = { ffh = it },
                            label = { Text("Wysokość (FFH)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { viewModel.calculate(selectedSystem, ffb, ffh) },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Build, null)
                        Spacer(Modifier.width(8.dp))
                        Text("DOBIERZ OKUCIA")
                    }
                }
            }

            // Error Message
            error?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(8.dp))
            }

            // Results
            if (components.isNotEmpty()) {
                Text("Lista Elementów:", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                
                components.forEach { comp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = comp.name, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                comp.note?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                comp.articleNumber?.let {
                                    Text(text = "Art. $it", style = MaterialTheme.typography.bodySmall, color = SafetyOrange)
                                }
                            }
                            Text(
                                text = "${comp.quantity} szt.",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.data.local.entity.PresetEntity
import com.example.warehouse.ui.components.DropdownField
import com.example.warehouse.ui.theme.DarkGrey
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.WasteFinderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteFinderScreen(
    onBackClick: () -> Unit,
    viewModel: WasteFinderViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val status by viewModel.searchStatus.collectAsState()
    val profiles by viewModel.profiles.collectAsState(initial = emptyList())
    val colors by viewModel.colors.collectAsState(initial = emptyList())
    val presets by viewModel.presets.collectAsState()

    var selectedProfile by remember { mutableStateOf("") }
    var selectedExtColor by remember { mutableStateOf("") }
    var selectedIntColor by remember { mutableStateOf("") }
    var minLength by remember { mutableStateOf("") }
    
    // Dialog for saving preset
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Zapisz ustawienia jako preset") },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Nazwa presetu (np. Veka Win/Win)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            viewModel.savePreset(newPresetName, selectedProfile, selectedExtColor, selectedIntColor)
                            showSavePresetDialog = false
                            newPresetName = ""
                        }
                    }
                ) { Text("Zapisz") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SZPERACZ ODPADÓW v2", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Wstecz", tint = Color.White)
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
                Text("SZYBKI WYBÓR", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        AssistChip(
                            onClick = {
                                selectedProfile = preset.profileCode
                                selectedExtColor = preset.externalColor
                                selectedIntColor = preset.internalColor
                            },
                            label = { Text(preset.name) },
                            leadingIcon = { Icon(Icons.Default.Star, null, tint = SafetyOrange, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Delete, 
                                    null, 
                                    modifier = Modifier.size(16.dp).clickable { viewModel.deletePreset(preset) },
                                    tint = Color.Gray
                                )
                            }
                        )
                    }
                }
            }

            // Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Parametry Odpadu", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        IconButton(onClick = { showSavePresetDialog = true }) {
                            Icon(Icons.Default.Star, "Zapisz Preset", tint = if (selectedProfile.isNotEmpty()) SafetyOrange else Color.Gray)
                        }
                    }
                    
                    // Profile Selection
                    DropdownField(
                        label = "Profil",
                        options = profiles.map { it.code },
                        selectedOption = selectedProfile,
                        onOptionSelected = { selectedProfile = it }
                    )

                    // Color Selection
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownField(
                                label = "Kolor Zewn.",
                                options = colors.map { it.code }, // Assuming unique codes or distinct
                                selectedOption = selectedExtColor,
                                onOptionSelected = { selectedExtColor = it }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownField(
                                label = "Kolor Wewn.",
                                options = colors.map { it.code } + "BIAŁY",
                                selectedOption = selectedIntColor,
                                onOptionSelected = { selectedIntColor = it }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = minLength,
                        onValueChange = { minLength = it },
                        label = { Text("Minimalna Długość (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val len = minLength.toIntOrNull()
                            if (selectedProfile.isNotEmpty() && len != null) {
                                viewModel.findWaste(
                                    selectedProfile, 
                                    len,
                                    selectedExtColor.takeIf { it.isNotEmpty() },
                                    selectedIntColor.takeIf { it.isNotEmpty() }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Search, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SZUKAJ ODPADU")
                        }
                    }
                }
            }


            // Result Area
            status?.let { msg ->
                Text(
                    text = msg,
                    color = if (result != null) Color.Green else Color.Red,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            result?.let { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGrey),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("ZNALAZŁEM!", color = SafetyOrange, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Długość: ${item.lengthMm} mm", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text("Lokalizacja: ${item.location.label}", style = MaterialTheme.typography.titleLarge, color = Color.Yellow)
                        Spacer(Modifier.height(8.dp))
                        Text("ID: ${item.id}", color = Color.Gray)
                        Text("Kolor: ${item.internalColor}/${item.externalColor}", color = Color.Gray)
                    }
                }
            }
        }
    }
}

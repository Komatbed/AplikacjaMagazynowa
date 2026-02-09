package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.AddInventoryViewModel
import com.example.warehouse.ui.viewmodel.AddInventoryViewModel.InternalColorMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryScreen(
    onBackClick: () -> Unit,
    viewModel: AddInventoryViewModel = viewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val colors by viewModel.colors.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val selectedExtColor by viewModel.selectedExternalColor.collectAsState()
    val internalMode by viewModel.internalColorMode.collectAsState()
    val coreColor by viewModel.calculatedCoreColor.collectAsState()

    var profileExpanded by remember { mutableStateOf(false) }
    var colorExpanded by remember { mutableStateOf(false) }
    
    // Dialog for saving preset
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Zapisz konfigurację jako preset") },
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
                        if (newPresetName.isNotBlank() && selectedProfile != null && selectedExtColor != null) {
                            viewModel.savePreset(newPresetName)
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
                title = { Text("DODAJ ELEMENT", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz", tint = Color.White)
                    }
                },
                actions = {
                    if (selectedProfile != null && selectedExtColor != null) {
                        IconButton(onClick = { showSavePresetDialog = true }) {
                            Icon(Icons.Default.Star, "Zapisz Preset", tint = SafetyOrange)
                        }
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
            // Delivery Days Info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DNI DOSTAW", style = MaterialTheme.typography.titleSmall, color = SafetyOrange, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Veka: Poniedziałek, Czwartek", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Salamander: Wtorek, Piątek", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Aluron: Środa", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Panele: Środa", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // Presets Row
            if (presets.isNotEmpty()) {
                Text("SZYBKI WYBÓR (PRESETY)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        AssistChip(
                            onClick = { viewModel.loadPreset(preset) },
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

            // Profile Selection
            ExposedDropdownMenuBox(
                expanded = profileExpanded,
                onExpandedChange = { profileExpanded = !profileExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProfile?.code ?: "Wybierz profil",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Profil") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                ExposedDropdownMenu(
                    expanded = profileExpanded,
                    onDismissRequest = { profileExpanded = false }
                ) {
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text("${profile.code} (${profile.system})") },
                            onClick = {
                                viewModel.selectProfile(profile)
                                profileExpanded = false
                            }
                        )
                    }
                }
            }

            // External Color Selection
            ExposedDropdownMenuBox(
                expanded = colorExpanded,
                onExpandedChange = { colorExpanded = !colorExpanded }
            ) {
                OutlinedTextField(
                    value = selectedExtColor?.name ?: selectedExtColor?.code ?: "Wybierz kolor zewn.",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kolor Zewnętrzny") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                ExposedDropdownMenu(
                    expanded = colorExpanded,
                    onDismissRequest = { colorExpanded = false }
                ) {
                    colors.forEach { color ->
                        DropdownMenuItem(
                            text = { Text("${color.name} (${color.code})") },
                            onClick = {
                                viewModel.selectExternalColor(color)
                                colorExpanded = false
                            }
                        )
                    }
                }
            }

            // Internal Color Selection (Buttons)
            Text("Kolor Wewnętrzny", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableButton(
                    text = "BIAŁY",
                    selected = internalMode == InternalColorMode.WHITE,
                    onClick = { viewModel.setInternalColorMode(InternalColorMode.WHITE) },
                    modifier = Modifier.weight(1f)
                )
                SelectableButton(
                    text = "TAKI SAM",
                    selected = internalMode == InternalColorMode.SAME_AS_EXTERNAL,
                    onClick = { viewModel.setInternalColorMode(InternalColorMode.SAME_AS_EXTERNAL) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Core Color (Read-only)
            OutlinedTextField(
                value = coreColor,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kolor Rdzenia (Automatyczny)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SafetyOrange,
                    unfocusedTextColor = SafetyOrange,
                    disabledTextColor = SafetyOrange,
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray
                ),
                enabled = false
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { /* TODO: Save logic */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
            ) {
                Text("DODAJ DO MAGAZYNU", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SelectableButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) SafetyOrange else Color.DarkGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (selected) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text)
    }
}

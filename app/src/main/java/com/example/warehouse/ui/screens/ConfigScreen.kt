package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.data.model.ColorDefinition
import com.example.warehouse.data.model.ProfileDefinition
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.theme.DarkGrey
import com.example.warehouse.ui.viewmodel.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onBackClick: () -> Unit,
    viewModel: ConfigViewModel = viewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val colors by viewModel.colors.collectAsState()
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var selectedTab by remember { mutableStateOf(0) } // 0 for Profiles, 1 for Colors
    
    // Dialog State
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (showAddDialog) {
        AddConfigDialog(
            isProfile = selectedTab == 0,
            onDismiss = { showAddDialog = false },
            onConfirm = { code, desc ->
                if (selectedTab == 0) {
                    viewModel.addProfile(code, desc)
                } else {
                    viewModel.addColor(code, desc)
                }
                showAddDialog = false
            }
        )
    }

    Scaffold(
        containerColor = DarkGrey,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SafetyOrange,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                }
                Text(
                    text = "KONFIGURACJA SŁOWNIKÓW",
                    style = MaterialTheme.typography.titleLarge,
                    color = SafetyOrange
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SafetyOrange
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("PROFILE") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("KOLORY") }
                )
            }

            // Error
            error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
            }

            // List
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SafetyOrange)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    if (selectedTab == 0) {
                        items(profiles) { profile ->
                            ConfigItemRow(
                                code = profile.code,
                                description = profile.description,
                                onDelete = { viewModel.deleteProfile(profile.id.toString()) } // UUID to String
                            )
                        }
                    } else {
                        items(colors) { color ->
                            ConfigItemRow(
                                code = color.code,
                                description = color.description,
                                onDelete = { viewModel.deleteColor(color.id.toString()) } // UUID to String
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigItemRow(code: String, description: String, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = code, style = MaterialTheme.typography.titleMedium, color = Color.White)
                if (description.isNotBlank()) {
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AddConfigDialog(
    isProfile: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = SafetyOrange,
        textContentColor = Color.White,
        onDismissRequest = onDismiss,
        title = { Text(if (isProfile) "Dodaj Profil" else "Dodaj Kolor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kod (np. 504010)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = SafetyOrange,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = SafetyOrange,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis (Opcjonalny)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = SafetyOrange,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = SafetyOrange,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        onConfirm(code, description)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
            ) {
                Text("DODAJ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANULUJ", color = Color.Gray)
            }
        }
    )
}

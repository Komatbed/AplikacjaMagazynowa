package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.theme.LighterGrey
import com.example.warehouse.ui.viewmodel.InventoryViewModel

import com.example.warehouse.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTakeScreen(
    onBackClick: () -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    var profileCode by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var internalColor by remember { mutableStateOf("") }
    var externalColor by remember { mutableStateOf("") }
    var coreColor by remember { mutableStateOf("") }
    
    // Configs
    val profiles by viewModel.profiles.collectAsState()
    val colors by viewModel.colors.collectAsState()

    // Dialog state
    var selectedItem by remember { mutableStateOf<InventoryItemDto?>(null) }
    var takeQuantity by remember { mutableStateOf("1") }
    
    val items by viewModel.items
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    if (selectedItem != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = SafetyOrange,
            textContentColor = Color.White,
            onDismissRequest = { selectedItem = null },
            title = { Text("Pobierz Element") },
            text = {
                Column {
                    Text("Profil: ${selectedItem?.profileCode}", color = Color.White)
                    Text("Lokalizacja: ${selectedItem?.location?.label}", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                    value = takeQuantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) takeQuantity = it },
                    label = { Text("Ilość do pobrania") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = SafetyOrange,
                        unfocusedLabelColor = Color.Gray,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = Color.Gray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = takeQuantity.toIntOrNull() ?: 0
                        if (qty > 0 && qty <= (selectedItem?.quantity ?: 0)) {
                            viewModel.takeItem(
                                InventoryTakeRequest(
                                    profileCode = selectedItem!!.profileCode,
                                    lengthMm = selectedItem!!.lengthMm,
                                    locationLabel = selectedItem!!.location.label,
                                    quantity = qty,
                                    reason = "MANUAL_TAKE"
                                )
                            ) {
                                onShowMessage("Pobrano $qty szt.")
                                selectedItem = null
                                // Optional: refresh list
                                viewModel.loadItems(
                                    location = location.takeIf { it.isNotBlank() },
                                    profileCode = profileCode.takeIf { it.isNotBlank() },
                                    internalColor = internalColor.takeIf { it.isNotBlank() },
                                    externalColor = externalColor.takeIf { it.isNotBlank() },
                                    coreColor = coreColor.takeIf { it.isNotBlank() }
                                )
                            }
                        } else {
                            onShowMessage("Nieprawidłowa ilość")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                ) {
                    Text("ZATWIERDŹ")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItem = null }) {
                    Text("ANULUJ", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("POBIERANIE RĘCZNE", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = SafetyOrange
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DropdownField(
                        label = "Kod Profila",
                        options = profiles,
                        selectedOption = profileCode,
                        onOptionSelected = { profileCode = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownField(
                        label = "Kolor Wewnętrzny",
                        options = colors,
                        selectedOption = internalColor,
                        onOptionSelected = { internalColor = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownField(
                        label = "Kolor Zewnętrzny",
                        options = colors,
                        selectedOption = externalColor,
                        onOptionSelected = { externalColor = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownField(
                        label = "Kolor Rdzenia (Opcjonalny)",
                        options = colors + listOf("BRAK"),
                        selectedOption = coreColor,
                        onOptionSelected = { coreColor = if (it == "BRAK") "" else it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lokalizacja (np. 01A)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = SafetyOrange,
                            unfocusedLabelColor = Color.Gray,
                            focusedBorderColor = SafetyOrange,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Button(
                        onClick = { 
                            viewModel.loadItems(
                                location = location.takeIf { it.isNotBlank() },
                                profileCode = profileCode.takeIf { it.isNotBlank() },
                                internalColor = internalColor.takeIf { it.isNotBlank() },
                                externalColor = externalColor.takeIf { it.isNotBlank() },
                                coreColor = coreColor.takeIf { it.isNotBlank() }
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SZUKAJ")
                    }
                }
            }

            // Results
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SafetyOrange)
                }
            } else if (error != null) {
                Text("Błąd: $error", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        InventoryItemRow(item) {
                            selectedItem = item
                            takeQuantity = "1"
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemRow(item: InventoryItemDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Profil: ${item.profileCode}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${item.internalColor} / ${item.externalColor}${if (!item.coreColor.isNullOrBlank()) " / ${item.coreColor}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                Text(
                    text = "Długość: ${item.lengthMm} mm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.location.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SafetyOrange,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "Ilość: ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

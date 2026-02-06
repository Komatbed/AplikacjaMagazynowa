package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.warehouse.util.HapticFeedbackManager
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBackClick: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val items by viewModel.items
    val profiles by viewModel.profiles.collectAsState()
    val colors by viewModel.colors.collectAsState()
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Filters
    var selectedProfile by remember { mutableStateOf<String?>(null) }
    var selectedInternalColor by remember { mutableStateOf<String?>(null) }
    var selectedExternalColor by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    // Dialogs
    var showEditLengthDialog by remember { mutableStateOf<InventoryItemDto?>(null) }

    if (showEditLengthDialog != null) {
        EditLengthDialog(
            item = showEditLengthDialog!!,
            onDismiss = { showEditLengthDialog = null },
            onConfirm = { newItem, newLength ->
                viewModel.updateItemLength(newItem, newLength)
                showEditLengthDialog = null
            }
        )
    }

    LaunchedEffect(selectedProfile, selectedInternalColor, selectedExternalColor) {
        viewModel.loadItems(
            profileCode = selectedProfile,
            internalColor = selectedInternalColor,
            externalColor = selectedExternalColor
        )
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STAN MAGAZYNOWY", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Wstecz", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportToCsv(context) }) {
                        Icon(Icons.Default.Share, "Eksportuj CSV", tint = Color.White)
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, "Filtry", tint = if(showFilters) SafetyOrange else Color.White)
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
        ) {
            if (showFilters) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Filtrowanie", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                        
                        // Profile Dropdown (Simplified as TextField for now, ideally ExposedDropdownMenu)
                        DropdownField("Profil", profiles, selectedProfile) { selectedProfile = it }
                        DropdownField("Kolor Wewn.", colors, selectedInternalColor) { selectedInternalColor = it }
                        DropdownField("Kolor Zewn.", colors, selectedExternalColor) { selectedExternalColor = it }
                        
                        Button(
                            onClick = {
                                selectedProfile = null
                                selectedInternalColor = null
                                selectedExternalColor = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Wyczyść filtry")
                        }
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SafetyOrange)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        InventoryItemRow(item, 
                            onReserve = { 
                                viewModel.takeItem(InventoryTakeRequest(
                                    locationLabel = item.location.label,
                                    profileCode = item.profileCode,
                                    lengthMm = item.lengthMm,
                                    quantity = 1,
                                    reason = "PRODUCTION"
                                )) {}
                            },
                            onDelete = {
                                viewModel.takeItem(InventoryTakeRequest(
                                    locationLabel = item.location.label,
                                    profileCode = item.profileCode,
                                    lengthMm = item.lengthMm,
                                    quantity = item.quantity,
                                    reason = "PRODUCTION"
                                )) {}
                            },
                            onEditLength = {
                                showEditLengthDialog = item
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemRow(
    item: InventoryItemDto,
    onReserve: () -> Unit,
    onDelete: () -> Unit,
    onEditLength: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Profil: ${item.profileCode}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Text(text = "${item.lengthMm} mm", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                     IconButton(onClick = onEditLength) {
                         Icon(Icons.Default.Edit, "Edytuj długość", tint = Color.Gray, modifier = Modifier.size(16.dp))
                     }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(text = "Kolory: ${item.internalColor} / ${item.externalColor} (Rdzeń: ${item.coreColor ?: "-"})", color = Color.Gray)
            Text(text = "Paleta: ${item.location.label} | Ilość: ${item.quantity}", color = Color.Gray)
            
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onReserve) {
                    Icon(Icons.Default.ShoppingCart, null, tint = SafetyOrange)
                    Spacer(Modifier.width(4.dp))
                    Text("Rezerwuj", color = SafetyOrange)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                    Spacer(Modifier.width(4.dp))
                    Text("Usuń", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun EditLengthDialog(
    item: InventoryItemDto,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItemDto, Int) -> Unit
) {
    var newLength by remember { mutableStateOf(item.lengthMm.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edycja Długości", color = SafetyOrange) },
        text = {
            Column {
                Text("Profil: ${item.profileCode}", color = Color.White)
                Text("Obecna długość: ${item.lengthMm} mm", color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newLength,
                    onValueChange = { newLength = it },
                    label = { Text("Nowa Długość (mm)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val length = newLength.toIntOrNull()
                    if (length != null && length > 0) {
                        onConfirm(item, length)
                    }
                }
            ) {
                Text("Zapisz", color = SafetyOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = SafetyOrange,
        textContentColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected ?: "Wszystkie",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

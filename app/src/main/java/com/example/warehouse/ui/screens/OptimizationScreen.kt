package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
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
import com.example.warehouse.ui.components.DropdownField
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.OptimizationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationScreen(
    onBackClick: () -> Unit,
    viewModel: OptimizationViewModel = viewModel()
) {
    var profileCode by remember { mutableStateOf("") }
    var internalColor by remember { mutableStateOf("") }
    var externalColor by remember { mutableStateOf("") }
    var coreColor by remember { mutableStateOf("") }

    // Adding pieces
    var currentLength by remember { mutableStateOf("") }
    var currentQuantity by remember { mutableStateOf("1") }
    
    val pieces = remember { mutableStateListOf<Int>() }

    val profiles by viewModel.profiles
    val colors by viewModel.colors
    val result by viewModel.result
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OPTYMALIZACJA CIĘCIA", color = SafetyOrange) },
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Configuration Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            }
        }

        // Add Pieces Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = currentLength,
                onValueChange = { if (it.all { c -> c.isDigit() }) currentLength = it },
                label = { Text("Długość (mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = currentQuantity,
                onValueChange = { if (it.all { c -> c.isDigit() }) currentQuantity = it },
                label = { Text("Ilość") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    val len = currentLength.toIntOrNull()
                    val qty = currentQuantity.toIntOrNull() ?: 1
                    if (len != null && len > 0 && qty > 0) {
                        repeat(qty) { pieces.add(len) }
                        currentLength = ""
                        currentQuantity = "1"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }

        // List of Pieces & Calculate Button
        Row(modifier = Modifier.weight(1f)) {
            // Left: List of pieces
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("Lista Detali (${pieces.size}):", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    items(pieces) { piece ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$piece mm", color = Color.White)
                            IconButton(onClick = { pieces.remove(piece) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Red)
                            }
                        }
                        Divider(color = Color.Gray, thickness = 0.5.dp)
                    }
                }
            }
            
            Spacer(Modifier.width(16.dp))

            // Right: Result or Button
            Column(modifier = Modifier.weight(1.5f).fillMaxHeight()) {
                Button(
                    onClick = {
                        if (profileCode.isNotBlank() && internalColor.isNotBlank() && externalColor.isNotBlank() && pieces.isNotEmpty()) {
                            val finalCore = if (coreColor.isBlank()) null else coreColor
                            viewModel.calculate(profileCode, internalColor, externalColor, finalCore, pieces)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                    enabled = !isLoading && pieces.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("OBLICZ PLAN CIĘCIA")
                    }
                }

                Spacer(Modifier.height(8.dp))

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                result?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                    ) {
                        Column(modifier = Modifier.padding(8.dp).fillMaxSize()) {
                            Text(
                                "Wynik (Wydajność: ${String.format("%.1f", res.efficiency)}%)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Green
                            )
                            Text("Odpad całkowity: ${res.totalWasteMm} mm", style = MaterialTheme.typography.bodySmall)
                            
                            Divider(Modifier.padding(vertical = 4.dp))
                            
                            LazyColumn {
                                items(res.steps) { step ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        val source = if(step.isNewBar) "NOWA SZTANGA" else "ODPAD ${step.sourceItemId?.take(4)}..."
                                        Text("$source (${step.sourceLengthMm}mm) @ ${step.locationLabel}", 
                                            style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        Text("Cięcia: ${step.cuts.joinToString(", ")} mm", style = MaterialTheme.typography.bodySmall)
                                        Text("Pozostało: ${step.remainingWasteMm} mm", style = MaterialTheme.typography.bodySmall, color = Color.Yellow)
                                    }
                                    Divider(color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

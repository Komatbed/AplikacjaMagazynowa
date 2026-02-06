package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

    var selectedProfile by remember { mutableStateOf("") }
    var minLength by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SZPERACZ ODPADÓW", color = SafetyOrange) },
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
            // Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Czego szukasz?", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    
                    // Profile Selection
                    DropdownField(
                        label = "Profil (np. P1, P2)",
                        options = profiles.map { it.code },
                        selected = selectedProfile.takeIf { it.isNotEmpty() },
                        onSelect = { selectedProfile = it }
                    )

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
                                viewModel.findWaste(selectedProfile, len)
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
                            Text("ZNAJDŹ ODPAD")
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

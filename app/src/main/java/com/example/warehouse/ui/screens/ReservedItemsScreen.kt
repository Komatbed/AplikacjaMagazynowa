package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.ReservedItemsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservedItemsScreen(
    onBackClick: () -> Unit,
    viewModel: ReservedItemsViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val filterUser by viewModel.filterUser.collectAsState()
    val filterDate by viewModel.filterDate.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZAREZERWOWANE", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Wstecz", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadItems() }) {
                        Icon(Icons.Default.Refresh, "Odśwież", tint = Color.White)
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
            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterUser,
                    onValueChange = { viewModel.updateFilterUser(it) },
                    label = { Text("Użytkownik") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = filterDate,
                    onValueChange = { viewModel.updateFilterDate(it) },
                    label = { Text("Data (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SafetyOrange)
                }
            } else if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        ReservedItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun ReservedItemCard(item: InventoryItemDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.profileCode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SafetyOrange
                )
                Text(
                    text = item.status,
                    color = if (item.status == "RESERVED") Color.Yellow else Color.Cyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("Lokalizacja: ${item.location.label}", color = Color.White)
            Text("Długość: ${item.lengthMm} mm", color = Color.White)
            Text("Ilość: ${item.quantity}", color = Color.White)
            if (item.reservedBy != null) {
                Text("Rezerwacja: ${item.reservedBy} (${item.reservationDate ?: ""})", color = Color.Gray)
            }
        }
    }
}

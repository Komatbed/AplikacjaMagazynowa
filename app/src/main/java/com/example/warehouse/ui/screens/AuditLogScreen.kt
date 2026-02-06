package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.DarkGrey
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.AuditLogViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    onBackClick: () -> Unit,
    viewModel: AuditLogViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    Scaffold(
        containerColor = DarkGrey
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
                }
                Text(
                    text = "RAPORTY I HISTORIA",
                    style = MaterialTheme.typography.titleLarge,
                    color = SafetyOrange,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Wyczyść", tint = Color.Gray)
                }
            }

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard("Dziś", stats.actionsToday.toString(), Modifier.weight(1f))
                StatsCard("Magazyn", stats.inventoryOps.toString(), Modifier.weight(1f))
                StatsCard("Konfig", stats.configChanges.toString(), Modifier.weight(1f))
            }

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text("Wszystkie") }
                )
                FilterChip(
                    selected = filter == "INVENTORY",
                    onClick = { viewModel.setFilter("INVENTORY") },
                    label = { Text("Magazyn") }
                )
                FilterChip(
                    selected = filter == "CONFIG",
                    onClick = { viewModel.setFilter("CONFIG") },
                    label = { Text("Konfiguracja") }
                )
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if(log.itemType == "CONFIG") Color.Cyan else SafetyOrange
                                )
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

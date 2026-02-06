package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.data.model.LocationStatusDto
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.theme.DarkGrey
import com.example.warehouse.ui.viewmodel.WarehouseMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseMapScreen(
    onBackClick: () -> Unit,
    onLocationClick: (String) -> Unit,
    viewModel: WarehouseMapViewModel = viewModel()
) {
    val locations by viewModel.locations
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    // Group by Rack Column (1, 2, 3... 25)
    // We want to display these groups horizontally (01, 02, 03...)
    // Inside each group, we stack cells vertically (01A, 01B, 01C)
    val rackColumns = remember(locations) {
        locations.groupBy { it.rowNumber } // Group by 1..25
            .toSortedMap()
            .mapValues { (_, locs) -> 
                // Sort by palette letter (A, B, C...)
                locs.sortedBy { it.label?.lastOrNull { char -> char.isLetter() } ?: 'Z' }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAPA MAGAZYNU", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Wstecz", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMap() }) {
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
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SafetyOrange)
                }
            } else if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            } else {
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = Color.Gray, text = "Puste")
                    LegendItem(color = Color(0xFF4CAF50), text = "Zajęte")
                    LegendItem(color = Color.Red, text = "Pełne")
                    LegendItem(color = SafetyOrange, text = "Odpady")
                }

                // Horizontal Scrollable Container for Rack Columns (01..25)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rackColumns.forEach { (colNum, locs) ->
                        // Vertical Strip for each Rack Column
                        Column(
                            modifier = Modifier
                                .width(120.dp) // Fixed width for each vertical strip
                                .wrapContentHeight()
                                .background(DarkGrey.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Header (01, 02...)
                            Text(
                                text = "Kolumna $colNum",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SafetyOrange,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                            )
                            
                            // Cells (A, B, C) - Just Column is fine for 3 items
                            locs.forEach { loc ->
                                LocationCell(location = loc, onClick = { onLocationClick(loc.label ?: "") })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
fun LocationCell(location: LocationStatusDto, onClick: () -> Unit) {
    val isFull = location.itemCount >= 50 // Threshold for "Full"
    val backgroundColor = when {
        location.itemCount == 0 -> Color.Gray.copy(alpha = 0.3f)
        location.isWaste -> SafetyOrange.copy(alpha = 0.8f) // Waste palette
        isFull -> Color.Red.copy(alpha = 0.8f) // Full palette
        else -> Color(0xFF4CAF50).copy(alpha = 0.8f) // Standard occupied palette
    }

    Card(
        modifier = Modifier
            .height(80.dp) // Fixed height for vertical stacking
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = location.label ?: "???",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${location.itemCount} szt.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            val codes = location.profileCodes ?: emptyList()
            if (codes.isNotEmpty()) {
                Text(
                    text = codes.firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

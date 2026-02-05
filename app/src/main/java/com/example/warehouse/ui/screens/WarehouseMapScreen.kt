package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.sp
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = Color.Gray, text = "Puste")
                    LegendItem(color = Color(0xFF4CAF50), text = "Pełne")
                    LegendItem(color = SafetyOrange, text = "Odpady")
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(locations) { loc ->
                        LocationCell(location = loc, onClick = { onLocationClick(loc.label ?: "") })
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
    val backgroundColor = when {
        location.itemCount == 0 -> Color.Gray.copy(alpha = 0.3f)
        location.isWaste -> SafetyOrange.copy(alpha = 0.8f) // Waste palette
        else -> Color(0xFF4CAF50).copy(alpha = 0.8f) // Standard palette
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
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

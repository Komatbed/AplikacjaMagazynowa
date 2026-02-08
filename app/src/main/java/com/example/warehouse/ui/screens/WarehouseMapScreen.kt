package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    
    // Stan dla wybranej palety (do podglądu)
    var selectedLocation by remember { mutableStateOf<LocationStatusDto?>(null) }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz", tint = Color.White)
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
                        .fillMaxWidth()
                        .weight(1f) // Mapa zajmuje dostępną przestrzeń
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rackColumns.forEach { (colNum, locs) ->
                        // Vertical Strip for each Rack Column
                        Column(
                            modifier = Modifier
                                .width(160.dp) // Zwiększona szerokość dla etykiet
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
                            
                            // Cells (A, B, C)
                            locs.forEach { loc ->
                                LocationCell(
                                    location = loc, 
                                    isSelected = selectedLocation?.id == loc.id,
                                    onClick = { selectedLocation = loc }
                                )
                            }
                        }
                    }
                }
                
                // Panel podglądu wybranej palety
                if (selectedLocation != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp), // Stała wysokość panelu podglądu
                        colors = CardDefaults.cardColors(containerColor = DarkGrey),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Paleta: ${selectedLocation?.label}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = SafetyOrange,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { onLocationClick(selectedLocation?.label ?: "") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                                ) {
                                    Text("ZOBACZ SZCZEGÓŁY")
                                }
                            }
                            
                            HorizontalDivider(color = Color.Gray)
                            
                            Text(
                                text = "Ilość: ${selectedLocation?.itemCount} szt.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            
                            val codes = selectedLocation?.profileCodes ?: emptyList()
                            Text(
                                text = if (codes.isNotEmpty()) "Zawartość: ${codes.joinToString(", ")}" else "Zawartość: Pusta",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 3
                            )
                        }
                    }
                } else {
                    // Placeholder gdy nic nie wybrano (opcjonalnie)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(DarkGrey.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Wybierz paletę, aby zobaczyć podgląd",
                            color = Color.Gray
                        )
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
fun LocationCell(
    location: LocationStatusDto, 
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isFull = location.itemCount >= 50
    val baseColor = when {
        location.itemCount == 0 -> Color.Gray.copy(alpha = 0.3f)
        location.isWaste -> SafetyOrange.copy(alpha = 0.8f)
        isFull -> Color.Red.copy(alpha = 0.8f)
        else -> Color(0xFF4CAF50).copy(alpha = 0.8f)
    }
    
    val backgroundColor = if (isSelected) baseColor.copy(alpha = 1f) else baseColor
    val borderWidth = if (isSelected) 3.dp else 0.dp
    val borderColor = if (isSelected) Color.White else Color.Transparent

    Card(
        modifier = Modifier
            .height(110.dp) // Zwiększona wysokość
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
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
            
            // Etykieta zawartości (15 znaków)
            val codes = location.profileCodes ?: emptyList()
            if (codes.isNotEmpty()) {
                val contentLabel = codes.joinToString(", ")
                Text(
                    text = if (contentLabel.length > 15) contentLabel.take(15) + "..." else contentLabel,
                    style = MaterialTheme.typography.labelSmall, // Mała czcionka
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

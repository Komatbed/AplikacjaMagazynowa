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
import androidx.compose.material.icons.filled.Edit
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
    
    // Stan dla edycji pojemności
    var showCapacityDialog by remember { mutableStateOf(false) }
    var newCapacityStr by remember { mutableStateOf("") }

    if (showCapacityDialog && selectedLocation != null) {
        AlertDialog(
            onDismissRequest = { showCapacityDialog = false },
            title = { Text("Ustaw pojemność palety") },
            text = {
                OutlinedTextField(
                    value = newCapacityStr,
                    onValueChange = { newCapacityStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Pojemność (szt.)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cap = newCapacityStr.toIntOrNull()
                        if (cap != null && cap > 0) {
                            viewModel.updateCapacity(selectedLocation!!.id, cap)
                            showCapacityDialog = false
                        }
                    }
                ) { Text("Zapisz") }
            },
            dismissButton = {
                TextButton(onClick = { showCapacityDialog = false }) { Text("Anuluj") }
            }
        )
    }

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
                            
                            // Capacity Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pojemność: ${selectedLocation?.itemCount} / ${selectedLocation?.capacity ?: 50}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                                IconButton(onClick = { 
                                    newCapacityStr = (selectedLocation?.capacity ?: 50).toString()
                                    showCapacityDialog = true 
                                }) {
                                    Icon(androidx.compose.material.icons.Icons.Filled.Edit, "Edytuj", tint = SafetyOrange)
                                }
                            }
                            
                            // Visualisation
                            val capacity = selectedLocation?.capacity ?: 50
                            val count = selectedLocation?.itemCount ?: 0
                            val percent = (count.toFloat() / capacity).coerceIn(0f, 1f)
                            val coreColors = selectedLocation?.coreColors ?: emptyList()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            ) {
                                if (percent > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(percent).fillMaxHeight()) {
                                        if (coreColors.isNotEmpty()) {
                                            coreColors.forEach { colorName ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(parseCoreColor(colorName))
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(SafetyOrange)
                                            )
                                        }
                                    }
                                }
                                
                                Text(
                                    text = "${(percent * 100).toInt()}%",
                                    modifier = Modifier.align(Alignment.Center),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 2f)
                                    ),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${location.itemCount} / ${location.capacity} szt.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            // Mini visualization in cell
            val colors = location.coreColors ?: emptyList()
            if (colors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .padding(1.dp)
                ) {
                    colors.forEach { colorName ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(parseCoreColor(colorName))
                        )
                    }
                }
            }
        }
    }
}

fun parseCoreColor(colorName: String): Color {
    return when (colorName.lowercase()) {
        "biały", "white", "bialy" -> Color.White
        "brąz", "brown", "braz" -> Color(0xFF5D4037)
        "karmel", "caramel" -> Color(0xFFD7CCC8)
        "szary", "grey", "gray" -> Color.Gray
        "antracyt", "anthracite" -> Color.DarkGray
        "czarny", "black" -> Color.Black
        else -> Color.LightGray
    }
}

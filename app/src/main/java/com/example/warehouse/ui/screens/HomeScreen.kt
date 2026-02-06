package com.example.warehouse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.LighterGrey
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.DashboardViewModel
import com.example.warehouse.ui.viewmodel.NewsItem

@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onManualTakeClick: () -> Unit,
    onWasteClick: () -> Unit,
    onOptimizationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onConfigClick: () -> Unit,
    onMapClick: () -> Unit,
    onIssueClick: () -> Unit,
    onMuntinClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onWindowCalcClick: () -> Unit,
    onReservationsClick: () -> Unit,
    onWasteFinderClick: () -> Unit,
    onHardwareClick: () -> Unit,
    onCatalogClick: () -> Unit,
    onTrainingClick: () -> Unit,
    onAddInventoryClick: () -> Unit,
    onAuditLogClick: () -> Unit,
    isOffline: Boolean = false,
    viewModel: DashboardViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints {
            val screenWidth = maxWidth
            val isTablet = screenWidth > 600.dp
            val isDesktop = screenWidth > 1000.dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MAGAZYN PVC",
                            style = MaterialTheme.typography.titleLarge, // Reduced font size
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "HALA PRODUKCYJNA A",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOffline) Color.Red.copy(alpha = 0.2f) else Color.Green.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isOffline) "OFFLINE" else "ONLINE",
                                color = if (isOffline) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Ustawienia",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                // News Widget
                if (stats.news.isNotEmpty()) {
                    NewsWidget(stats.news.take(5))
                }

                // Dashboard Stats
                // Desktop: 4 cols, Tablet: 2 cols, Mobile: 1 col (or 2 small)
                // Using simple Rows for grid layout based on screen size
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tiles = listOf(
                        Triple("STAN CAŁKOWITY", "${stats.totalItems}", stats.totalItemsChange) to onInventoryClick,
                        Triple("REZERWACJE", "${stats.reservationCount}", stats.reservationChange) to onReservationsClick,
                        Triple("WOLNE PALETY", "${stats.freePalettes}", stats.freePalettesChange) to onMapClick,
                        Triple("ZAJĘTOŚĆ", "${"%.1f".format(stats.occupancyPercent)}%", stats.occupancyChange) to onMapClick
                    )

                    if (isDesktop) {
                        // 1 Row, 4 Columns
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            tiles.forEach { (data, onClick) ->
                                StatisticCard(
                                    title = data.first,
                                    value = data.second,
                                    change = data.third,
                                    icon = Icons.Default.Inventory, // Generic icon or specific
                                    color = SafetyOrange,
                                    onClick = onClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // 2 Rows, 2 Columns (Tablet/Mobile)
                        tiles.chunked(2).forEach { rowTiles ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                rowTiles.forEach { (data, onClick) ->
                                    StatisticCard(
                                        title = data.first,
                                        value = data.second,
                                        change = data.third,
                                        icon = Icons.Default.Inventory,
                                        color = SafetyOrange,
                                        onClick = onClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill empty space if odd number
                                if (rowTiles.size < 2) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Quick Actions Grid
                Text(
                    text = "SZYBKIE AKCJE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Layout for buttons
                // Desktop/Tablet: 4 buttons per row? Or keep existing 2 per row structure but optimize sizing
                // User asked for "maksymalnie 4 kafelki w rzędzie na desktopie" (refers to stats? or all tiles?)
                // Assuming stats primarily. But buttons can also be 4 per row on desktop.
                
                val buttons = listOf(
                    Triple("SKANUJ (OCR)", Icons.Default.CameraAlt, onScanClick),
                    Triple("WYDANIE RĘCZNE", Icons.Default.Edit, onManualTakeClick),
                    Triple("MAPA MAGAZYNU", Icons.Default.Map, onMapClick),
                    Triple("OPTYMALIZACJA", Icons.Default.ContentCut, onOptimizationClick),
                    Triple("ZGŁOŚ PROBLEM", Icons.Default.Warning, onIssueClick),
                    Triple("SŁOWNIKI", Icons.Default.List, onConfigClick),
                    Triple("SZPROSY", Icons.Default.Calculate, onMuntinClick),
                    Triple("STAN MAGAZYNU", Icons.Default.Inventory, onInventoryClick),
                    Triple("KALKULATOR OKIEN", Icons.Default.Window, onWindowCalcClick),
                    Triple("REZERWACJE", Icons.Default.Bookmark, onReservationsClick),
                    Triple("SZPERACZ ODPADÓW", Icons.Default.Search, onWasteFinderClick),
                    Triple("ASYSTENT OKUĆ", Icons.Default.Build, onHardwareClick),
                    Triple("KATALOG PRODUKTÓW", Icons.Default.MenuBook, onCatalogClick),
                    Triple("BAZA WIEDZY", Icons.Default.Info, onTrainingClick),
                    Triple("DODAJ ELEMENT", Icons.Default.Add, onAddInventoryClick),
                    Triple("HISTORIA ZMIAN", Icons.Default.History, onAuditLogClick)
                )

                val cols = if (isDesktop) 4 else if (isTablet) 3 else 2
                
                buttons.chunked(cols).forEach { rowButtons ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowButtons.forEach { (text, icon, onClick) ->
                            DashboardButton(text, icon, onClick, Modifier.weight(1f))
                        }
                        repeat(cols - rowButtons.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsWidget(news: List<NewsItem>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("AKTUALNOŚCI", style = MaterialTheme.typography.titleSmall, color = SafetyOrange, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            news.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.title, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                    Text(item.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    change: Double,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp) // Fixed height to prevent wrapping issues
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                
                val changeColor = if (change >= 0) Color.Green else Color.Red
                val changeSign = if (change > 0) "+" else ""
                Text(
                    "$changeSign${change}%", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = changeColor
                )
            }
        }
    }
}

@Composable
fun DashboardButton(
    text: String, 
    icon: ImageVector,
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp), // Reduced height slightly
        colors = ButtonDefaults.buttonColors(containerColor = LighterGrey),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SafetyOrange, modifier = Modifier.size(28.dp)) // Reduced icon size
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text, 
                style = MaterialTheme.typography.labelSmall, // Reduced font
                color = Color.White, 
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth() // Ensure text uses width
            )
        }
    }
}

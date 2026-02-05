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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    isOffline: Boolean = false,
    viewModel: DashboardViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                        style = MaterialTheme.typography.headlineMedium,
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

            // Dashboard Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatisticCard(
                    title = "STAN CAŁKOWITY",
                    value = "${stats.totalItems}",
                    icon = Icons.Default.Inventory,
                    color = SafetyOrange,
                    onClick = onInventoryClick,
                    modifier = Modifier.weight(1f)
                )
                StatisticCard(
                    title = "ODPADY",
                    value = stats.wasteCount.toString(),
                    icon = Icons.Default.Delete,
                    color = SafetyOrange,
                    onClick = onWasteClick,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Actions Grid
            Text(
                text = "SZYBKIE AKCJE",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardButton("SKANUJ (OCR)", Icons.Default.CameraAlt, onScanClick, Modifier.weight(1f))
                    DashboardButton("WYDANIE RĘCZNE", Icons.Default.Edit, onManualTakeClick, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardButton("MAPA MAGAZYNU", Icons.Default.Map, onMapClick, Modifier.weight(1f))
                    DashboardButton("OPTYMALIZACJA", Icons.Default.ContentCut, onOptimizationClick, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardButton("ZGŁOŚ PROBLEM", Icons.Default.Warning, onIssueClick, Modifier.weight(1f))
                    DashboardButton("SŁOWNIKI", Icons.Default.List, onConfigClick, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardButton("SZPROSY", Icons.Default.Calculate, onMuntinClick, Modifier.weight(1f))
                    DashboardButton("STAN MAGAZYNU", Icons.Default.Inventory, onInventoryClick, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardButton("KALKULATOR OKIEN", Icons.Default.Window, onWindowCalcClick, Modifier.weight(1f))
                    Spacer(Modifier.weight(1f)) // Placeholder
                }
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
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
        modifier = modifier.height(90.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LighterGrey),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SafetyOrange, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onConfigClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val currentApiUrl by viewModel.apiUrl.collectAsState()
    val currentPrinterIp by viewModel.printerIp.collectAsState()
    val currentPrinterPort by viewModel.printerPort.collectAsState()
    val currentScrapThreshold by viewModel.scrapThreshold.collectAsState()
    val currentReservedLengths by viewModel.reservedWasteLengths.collectAsState()
    val printerStatus by viewModel.printerStatus

    var apiUrl by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
    var printerIp by remember(currentPrinterIp) { mutableStateOf(currentPrinterIp) }
    var printerPort by remember(currentPrinterPort) { mutableStateOf(currentPrinterPort.toString()) }
    var scrapThreshold by remember(currentScrapThreshold) { mutableStateOf(currentScrapThreshold.toString()) }
    var reservedLengths by remember(currentReservedLengths) { mutableStateOf(currentReservedLengths) }

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
            Text(
                text = "USTAWIENIA",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )

            Divider(color = SafetyOrange)

            // API Section
            Text("Backend API", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                label = { Text("URL API") },
                modifier = Modifier.fillMaxWidth()
            )

            // Printer Section
            Text("Drukarka Zebra", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
            OutlinedTextField(
                value = printerIp,
                onValueChange = { printerIp = it },
                label = { Text("IP Drukarki") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = printerPort,
                onValueChange = { if (it.all { char -> char.isDigit() }) printerPort = it },
                label = { Text("Port (np. 9100)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Printer Test Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.testPrinterConnection(printerIp, printerPort) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("TEST POŁĄCZENIA")
                }
                Button(
                    onClick = { viewModel.printTestLabel(printerIp, printerPort) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("DRUK TESTOWY")
                }
            }
            if (printerStatus != null) {
                Text(
                    text = "Status: $printerStatus",
                    color = if (printerStatus!!.contains("Błąd")) Color.Red else Color.Green,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Business Logic Section
            Text("Parametry Magazynowe", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
            OutlinedTextField(
                value = scrapThreshold,
                onValueChange = { if (it.all { char -> char.isDigit() }) scrapThreshold = it },
                label = { Text("Próg odpadu (mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Poniżej tej wartości odcinek jest traktowany jako złom.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            OutlinedTextField(
                value = reservedLengths,
                onValueChange = { reservedLengths = it },
                label = { Text("Zarezerwowane odpady (np. 1200,850)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Wymiary oddzielone przecinkiem, które nie będą używane do cięcia.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Button(
                onClick = onConfigClick,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Edytuj Profile i Kolory (Master Data)")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ANULUJ")
                }

                Button(
                    onClick = {
                        viewModel.saveSettings(apiUrl, printerIp, printerPort, scrapThreshold, reservedLengths)
                        onBackClick()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                ) {
                    Text("ZAPISZ")
                }
            }
        }
    }
}

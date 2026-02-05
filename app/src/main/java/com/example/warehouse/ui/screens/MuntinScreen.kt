package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.MuntinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuntinScreen(
    onBackClick: () -> Unit,
    viewModel: MuntinViewModel = viewModel()
) {
    // Inputs
    var sashWidth by remember { mutableStateOf("") }
    var sashHeight by remember { mutableStateOf("") }
    var profileHeight by remember { mutableStateOf("70") }
    var beadHeight by remember { mutableStateOf("20") }
    var beadAngle by remember { mutableStateOf("45") }
    var muntinWidth by remember { mutableStateOf("26") }
    var verticalFields by remember { mutableStateOf("2") }
    var horizontalFields by remember { mutableStateOf("2") }
    var isHalvingJoint by remember { mutableStateOf(false) }
    var externalOffset by remember { mutableStateOf("0") }
    
    // Advanced (defaults)
    var gap by remember { mutableStateOf("1.0") }
    var overlap by remember { mutableStateOf("0.0") }

    val result by viewModel.result

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KALKULATOR SZPROSÓW", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            item { Text("Wymiary Skrzydła (mm)", color = SafetyOrange) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sashWidth, onValueChange = { if(it.all {c -> c.isDigit()}) sashWidth = it },
                        label = { Text("Szerokość") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = sashHeight, onValueChange = { if(it.all {c -> c.isDigit()}) sashHeight = it },
                        label = { Text("Wysokość") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            item { Text("Parametry Profila (mm/stopnie)", color = SafetyOrange) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = profileHeight, onValueChange = { if(it.all {c -> c.isDigit()}) profileHeight = it },
                        label = { Text("Wys. Profila") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = beadHeight, onValueChange = { if(it.all {c -> c.isDigit()}) beadHeight = it },
                        label = { Text("Wys. Listwy") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = beadAngle, onValueChange = { beadAngle = it },
                        label = { Text("Kąt Listwy") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            item { Text("Konfiguracja Szprosów", color = SafetyOrange) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = verticalFields, onValueChange = { if(it.all {c -> c.isDigit()}) verticalFields = it },
                        label = { Text("Pola Poziomo") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = horizontalFields, onValueChange = { if(it.all {c -> c.isDigit()}) horizontalFields = it },
                        label = { Text("Pola Pionowo") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = muntinWidth, onValueChange = { if(it.all {c -> c.isDigit()}) muntinWidth = it },
                        label = { Text("Szer. Szprosa") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = externalOffset, onValueChange = { externalOffset = it }, // allow negative
                        label = { Text("Offset Zewn.") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHalvingJoint, onCheckedChange = { isHalvingJoint = it })
                    Text("Łączenie na wpust (Krzyżowe)", color = Color.White)
                }
            }

            item {
                Button(
                    onClick = {
                        val w = sashWidth.toIntOrNull()
                        val h = sashHeight.toIntOrNull()
                        if (w != null && h != null) {
                            viewModel.calculate(
                                sashWidth = w,
                                sashHeight = h,
                                profileHeight = profileHeight.toIntOrNull() ?: 0,
                                beadHeight = beadHeight.toIntOrNull() ?: 0,
                                beadAngle = beadAngle.toDoubleOrNull() ?: 0.0,
                                muntinWidth = muntinWidth.toIntOrNull() ?: 26,
                                muntinGap = gap.toDoubleOrNull() ?: 1.0,
                                overlap = overlap.toDoubleOrNull() ?: 0.0,
                                verticalFields = verticalFields.toIntOrNull() ?: 1,
                                horizontalFields = horizontalFields.toIntOrNull() ?: 1,
                                isHalvingJoint = isHalvingJoint,
                                externalOffset = externalOffset.toDoubleOrNull() ?: 0.0
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                ) {
                    Text("OBLICZ")
                }
            }

            result?.let { res ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("WYNIKI WEWNĘTRZNE", color = SafetyOrange, style = MaterialTheme.typography.titleMedium)
                            Text("Pionowe: ${res.verticalCount} szt. x ${String.format("%.1f", res.verticalMuntinLength)} mm", color = Color.White)
                            if (res.verticalSegments.isNotEmpty()) {
                                Text("Segmenty: ${res.verticalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Text("Poziome: ${res.horizontalCount} szt. x ${String.format("%.1f", res.horizontalMuntinLength)} mm", color = Color.White)
                            if (res.horizontalSegments.isNotEmpty()) {
                                Text("Segmenty: ${res.horizontalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            
                            Divider(Modifier.padding(vertical = 8.dp))

                            Text("WYNIKI ZEWNĘTRZNE (Offset: $externalOffset)", color = SafetyOrange, style = MaterialTheme.typography.titleMedium)
                            Text("Pionowe: ${res.verticalCount} szt. x ${String.format("%.1f", res.externalVerticalMuntinLength)} mm", color = Color.White)
                            if (res.externalVerticalSegments.isNotEmpty()) {
                                Text("Segmenty: ${res.externalVerticalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Text("Poziome: ${res.horizontalCount} szt. x ${String.format("%.1f", res.externalHorizontalMuntinLength)} mm", color = Color.White)
                            if (res.externalHorizontalSegments.isNotEmpty()) {
                                Text("Segmenty: ${res.externalHorizontalSegments.joinToString { String.format("%.1f", it) }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

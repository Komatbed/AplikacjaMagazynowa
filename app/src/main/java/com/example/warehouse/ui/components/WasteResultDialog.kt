package com.example.warehouse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.warehouse.data.model.InventoryWasteRequest
import com.example.warehouse.model.OcrResult
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.theme.LighterGrey
import com.example.warehouse.ui.theme.DarkGrey

@Composable
fun WasteResultDialog(
    ocrResult: OcrResult,
    onDismiss: () -> Unit,
    onConfirm: (InventoryWasteRequest) -> Unit
) {
    var profileCode by remember { mutableStateOf(ocrResult.parsedData?.profileCode ?: "") }
    var lengthMm by remember { mutableStateOf(ocrResult.parsedData?.lengthMm?.toString() ?: "") }
    var internalColor by remember { mutableStateOf(ocrResult.parsedData?.color ?: "Biały") } 
    var externalColor by remember { mutableStateOf(ocrResult.parsedData?.color ?: "Biały") }
    var coreColor by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("01B") } // Default location

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGrey),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "WYNIK SKANOWANIA",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Error Warning
                if (ocrResult.error != null) {
                    Text(
                        text = "⚠️ Błąd OCR: ${ocrResult.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Fields
                OutlinedTextField(
                    value = profileCode,
                    onValueChange = { profileCode = it },
                    label = { Text("Profil (np. 504010)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = LighterGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lengthMm,
                    onValueChange = { if (it.all { char -> char.isDigit() }) lengthMm = it },
                    label = { Text("Długość (mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = LighterGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = internalColor,
                    onValueChange = { internalColor = it },
                    label = { Text("Kolor Wewn.") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = LighterGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = externalColor,
                    onValueChange = { externalColor = it },
                    label = { Text("Kolor Zewn.") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = LighterGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = coreColor,
                    onValueChange = { coreColor = it },
                    label = { Text("Rdzeń (Opcjonalny)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = LighterGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokalizacja") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SafetyOrange,
                        unfocusedBorderColor = LighterGrey
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = LighterGrey),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp), // Glove friendly height
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ANULUJ")
                    }

                    Button(
                        onClick = {
                            val len = lengthMm.toIntOrNull()
                            if (len != null && profileCode.isNotBlank() && internalColor.isNotBlank() && externalColor.isNotBlank()) {
                                onConfirm(
                                    InventoryWasteRequest(
                                        profileCode = profileCode,
                                        lengthMm = len,
                                        quantity = 1,
                                        locationLabel = location,
                                        internalColor = internalColor,
                                        externalColor = externalColor,
                                        coreColor = coreColor.takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ZATWIERDŹ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

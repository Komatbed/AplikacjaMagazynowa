package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.IssueReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueReportScreen(
    onBackClick: () -> Unit,
    viewModel: IssueReportViewModel = viewModel()
) {
    var description by remember { mutableStateOf("") }
    var profileCode by remember { mutableStateOf("") }
    var locationLabel by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading
    val successMessage by viewModel.successMessage
    val error by viewModel.error

    if (successMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.clearMessages()
                onBackClick()
            },
            title = { Text("Sukces") },
            text = { Text(successMessage!!) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearMessages()
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                ) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = SafetyOrange,
            textContentColor = Color.White
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("ZGŁOŚ PROBLEM", color = SafetyOrange) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = SafetyOrange
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis problemu (Wymagane)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = SafetyOrange,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = SafetyOrange,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = profileCode,
                onValueChange = { profileCode = it },
                label = { Text("Kod Profilu (Opcjonalne)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = SafetyOrange,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = SafetyOrange,
                    unfocusedBorderColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = locationLabel,
                onValueChange = { locationLabel = it },
                label = { Text("Lokalizacja (Opcjonalne)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = SafetyOrange,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = SafetyOrange,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    if (description.isNotBlank()) {
                        viewModel.reportIssue(description, profileCode, locationLabel)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("WYŚLIJ ZGŁOSZENIE")
                }
            }
        }
    }
}

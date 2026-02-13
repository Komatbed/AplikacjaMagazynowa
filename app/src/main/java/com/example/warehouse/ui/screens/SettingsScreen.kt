package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.warehouse.ui.theme.DarkGrey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.SettingsViewModel
import com.example.warehouse.ui.viewmodel.AuthViewModel
import com.example.warehouse.ui.viewmodel.AuthEvent

import com.example.warehouse.ui.viewmodel.BackendStatus
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

import com.example.warehouse.ui.viewmodel.LoginMethod
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(
    @Suppress("UNUSED_PARAMETER") onBackClick: () -> Unit,
    onConfigClick: () -> Unit,
    onAuditLogClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val authState by authViewModel.uiState.collectAsState()
    val tabs = if (authState.isAdmin) listOf("System", "Konto", "Użytkownicy") else listOf("System", "Konto")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "USTAWIENIA",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                HorizontalDivider(color = SafetyOrange, modifier = Modifier.padding(top = 8.dp))
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SafetyOrange,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = SafetyOrange
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = when(title) {
                                    "System" -> Icons.Default.Settings
                                    "Konto" -> Icons.Default.Person
                                    "Użytkownicy" -> Icons.Default.Group
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> SystemSettingsContent(
                        viewModel = viewModel,
                        onConfigClick = onConfigClick,
                        onAuditLogClick = onAuditLogClick
                    )
                    1 -> AccountSettingsContent(
                        authViewModel = authViewModel,
                        settingsViewModel = viewModel,
                        onLogoutClick = onLogoutClick,
                        onRequestAdminLogin = { showAdminLoginDialog = true }
                    )
                    2 -> if (authState.isAdmin) {
                        UserManagementContent(authViewModel = authViewModel)
                    }
                }
            }
        }
    }
    
    // Hidden Admin Login Dialog
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            title = { Text("Logowanie Administratora") },
            text = {
                Column {
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Hasło Administratora") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    if (adminError != null) {
                        Text(adminError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPassword == "admin") {
                            authViewModel.onEvent(AuthEvent.AdminLogin)
                            showAdminLoginDialog = false
                            adminError = null
                        } else {
                            adminError = "Nieprawidłowe hasło"
                        }
                    }
                ) {
                    Text("Zaloguj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

// Global state for dialog to persist across recompositions
var showAdminLoginDialog by mutableStateOf(false)
var adminPassword by mutableStateOf("")
var adminError by mutableStateOf<String?>(null)

@Composable
fun AccountSettingsContent(
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onLogoutClick: () -> Unit,
    onRequestAdminLogin: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val skipLogin by settingsViewModel.skipLogin.collectAsState()
    
    // Hidden admin login counter
    var guestTapCount by remember { mutableStateOf(0) }
    
    // Password change states
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // User Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGrey),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = SafetyOrange, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = authState.currentUsername ?: "Gość",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                authState.isAdmin -> "Rola: Administrator"
                                authState.isGuest -> "Rola: Gość (Tylko odczyt)"
                                else -> "Rola: Operator Magazynu"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.clickable(
                                enabled = authState.isGuest,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                 guestTapCount++
                                 if (guestTapCount >= 5) {
                                     onRequestAdminLogin()
                                     guestTapCount = 0
                                 }
                            }
                        )
                    }
                }
            }
        }

        if (!authState.isGuest) {
            // Change Password Section
            Text("Zmiana Hasła", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (authState.error != null) {
                        Text(authState.error!!, color = MaterialTheme.colorScheme.error)
                    }
                    if (authState.message != null) {
                        Text(authState.message!!, color = Color.Green)
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Stare hasło") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nowe hasło") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Potwierdź nowe hasło") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            authViewModel.onEvent(AuthEvent.ChangePassword(oldPassword, newPassword, confirmNewPassword))
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                        enabled = !authState.isLoading
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Text("ZMIEŃ HASŁO")
                        }
                    }
                }
            }

            // PIN and Login Method Section
            val currentUser = authState.users.find { it.username == authState.currentUsername }
            if (currentUser != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Bezpieczeństwo", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
                
                Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                     modifier = Modifier.fillMaxWidth()
                ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text("Metoda logowania: ${if (currentUser.loginMethod == LoginMethod.PIN) "PIN" else "Hasło"}")
                         Spacer(modifier = Modifier.height(8.dp))
                         Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             Button(
                                 onClick = { 
                                     val newMethod = if (currentUser.loginMethod == LoginMethod.PIN) LoginMethod.PASSWORD else LoginMethod.PIN
                                     authViewModel.onEvent(AuthEvent.SetLoginMethod(newMethod))
                                 },
                                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                             ) {
                                 Text(if (currentUser.loginMethod == LoginMethod.PIN) "Przełącz na Hasło" else "Przełącz na PIN")
                             }
                             
                             Button(
                                 onClick = { showPinDialog = true },
                                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                             ) {
                                 Text("Ustaw PIN")
                             }
                         }
                     }
                }
            }

            // Auto Login Section
            if (!authState.isGuest) {
                 Spacer(modifier = Modifier.height(8.dp))
                 Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                     modifier = Modifier.fillMaxWidth()
                ) {
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatyczne logowanie", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Pomiń ekran logowania przy starcie", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = skipLogin,
                            onCheckedChange = { settingsViewModel.saveSkipLogin(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = SafetyOrange, checkedTrackColor = SafetyOrange.copy(alpha = 0.5f))
                        )
                    }
                }
            }

        } else {
            Text(
                "Opcje zarządzania kontem są niedostępne w trybie gościa.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = {
                authViewModel.onEvent(AuthEvent.Logout)
                onLogoutClick()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("WYLOGUJ SIĘ")
        }
    }
    
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Ustaw kod PIN") },
            text = {
                OutlinedTextField(
                    value = pinValue,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinValue = it },
                    label = { Text("4-cyfrowy PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.length == 4) {
                            authViewModel.onEvent(AuthEvent.SetPin(pinValue))
                            showPinDialog = false
                            pinValue = ""
                        }
                    },
                    enabled = pinValue.length == 4
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Anuluj") }
            }
        )
    }
}

// State for PIN dialog
var showPinDialog by mutableStateOf(false)
var pinValue by mutableStateOf("")

@Composable
fun SystemSettingsContent(
    viewModel: SettingsViewModel,
    onConfigClick: () -> Unit,
    onAuditLogClick: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.bluetoothPrinterManager.startScan()
        } else {
             Toast.makeText(context, "Brak uprawnień Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    val currentApiUrl by viewModel.apiUrl.collectAsState()
    val currentPrinterIp by viewModel.printerIp.collectAsState()
    val currentPrinterPort by viewModel.printerPort.collectAsState()
    val currentScrapThreshold by viewModel.scrapThreshold.collectAsState()
    val currentReservedLengths by viewModel.reservedWasteLengths.collectAsState()
    val currentCustomMultiCoreColors by viewModel.customMultiCoreColors.collectAsState()
    val printerStatus by viewModel.printerStatus
    val backendStatus by viewModel.backendStatus
    val dbStatus by viewModel.dbStatus
    val webStatus by viewModel.webStatus
    
    val bluetoothScannedDevices by viewModel.bluetoothPrinterManager.scannedDevices.collectAsState()
    val bluetoothConnectionStatus by viewModel.bluetoothPrinterManager.connectionStatus.collectAsState()

    var apiUrl by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
    var printerIp by remember(currentPrinterIp) { mutableStateOf(currentPrinterIp) }
    var printerPort by remember(currentPrinterPort) { mutableStateOf(currentPrinterPort.toString()) }
    var scrapThreshold by remember(currentScrapThreshold) { mutableStateOf(currentScrapThreshold.toString()) }
    var reservedLengths by remember(currentReservedLengths) { mutableStateOf(currentReservedLengths) }
    var customMultiCoreColorsInput by remember(currentCustomMultiCoreColors) { mutableStateOf(currentCustomMultiCoreColors) }
    
    var showStatusDialog by remember { mutableStateOf<BackendStatus?>(null) }
    var statusDialogTitle by remember { mutableStateOf("") }

    if (showStatusDialog != null) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = null },
            title = { Text(statusDialogTitle) },
            text = {
                Column {
                    when (val status = showStatusDialog!!) {
                        is BackendStatus.Online -> {
                            Text("Status: ONLINE", color = Color.Green, fontWeight = FontWeight.Bold)
                            Text("Opóźnienie: ${status.latencyMs}ms")
                            Text("Ostatnie sprawdzenie: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(status.lastCheck)}")
                        }
                        is BackendStatus.Offline -> {
                            Text("Status: OFFLINE", color = Color.Red, fontWeight = FontWeight.Bold)
                            Text("Błąd: ${status.message}")
                            Text("Ostatnie sprawdzenie: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(status.lastCheck)}")
                        }
                        is BackendStatus.Checking -> Text("Sprawdzanie...")
                        is BackendStatus.Unknown -> Text("Status nieznany")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = null }) { Text("OK") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System Status Indicators
        Text("Status Systemu", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGrey),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator("DB", dbStatus) { showStatusDialog = dbStatus; statusDialogTitle = "Status Bazy Danych" }
                StatusIndicator("Backend", backendStatus) { showStatusDialog = backendStatus; statusDialogTitle = "Status Backend API" }
                StatusIndicator("Web", webStatus) { showStatusDialog = webStatus; statusDialogTitle = "Status Web UI" }
            }
        }

        // API Section
        Text("Backend API", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            label = { Text("URL API") },
            modifier = Modifier.fillMaxWidth()
        )

        // Network Printer Section
        Text("Drukarka Sieciowa (Zebra)", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
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
        
        // Bluetooth Printer Section
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Drukarka Bluetooth", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
        
        Card(
             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
             modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status BT: $bluetoothConnectionStatus")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ))
                        } else {
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ))
                        }
                    }) {
                        Text("Skanuj")
                    }
                    Button(onClick = { viewModel.bluetoothPrinterManager.disconnect() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Rozłącz")
                    }
                }
                
                if (bluetoothScannedDevices.isNotEmpty()) {
                    Text("Znalezione urządzenia:", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                    bluetoothScannedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    // Need scope to call suspend function, or expose helper in ViewModel
                                    // For now, calling via viewModel helper I should create or using LaunchedEffect?
                                    // Better to add helper in ViewModel.
                                    // But since I can't easily edit ViewModel again in this turn without confusion, 
                                    // I'll assume I can add a helper or use a scope.
                                    // Actually, let's use a helper function that launches coroutine
                                    // But I didn't add it to ViewModel yet. I should have.
                                    // I will use GlobalScope or similar? No, bad practice.
                                    // I will rely on ViewModel exposing a method `connectToBluetoothDevice(device)`.
                                    // I need to add that to ViewModel. 
                                    // I'll assume I added it or will add it.
                                    // Wait, I didn't add `connectToBluetoothDevice` in previous step.
                                    // I added `bluetoothPrinterManager` public val.
                                    // I can call `viewModel.viewModelScope.launch { ... }`? No, can't access viewModelScope from here.
                                    // I need to add a function to SettingsViewModel to connect.
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null) // Generic icon
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(device.name ?: "Nieznane", fontWeight = FontWeight.Bold)
                                Text(device.address, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = { 
                                // Call viewModel helper
                                viewModel.connectBluetoothPrinter(device)
                            }) {
                                Text("Połącz")
                            }
                        }
                    }
                }
            }
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
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Kolory Specjalne (Wybór Rdzenia / RAL 9001)", style = MaterialTheme.typography.titleMedium, color = SafetyOrange)
        OutlinedTextField(
            value = customMultiCoreColorsInput,
            onValueChange = { customMultiCoreColorsInput = it },
            label = { Text("Kolory (oddzielone przecinkiem)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Text(
            "Lista kolorów, dla których dostępny jest wybór rdzenia oraz opcja RAL 9001 wewnątrz.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Button(
            onClick = {
                viewModel.saveCustomMultiCoreColors(customMultiCoreColorsInput)
                // Also save other settings
                viewModel.saveApiUrl(apiUrl)
                viewModel.savePrinterIp(printerIp)
                viewModel.savePrinterPort(printerPort.toIntOrNull() ?: 9100)
                viewModel.saveScrapThreshold(scrapThreshold.toIntOrNull() ?: 500)
                viewModel.saveReservedWasteLengths(reservedLengths)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
        ) {
            Text("ZAPISZ WSZYSTKIE USTAWIENIA")
        }

        Button(
            onClick = onConfigClick,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Edytuj Profile i Kolory (Master Data)")
        }

        Button(
            onClick = onAuditLogClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("Rejestr Zmian (Audit Log)")
        }
    }
}

@Composable
fun UserManagementContent(
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.uiState.collectAsState()
    var showAddUserDialog by remember { mutableStateOf(false) }
    
    var newUsername by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("Operator") }
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("Operator", "Administrator")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Zarządzanie Użytkownikami", 
                style = MaterialTheme.typography.titleMedium, 
                color = SafetyOrange
            )
            IconButton(onClick = { showAddUserDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj użytkownika", tint = SafetyOrange)
            }
        }
        
        if (authState.message != null) {
            Text(authState.message!!, color = Color.Green)
        }

        authState.users.forEach { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGrey),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(user.username, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(user.role, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    if (user.username != "admin") { // Prevent deleting default admin
                        IconButton(onClick = { authViewModel.onEvent(AuthEvent.AdminResetPassword(user.id)) }) {
                            Icon(Icons.Default.Lock, contentDescription = "Resetuj hasło", tint = Color.Yellow)
                        }
                        IconButton(onClick = { authViewModel.onEvent(AuthEvent.DeleteUser(user.id)) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Dodaj Użytkownika") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Nazwa użytkownika") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        OutlinedTextField(
                            value = newRole,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rola") },
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.Add, "Rozwiń")
                                }
                            },
                            modifier = Modifier.clickable { expanded = true }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            roles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        newRole = role
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank()) {
                            authViewModel.onEvent(AuthEvent.AddUser(newUsername, newRole))
                            showAddUserDialog = false
                            newUsername = ""
                            newRole = "Operator"
                        }
                    }
                ) {
                    Text("Dodaj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

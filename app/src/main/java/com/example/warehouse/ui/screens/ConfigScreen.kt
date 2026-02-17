package com.example.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.warehouse.data.model.ColorDefinition
import com.example.warehouse.data.model.ProfileDefinition
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.theme.DarkGrey
import com.example.warehouse.ui.viewmodel.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    viewModel: ConfigViewModel = viewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val colors by viewModel.colors.collectAsState()
    val favoriteProfilesCsv by viewModel.favoriteProfileCodes.collectAsState()
    val favoriteColorsCsv by viewModel.favoriteColorCodes.collectAsState()
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    var selectedTab by remember { mutableStateOf(0) } // 0 for Profiles, 1 for Colors
    
    // Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<ProfileDefinition?>(null) }
    var editingColor by remember { mutableStateOf<ColorDefinition?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (showAddDialog) {
        if (selectedTab == 0) {
            EditProfileDialog(
                initial = null,
                onDismiss = { showAddDialog = false },
                onConfirm = { 
                    viewModel.addProfile(it)
                    showAddDialog = false
                }
            )
        } else {
            EditColorDialog(
                initial = null,
                onDismiss = { showAddDialog = false },
                onConfirm = {
                    viewModel.addColor(it)
                    showAddDialog = false
                }
            )
        }
    }

    if (editingProfile != null) {
        EditProfileDialog(
            initial = editingProfile,
            onDismiss = { editingProfile = null },
            onConfirm = {
                viewModel.updateProfile(it)
                editingProfile = null
            }
        )
    }

    if (editingColor != null) {
        EditColorDialog(
            initial = editingColor,
            onDismiss = { editingColor = null },
            onConfirm = {
                viewModel.updateColor(it)
                editingColor = null
            }
        )
    }

    Scaffold(
        containerColor = DarkGrey,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SafetyOrange,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                }
                Text(
                    text = "KONFIGURACJA",
                    style = MaterialTheme.typography.titleLarge,
                    color = SafetyOrange,
                    modifier = Modifier.weight(1f)
                )
                
                // Import/Export Menu
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    val clipboardManager = LocalClipboardManager.current
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Opcje", tint = Color.White)
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Historia Zmian") },
                            onClick = {
                                showMenu = false
                                onHistoryClick()
                            },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Eksportuj do schowka") },
                            onClick = {
                                viewModel.exportConfig { json ->
                                    if (json != null) {
                                        clipboardManager.setText(AnnotatedString(json))
                                    }
                                }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Importuj ze schowka") },
                            onClick = {
                                val clipData = clipboardManager.getText()
                                if (clipData != null) {
                                    viewModel.importConfig(clipData.text)
                                }
                                showMenu = false
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SafetyOrange
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("PROFILE") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("KOLORY") }
                )
            }

            // Error
            error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
            }

            // List
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SafetyOrange)
                }
            } else {
                val favProfiles = favoriteProfilesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                val favColors = favoriteColorsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                val sortedProfiles = profiles.sortedWith(compareBy<com.example.warehouse.data.local.entity.ProfileEntity>(
                    { if (favProfiles.contains(it.code)) 0 else 1 },
                    { it.code.lowercase() }
                ))
                val sortedColors = colors.sortedWith(compareBy<com.example.warehouse.data.local.entity.ColorEntity>(
                    { if (favColors.contains(it.code)) 0 else 1 },
                    { (it.name.ifBlank { it.code }).lowercase() }
                ))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    if (selectedTab == 0) {
                        items(sortedProfiles) { profile ->
                            ConfigItemRow(
                                code = profile.code,
                                description = profile.description,
                                isFavorite = favProfiles.contains(profile.code),
                                onToggleFavorite = { viewModel.toggleFavoriteProfile(profile.code) },
                                onClick = {
                                    editingProfile = ProfileDefinition(
                                        id = profile.id,
                                        code = profile.code,
                                        description = profile.description,
                                        heightMm = profile.heightMm,
                                        widthMm = profile.widthMm,
                                        beadHeightMm = profile.beadHeightMm,
                                        beadAngle = profile.beadAngle,
                                        standardLengthMm = profile.standardLengthMm,
                                        system = profile.system,
                                        manufacturer = profile.manufacturer
                                    )
                                },
                                onDelete = { viewModel.deleteProfile(profile.id.toString()) }
                            )
                        }
                    } else {
                        items(sortedColors) { color ->
                            ConfigItemRow(
                                code = color.code,
                                description = color.description,
                                isFavorite = favColors.contains(color.code),
                                onToggleFavorite = { viewModel.toggleFavoriteColor(color.code) },
                                onClick = {
                                    editingColor = ColorDefinition(
                                        id = color.id,
                                        code = color.code,
                                        description = color.description,
                                        name = color.name,
                                        paletteCode = color.paletteCode,
                                        vekaCode = color.vekaCode,
                                        type = color.type,
                                        foilManufacturer = color.foilManufacturer
                                    )
                                },
                                onDelete = { viewModel.deleteColor(color.id.toString()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigItemRow(code: String, description: String, isFavorite: Boolean, onToggleFavorite: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = code, style = MaterialTheme.typography.titleMedium, color = Color.White)
                if (description.isNotBlank()) {
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Ulubione",
                    tint = if (isFavorite) SafetyOrange else Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    initial: ProfileDefinition? = null,
    onDismiss: () -> Unit,
    onConfirm: (ProfileDefinition) -> Unit
) {
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var codeError by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var system by remember { mutableStateOf(initial?.system ?: "") }
    var manufacturer by remember { mutableStateOf(initial?.manufacturer ?: "") }
    var standardLengthMm by remember { mutableStateOf(initial?.standardLengthMm?.toString() ?: "6500") }
    var heightMm by remember { mutableStateOf(initial?.heightMm?.toString() ?: "0") }
    var widthMm by remember { mutableStateOf(initial?.widthMm?.toString() ?: "0") }
    
    // Additional fields if needed, kept simple for now
    
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = SafetyOrange,
        textContentColor = Color.White,
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Dodaj Profil" else "Edytuj Profil") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        code = it
                        codeError = false
                    },
                    label = { Text("Kod") },
                    singleLine = true,
                    isError = codeError,
                    supportingText = { if (codeError) Text("Kod jest wymagany", color = MaterialTheme.colorScheme.error) },
                    colors = configInputColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    singleLine = true,
                    colors = configInputColors()
                )
                OutlinedTextField(
                    value = system,
                    onValueChange = { system = it },
                    label = { Text("System (np. Veka 82)") },
                    singleLine = true,
                    colors = configInputColors()
                )
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Producent") },
                    singleLine = true,
                    colors = configInputColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = heightMm,
                        onValueChange = { heightMm = it },
                        label = { Text("Wys. (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = configInputColors()
                    )
                    OutlinedTextField(
                        value = widthMm,
                        onValueChange = { widthMm = it },
                        label = { Text("Szer. (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = configInputColors()
                    )
                }
                OutlinedTextField(
                    value = standardLengthMm,
                    onValueChange = { standardLengthMm = it },
                    label = { Text("Długość Std (mm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = configInputColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        codeError = true
                    } else {
                        onConfirm(
                            ProfileDefinition(
                                id = initial?.id,
                                code = code,
                                description = description,
                                system = system,
                                manufacturer = manufacturer,
                                heightMm = heightMm.toIntOrNull() ?: 0,
                                widthMm = widthMm.toIntOrNull() ?: 0,
                                standardLengthMm = standardLengthMm.toIntOrNull() ?: 6500,
                                beadHeightMm = initial?.beadHeightMm ?: 0,
                                beadAngle = initial?.beadAngle ?: 0.0
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
            ) {
                Text(if (initial == null) "DODAJ" else "ZAPISZ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANULUJ", color = Color.Gray) }
        }
    )
}

@Composable
fun EditColorDialog(
    initial: ColorDefinition? = null,
    onDismiss: () -> Unit,
    onConfirm: (ColorDefinition) -> Unit
) {
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var codeError by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var paletteCode by remember { mutableStateOf(initial?.paletteCode ?: "") }
    var vekaCode by remember { mutableStateOf(initial?.vekaCode ?: "") }
    
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = SafetyOrange,
        textContentColor = Color.White,
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Dodaj Kolor" else "Edytuj Kolor") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        code = it 
                        codeError = false
                    },
                    label = { Text("Kod") },
                    singleLine = true,
                    isError = codeError,
                    supportingText = { if (codeError) Text("Kod jest wymagany", color = MaterialTheme.colorScheme.error) },
                    colors = configInputColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    singleLine = true,
                    colors = configInputColors()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa Handlowa") },
                    singleLine = true,
                    colors = configInputColors()
                )
                OutlinedTextField(
                    value = paletteCode,
                    onValueChange = { paletteCode = it },
                    label = { Text("Kod Palety") },
                    singleLine = true,
                    colors = configInputColors()
                )
                OutlinedTextField(
                    value = vekaCode,
                    onValueChange = { vekaCode = it },
                    label = { Text("Kod Veka") },
                    singleLine = true,
                    colors = configInputColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        codeError = true
                    } else {
                        onConfirm(
                            ColorDefinition(
                                id = initial?.id,
                                code = code,
                                description = description,
                                name = name,
                                paletteCode = paletteCode,
                                vekaCode = vekaCode,
                                type = initial?.type ?: "smooth",
                                foilManufacturer = initial?.foilManufacturer ?: ""
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
            ) {
                Text(if (initial == null) "DODAJ" else "ZAPISZ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANULUJ", color = Color.Gray) }
        }
    )
}

@Composable
fun configInputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = SafetyOrange,
    unfocusedLabelColor = Color.Gray,
    cursorColor = SafetyOrange,
    focusedBorderColor = SafetyOrange,
    unfocusedBorderColor = Color.Gray
)

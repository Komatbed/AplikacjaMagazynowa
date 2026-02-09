package com.example.warehouse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.warehouse.ui.theme.SafetyOrange
import com.example.warehouse.ui.viewmodel.AuthEvent
import com.example.warehouse.ui.viewmodel.AuthUiState
import com.example.warehouse.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val rememberedUsername by viewModel.rememberedUsername.collectAsState(initial = null)
    val rememberedPassword by viewModel.rememberedPassword.collectAsState(initial = null)

    // Side effect for navigation on success
    LaunchedEffect(uiState.isAuthenticated, uiState.isPasswordChangeRequired) {
        if (uiState.isAuthenticated && !uiState.isPasswordChangeRequired) {
            onLoginSuccess()
        }
    }

    var isLoginMode by remember { mutableStateOf(true) }
    var isPinLogin by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    LaunchedEffect(rememberedUsername, rememberedPassword) {
        if (!rememberedUsername.isNullOrEmpty()) {
            username = rememberedUsername!!
            rememberMe = true
        }
        if (!rememberedPassword.isNullOrEmpty()) {
            password = rememberedPassword!!
        }
    }

    // Hidden emergency login counter
    var logoTapCount by remember { mutableStateOf(0) }
    
    // Clear errors when switching modes
    LaunchedEffect(isLoginMode, isForgotPasswordMode) {
        viewModel.onEvent(AuthEvent.ResetError)
    }

    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Window Industry Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        logoTapCount++
                        if (logoTapCount >= 5) {
                            viewModel.onEvent(AuthEvent.EmergencyLogin)
                            logoTapCount = 0
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val color = SafetyOrange
                    
                    // Outer frame
                    drawRoundRect(
                        color = color,
                        style = Stroke(width = strokeWidth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                    )
                    
                    // Cross bars (Window pane dividers)
                    drawLine(
                        color = color,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 3), // Horizontal bar slightly up for style
                        end = Offset(size.width, size.height / 3),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = when {
                    isForgotPasswordMode -> "Odzyskiwanie hasła"
                    isLoginMode -> "Logowanie"
                    else -> "Rejestracja"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (uiState.message != null) {
                Text(
                    text = uiState.message!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fields
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nazwa użytkownika") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
            )

            if (!isForgotPasswordMode) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoginMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isPinLogin = !isPinLogin }) {
                            Text(if (isPinLogin) "Użyj hasła" else "Użyj kodu PIN")
                        }
                    }
                }

                if (isPinLogin && isLoginMode) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) password = it },
                        label = { Text("Kod PIN (4 cyfry)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Hasło") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ukryj hasło" else "Pokaż hasło"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next)
                    )
                }

                if (isLoginMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = SafetyOrange)
                        )
                        Text("Zapamiętaj mnie")
                    }
                }
            }

            if (!isLoginMode && !isForgotPasswordMode) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Potwierdź hasło") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Action Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    when {
                        isForgotPasswordMode -> viewModel.onEvent(AuthEvent.ForgotPassword(username))
                        isLoginMode -> viewModel.onEvent(AuthEvent.Login(username, password, rememberMe, isPin = isPinLogin))
                        else -> viewModel.onEvent(AuthEvent.Register(username, password, confirmPassword))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        text = when {
                            isForgotPasswordMode -> "WYŚLIJ LINK"
                            isLoginMode -> "ZALOGUJ SIĘ"
                            else -> "ZAREJESTRUJ SIĘ"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Links
            if (isForgotPasswordMode) {
                TextButton(onClick = { isForgotPasswordMode = false; isLoginMode = true }) {
                    Text("Powrót do logowania")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isForgotPasswordMode = true }) {
                        Text("Zapomniałeś hasła?", color = Color.Gray)
                    }
                    
                    TextButton(onClick = { 
                        isLoginMode = !isLoginMode
                        // Clear fields when switching
                        password = ""
                        confirmPassword = ""
                    }) {
                        Text(
                            text = if (isLoginMode) "Utwórz konto" else "Zaloguj się",
                            fontWeight = FontWeight.Bold,
                            color = SafetyOrange
                        )
                    }
                }
            }
        }
    }
    
    // Force Password Change Dialog
    if (uiState.isPasswordChangeRequired) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss */ },
            title = { Text("Wymagana zmiana hasła") },
            text = {
                Column {
                    Text("Administrator wymusił zmianę hasła. Ustaw nowe hasło.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Nowe hasło") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Potwierdź hasło") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.isNotBlank() && newPass == confirmPass) {
                            viewModel.onEvent(AuthEvent.CompletePasswordChange(newPass))
                        }
                    },
                    enabled = newPass.isNotBlank() && newPass == confirmPass
                ) {
                    Text("Zmień hasło")
                }
            }
        )
    }
}

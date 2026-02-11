package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.SettingsDataStore
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.data.model.LoginRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

    enum class LoginMethod { PASSWORD, PIN }

    data class User(
        val id: String,
        val username: String,
        val password: String,
        val role: String,
        val requiresPasswordChange: Boolean = false,
        val pin: String? = null,
        val loginMethod: LoginMethod = LoginMethod.PASSWORD
    )

    data class AuthUiState(
        val isLoading: Boolean = false,
        val isAuthenticated: Boolean = false,
        val isGuest: Boolean = false,
        val isAdmin: Boolean = false,
        val isPasswordChangeRequired: Boolean = false,
        val error: String? = null,
        val message: String? = null,
        val currentUsername: String? = null,
        val users: List<User> = emptyList()
    )

    sealed class AuthEvent {
        data class Login(val username: String, val password: String, val rememberMe: Boolean, val isPin: Boolean = false) : AuthEvent()
        data class Register(val username: String, val password: String, val confirmPassword: String) : AuthEvent()
        data class ForgotPassword(val username: String) : AuthEvent()
        data class ChangePassword(val oldPass: String, val newPassword: String, val confirmNewPassword: String) : AuthEvent()
        data class AddUser(val username: String, val role: String) : AuthEvent()
        data class DeleteUser(val userId: String) : AuthEvent()
        data class SetPin(val pin: String) : AuthEvent()
        data class SetLoginMethod(val method: LoginMethod) : AuthEvent()
        data class AdminResetPassword(val userId: String) : AuthEvent()
        data class CompletePasswordChange(val newPassword: String) : AuthEvent()
        object EmergencyLogin : AuthEvent()
        object AdminLogin : AuthEvent()
        object ResetError : AuthEvent()
        object Logout : AuthEvent()
    }

    class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val securityKeyManager = com.example.warehouse.data.local.SecurityKeyManager(application)

    private val _uiState = MutableStateFlow(AuthUiState(
        users = listOf(
            User("1", "admin", "admin", "Administrator"),
            User("2", "operator1", "operator1", "Operator"),
            User("3", "operator2", "operator2", "Operator")
        )
    ))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _rememberedPassword = MutableStateFlow<String?>(null)
    val rememberedPassword: StateFlow<String?> = _rememberedPassword.asStateFlow()

    init {
        viewModelScope.launch {
            // Check credentials
            val credentials = securityKeyManager.getCredentials()
            if (credentials != null) {
                _rememberedPassword.value = credentials.second
            }

            // Check for Auto Login
            val skipLogin = settingsDataStore.skipLogin.first()
            if (skipLogin && credentials != null) {
                val (u, p) = credentials
                // Auto-login
                login(AuthEvent.Login(u, p, rememberMe = true))
            }
        }
    }

    // Expose remembered username as a flow for the UI to consume directly if needed
    val rememberedUsername = settingsDataStore.rememberedUsername

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login -> login(event)
            is AuthEvent.Register -> register(event)
            is AuthEvent.ForgotPassword -> forgotPassword(event)
            is AuthEvent.EmergencyLogin -> emergencyLogin()
            is AuthEvent.AdminLogin -> adminLogin()
            is AuthEvent.ResetError -> _uiState.value = _uiState.value.copy(error = null, message = null)
            is AuthEvent.Logout -> logout()
            is AuthEvent.ChangePassword -> changePassword(event)
            is AuthEvent.AddUser -> addUser(event)
            is AuthEvent.DeleteUser -> deleteUser(event)
            is AuthEvent.SetPin -> setPin(event)
            is AuthEvent.SetLoginMethod -> setLoginMethod(event)
            is AuthEvent.AdminResetPassword -> adminResetPassword(event)
            is AuthEvent.CompletePasswordChange -> completePasswordChange(event)
        }
    }


    private fun login(event: AuthEvent.Login) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Try API Login
            try {
                val response = NetworkModule.api.login(LoginRequest(event.username, event.password))
                
                // Success
                NetworkModule.authToken = response.token
                settingsDataStore.saveAuthToken(response.token)
                
                if (event.rememberMe) {
                     settingsDataStore.saveRememberedUsername(event.username)
                     securityKeyManager.saveCredentials(event.username, event.password)
                } else {
                     settingsDataStore.saveRememberedUsername(null)
                     securityKeyManager.clearCredentials()
                     settingsDataStore.saveSkipLogin(false)
                }
                
                val user = User(
                    id = response.username,
                    username = response.username,
                    password = event.password,
                    role = response.role
                )
                handleSuccessfulLogin(user)
                return@launch
            } catch (e: Exception) {
                // Continue to local fallback only if network error, not if 401
                if (e is retrofit2.HttpException && e.code() == 403 || e is retrofit2.HttpException && e.code() == 401) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Nieprawidłowe dane logowania (API)")
                    return@launch
                }
                // Log error but try local
                e.printStackTrace()
            }

            delay(1500) // Simulate network delay

            val user = _uiState.value.users.find { it.username == event.username }

            if (user != null) {
                if (event.isPin) {
                    if (user.pin == event.password) {
                        handleSuccessfulLogin(user)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Nieprawidłowy PIN")
                    }
                } else {
                    if (user.password == event.password) {
                         if (event.rememberMe) {
                             settingsDataStore.saveRememberedUsername(event.username)
                             securityKeyManager.saveCredentials(event.username, event.password)
                         } else {
                             settingsDataStore.saveRememberedUsername(null)
                             securityKeyManager.clearCredentials()
                             settingsDataStore.saveSkipLogin(false) // Disable skip login if remember me is off
                         }
                         handleSuccessfulLogin(user)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Nieprawidłowe hasło")
                    }
                }
            } else {
                // Fallback for demo users not in list (if any)
                if (event.username == "admin" && event.password == "admin") {
                     _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        isAuthenticated = true, 
                        isGuest = false,
                        isAdmin = true,
                        currentUsername = "admin"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Nieprawidłowa nazwa użytkownika lub hasło")
                }
            }
        }
    }

    private fun handleSuccessfulLogin(user: User) {
        val isAdmin = user.role == "Administrator" || user.username == "admin"
        
        if (user.requiresPasswordChange) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = true, // Logged in but needs action
                isGuest = false,
                isAdmin = isAdmin,
                currentUsername = user.username,
                isPasswordChangeRequired = true
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = true,
                isGuest = false,
                isAdmin = isAdmin,
                currentUsername = user.username,
                isPasswordChangeRequired = false
            )
        }
    }

    private fun setPin(event: AuthEvent.SetPin) {
        val current = _uiState.value.currentUsername ?: return
        val updatedUsers = _uiState.value.users.map {
            if (it.username == current) it.copy(pin = event.pin) else it
        }
        _uiState.value = _uiState.value.copy(users = updatedUsers, message = "PIN został ustawiony")
    }

    private fun setLoginMethod(event: AuthEvent.SetLoginMethod) {
        val current = _uiState.value.currentUsername ?: return
        val updatedUsers = _uiState.value.users.map {
            if (it.username == current) it.copy(loginMethod = event.method) else it
        }
        _uiState.value = _uiState.value.copy(users = updatedUsers)
    }

    private fun adminResetPassword(event: AuthEvent.AdminResetPassword) {
        val updatedUsers = _uiState.value.users.map {
            if (it.id == event.userId) it.copy(requiresPasswordChange = true) else it
        }
        _uiState.value = _uiState.value.copy(users = updatedUsers, message = "Wymuszono zmianę hasła")
    }

    private fun completePasswordChange(event: AuthEvent.CompletePasswordChange) {
         val current = _uiState.value.currentUsername ?: return
         val updatedUsers = _uiState.value.users.map {
            if (it.username == current) it.copy(requiresPasswordChange = false, password = event.newPassword) else it
        }
        _uiState.value = _uiState.value.copy(
            users = updatedUsers, 
            isPasswordChangeRequired = false, 
            message = "Hasło zostało zmienione"
        )
    }

    private fun register(event: AuthEvent.Register) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            delay(2000) // Simulate network delay

            if (event.username.isNotBlank() && event.password.isNotBlank() && event.password == event.confirmPassword) {
                // Mock success
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    isAuthenticated = true, 
                    isGuest = false,
                    isAdmin = false,
                    currentUsername = event.username
                )
            } else {
                val errorMsg = if (event.password != event.confirmPassword) "Hasła nie są identyczne" else "Wypełnij wszystkie pola"
                _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
            }
        }
    }

    private fun forgotPassword(event: AuthEvent.ForgotPassword) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            delay(1000)
            if (event.username.isNotBlank()) {
                 _uiState.value = _uiState.value.copy(isLoading = false, message = "Link resetujący został wysłany (mock)")
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Podaj nazwę użytkownika")
            }
        }
    }

    private fun emergencyLogin() {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = true, 
            isGuest = true,
            isAdmin = false,
            currentUsername = "Gość"
        )
    }

    private fun adminLogin() {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = true,
            isGuest = false,
            isAdmin = true,
            currentUsername = "admin"
        )
    }


    private fun logout() {
        viewModelScope.launch {
            settingsDataStore.saveSkipLogin(false)
        }
        _uiState.value = AuthUiState() // Reset to initial state
    }

    private fun changePassword(event: AuthEvent.ChangePassword) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            delay(1500)
            
            val current = _uiState.value.currentUsername
            val user = _uiState.value.users.find { it.username == current }

            if (user != null && user.password == event.oldPass) {
                 if (event.newPassword == event.confirmNewPassword && event.newPassword.isNotBlank()) {
                    val updatedUsers = _uiState.value.users.map {
                        if (it.username == current) it.copy(password = event.newPassword) else it
                    }
                    _uiState.value = _uiState.value.copy(users = updatedUsers, isLoading = false, message = "Hasło zostało zmienione pomyślnie")
                } else {
                     _uiState.value = _uiState.value.copy(isLoading = false, error = "Nowe hasła nie są identyczne")
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Stare hasło jest nieprawidłowe")
            }
        }
    }
    private fun addUser(event: AuthEvent.AddUser) {
        val newUser = User(
            id = System.currentTimeMillis().toString(),
            username = event.username,
            password = event.username, // Default password = username
            role = event.role,
            requiresPasswordChange = true
        )
        val updatedList = _uiState.value.users + newUser
        _uiState.value = _uiState.value.copy(users = updatedList, message = "Dodano użytkownika ${event.username} (Hasło: ${event.username})")
    }

    private fun deleteUser(event: AuthEvent.DeleteUser) {
        val updatedList = _uiState.value.users.filter { it.id != event.userId }
        _uiState.value = _uiState.value.copy(users = updatedList, message = "Usunięto użytkownika")
    }
}
package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.data.model.InventoryWasteRequest
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.util.Log

class InventoryViewModel @JvmOverloads constructor(
    application: Application,
    repo: InventoryRepository? = null
) : AndroidViewModel(application) {
    private val TAG = "WAREHOUSE_DEBUG"
    private val repository = repo ?: InventoryRepository(application)

    private val _items = mutableStateOf<List<InventoryItemDto>>(emptyList())
    val items: State<List<InventoryItemDto>> = _items

    val profiles = repository.getProfilesFlow()
        .map { list -> list.map { it.code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val colors = repository.getColorsFlow()
        .map { list -> list.map { it.code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        // Observe configs immediately
    }

    fun loadItems(
        location: String? = null, 
        profileCode: String? = null,
        internalColor: String? = null,
        externalColor: String? = null,
        coreColor: String? = null
    ) {
        Log.d(TAG, "VM loadItems: Filtrowanie...")
        // Observe local DB
        viewModelScope.launch {
            repository.getItemsFlow(location, profileCode, internalColor, externalColor, coreColor).collectLatest {
                Log.d(TAG, "VM Flow update: ${it.size} elementów")
                _items.value = it
            }
        }

        // Trigger network refresh
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Refresh Configs as well
            launch {
                Log.d(TAG, "VM: Odświeżanie konfiguracji")
                repository.refreshConfig()
            }

            Log.d(TAG, "VM: Odświeżanie items z sieci")
            val result = repository.refreshItems(location, profileCode, internalColor, externalColor, coreColor)
            
            result.onFailure {
                Log.w(TAG, "VM Error: ${it.message}")
                // If offline, we just show a message but keep local data
                _error.value = "Tryb offline: Wyświetlam lokalne dane"
            }
            
            _isLoading.value = false
        }
    }

    fun registerWaste(request: InventoryWasteRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.registerWaste(request)
            onSuccess() // Optimistic UI update
            _isLoading.value = false
        }
    }
    
    fun takeItem(request: InventoryTakeRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.takeItem(request)
            onSuccess() // Optimistic UI update
            _isLoading.value = false
        }
    }

    fun updateItemLength(item: InventoryItemDto, newLength: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateItemLength(item, newLength)
            _isLoading.value = false
        }
    }
}

package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import android.util.Log
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

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

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
            repository.updateItemLength(item, newLength)
            // Refresh list
            val currentItems = _items.value.toMutableList()
            val index = currentItems.indexOfFirst { it.id == item.id }
            if (index != -1) {
                currentItems[index] = item.copy(lengthMm = newLength)
                _items.value = currentItems
            }
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            try {
                val file = File(context.cacheDir, "inventory_export.csv")
                val writer = FileWriter(file)
                
                // Header
                writer.append("ID,Status,Profil,Kolor Wew,Kolor Zew,Dlugosc(mm),Lokalizacja\n")
                
                // Data
                _items.value.forEach { item ->
                    writer.append("${item.id},${item.status},${item.profileCode},${item.internalColor},${item.externalColor},${item.lengthMm},${item.location.label}\n")
                }
                
                writer.flush()
                writer.close()

                // Share Intent
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val shareIntent = Intent.createChooser(intent, "Eksportuj Magazyn (CSV)")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Błąd eksportu: ${e.message}"
            }
        }
    }
}

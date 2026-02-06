package com.example.warehouse.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.warehouse.util.FittingComponent
import com.example.warehouse.util.FittingSystem
import com.example.warehouse.util.HardwareCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HardwarePickerViewModel : ViewModel() {
    
    private val _components = MutableStateFlow<List<FittingComponent>>(emptyList())
    val components: StateFlow<List<FittingComponent>> = _components.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun calculate(system: FittingSystem, ffb: String, ffh: String) {
        _error.value = null
        val w = ffb.toIntOrNull()
        val h = ffh.toIntOrNull()

        if (w == null || h == null) {
            _error.value = "Wprowadź poprawne wymiary (liczby całkowite mm)"
            return
        }

        if (w < 260 || h < 260) {
            _error.value = "Wymiary poza zakresem (min 260mm)"
            return
        }

        val result = HardwareCalculator.calculate(system, w, h)
        _components.value = result
    }

    fun clear() {
        _components.value = emptyList()
        _error.value = null
    }
}

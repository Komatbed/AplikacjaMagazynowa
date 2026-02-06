package com.example.warehouse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.CatalogCategory
import com.example.warehouse.data.model.CatalogProduct
import com.example.warehouse.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val repository: CatalogRepository = CatalogRepository()
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CatalogCategory>>(emptyList())
    val categories: StateFlow<List<CatalogCategory>> = _categories.asStateFlow()

    private val _products = MutableStateFlow<List<CatalogProduct>>(emptyList())
    val products: StateFlow<List<CatalogProduct>> = _products.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CatalogCategory?>(null)
    val selectedCategory: StateFlow<CatalogCategory?> = _selectedCategory.asStateFlow()

    private val _selectedProduct = MutableStateFlow<CatalogProduct?>(null)
    val selectedProduct: StateFlow<CatalogProduct?> = _selectedProduct.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().collect {
                _categories.value = it
            }
        }
    }

    fun selectCategory(categoryId: String) {
        viewModelScope.launch {
            // Find category object
            val category = _categories.value.find { it.id == categoryId }
            _selectedCategory.value = category
            
            // Load products for this category
            repository.getProductsByCategory(categoryId).collect {
                _products.value = it
            }
        }
    }

    fun selectProduct(productId: String) {
        viewModelScope.launch {
            repository.getProductById(productId).collect {
                _selectedProduct.value = it
            }
        }
    }

    fun clearCategorySelection() {
        _selectedCategory.value = null
        _products.value = emptyList()
    }

    fun clearProductSelection() {
        _selectedProduct.value = null
    }
}

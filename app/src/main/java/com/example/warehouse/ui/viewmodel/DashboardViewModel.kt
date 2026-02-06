package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NewsItem(
    val title: String,
    val date: String,
    val content: String
)

data class DashboardStats(
    val totalItems: Int = 0,
    val totalItemsChange: Double = 0.0,
    val totalLength: Int = 0,
    val wasteCount: Int = 0,
    val fullCount: Int = 0,
    val uniqueProfiles: Int = 0,
    val reservationCount: Int = 0,
    val reservationChange: Double = 0.0,
    val freePalettes: Int = 0,
    val freePalettesChange: Double = 0.0,
    val occupancyPercent: Double = 0.0,
    val occupancyChange: Double = 0.0,
    val news: List<NewsItem> = emptyList()
)

class DashboardViewModel @JvmOverloads constructor(
    application: Application,
    repo: InventoryRepository? = null
) : AndroidViewModel(application) {
    private val repository = repo ?: InventoryRepository(application)

    val stats: StateFlow<DashboardStats> = repository.getItemsFlow()
        .map { items -> calculateStats(items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    private fun calculateStats(items: List<InventoryItemDto>): DashboardStats {
        val totalItems = items.sumOf { it.quantity }
        val wasteCount = items.filter { it.status == "WASTE" || it.lengthMm < 6000 }.sumOf { it.quantity }
        val fullCount = items.filter { it.status == "FULL" || it.lengthMm >= 6000 }.sumOf { it.quantity }
        val uniqueProfiles = items.map { it.profileCode }.distinct().count()
        
        val reservationCount = items.filter { it.status == "RESERVED" || it.status == "IN_PROGRESS" }.sumOf { it.quantity }
            
            // Mocking warehouse capacity data since LocationRepository is separate
            // Assuming total capacity ~500 palettes
            val occupiedPalettes = items.map { it.location.paletteNumber }.distinct().count()
            val totalPalettes = 500
        val freePalettes = totalPalettes - occupiedPalettes
        val occupancyPercent = (occupiedPalettes.toDouble() / totalPalettes.toDouble()) * 100.0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        return DashboardStats(
            totalItems = totalItems,
            totalItemsChange = 5.2, // Mocked change
            totalLength = items.sumOf { it.lengthMm * it.quantity },
            wasteCount = wasteCount,
            fullCount = fullCount,
            uniqueProfiles = uniqueProfiles,
            reservationCount = reservationCount,
            reservationChange = 12.5, // Mocked change
            freePalettes = freePalettes,
            freePalettesChange = -2.1, // Mocked change
            occupancyPercent = occupancyPercent,
            occupancyChange = 1.8, // Mocked change
            news = listOf(
                NewsItem("Aktualizacja Systemu", today, "Wdrożono nową wersję aplikacji v2.1."),
                NewsItem("Dostawa Profili", "2024-05-20", "Planowana dostawa profili Veka - 24 palety."),
                NewsItem("Inwentaryzacja", "2024-05-15", "Zakończono inwentaryzację sektora A."),
                NewsItem("Nowe Zasady", "2024-05-10", "Zmieniono zasady rezerwacji odpadów."),
                NewsItem("Awaria Wózka", "2024-05-05", "Wózek widłowy nr 3 w naprawie.")
            )
        )
    }
}


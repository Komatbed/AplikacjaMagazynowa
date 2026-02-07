package com.example.warehouse.controller

import com.example.warehouse.repository.InventoryItemRepository
import com.example.warehouse.repository.IssueReportRepository
import com.example.warehouse.repository.OperationLogRepository
import com.example.warehouse.repository.ShortageRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val inventoryItemRepository: InventoryItemRepository,
    private val operationLogRepository: OperationLogRepository,
    private val issueReportRepository: IssueReportRepository,
    private val shortageRepository: ShortageRepository
) {

    @GetMapping("/stats")
    fun getStats(): DashboardStats {
        val totalItems = inventoryItemRepository.count()
        val lowStockItems = inventoryItemRepository.findAll().filter { it.quantity < 10 }.size // Ideally do this in DB query
        val activeClaims = issueReportRepository.findAll().filter { it.status == com.example.warehouse.model.IssueStatus.NEW || it.status == com.example.warehouse.model.IssueStatus.IN_PROGRESS }.size
        val activeShortages = shortageRepository.findAll().filter { it.status == "NEW" || it.status == "IN_PROGRESS" }.size // Ideally enum
        
        // Recent activity: get last 5 logs
        val recentActivity = operationLogRepository.findTop10ByOrderByTimestampDesc().take(5).map { log ->
            ActivityDto(
                id = log.id.toString(),
                type = log.operationType,
                description = log.reason ?: "${log.operationType} - ${log.quantityChange}",
                timestamp = log.timestamp
            )
        }

        return DashboardStats(
            totalItems = totalItems.toInt(),
            lowStockCount = lowStockItems,
            pendingClaims = activeClaims,
            activeShortages = activeShortages,
            recentActivity = recentActivity
        )
    }

    data class DashboardStats(
        val totalItems: Int,
        val lowStockCount: Int,
        val pendingClaims: Int,
        val activeShortages: Int,
        val recentActivity: List<ActivityDto>
    )

    data class ActivityDto(
        val id: String,
        val type: String,
        val description: String,
        val timestamp: java.time.LocalDateTime
    )
}

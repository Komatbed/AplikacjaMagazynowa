package com.example.warehouse.data.repository

import android.content.Context
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.data.api.WarehouseApi
import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.dao.AuditLogDao
import com.example.warehouse.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

class AuditRepository(
    private val auditLogDao: AuditLogDao,
    private val apiProvider: () -> WarehouseApi = { NetworkModule.api }
) {
    constructor(context: Context) : this(
        WarehouseDatabase.getDatabase(context).auditLogDao()
    )

    private val api get() = apiProvider()

    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogs()

    suspend fun clearAuditLogs() {
        auditLogDao.clearLogs()
    }

    suspend fun logAction(action: String, itemType: String, details: String) {
        auditLogDao.insert(AuditLogEntity(
            action = action,
            itemType = itemType,
            details = details
        ))
    }
}

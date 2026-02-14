package com.example.warehouse.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.InventoryItemEntity
import com.example.warehouse.data.local.entity.OperationType
import com.example.warehouse.data.local.entity.PendingOperationEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.data.model.InventoryTakeResponse
import com.example.warehouse.data.model.InventoryWasteRequest
import com.example.warehouse.data.model.LocationDto
import com.example.warehouse.data.model.InventoryItemUpdatePayload
import com.example.warehouse.work.SyncWorker
import com.example.warehouse.data.api.WarehouseApi
import com.example.warehouse.data.local.dao.AuditLogDao
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.dao.InventoryDao
import com.example.warehouse.data.local.dao.PendingOperationDao
import com.example.warehouse.data.local.entity.AuditLogEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import android.util.Log

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val pendingDao: PendingOperationDao,
    private val auditLogDao: AuditLogDao,
    private val workManager: WorkManager,
    private val apiProvider: () -> WarehouseApi = { NetworkModule.api }
) {
    constructor(context: Context) : this(
        WarehouseDatabase.getDatabase(context).inventoryDao(),
        WarehouseDatabase.getDatabase(context).pendingOperationDao(),
        WarehouseDatabase.getDatabase(context).auditLogDao(),
        WorkManager.getInstance(context)
    )

    private val TAG = "WAREHOUSE_DEBUG"
    private val api get() = apiProvider()
    private val gson = Gson()

    // Read from DB (Offline-first)
    fun getItemsFlow(location: String? = null, profileCode: String? = null, internalColor: String? = null, externalColor: String? = null, coreColor: String? = null): Flow<List<InventoryItemDto>> {
        return inventoryDao.getItemsFiltered(location, profileCode, internalColor, externalColor, coreColor)
            .map { entities ->
                entities.map { entity ->
                    InventoryItemDto(
                        id = entity.id,
                        location = mapLocation(entity.locationLabel), // Simplified location
                        profileCode = entity.profileCode,
                        internalColor = entity.internalColor,
                        externalColor = entity.externalColor,
                        coreColor = entity.coreColor,
                        lengthMm = entity.lengthMm,
                        quantity = entity.quantity,
                        status = entity.status
                    )
                }
            }
    }

    // Trigger Network Refresh
    suspend fun refreshItems(location: String? = null, profileCode: String? = null, internalColor: String? = null, externalColor: String? = null, coreColor: String? = null): Result<Unit> {
        Log.d(TAG, "refreshItems: loc=$location profile=$profileCode int=$internalColor ext=$externalColor")
        return try {
            val response = api.getItems(location, profileCode, internalColor, externalColor, coreColor)
            Log.d(TAG, "refreshItems: Pobranno ${response.size} elementów z API")
            
            // Smart Clear: If we have specific filters that cover a full set (e.g. all items in location),
            // we should clear that set locally to avoid ghost items (deleted on server but kept locally).
            if (location != null && profileCode == null && internalColor == null && externalColor == null && coreColor == null) {
                inventoryDao.deleteByLocation(location)
            } else if (profileCode != null && location == null && internalColor == null && externalColor == null && coreColor == null) {
                inventoryDao.deleteByProfile(profileCode)
            } else if (location == null && profileCode == null && internalColor == null && externalColor == null && coreColor == null) {
                // No filters = Full Sync
                inventoryDao.clearAll()
            }
            // If complex filters are used, we default to Merge (Insert/Replace) without deletion, 
            // as we can't easily delete "partial" matches without a complex DELETE query matching the API filter logic.

            // Save to DB
            val entities = response.map { dto ->
                InventoryItemEntity(
                    id = dto.id,
                    locationLabel = dto.location.label,
                    profileCode = dto.profileCode,
                    internalColor = dto.internalColor,
                    externalColor = dto.externalColor,
                    coreColor = dto.coreColor,
                    lengthMm = dto.lengthMm,
                    quantity = dto.quantity,
                    status = dto.status
                )
            }
            inventoryDao.insertAll(entities)
            Log.d(TAG, "refreshItems: Zapisano do bazy lokalnej")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "refreshItems Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addItem(
        locationLabel: String,
        profileCode: String,
        internalColor: String,
        externalColor: String,
        coreColor: String?,
        lengthMm: Int,
        quantity: Int,
        status: String = "AVAILABLE"
    ): Result<Unit> {
        return try {
            val id = java.util.UUID.randomUUID().toString()
            val entity = InventoryItemEntity(
                id = id,
                locationLabel = locationLabel,
                profileCode = profileCode,
                internalColor = internalColor,
                externalColor = externalColor,
                coreColor = coreColor,
                lengthMm = lengthMm,
                quantity = quantity,
                status = status
            )
            inventoryDao.insertAll(listOf(entity))
            auditLogDao.insert(AuditLogEntity(
                action = "ADD_ITEM",
                itemType = "INVENTORY",
                details = "Dodano: $profileCode $lengthMm mm ($quantity) do $locationLabel"
            ))
            try {
                val dto = InventoryItemDto(
                    id = id,
                    location = mapLocation(locationLabel),
                    profileCode = profileCode,
                    internalColor = internalColor,
                    externalColor = externalColor,
                    coreColor = coreColor,
                    lengthMm = lengthMm,
                    quantity = quantity,
                    status = status
                )
                api.addItem(dto)
            } catch (e: Exception) {
                val dto = InventoryItemDto(
                    id = id,
                    location = mapLocation(locationLabel),
                    profileCode = profileCode,
                    internalColor = internalColor,
                    externalColor = externalColor,
                    coreColor = coreColor,
                    lengthMm = lengthMm,
                    quantity = quantity,
                    status = status
                )
                val op = PendingOperationEntity(
                    type = OperationType.ADD_ITEM,
                    payloadJson = gson.toJson(dto)
                )
                pendingDao.insert(op)
                scheduleSync()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addItem Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun takeItem(request: InventoryTakeRequest) {
        Log.d(TAG, "takeItem: $request")
        // Add to Pending Queue
        val operation = PendingOperationEntity(
            type = OperationType.TAKE_ITEM,
            payloadJson = gson.toJson(request)
        )
        pendingDao.insert(operation)
        
        auditLogDao.insert(AuditLogEntity(
            action = "TAKE_ITEM",
            itemType = "INVENTORY",
            details = "Pobrano: ${request.profileCode}, ${request.lengthMm}mm"
        ))
        
        Log.d(TAG, "takeItem: Dodano do kolejki operacji (offline)")
        scheduleSync()
    }

    suspend fun registerWaste(request: InventoryWasteRequest) {
        Log.d(TAG, "registerWaste: $request")
        // Add to Pending Queue
        val operation = PendingOperationEntity(
            type = OperationType.REGISTER_WASTE,
            payloadJson = gson.toJson(request)
        )
        pendingDao.insert(operation)

        auditLogDao.insert(AuditLogEntity(
            action = "REGISTER_WASTE",
            itemType = "INVENTORY",
            details = "Odpad: ${request.profileCode}, ${request.lengthMm}mm"
        ))

        Log.d(TAG, "registerWaste: Dodano do kolejki operacji (offline)")
        scheduleSync()
    }
    
    suspend fun updateItemLength(item: InventoryItemDto, newLength: Int) {
        // Optimistic UI Update
        inventoryDao.updateLength(item.id, newLength)

        // Add to Pending Queue
        val payload = InventoryItemUpdatePayload(item.id, newLength)
        val operation = PendingOperationEntity(
            type = OperationType.UPDATE_ITEM_LENGTH,
            payloadJson = gson.toJson(payload)
        )
        pendingDao.insert(operation)
        
        auditLogDao.insert(AuditLogEntity(
            action = "UPDATE_LENGTH",
            itemType = "INVENTORY",
            details = "Zmiana długości ID ${item.id}: ${item.lengthMm}mm -> ${newLength}mm"
        ))

        scheduleSync()
    }
    
    suspend fun checkConnection(): Result<Unit> {
        return try {
            api.getConfig() // Lightweight call
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWarehouseMap(): Result<List<com.example.warehouse.data.model.LocationStatusDto>> {
        return try {
            val map = api.getWarehouseMap()
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLocationCapacity(id: Int, capacity: Int): Result<Unit> {
        return try {
            api.updateLocationCapacity(id, capacity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportIssue(description: String, profileCode: String? = null, locationLabel: String? = null): Result<Unit> {
        return try {
            api.reportIssue(com.example.warehouse.data.model.IssueReportRequest(description, profileCode, locationLabel))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItemById(id: String): Result<Unit> {
        return try {
            inventoryDao.deleteById(id)
            auditLogDao.insert(AuditLogEntity(
                action = "DELETE_ITEM",
                itemType = "INVENTORY",
                details = "Usunięto element: $id"
            ))
            try {
                api.deleteItem(id)
                Result.success(Unit)
            } catch (e: Exception) {
                val payload = com.example.warehouse.data.model.InventoryItemDeletePayload(id)
                val op = PendingOperationEntity(
                    type = OperationType.DELETE_ITEM,
                    payloadJson = gson.toJson(payload)
                )
                pendingDao.insert(op)
                scheduleSync()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Assembler Tools
    suspend fun findOptimalWaste(profileCode: String, minLength: Int, externalColor: String? = null, internalColor: String? = null): InventoryItemDto? {
        return inventoryDao.findBestWaste(profileCode, minLength, externalColor, internalColor)?.let { entity ->
            InventoryItemDto(
                id = entity.id,
                location = mapLocation(entity.locationLabel),
                profileCode = entity.profileCode,
                internalColor = entity.internalColor,
                externalColor = entity.externalColor,
                coreColor = entity.coreColor,
                lengthMm = entity.lengthMm,
                quantity = entity.quantity,
                status = entity.status
            )
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10L,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueue(syncRequest)
    }
    
    private fun mapLocation(label: String): LocationDto {
        val regex = Regex("""Z(\d+)[\-\s]?R(\d+)[\-\s]?P(\d+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(label)
        return if (match != null) {
            val (zone, row, pos) = match.destructured
            LocationDto(zone.toLongOrNull() ?: 0L, row.toIntOrNull() ?: 0, pos.toIntOrNull() ?: 0, label)
        } else {
            LocationDto(0L, 0, 0, label)
        }
    }
}

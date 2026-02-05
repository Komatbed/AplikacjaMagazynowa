package com.example.warehouse.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import android.util.Log

class InventoryRepository(context: Context) {
    private val TAG = "WAREHOUSE_DEBUG"
    private val api get() = NetworkModule.api
    private val db = WarehouseDatabase.getDatabase(context)
    private val inventoryDao = db.inventoryDao()
    private val pendingDao = db.pendingOperationDao()
    private val configDao = db.configDao()
    private val workManager by lazy { WorkManager.getInstance(context) }
    private val gson = Gson()

    // Read from DB (Offline-first)
    fun getItemsFlow(location: String? = null, profileCode: String? = null, internalColor: String? = null, externalColor: String? = null, coreColor: String? = null): Flow<List<InventoryItemDto>> {
        return inventoryDao.getItemsFiltered(location, profileCode, internalColor, externalColor, coreColor)
            .map { entities ->
                entities.map { entity ->
                    InventoryItemDto(
                        id = entity.id,
                        location = LocationDto(0, 0, 0, entity.locationLabel), // Simplified location
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

    suspend fun takeItem(request: InventoryTakeRequest) {
        Log.d(TAG, "takeItem: $request")
        // Add to Pending Queue
        val operation = PendingOperationEntity(
            type = OperationType.TAKE_ITEM,
            payloadJson = gson.toJson(request)
        )
        pendingDao.insert(operation)
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
        scheduleSync()
    }
    
    // Config Methods
    fun getProfilesFlow(): Flow<List<ProfileEntity>> = configDao.getProfiles()
    fun getColorsFlow(): Flow<List<ColorEntity>> = configDao.getColors()

    suspend fun refreshConfig(): Result<Unit> {
        return try {
            val profiles = api.getProfiles()
            val colors = api.getColors()

            configDao.insertProfiles(profiles.map { ProfileEntity(it.code, it.id ?: java.util.UUID.randomUUID().toString(), it.description) })
            configDao.insertColors(colors.map { ColorEntity(it.code, it.id ?: java.util.UUID.randomUUID().toString(), it.description) })
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addProfile(code: String, description: String): Result<Unit> {
        return try {
            val profile = com.example.warehouse.data.model.ProfileDefinition(code = code, description = description)
            api.addProfile(profile)
            refreshConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            api.deleteProfile(id)
            refreshConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addColor(code: String, description: String): Result<Unit> {
        return try {
            val color = com.example.warehouse.data.model.ColorDefinition(code = code, description = description)
            api.addColor(color)
            refreshConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteColor(id: String): Result<Unit> {
        return try {
            api.deleteColor(id)
            refreshConfig()
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

    suspend fun reportIssue(description: String, profileCode: String? = null, locationLabel: String? = null): Result<Unit> {
        return try {
            api.reportIssue(com.example.warehouse.data.model.IssueReportRequest(description, profileCode, locationLabel))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(syncRequest)
    }
}

package com.example.warehouse.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.entity.OperationType
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.data.model.InventoryWasteRequest
import com.example.warehouse.data.model.InventoryItemUpdatePayload
import com.google.gson.Gson

import com.example.warehouse.data.repository.ConfigRepository

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = WarehouseDatabase.getDatabase(context)
    private val pendingDao = db.pendingOperationDao()
    private val api = NetworkModule.api
    private val gson = Gson()
    private val configRepository = ConfigRepository(db.configDao(), db.auditLogDao())

    override suspend fun doWork(): Result {
        // 1. Sync Configuration (Profiles, Colors, Core Rules)
        try {
            configRepository.refreshConfig()
        } catch (e: Exception) {
            e.printStackTrace()
            // Config sync fail shouldn't stop operation sync, but good to log
        }

        val pendingOps = pendingDao.getAllPending()
        
        if (pendingOps.isEmpty()) {
            return Result.success()
        }

        var successCount = 0
        
        for (op in pendingOps) {
            try {
                when (op.type) {
                    OperationType.REGISTER_WASTE -> {
                        val request = gson.fromJson(op.payloadJson, InventoryWasteRequest::class.java)
                        api.registerWaste(request)
                    }
                    OperationType.TAKE_ITEM -> {
                        val request = gson.fromJson(op.payloadJson, InventoryTakeRequest::class.java)
                        api.takeItem(request)
                    }
                    OperationType.UPDATE_ITEM_LENGTH -> {
                        val payload = gson.fromJson(op.payloadJson, InventoryItemUpdatePayload::class.java)
                        api.updateItemLength(payload.id, payload.length)
                    }
                }
                // If successful, remove from queue
                pendingDao.delete(op)
                successCount++
            } catch (e: Exception) {
                // If network error, keep in queue (will retry next time)
                // Optionally increment retry count or mark as failed
                e.printStackTrace()
            }
        }

        // Trigger a fresh fetch of items if we made changes
        if (successCount > 0) {
            try {
                val items = api.getItems()
                // Map DTO to Entity and update DB
                val entities = items.map { dto ->
                    com.example.warehouse.data.local.entity.InventoryItemEntity(
                        id = dto.id,
                        locationLabel = dto.location.label,
                        profileCode = dto.profileCode,
                        lengthMm = dto.lengthMm,
                        quantity = dto.quantity,
                        status = dto.status
                    )
                }
                db.inventoryDao().clearAll() // Or smarter update
                db.inventoryDao().insertAll(entities)
            } catch (e: Exception) {
                // Ignore fetch error, at least we pushed changes
            }
        }

        return if (successCount == pendingOps.size) Result.success() else Result.retry()
    }
}

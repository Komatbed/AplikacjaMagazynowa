package com.example.warehouse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.warehouse.data.local.entity.PendingOperationEntity

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperationEntity)

    @Delete
    suspend fun delete(operation: PendingOperationEntity)
    
    @Query("SELECT COUNT(*) FROM pending_operations")
    suspend fun getPendingCount(): Int
}

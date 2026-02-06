package com.example.warehouse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.warehouse.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY locationLabel ASC")
    fun getAllItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE locationLabel = :location ORDER BY lengthMm DESC")
    fun getItemsByLocation(location: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE profileCode = :profileCode ORDER BY lengthMm DESC")
    fun getItemsByProfile(profileCode: String): Flow<List<InventoryItemEntity>>
    
    @Query("SELECT * FROM inventory_items WHERE (:profileCode IS NULL OR profileCode = :profileCode) AND (:location IS NULL OR locationLabel = :location) AND (:internalColor IS NULL OR internalColor = :internalColor) AND (:externalColor IS NULL OR externalColor = :externalColor) AND (:coreColor IS NULL OR coreColor = :coreColor) ORDER BY lengthMm DESC")
    fun getItemsFiltered(location: String?, profileCode: String?, internalColor: String?, externalColor: String?, coreColor: String?): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItemEntity>)

    @Query("DELETE FROM inventory_items")
    suspend fun clearAll()

    @Query("UPDATE inventory_items SET lengthMm = :newLength WHERE id = :id")
    suspend fun updateLength(id: String, newLength: Int)

    // Assembler Tool: Find the smallest piece that is long enough (reduces waste of full bars)
    @Query("""
        SELECT * FROM inventory_items 
        WHERE profileCode = :profileCode 
        AND lengthMm >= :minLength 
        AND status != 'RESERVED'
        ORDER BY lengthMm ASC 
        LIMIT 1
    """)
    suspend fun findBestWaste(profileCode: String, minLength: Int): InventoryItemEntity?
}

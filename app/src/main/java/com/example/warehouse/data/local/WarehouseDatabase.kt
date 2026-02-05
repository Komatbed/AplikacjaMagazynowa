package com.example.warehouse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.dao.InventoryDao
import com.example.warehouse.data.local.dao.PendingOperationDao
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.InventoryItemEntity
import com.example.warehouse.data.local.entity.PendingOperationEntity
import com.example.warehouse.data.local.entity.ProfileEntity

@Database(
    entities = [
        InventoryItemEntity::class, 
        PendingOperationEntity::class,
        ProfileEntity::class,
        ColorEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class WarehouseDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: WarehouseDatabase? = null

        fun getDatabase(context: Context): WarehouseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarehouseDatabase::class.java,
                    "warehouse_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

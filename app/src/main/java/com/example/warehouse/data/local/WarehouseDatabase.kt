package com.example.warehouse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.warehouse.data.local.dao.AuditLogDao
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.dao.InventoryDao
import com.example.warehouse.data.local.dao.PendingOperationDao
import com.example.warehouse.data.local.dao.PresetDao
import com.example.warehouse.data.local.entity.AuditLogEntity
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.CoreColorRuleEntity
import com.example.warehouse.data.local.entity.InventoryItemEntity
import com.example.warehouse.data.local.entity.PendingOperationEntity
import com.example.warehouse.data.local.entity.PresetEntity
import com.example.warehouse.data.local.entity.ProfileEntity

@Database(
    entities = [
        InventoryItemEntity::class, 
        PendingOperationEntity::class,
        ProfileEntity::class,
        ColorEntity::class,
        CoreColorRuleEntity::class,
        PresetEntity::class,
        AuditLogEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class WarehouseDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun configDao(): ConfigDao
    abstract fun presetDao(): PresetDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: WarehouseDatabase? = null

        fun getDatabase(context: Context): WarehouseDatabase {
            return INSTANCE ?: synchronized(this) {
                // Ensure SQLCipher native libraries are loaded
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

                // Initialize SQLCipher key
                val keyManager = SecurityKeyManager(context.applicationContext)
                val passphrase = keyManager.getDatabasePassphrase()
                val factory = net.sqlcipher.database.SupportFactory(passphrase)

                // Check for unencrypted database file (migration from plain SQLite to SQLCipher)
                // If the file exists and has the SQLite header, it's unencrypted. Delete it to start fresh with encryption.
                val dbFile = context.getDatabasePath("warehouse_database")
                if (dbFile.exists()) {
                    try {
                        val header = ByteArray(16)
                        dbFile.inputStream().use { it.read(header) }
                        val headerString = String(header, Charsets.US_ASCII)
                        if (headerString.startsWith("SQLite format 3")) {
                            // Found unencrypted database, delete it
                            context.deleteDatabase("warehouse_database")
                        }
                    } catch (e: Exception) {
                        // Ignore read errors
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarehouseDatabase::class.java,
                    "warehouse_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

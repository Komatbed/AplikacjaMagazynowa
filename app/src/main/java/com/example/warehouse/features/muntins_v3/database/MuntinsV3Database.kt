package com.example.warehouse.features.muntins_v3.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.warehouse.features.muntins_v3.database.dao.MuntinsV3Dao
import com.example.warehouse.features.muntins_v3.database.entity.*

@Database(
    entities = [
        ProfileEntity::class,
        GlassBeadEntity::class,
        MuntinEntity::class,
        ProjectEntity::class,
        LayoutEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MuntinsV3Database : RoomDatabase() {
    abstract fun muntinsV3Dao(): MuntinsV3Dao

    companion object {
        @Volatile
        private var INSTANCE: MuntinsV3Database? = null

        fun getDatabase(context: Context): MuntinsV3Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MuntinsV3Database::class.java,
                    "muntins_v3_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

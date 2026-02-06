package com.example.warehouse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

import com.example.warehouse.data.local.entity.CoreColorRuleEntity

@Dao
interface ConfigDao {
    @Query("SELECT * FROM profile_definitions ORDER BY code ASC")
    fun getProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile_definitions ORDER BY code ASC")
    suspend fun getProfilesSync(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Query("SELECT * FROM color_definitions ORDER BY code ASC")
    fun getColors(): Flow<List<ColorEntity>>

    @Query("SELECT * FROM color_definitions ORDER BY code ASC")
    suspend fun getColorsSync(): List<ColorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColors(colors: List<ColorEntity>)

    @Query("SELECT * FROM core_color_rules")
    fun getCoreColorRules(): Flow<List<CoreColorRuleEntity>>

    @Query("SELECT * FROM core_color_rules WHERE ext_color_code = :extCode")
    suspend fun getCoreColorRule(extCode: String): CoreColorRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoreColorRules(rules: List<CoreColorRuleEntity>)
}

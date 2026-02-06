package com.example.warehouse.data.repository

import com.example.warehouse.data.api.WarehouseApi
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.CoreColorRuleEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConfigRepository(
    private val api: WarehouseApi,
    private val configDao: ConfigDao
) {
    // Data Sources
    val profiles: Flow<List<ProfileEntity>> = configDao.getProfiles()
    val colors: Flow<List<ColorEntity>> = configDao.getColors()
    val coreRules: Flow<List<CoreColorRuleEntity>> = configDao.getCoreColorRules()

    // Sync Logic
    suspend fun syncConfiguration() {
        try {
            // 1. Fetch Profiles
            val profilesApi = api.getProfiles()
            val profilesEntities = profilesApi.map { 
                ProfileEntity(
                    code = it.code,
                    id = it.id ?: "",
                    description = it.description,
                    heightMm = it.heightMm,
                    widthMm = it.widthMm,
                    beadHeightMm = it.beadHeightMm,
                    beadAngle = it.beadAngle,
                    standardLengthMm = it.standardLengthMm,
                    system = it.system,
                    manufacturer = it.manufacturer
                )
            }
            configDao.insertProfiles(profilesEntities)

            // 2. Fetch Colors
            val colorsApi = api.getColors()
            val colorsEntities = colorsApi.map {
                ColorEntity(
                    code = it.code,
                    id = it.id ?: "",
                    description = it.description,
                    name = it.name,
                    paletteCode = it.paletteCode,
                    vekaCode = it.vekaCode,
                    type = it.type,
                    foilManufacturer = it.foilManufacturer
                )
            }
            configDao.insertColors(colorsEntities)

            // 3. Fetch Core Rules
            val rulesMap = api.getCoreRules()
            val rulesEntities = rulesMap.map { (ext, core) ->
                CoreColorRuleEntity(extColorCode = ext, coreColorCode = core)
            }
            configDao.insertCoreColorRules(rulesEntities)

        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app, we might handle offline mode here (do nothing if offline)
        }
    }
}

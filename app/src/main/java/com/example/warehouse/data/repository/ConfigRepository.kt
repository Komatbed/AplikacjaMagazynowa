package com.example.warehouse.data.repository

import android.content.Context
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.data.api.WarehouseApi
import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.dao.AuditLogDao
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.entity.AuditLogEntity
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.dao.PendingOperationDao
import com.example.warehouse.data.local.entity.OperationType
import com.example.warehouse.data.local.entity.PendingOperationEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.data.model.ColorDefinition
import com.example.warehouse.data.model.ProfileDefinition
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException
import kotlinx.coroutines.flow.Flow

class ConfigRepository(
    private val configDao: ConfigDao,
    private val auditLogDao: AuditLogDao,
    private val pendingOperationDao: PendingOperationDao,
    private val apiProvider: () -> WarehouseApi = { NetworkModule.api }
) {
    constructor(context: Context) : this(
        WarehouseDatabase.getDatabase(context).configDao(),
        WarehouseDatabase.getDatabase(context).auditLogDao(),
        WarehouseDatabase.getDatabase(context).pendingOperationDao()
    )

    private val gson = Gson()
    private val api get() = apiProvider()

    // Config Methods
    fun getProfilesFlow(): Flow<List<ProfileEntity>> = configDao.getProfiles()
    fun getColorsFlow(): Flow<List<ColorEntity>> = configDao.getColors()
    fun getCoreColorRulesFlow() = configDao.getCoreColorRules()

    suspend fun refreshConfig(): Result<Unit> {
        return try {
            val profiles = api.getProfiles()
            val colors = api.getColors()

            configDao.insertProfiles(profiles.map { 
                ProfileEntity(
                    code = it.code, 
                    id = it.id ?: java.util.UUID.randomUUID().toString(), 
                    description = it.description,
                    heightMm = it.heightMm,
                    widthMm = it.widthMm,
                    beadHeightMm = it.beadHeightMm,
                    beadAngle = it.beadAngle,
                    standardLengthMm = it.standardLengthMm,
                    system = it.system,
                    manufacturer = it.manufacturer
                ) 
            })
            configDao.insertColors(colors.map { 
                ColorEntity(
                    code = it.code, 
                    id = it.id ?: java.util.UUID.randomUUID().toString(), 
                    description = it.description,
                    name = it.name,
                    paletteCode = it.paletteCode,
                    vekaCode = it.vekaCode,
                    type = it.type,
                    foilManufacturer = it.foilManufacturer
                ) 
            })
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseError(e: Exception): Exception {
        if (e is HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val json = gson.fromJson(errorBody, JsonObject::class.java)
                    val error = json.get("error")?.asString ?: "Błąd"
                    val details = json.get("details")?.asString ?: ""
                    return Exception(if (details.isNotBlank()) "$error: $details" else error)
                }
            } catch (ex: Exception) {
                // Ignore parsing errors
            }
        }
        return e
    }

    suspend fun addProfile(profile: ProfileDefinition): Result<Unit> {
        // 1. Save locally first (Optimistic UI)
        try {
            configDao.insertProfiles(listOf(
                ProfileEntity(
                    code = profile.code,
                    id = profile.id ?: java.util.UUID.randomUUID().toString(),
                    description = profile.description,
                    heightMm = profile.heightMm,
                    widthMm = profile.widthMm,
                    beadHeightMm = profile.beadHeightMm,
                    beadAngle = profile.beadAngle,
                    standardLengthMm = profile.standardLengthMm,
                    system = profile.system,
                    manufacturer = profile.manufacturer
                )
            ))
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return try {
            api.addProfile(profile)
            // If successful, we assume local data is consistent (or we could refresh)
            
            auditLogDao.insert(AuditLogEntity(
                action = "ADD_PROFILE",
                itemType = "CONFIG",
                details = "Dodano profil: ${profile.code} (${profile.description})"
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            val parsed = parseError(e)
            // If network error (not validation error), queue for sync
            if (e !is HttpException || e.code() >= 500) {
                 try {
                     pendingOperationDao.insert(PendingOperationEntity(
                         type = OperationType.ADD_PROFILE,
                         payloadJson = gson.toJson(profile)
                     ))
                     Result.success(Unit) // Return success to UI because we queued it
                 } catch (localEx: Exception) {
                     Result.failure(parsed)
                 }
            } else {
                // Validation error (400, 409) - do not queue, return error to user
                // Also roll back local insertion? Ideally yes, but for now simple optimistic
                // For a proper rollback, we should delete the inserted profile.
                // configDao.deleteProfile(profile.code) // Assuming such method exists
                Result.failure(parsed)
            }
        }
    }

    suspend fun updateProfile(profile: ProfileDefinition): Result<Unit> {
        return try {
            if (profile.id != null) {
                api.updateProfile(profile.id, profile)
            }
            // For offline/local simulation, we just upsert to DB
            configDao.insertProfiles(listOf(
                ProfileEntity(
                    code = profile.code,
                    id = profile.id ?: java.util.UUID.randomUUID().toString(),
                    description = profile.description,
                    heightMm = profile.heightMm,
                    widthMm = profile.widthMm,
                    beadHeightMm = profile.beadHeightMm,
                    beadAngle = profile.beadAngle,
                    standardLengthMm = profile.standardLengthMm,
                    system = profile.system,
                    manufacturer = profile.manufacturer
                )
            ))

            auditLogDao.insert(AuditLogEntity(
                action = "UPDATE_PROFILE",
                itemType = "CONFIG",
                details = "Zaktualizowano profil: ${profile.code}"
            ))
            
            Result.success(Unit)
        } catch (e: Exception) {
            val parsed = parseError(e)
            if (e is HttpException && e.code() in 400..499) {
                return Result.failure(parsed)
            }

             // Fallback to local only if API fails
            try {
                 configDao.insertProfiles(listOf(
                    ProfileEntity(
                        code = profile.code,
                        id = profile.id ?: java.util.UUID.randomUUID().toString(),
                        description = profile.description,
                        heightMm = profile.heightMm,
                        widthMm = profile.widthMm,
                        beadHeightMm = profile.beadHeightMm,
                        beadAngle = profile.beadAngle,
                        standardLengthMm = profile.standardLengthMm,
                        system = profile.system,
                        manufacturer = profile.manufacturer
                    )
                ))
                 auditLogDao.insert(AuditLogEntity(
                    action = "UPDATE_PROFILE_LOCAL",
                    itemType = "CONFIG",
                    details = "Zaktualizowano profil (offline): ${profile.code}"
                ))
                Result.success(Unit)
            } catch (localEx: Exception) {
                Result.failure(parsed)
            }
        }
    }

    suspend fun addProfile(code: String, description: String): Result<Unit> {
        return addProfile(ProfileDefinition(code = code, description = description))
    }

    suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            api.deleteProfile(id)
            refreshConfig()
            
            auditLogDao.insert(AuditLogEntity(
                action = "DELETE_PROFILE",
                itemType = "CONFIG",
                details = "Usunięto profil ID: $id"
            ))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addColor(color: ColorDefinition): Result<Unit> {
        // 1. Save locally first
        try {
            configDao.insertColors(listOf(
                ColorEntity(
                    code = color.code,
                    id = color.id ?: java.util.UUID.randomUUID().toString(),
                    description = color.description,
                    name = color.name,
                    paletteCode = color.paletteCode,
                    vekaCode = color.vekaCode,
                    type = color.type,
                    foilManufacturer = color.foilManufacturer
                )
            ))
        } catch (e: Exception) {
             return Result.failure(e)
        }

        return try {
            api.addColor(color)
            auditLogDao.insert(AuditLogEntity(
                action = "ADD_COLOR",
                itemType = "CONFIG",
                details = "Dodano kolor: ${color.code} (${color.description})"
            ))
            Result.success(Unit)
        } catch (e: Exception) {
             val parsed = parseError(e)
             if (e !is HttpException || e.code() >= 500) {
                 try {
                     pendingOperationDao.insert(PendingOperationEntity(
                         type = OperationType.ADD_COLOR,
                         payloadJson = gson.toJson(color)
                     ))
                     Result.success(Unit)
                 } catch (localEx: Exception) {
                     Result.failure(parsed)
                 }
             } else {
                 Result.failure(parsed)
             }
        }
    }

    suspend fun updateColor(color: ColorDefinition): Result<Unit> {
        return try {
            if (color.id != null) {
                api.updateColor(color.id, color)
            }
            configDao.insertColors(listOf(
                ColorEntity(
                    code = color.code,
                    id = color.id ?: java.util.UUID.randomUUID().toString(),
                    description = color.description,
                    name = color.name,
                    paletteCode = color.paletteCode,
                    vekaCode = color.vekaCode,
                    type = color.type,
                    foilManufacturer = color.foilManufacturer
                )
            ))
            
            auditLogDao.insert(AuditLogEntity(
                action = "UPDATE_COLOR",
                itemType = "CONFIG",
                details = "Zaktualizowano kolor: ${color.code}"
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            val parsed = parseError(e)
            if (e is HttpException && e.code() in 400..499) {
                return Result.failure(parsed)
            }

             try {
                configDao.insertColors(listOf(
                    ColorEntity(
                        code = color.code,
                        id = color.id ?: java.util.UUID.randomUUID().toString(),
                        description = color.description,
                        name = color.name,
                        paletteCode = color.paletteCode,
                        vekaCode = color.vekaCode,
                        type = color.type,
                        foilManufacturer = color.foilManufacturer
                    )
                ))
                auditLogDao.insert(AuditLogEntity(
                    action = "UPDATE_COLOR_LOCAL",
                    itemType = "CONFIG",
                    details = "Zaktualizowano kolor (offline): ${color.code}"
                ))
                Result.success(Unit)
            } catch (localEx: Exception) {
                Result.failure(parsed)
            }
        }
    }

    suspend fun addColor(code: String, description: String): Result<Unit> {
        return addColor(ColorDefinition(code = code, description = description))
    }

    suspend fun deleteColor(id: String): Result<Unit> {
        return try {
            api.deleteColor(id)
            refreshConfig()
            
            auditLogDao.insert(AuditLogEntity(
                action = "DELETE_COLOR",
                itemType = "CONFIG",
                details = "Usunięto kolor ID: $id"
            ))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Import/Export
    data class ConfigExportData(
        val profiles: List<ProfileEntity> = emptyList(),
        val colors: List<ColorEntity> = emptyList()
    )

    suspend fun exportConfig(): String {
        val profiles = configDao.getProfilesSync()
        val colors = configDao.getColorsSync()
        val exportData = ConfigExportData(profiles, colors)
        return gson.toJson(exportData)
    }

    suspend fun importConfig(json: String): Result<Unit> {
        return try {
            val exportData = gson.fromJson(json, ConfigExportData::class.java)
            
            if (!exportData.profiles.isNullOrEmpty()) {
                configDao.insertProfiles(exportData.profiles)
            }
            if (!exportData.colors.isNullOrEmpty()) {
                configDao.insertColors(exportData.colors)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.warehouse.features.muntins_v3.repository

import android.content.Context
import com.example.warehouse.features.muntins_v3.database.MuntinsV3Database
import com.example.warehouse.features.muntins_v3.database.dao.MuntinsV3Dao
import com.example.warehouse.features.muntins_v3.database.entity.*
import kotlinx.coroutines.flow.Flow

class MuntinsV3Repository(
    private val dao: MuntinsV3Dao
) {
    // Secondary constructor for convenience in ViewModels
    constructor(context: Context) : this(
        MuntinsV3Database.getDatabase(context).muntinsV3Dao()
    )

    // --- Projects ---
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? {
        return dao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity): Long {
        return dao.insertProject(project)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        dao.deleteProject(project)
    }

    // --- Layouts ---
    suspend fun getLayoutByProjectId(projectId: Long): LayoutEntity? {
        return dao.getLayoutByProjectId(projectId)
    }

    suspend fun saveLayout(layout: LayoutEntity) {
        dao.insertLayout(layout)
    }

    // --- Configuration (Profiles, Beads, Muntins) ---
    val allProfiles: Flow<List<ProfileEntity>> = dao.getAllProfiles()
    val allBeads: Flow<List<GlassBeadEntity>> = dao.getAllGlassBeads()
    val allMuntins: Flow<List<MuntinEntity>> = dao.getAllMuntins()
    
    suspend fun getProfileById(id: Long): ProfileEntity? = dao.getProfileById(id)
    suspend fun getGlassBeadById(id: Long): GlassBeadEntity? = dao.getGlassBeadById(id)
    suspend fun getMuntinById(id: Long): MuntinEntity? = dao.getMuntinById(id)
    
    // --- Maintenance ---
    suspend fun clearAll() {
        // Order matters due to FKs
        dao.clearLayouts()
        dao.clearProjects()
        dao.clearMuntins()
        dao.clearGlassBeads()
        dao.clearProfiles()
    }
    suspend fun saveProfile(profile: ProfileEntity) {
        dao.insertProfile(profile)
    }

    suspend fun saveGlassBead(bead: GlassBeadEntity) {
        dao.insertGlassBead(bead)
    }

    suspend fun saveMuntin(muntin: MuntinEntity) {
        dao.insertMuntin(muntin)
    }
}

package com.example.warehouse.features.muntins_v3.database.dao

import androidx.room.*
import com.example.warehouse.features.muntins_v3.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MuntinsV3Dao {
    // Projects
    @Query("SELECT * FROM v3_projects ORDER BY created_at DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM v3_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    // Layouts
    @Query("SELECT * FROM v3_layouts WHERE project_id = :projectId")
    suspend fun getLayoutByProjectId(projectId: Long): LayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: LayoutEntity)

    // Profiles
    @Query("SELECT * FROM v3_profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM v3_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    // Glass Beads
    @Query("SELECT * FROM v3_glass_beads")
    fun getAllGlassBeads(): Flow<List<GlassBeadEntity>>

    @Query("SELECT * FROM v3_glass_beads WHERE id = :id")
    suspend fun getGlassBeadById(id: Long): GlassBeadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlassBead(bead: GlassBeadEntity)

    // Muntins
    @Query("SELECT * FROM v3_muntins")
    fun getAllMuntins(): Flow<List<MuntinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuntin(muntin: MuntinEntity)
}

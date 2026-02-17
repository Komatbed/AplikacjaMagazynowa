package com.example.warehouse.data.api

import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.InventoryTakeRequest
import com.example.warehouse.data.model.InventoryTakeResponse
import com.example.warehouse.data.model.InventoryWasteRequest
import com.example.warehouse.data.model.LocationStatusDto
import com.example.warehouse.data.model.IssueReportRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

import com.example.warehouse.data.model.OptimizationRequest
import com.example.warehouse.data.model.WasteRecommendationResponse
import com.example.warehouse.data.model.ProfileDefinition
import com.example.warehouse.data.model.ColorDefinition
import com.example.warehouse.data.model.MuntinsV3Config
import com.example.warehouse.data.model.LoginRequest
import com.example.warehouse.data.model.AuthResponse
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.PUT

interface WarehouseApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("inventory/config")
    suspend fun getConfig(): Map<String, List<String>>

    @GET("config/warehouse")
    suspend fun getWarehouseConfig(): Map<String, Any>

    @GET("config/muntins-v3")
    suspend fun getMuntinsV3Config(): MuntinsV3Config

    // Config Management
    @GET("config/profiles")
    suspend fun getProfiles(): List<ProfileDefinition>

    @POST("config/profiles")
    suspend fun addProfile(@Body profile: ProfileDefinition): ProfileDefinition

    @PUT("config/profiles/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body profile: ProfileDefinition): ProfileDefinition

    @DELETE("config/profiles/{id}")
    suspend fun deleteProfile(@Path("id") id: String)

    @GET("config/colors")
    suspend fun getColors(): List<ColorDefinition>

    @POST("config/colors")
    suspend fun addColor(@Body color: ColorDefinition): ColorDefinition

    @PUT("config/colors/{id}")
    suspend fun updateColor(@Path("id") id: String, @Body color: ColorDefinition): ColorDefinition

    @DELETE("config/colors/{id}")
    suspend fun deleteColor(@Path("id") id: String)

    @GET("config/core-rules")
    suspend fun getCoreRules(): Map<String, String>

    @POST("config/reload-defaults")
    suspend fun reloadDefaults(): Map<String, String>

    @POST("optimization/calculate")
    suspend fun calculateOptimization(@Body request: OptimizationRequest): com.example.warehouse.data.model.CutPlanResponse

    @GET("inventory/items")
    suspend fun getItems(
        @Query("location") location: String? = null,
        @Query("profileCode") profileCode: String? = null,
        @Query("internalColor") internalColor: String? = null,
        @Query("externalColor") externalColor: String? = null,
        @Query("coreColor") coreColor: String? = null
    ): List<InventoryItemDto>

    @POST("inventory/take")
    suspend fun takeItem(@Body request: InventoryTakeRequest): InventoryTakeResponse

    @POST("inventory/waste")
    suspend fun registerWaste(@Body request: InventoryWasteRequest): InventoryItemDto

    @GET("locations/map")
    suspend fun getWarehouseMap(): List<LocationStatusDto>

    @PUT("locations/{id}/capacity")
    suspend fun updateLocationCapacity(@Path("id") id: Int, @Body capacity: Int): Any

    @POST("issues")
    suspend fun reportIssue(@Body request: IssueReportRequest): Any

    @PUT("inventory/items/{id}/length")
    suspend fun updateItemLength(@Path("id") id: String, @Body length: Int): InventoryItemDto
    
    @POST("inventory/items")
    suspend fun addItem(@Body item: InventoryItemDto): InventoryItemDto
    
    @DELETE("inventory/items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Any
    
    @GET("users")
    suspend fun getUsers(): List<com.example.warehouse.data.model.UserDto>
    
    @POST("users")
    suspend fun createUser(@Body request: com.example.warehouse.data.model.UserCreateRequest): com.example.warehouse.data.model.UserDto
    
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Any
    
    @PUT("users/{id}/role")
    suspend fun changeUserRole(@Path("id") id: String, @Body request: com.example.warehouse.data.model.RoleChangeRequest): com.example.warehouse.data.model.UserDto
    
    @POST("users/{id}/reset-password")
    suspend fun resetPassword(@Path("id") id: String, @Body request: com.example.warehouse.data.model.PasswordResetRequest): Any
    
    @PUT("users/me/password")
    suspend fun changeOwnPassword(@Body request: com.example.warehouse.data.model.PasswordResetRequest): Any
    
    @PUT("users/me/password-with-old")
    suspend fun changeOwnPasswordWithOld(@Body request: com.example.warehouse.data.model.ChangePasswordWithOldRequest): Any
    
    @GET("users/me/preferences")
    suspend fun getUserPreferences(): com.example.warehouse.data.model.UserPreferencesDto
    
    @PUT("users/me/preferences")
    suspend fun updateUserPreferences(@Body request: com.example.warehouse.data.model.UserPreferencesDto): com.example.warehouse.data.model.UserPreferencesDto
}

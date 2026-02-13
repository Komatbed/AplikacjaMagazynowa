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
}

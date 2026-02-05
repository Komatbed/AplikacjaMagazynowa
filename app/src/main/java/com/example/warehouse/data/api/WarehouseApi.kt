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
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.PUT

interface WarehouseApi {
    @GET("inventory/config")
    suspend fun getConfig(): Map<String, List<String>>

    // Config Management
    @GET("config/profiles")
    suspend fun getProfiles(): List<ProfileDefinition>

    @POST("config/profiles")
    suspend fun addProfile(@Body profile: ProfileDefinition): ProfileDefinition

    @DELETE("config/profiles/{id}")
    suspend fun deleteProfile(@Path("id") id: String)

    @GET("config/colors")
    suspend fun getColors(): List<ColorDefinition>

    @POST("config/colors")
    suspend fun addColor(@Body color: ColorDefinition): ColorDefinition

    @DELETE("config/colors/{id}")
    suspend fun deleteColor(@Path("id") id: String)

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

    @POST("issues")
    suspend fun reportIssue(@Body request: IssueReportRequest): Any

    @PUT("inventory/items/{id}/length")
    suspend fun updateItemLength(@Path("id") id: String, @Body length: Int): InventoryItemDto
}

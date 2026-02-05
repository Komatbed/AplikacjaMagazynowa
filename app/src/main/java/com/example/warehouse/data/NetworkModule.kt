package com.example.warehouse.data

import com.example.warehouse.data.api.WarehouseApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // Default to local PC IP
    private var currentUrl = "http://192.168.1.101:8080/api/v1/"
    
    private var retrofit: Retrofit = createRetrofit(currentUrl)

    @Volatile
    var api: WarehouseApi = retrofit.create(WarehouseApi::class.java)
        private set

    fun updateUrl(newUrl: String) {
        if (newUrl.isBlank()) return
        val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (formattedUrl == currentUrl) return
        
        currentUrl = formattedUrl
        retrofit = createRetrofit(formattedUrl)
        api = retrofit.create(WarehouseApi::class.java)
    }

    private fun createRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

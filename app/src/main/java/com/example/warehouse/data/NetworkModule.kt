package com.example.warehouse.data

import com.example.warehouse.data.api.WarehouseApi
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // Default to VPS IP
    private var currentUrl = "http://51.77.59.105:8080/api/v1/"
    
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
        // Security: Certificate Pinning (Wymaga HTTPS)
        // TODO: Wprowadź prawdziwy hash SHA-256 certyfikatu serwera produkcyjnego
        val certificatePinner = CertificatePinner.Builder()
            // .add("51.77.59.105", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        val client = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

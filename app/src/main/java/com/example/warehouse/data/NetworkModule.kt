package com.example.warehouse.data

import com.example.warehouse.data.api.WarehouseApi
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    // Default to VPS IP (Port 443 via Nginx/HTTPS)
    private var currentUrl = "https://51.77.59.105/api/v1/"
    
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

    // TODO: W produkcji użyć Certificate Pinning z prawdziwym SHA-256
    // Obecnie używamy Self-Signed Certificate dla adresu IP 51.77.59.105
    private fun createRetrofit(url: String): Retrofit {
        // Handle Self-Signed Certificates (Explicit Trust for VPS IP)
        // Since we use IP address with self-signed cert, we must trust it.
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })

        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

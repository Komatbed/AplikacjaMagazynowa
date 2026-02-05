package com.example.warehouse

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.warehouse.data.local.SettingsDataStore

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val error = intent.getStringExtra("error") ?: "Unknown error"
        android.util.Log.e("WAREHOUSE_CRASH", "CrashActivity Displaying Error:\n$error")

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)

        val title = TextView(this)
        title.text = "CRITICAL ERROR"
        title.textSize = 24f
        title.setTextColor(android.graphics.Color.RED)
        layout.addView(title)
        
        // Add Send Button
        val sendButton = Button(this)
        sendButton.text = "Wyślij log na Email"
        sendButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 32
            bottomMargin = 32
        }
        sendButton.setOnClickListener {
            shareErrorLog(error)
        }
        layout.addView(sendButton)

        val message = TextView(this)
        message.text = "The application crashed unexpectedly.\n\n$error"
        message.textSize = 14f
        // Enable text selection
        message.setTextIsSelectable(true)
        layout.addView(message)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun shareErrorLog(error: String) {
        // Send to Backend
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val settings = SettingsDataStore(applicationContext)
                val baseUrl = settings.apiUrl.first()
                // Ensure baseUrl ends with /
                val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                val url = java.net.URL("${safeBaseUrl}logs/crash")
                
                with(url.openConnection() as java.net.HttpURLConnection) {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5000
                    readTimeout = 5000
                    
                    val json = """
                        {
                            "error": "${error.replace("\"", "\\\"").replace("\n", "\\n")}",
                            "device": "${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                        }
                    """.trimIndent()
                    
                    outputStream.write(json.toByteArray())
                    outputStream.flush()
                    
                    if (responseCode == 200) {
                        runOnUiThread {
                            android.widget.Toast.makeText(this@CrashActivity, "Log wysłany do serwera ($safeBaseUrl)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    android.widget.Toast.makeText(this@CrashActivity, "Nie udało się wysłać logu: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Warehouse App Crash Log")
            putExtra(Intent.EXTRA_TEXT, error)
        }
        val shareIntent = Intent.createChooser(sendIntent, "Wyślij log przez...")
        startActivity(shareIntent)
    }
}

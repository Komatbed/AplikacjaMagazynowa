package com.example.warehouse

import android.app.Application
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

class WarehouseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the exception immediately to Logcat so we can see it in live debugging
            android.util.Log.e("WAREHOUSE_CRASH", "FATAL EXCEPTION in thread ${thread.name}", throwable)

            try {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("error", throwable.stackTraceToString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                
                // Allow some time for the intent to be sent before killing
                Thread.sleep(500)
                
                // Kill the process after starting the activity
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

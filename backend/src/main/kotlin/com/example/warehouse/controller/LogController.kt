package com.example.warehouse.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/logs")
class LogController {

    private val logger = LoggerFactory.getLogger(LogController::class.java)
    private val logFile = File("logs/mobile_crash.log")

    init {
        logFile.parentFile.mkdirs()
    }

    @PostMapping("/crash")
    fun reportCrash(@RequestBody crashData: Map<String, String>) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val error = crashData["error"] ?: "Unknown error"
        val device = crashData["device"] ?: "Unknown device"
        
        val logEntry = """
            
            [$timestamp] DEVICE: $device
            --------------------------------------------------
            $error
            --------------------------------------------------
        """.trimIndent()

        // Write to file
        try {
            PrintWriter(FileWriter(logFile, true)).use { writer ->
                writer.println(logEntry)
            }
            logger.error("Mobile Crash Reported: $error")
        } catch (e: Exception) {
            logger.error("Failed to write crash log", e)
        }
    }
}

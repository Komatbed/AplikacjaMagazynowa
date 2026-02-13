package com.example.warehouse.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.File

@Service
class CoreColorService(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(CoreColorService::class.java)
    private var coreColorMap: Map<String, String> = emptyMap()

    @PostConstruct
    fun init() {
        reload()
    }

    fun reload() {
        try {
            // Try FileSystem first
            val file = File("src/main/resources/core_color_map.json")
            if (file.exists()) {
                coreColorMap = objectMapper.readValue(
                    file,
                    object : TypeReference<Map<String, String>>() {}
                )
                logger.info("Reloaded core_color_map.json from file system with ${coreColorMap.size} entries.")
                return
            }

            // Fallback to Classpath
            val resource = ClassPathResource("core_color_map.json")
            if (resource.exists()) {
                coreColorMap = objectMapper.readValue(
                    resource.inputStream,
                    object : TypeReference<Map<String, String>>() {}
                )
                logger.info("Reloaded core_color_map.json from classpath with ${coreColorMap.size} entries.")
            } else {
                logger.warn("core_color_map.json not found!")
            }
        } catch (e: IOException) {
            logger.error("Failed to reload core_color_map.json", e)
        }
    }

    fun getCoreColor(extColorCode: String, intColorCode: String): String {
        // Normalization
        val ext = extColorCode.lowercase()
        val int = intColorCode.lowercase()
        
        // Rule 1: If any side is white, core is white
        // Adjust "white" check based on your actual codes (e.g. "9016", "white", "weiss")
        if (isWhite(ext) || isWhite(int)) {
            return "white"
        }

        // Rule 2: Lookup external color in map
        return coreColorMap[ext] ?: "white" // Default to white if unknown mapping
    }

    private fun isWhite(code: String): Boolean {
        return code.contains("white") || code.contains("biały") || code.contains("9016")
    }
    
    fun getMapping(): Map<String, String> {
        return coreColorMap
    }
}

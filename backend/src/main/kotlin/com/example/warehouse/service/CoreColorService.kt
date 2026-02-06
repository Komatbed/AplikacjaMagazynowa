package com.example.warehouse.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class CoreColorService(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(CoreColorService::class.java)
    private var coreColorMap: Map<String, String> = emptyMap()

    @PostConstruct
    fun init() {
        try {
            val resource = ClassPathResource("core_color_map.json")
            if (resource.exists()) {
                coreColorMap = objectMapper.readValue(
                    resource.inputStream,
                    object : TypeReference<Map<String, String>>() {}
                )
                logger.info("Loaded core_color_map.json with ${coreColorMap.size} entries.")
            } else {
                logger.warn("core_color_map.json not found!")
            }
        } catch (e: IOException) {
            logger.error("Failed to load core_color_map.json", e)
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

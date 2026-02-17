package com.example.warehouse.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.File

@Component
class WarehouseConfig(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(WarehouseConfig::class.java)

    var lowStockThreshold: Int = 5
        
    var defaultPalletCapacity: Int = 50
        
    var reserveWasteLengths: List<Int> = emptyList()

    var customMultiCoreColors: List<String> = emptyList()

    var ral9001EligibleColors: List<String> = emptyList()

    @PostConstruct
    fun init() {
        reload()
    }

    fun reload() {
        try {
            // Try FileSystem first
            val file = File("src/main/resources/warehouse_config.json")
            if (file.exists()) {
                loadFromDto(objectMapper.readValue(file, WarehouseConfigDto::class.java))
                logger.info("Reloaded warehouse_config.json from file system")
                return
            }

            // Fallback to Classpath
            val resource = ClassPathResource("warehouse_config.json")
            if (resource.exists()) {
                loadFromDto(objectMapper.readValue(resource.inputStream, WarehouseConfigDto::class.java))
                logger.info("Reloaded warehouse_config.json from classpath")
            } else {
                logger.warn("warehouse_config.json not found, using defaults.")
            }
        } catch (e: Exception) {
            logger.error("Failed to reload warehouse_config.json", e)
        }
    }

    private fun loadFromDto(config: WarehouseConfigDto) {
        this.lowStockThreshold = config.lowStockThreshold
        this.defaultPalletCapacity = config.defaultPalletCapacity
        this.reserveWasteLengths = config.reserveWasteLengths
        this.customMultiCoreColors = config.customMultiCoreColors
        this.ral9001EligibleColors = config.ral9001EligibleColors.ifEmpty { config.customMultiCoreColors }
    }

    private data class WarehouseConfigDto(
        @JsonProperty("lowStockThreshold") val lowStockThreshold: Int = 5,
        @JsonProperty("defaultPalletCapacity") val defaultPalletCapacity: Int = 50,
        @JsonProperty("reserveWasteLengths") val reserveWasteLengths: List<Int> = emptyList(),
        @JsonProperty("customMultiCoreColors") val customMultiCoreColors: List<String> = emptyList(),
        @JsonProperty("ral9001EligibleColors") val ral9001EligibleColors: List<String> = emptyList()
    )
}

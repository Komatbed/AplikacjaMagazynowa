package com.example.warehouse.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class WarehouseConfig(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(WarehouseConfig::class.java)

    var lowStockThreshold: Int = 5
        
    var defaultPalletCapacity: Int = 50
        
    var reserveWasteLengths: List<Int> = emptyList()

    var customMultiCoreColors: List<String> = emptyList()

    @PostConstruct
    fun init() {
        try {
            val resource = ClassPathResource("warehouse_config.json")
            if (resource.exists()) {
                val config = objectMapper.readValue(resource.inputStream, WarehouseConfigDto::class.java)
                this.lowStockThreshold = config.lowStockThreshold
                this.defaultPalletCapacity = config.defaultPalletCapacity
                this.reserveWasteLengths = config.reserveWasteLengths
                this.customMultiCoreColors = config.customMultiCoreColors
                logger.info("Loaded warehouse_config.json: $config")
            } else {
                logger.warn("warehouse_config.json not found, using defaults.")
            }
        } catch (e: Exception) {
            logger.error("Failed to load warehouse_config.json", e)
        }
    }

    private data class WarehouseConfigDto(
        @JsonProperty("lowStockThreshold") val lowStockThreshold: Int = 5,
        @JsonProperty("defaultPalletCapacity") val defaultPalletCapacity: Int = 50,
        @JsonProperty("reserveWasteLengths") val reserveWasteLengths: List<Int> = emptyList(),
        @JsonProperty("customMultiCoreColors") val customMultiCoreColors: List<String> = emptyList()
    )
}
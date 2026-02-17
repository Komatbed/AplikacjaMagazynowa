package com.example.warehouse.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.File

@Component
class MuntinsV3Config(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(MuntinsV3Config::class.java)

    var profiles: List<ProfileConfig> = emptyList()
    var beads: List<BeadConfig> = emptyList()
    var muntins: List<MuntinConfig> = emptyList()

    @PostConstruct
    fun init() {
        reload()
    }

    fun reload() {
        try {
            val file = File("src/main/resources/initial_data/muntins_v3.json")
            if (file.exists()) {
                loadFromDto(objectMapper.readValue(file, MuntinsV3ConfigDto::class.java))
                logger.info("Reloaded muntins_v3.json from file system")
                return
            }

            val resource = ClassPathResource("initial_data/muntins_v3.json")
            if (resource.exists()) {
                loadFromDto(objectMapper.readValue(resource.inputStream, MuntinsV3ConfigDto::class.java))
                logger.info("Reloaded muntins_v3.json from classpath")
            } else {
                logger.warn("muntins_v3.json not found, using empty config.")
            }
        } catch (e: Exception) {
            logger.error("Failed to reload muntins_v3.json", e)
        }
    }

    private fun loadFromDto(dto: MuntinsV3ConfigDto) {
        profiles = dto.profiles
        beads = dto.beads
        muntins = dto.muntins
    }

    data class ProfileConfig(
        @JsonProperty("name") val name: String = "",
        @JsonProperty("glassOffsetX") val glassOffsetX: Double = 0.0,
        @JsonProperty("glassOffsetY") val glassOffsetY: Double = 0.0,
        @JsonProperty("outerConstructionAngleDeg") val outerConstructionAngleDeg: Double = 90.0
    )

    data class BeadConfig(
        @JsonProperty("name") val name: String = "",
        @JsonProperty("angleFace") val angleFace: Double = 0.0,
        @JsonProperty("effectiveGlassOffset") val effectiveGlassOffset: Double = 0.0
    )

    data class MuntinConfig(
        @JsonProperty("name") val name: String = "",
        @JsonProperty("width") val width: Double = 0.0,
        @JsonProperty("thickness") val thickness: Double = 0.0,
        @JsonProperty("wallAngleDeg") val wallAngleDeg: Double = 90.0
    )

    private data class MuntinsV3ConfigDto(
        @JsonProperty("profiles") val profiles: List<ProfileConfig> = emptyList(),
        @JsonProperty("beads") val beads: List<BeadConfig> = emptyList(),
        @JsonProperty("muntins") val muntins: List<MuntinConfig> = emptyList()
    )
}


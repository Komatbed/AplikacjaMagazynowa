package com.example.warehouse.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.File

@Component
class PalletConfig(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(PalletConfig::class.java)

    private var palletsByLabel: Map<String, PalletDefinition> = emptyMap()

    @PostConstruct
    fun init() {
        reload()
    }

    fun reload() {
        try {
            val file = File("src/main/resources/pallet_config.json")
            if (file.exists()) {
                val dto = objectMapper.readValue(file, PalletConfigDto::class.java)
                loadFromDto(dto)
                logger.info("Reloaded pallet_config.json from file system with ${palletsByLabel.size} pallets.")
                return
            }

            val resource = ClassPathResource("pallet_config.json")
            if (resource.exists()) {
                val dto = objectMapper.readValue(resource.inputStream, PalletConfigDto::class.java)
                loadFromDto(dto)
                logger.info("Reloaded pallet_config.json from classpath with ${palletsByLabel.size} pallets.")
            } else {
                logger.warn("pallet_config.json not found, pallet metadata will not be applied.")
            }
        } catch (e: Exception) {
            logger.error("Failed to reload pallet_config.json", e)
        }
    }

    private fun loadFromDto(dto: PalletConfigDto) {
        palletsByLabel = dto.pallets.associateBy { it.label }
    }

    fun getPallet(label: String): PalletDefinition? {
        return palletsByLabel[label]
    }

    fun getAllPallets(): List<PalletDefinition> {
        return palletsByLabel.values.sortedBy { it.label }
    }

    fun suggestPalletLabel(
        profileCode: String,
        internalColor: String?,
        externalColor: String?,
        coreColor: String?,
        isWaste: Boolean
    ): String? {
        val pallets = getAllPallets()
        if (pallets.isEmpty()) {
            return null
        }

        val candidates = pallets.filter { def ->
            val type = def.details?.type
            if (type == null) {
                true
            } else {
                if (isWaste) {
                    type == "WASTE" || type == "MIXED"
                } else {
                    type == "FULL_BARS" || type == "MIXED"
                }
            }
        }

        val effectiveCandidates = if (candidates.isNotEmpty()) candidates else pallets

        var best: PalletDefinition? = null
        var bestScore = -1

        val candidateColors = listOfNotNull(coreColor, internalColor, externalColor)

        for (pallet in effectiveCandidates) {
            var score = 0
            for (group in pallet.groups) {
                if (group.profileCodes.contains(profileCode)) {
                    score += 10
                }
                if (group.colorCodes.isNotEmpty() && candidateColors.any { group.colorCodes.contains(it) }) {
                    score += 5
                }
            }

            if (score > bestScore) {
                bestScore = score
                best = pallet
            } else if (score == bestScore && best != null) {
                val bestRow = best.details?.row ?: Int.MAX_VALUE
                val currentRow = pallet.details?.row ?: Int.MAX_VALUE
                if (currentRow < bestRow) {
                    best = pallet
                } else if (currentRow == bestRow) {
                    val bestZone = best.details?.zone ?: "Z"
                    val currentZone = pallet.details?.zone ?: "Z"
                    if (currentZone < bestZone) {
                        best = pallet
                    } else if (currentZone == bestZone && pallet.label < best.label) {
                        best = pallet
                    }
                }
            }
        }

        return best?.label
    }

    data class PalletConfigDto(
        @JsonProperty("pallets") val pallets: List<PalletDefinition> = emptyList()
    )

    data class PalletDetails(
        @JsonProperty("zone") val zone: String? = null,
        @JsonProperty("row") val row: Int? = null,
        @JsonProperty("type") val type: String? = null
    )

    data class PalletGroup(
        @JsonProperty("name") val name: String = "",
        @JsonProperty("profileCodes") val profileCodes: List<String> = emptyList(),
        @JsonProperty("colorCodes") val colorCodes: List<String> = emptyList()
    )

    data class PalletDefinition(
        @JsonProperty("label") val label: String,
        @JsonProperty("displayName") val displayName: String? = null,
        @JsonProperty("description") val description: String? = null,
        @JsonProperty("capacity") val capacity: Int? = null,
        @JsonProperty("overflowThresholdPercent") val overflowThresholdPercent: Int? = null,
        @JsonProperty("fillAnimation") val fillAnimation: String? = null,
        @JsonProperty("details") val details: PalletDetails? = null,
        @JsonProperty("groups") val groups: List<PalletGroup> = emptyList()
    )
}

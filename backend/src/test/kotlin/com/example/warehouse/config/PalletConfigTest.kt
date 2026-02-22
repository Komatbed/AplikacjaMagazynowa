package com.example.warehouse.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PalletConfigTest {

    @Test
    fun `loads pallet_config json and exposes pallet types`() {
        val mapper = jacksonObjectMapper()
        val config = PalletConfig(mapper)

        config.reload()

        val all = config.getAllPallets()
        assert(all.isNotEmpty())

        val first = config.getPallet("01A")
        assertNotNull(first)
        assertEquals("MIXED", first?.details?.type)

        val waste = config.getPallet("01C")
        assertNotNull(waste)
        assertEquals("MIXED", waste?.details?.type)
    }

    @Test
    fun `suggests pallet based on profile code`() {
        val mapper = jacksonObjectMapper()
        val config = PalletConfig(mapper)

        config.reload()

        val suggested = config.suggestPalletLabel(
            profileCode = "101350",
            internalColor = null,
            externalColor = null,
            coreColor = null,
            isWaste = false
        )

        assertEquals("01A", suggested)
    }
}


package com.example.warehouse.controller

import com.example.warehouse.model.ColorDefinition
import com.example.warehouse.model.ProfileDefinition
import com.example.warehouse.repository.ColorDefinitionRepository
import com.example.warehouse.repository.ProfileDefinitionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@WithMockUser(username = "testuser", roles = ["USER", "ADMIN"])
class ConfigurationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var profileRepository: ProfileDefinitionRepository

    @Autowired
    private lateinit var colorRepository: ColorDefinitionRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        profileRepository.deleteAll()
        colorRepository.deleteAll()
    }

    @Test
    fun `should return pallet summary grouped by type`() {
        val result = mockMvc.perform(get("/api/v1/config/pallet-summary"))
            .andExpect(status().isOk)
            .andReturn()

        val json = result.response.contentAsString
        val node = objectMapper.readTree(json)
        val counts = node.get("counts")

        val fieldNames = counts.fieldNames().asSequence().toList()
        assertTrue(fieldNames.isNotEmpty())
        fieldNames.forEach { type ->
            val count = counts.get(type).asInt()
            assertTrue(count > 0)
        }
    }

    // --- PROFILE TESTS ---

    @Test
    fun `should create new profile`() {
        val profile = ProfileDefinition(
            code = "P001",
            description = "Test Profile",
            system = "Test System",
            manufacturer = "Test Mfg"
        )

        mockMvc.perform(post("/api/v1/config/profiles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(profile)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("P001"))
            .andExpect(jsonPath("$.id").exists())

        assertEquals(1, profileRepository.count())
    }

    @Test
    fun `should return 400 when creating profile with blank code`() {
        val profile = ProfileDefinition(code = "", description = "Invalid")

        mockMvc.perform(post("/api/v1/config/profiles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(profile)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 409 when creating duplicate profile`() {
        val profile = ProfileDefinition(code = "P001", description = "Original")
        profileRepository.save(profile)

        val duplicate = ProfileDefinition(code = "P001", description = "Duplicate")

        mockMvc.perform(post("/api/v1/config/profiles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicate)))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `should update existing profile`() {
        val profile = profileRepository.save(ProfileDefinition(code = "P001", description = "Original"))
        val update = profile.copy(description = "Updated")

        mockMvc.perform(put("/api/v1/config/profiles/${profile.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("Updated"))

        val fetched = profileRepository.findById(profile.id).get()
        assertEquals("Updated", fetched.description)
    }

    @Test
    fun `should reload defaults from json files`() {
        assertEquals(0, profileRepository.count())

        mockMvc.perform(post("/api/v1/config/reload-defaults"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.profiles").exists())
            .andExpect(jsonPath("$.beans").exists())
    }

    @Test
    fun `should delete profile`() {
        val profile = profileRepository.save(ProfileDefinition(code = "P001"))

        mockMvc.perform(delete("/api/v1/config/profiles/${profile.id}"))
            .andExpect(status().isOk)

        assertTrue(profileRepository.findById(profile.id).isEmpty)
    }

    // --- COLOR TESTS ---

    @Test
    fun `should create new color`() {
        val color = ColorDefinition(
            code = "C001",
            description = "Test Color",
            name = "White"
        )

        mockMvc.perform(post("/api/v1/config/colors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(color)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("C001"))
            .andExpect(jsonPath("$.id").exists())

        assertEquals(1, colorRepository.count())
    }

    @Test
    fun `should return 400 when creating color with blank code`() {
        val color = ColorDefinition(code = "", description = "Invalid")

        mockMvc.perform(post("/api/v1/config/colors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(color)))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 409 when creating duplicate color`() {
        val color = ColorDefinition(code = "C001", description = "Original")
        colorRepository.save(color)

        val duplicate = ColorDefinition(code = "C001", description = "Duplicate")

        mockMvc.perform(post("/api/v1/config/colors")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicate)))
            .andExpect(status().isConflict)
    }

    @Test
    fun `should update existing color`() {
        val color = colorRepository.save(ColorDefinition(code = "C001", name = "Original"))
        val update = color.copy(name = "Updated")

        mockMvc.perform(put("/api/v1/config/colors/${color.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated"))

        val fetched = colorRepository.findById(color.id).get()
        assertEquals("Updated", fetched.name)
    }

    @Test
    fun `should delete color`() {
        val color = colorRepository.save(ColorDefinition(code = "C001"))

        mockMvc.perform(delete("/api/v1/config/colors/${color.id}"))
            .andExpect(status().isOk)

        assertTrue(colorRepository.findById(color.id).isEmpty)
    }
}

package com.example.warehouse

import com.example.warehouse.controller.QualityController
import com.example.warehouse.controller.ShortageController
import com.example.warehouse.dto.InventoryReceiptRequest
import com.example.warehouse.dto.InventoryTakeRequest
import com.example.warehouse.dto.InventoryWasteRequest
import com.example.warehouse.model.*
import com.example.warehouse.repository.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FunctionalTests {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var inventoryItemRepository: InventoryItemRepository
    @Autowired private lateinit var issueReportRepository: IssueReportRepository
    @Autowired private lateinit var shortageRepository: ShortageRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var messageRepository: MessageRepository
    @Autowired private lateinit var locationRepository: LocationRepository
    @Autowired private lateinit var operationLogRepository: OperationLogRepository

    private val mapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        operationLogRepository.deleteAll()
        inventoryItemRepository.deleteAll()
        issueReportRepository.deleteAll()
        shortageRepository.deleteAll()
        messageRepository.deleteAll()
        userRepository.deleteAll()
        locationRepository.deleteAll()
    }

    @Test
    @WithMockUser(username = "warehouse_worker", roles = ["WAREHOUSE"])
    fun `Inventory Flow - Receipt and History`() {
        // 1. Create Location
        locationRepository.save(Location(rowNumber = 1, paletteNumber = 1, label = "A-01-01"))

        // 2. Register Receipt
        val receiptRequest = InventoryReceiptRequest(
            profileCode = "P-123",
            lengthMm = 6000,
            quantity = 100,
            locationLabel = "A-01-01",
            internalColor = "White",
            externalColor = "Anthracite"
        )

        mockMvc.perform(post("/api/v1/inventory/receipt")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(receiptRequest)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.profileCode").value("P-123"))

        // 3. Verify Item Created
        val items = inventoryItemRepository.findAll()
        assertEquals(1, items.size)
        assertEquals(100, items[0].quantity)

        // 4. Check History (Logs)
        mockMvc.perform(get("/api/v1/inventory/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].operationType").value("RECEIPT"))
    }

    @Test
    @WithMockUser(username = "warehouse_worker", roles = ["WAREHOUSE"])
    fun `Inventory Flow - Issue reduces quantity and logs history`() {
        val location = locationRepository.save(Location(rowNumber = 1, paletteNumber = 2, label = "A-01-02"))

        inventoryItemRepository.save(
            InventoryItem(
                location = location,
                profileCode = "P-456",
                lengthMm = 6000,
                quantity = 100,
                internalColor = "White",
                externalColor = "Anthracite"
            )
        )

        val takeRequest = InventoryTakeRequest(
            locationLabel = "A-01-02",
            profileCode = "P-456",
            lengthMm = 6000,
            quantity = 40,
            reason = "PRODUCTION",
            force = true
        )

        mockMvc.perform(
            post("/api/v1/inventory/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(takeRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.newQuantity").value(60))

        val itemsAfter = inventoryItemRepository.findAll()
        assertEquals(1, itemsAfter.size)
        assertEquals(60, itemsAfter[0].quantity)

        mockMvc.perform(get("/api/v1/inventory/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].operationType").value("ISSUE"))
    }

    @Test
    @WithMockUser(username = "warehouse_worker", roles = ["WAREHOUSE"])
    fun `Inventory Flow - Waste merges similar items`() {
        val location = locationRepository.save(Location(rowNumber = 1, paletteNumber = 3, label = "A-02-01"))

        inventoryItemRepository.save(
            InventoryItem(
                location = location,
                profileCode = "P-WASTE",
                lengthMm = 500,
                quantity = 10,
                internalColor = "White",
                externalColor = "Anthracite",
                coreColor = null
            )
        )

        val wasteRequest = InventoryWasteRequest(
            sourceProfileId = null,
            lengthMm = 500,
            locationLabel = "A-02-01",
            quantity = 5,
            profileCode = "P-WASTE",
            internalColor = "White",
            externalColor = "Anthracite",
            coreColor = null
        )

        mockMvc.perform(
            post("/api/v1/inventory/waste")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(wasteRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.quantity").value(15))

        val itemsAfter = inventoryItemRepository.findAll()
        assertEquals(1, itemsAfter.size)
        assertEquals(15, itemsAfter[0].quantity)
    }

    @Test
    @WithMockUser(username = "warehouse_worker", roles = ["WAREHOUSE"])
    fun `Inventory Flow - Delete is idempotent`() {
        val location = locationRepository.save(Location(rowNumber = 1, paletteNumber = 4, label = "A-03-01"))

        val item = inventoryItemRepository.save(
            InventoryItem(
                location = location,
                profileCode = "P-DEL",
                lengthMm = 6000,
                quantity = 20,
                internalColor = "White",
                externalColor = "Anthracite"
            )
        )

        mockMvc.perform(
            delete("/api/v1/inventory/items/${item.id}")
        )

        mockMvc.perform(
            delete("/api/v1/inventory/items/${item.id}")
        )

        val itemsAfter = inventoryItemRepository.findAll()
        assertEquals(0, itemsAfter.size)
    }

    @Test
    @WithMockUser(username = "qc_inspector", roles = ["QUALITY"])
    fun `Quality Flow - Manage Claims`() {
        // 1. Create initial claim
        val claim = issueReportRepository.save(IssueReport(
            orderNumber = "ORD-001",
            deliveryDate = java.time.LocalDate.now(),
            description = "Damaged",
            partNumber = "P-123",
            quantity = 5,
            status = IssueStatus.NEW
        ))

        // 2. Get Claims
        mockMvc.perform(get("/api/v1/quality/claims"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].orderNumber").value("ORD-001"))

        // 3. Update Decision
        val decision = QualityController.DecisionRequest("RESOLVED", "Refunded")
        mockMvc.perform(post("/api/v1/quality/claims/${claim.id}/decision")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(decision)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.decisionNote").value("Refunded"))
    }

    @Test
    @WithMockUser(username = "worker1", roles = ["WORKER"])
    fun `Shortage Flow - Report Shortage`() {
        // 1. Setup User
        val user = userRepository.save(User(
            login = "worker1", 
            passwordHash = "pass", 
            fullName = "Worker One", 
            role = Role.PRACOWNIK
        ))

        // 2. Report Shortage
        val shortageReq = ShortageController.ShortageRequest("Screw M5", "Out of stock", "HIGH")
        mockMvc.perform(post("/api/v1/shortages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(shortageReq)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.itemName").value("Screw M5"))

        // 3. Verify List
        mockMvc.perform(get("/api/v1/shortages"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].priority").value("HIGH"))
    }

    @Test
    @WithMockUser(username = "manager", roles = ["MANAGER"])
    fun `Dashboard Flow - Stats`() {
        // 1. Seed Data
        val loc = locationRepository.save(Location(rowNumber = 1, paletteNumber = 1, label = "X-00"))
        inventoryItemRepository.save(InventoryItem(
            location = loc, 
            profileCode = "A", 
            lengthMm = 1000, 
            quantity = 5
        )) // Low stock
        
        shortageRepository.save(Shortage(
            reportedById = null, 
            itemName = "Test", 
            description = "Desc",
            priority = "LOW"
        ))

        // 2. Get Stats
        mockMvc.perform(get("/api/v1/dashboard/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.lowStockCount").value(1))
            .andExpect(jsonPath("$.activeShortages").value(1))
    }
}

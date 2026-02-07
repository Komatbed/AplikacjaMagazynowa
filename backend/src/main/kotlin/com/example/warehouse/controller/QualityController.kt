package com.example.warehouse.controller

import com.example.warehouse.model.IssueReport
import com.example.warehouse.model.IssueStatus
import com.example.warehouse.repository.IssueReportRepository
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/quality")
class QualityController(
    private val issueReportRepository: IssueReportRepository
) {

    // api.js: getClaims() -> /quality/claims
    @GetMapping("/claims")
    fun getAllClaims(): List<IssueReport> {
        return issueReportRepository.findAll()
    }

    // api.js: updateClaimDecision(id, decisionData) -> POST /quality/claims/${id}/decision
    @PostMapping("/claims/{id}/decision")
    fun updateClaimDecision(@PathVariable id: UUID, @RequestBody decisionData: DecisionRequest): IssueReport {
        val claim = issueReportRepository.findById(id).orElseThrow { RuntimeException("Claim not found") }
        
        try {
            claim.status = IssueStatus.valueOf(decisionData.status)
        } catch (e: IllegalArgumentException) {
            // Keep old status or throw error? Let's default to current or log
            throw RuntimeException("Invalid status: ${decisionData.status}")
        }
        
        claim.decisionNote = decisionData.note
        claim.updatedAt = java.time.LocalDateTime.now()
        
        return issueReportRepository.save(claim)
    }

    data class DecisionRequest(val status: String, val note: String?)
}

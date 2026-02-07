package com.example.warehouse.controller

import com.example.warehouse.dto.IssueReportRequest
import com.example.warehouse.model.IssueReport
import com.example.warehouse.model.IssueStatus
import com.example.warehouse.repository.IssueReportRepository
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/issues")
class IssueReportController(
    private val repository: IssueReportRepository
) {

    @PostMapping
    fun createIssue(@RequestBody request: IssueReportRequest): IssueReport {
        val issue = IssueReport(
            description = request.description,
            partNumber = request.profileCode, // Mapping profileCode to partNumber
            // locationLabel is not directly stored in IssueReport entity currently, adding it to description or ignoring
            decisionNote = if (request.locationLabel != null) "Location: ${request.locationLabel}" else null,
            status = IssueStatus.NEW,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return repository.save(issue)
    }

    @GetMapping
    fun getAllIssues(): List<IssueReport> {
        return repository.findAll()
    }
}

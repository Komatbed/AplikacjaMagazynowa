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

    private val logFile = java.io.File("issues.log")

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
        
        // Write to file
        try {
            logFile.appendText("DATE: ${issue.createdAt} | DESC: ${issue.description} | PART: ${issue.partNumber} | LOC: ${request.locationLabel}\n")
        } catch (e: Exception) {
            println("Failed to write to issue log: ${e.message}")
        }

        return repository.save(issue)
    }

    @GetMapping
    fun getAllIssues(): List<IssueReport> {
        return repository.findAll()
    }

    @GetMapping("/logs")
    fun getIssueLogs(): String {
        return if (logFile.exists()) {
            logFile.readText()
        } else {
            "No logs found."
        }
    }
}

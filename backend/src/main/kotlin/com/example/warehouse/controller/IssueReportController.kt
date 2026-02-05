package com.example.warehouse.controller

import com.example.warehouse.model.IssueReport
import com.example.warehouse.repository.IssueReportRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/issues")
class IssueReportController(
    private val repository: IssueReportRepository
) {

    @PostMapping
    fun createIssue(@RequestBody issue: IssueReport): IssueReport {
        return repository.save(issue)
    }

    @GetMapping
    fun getAllIssues(): List<IssueReport> {
        return repository.findAll()
    }
}

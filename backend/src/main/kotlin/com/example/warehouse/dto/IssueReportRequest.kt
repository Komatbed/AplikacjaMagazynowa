package com.example.warehouse.dto

data class IssueReportRequest(
    val description: String,
    val profileCode: String? = null,
    val locationLabel: String? = null
)

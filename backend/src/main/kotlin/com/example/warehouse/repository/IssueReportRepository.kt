package com.example.warehouse.repository

import com.example.warehouse.model.IssueReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IssueReportRepository : JpaRepository<IssueReport, Long>

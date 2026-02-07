package com.example.warehouse.repository

import com.example.warehouse.model.IssueReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface IssueReportRepository : JpaRepository<IssueReport, UUID>

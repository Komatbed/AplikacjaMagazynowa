package com.example.warehouse.repository

import com.example.warehouse.model.OperationLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OperationLogRepository : JpaRepository<OperationLog, UUID> {
    fun findAllByOrderByTimestampDesc(): List<OperationLog>
    fun findTop10ByOrderByTimestampDesc(): List<OperationLog>
}

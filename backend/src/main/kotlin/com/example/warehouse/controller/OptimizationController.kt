package com.example.warehouse.controller

import com.example.warehouse.config.WarehouseConfig
import com.example.warehouse.model.CutPlan
import com.example.warehouse.model.OptimizationRequest
import com.example.warehouse.service.OptimizationService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/optimization")
class OptimizationController(
    private val optimizationService: OptimizationService,
    private val warehouseConfig: WarehouseConfig
) {

    @PostMapping("/calculate")
    fun calculateCuts(@RequestBody request: OptimizationRequest): CutPlan {
        val effectiveRequest = if (request.reserveWasteLengths.isEmpty()) {
            request.copy(reserveWasteLengths = warehouseConfig.reserveWasteLengths)
        } else {
            request
        }
        return optimizationService.optimizeCuts(effectiveRequest)
    }
}

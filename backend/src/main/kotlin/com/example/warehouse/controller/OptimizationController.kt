package com.example.warehouse.controller

import com.example.warehouse.model.CutPlan
import com.example.warehouse.model.OptimizationRequest
import com.example.warehouse.service.OptimizationService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/optimization")
class OptimizationController(
    private val optimizationService: OptimizationService
) {

    @PostMapping("/calculate")
    fun calculateCuts(@RequestBody request: OptimizationRequest): CutPlan {
        return optimizationService.optimizeCuts(request)
    }
}

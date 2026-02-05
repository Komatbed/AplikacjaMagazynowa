package com.example.warehouse.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.warehouse.util.MuntinCalculator

class MuntinViewModel : ViewModel() {
    
    private val _result = mutableStateOf<MuntinCalculator.MuntinResult?>(null)
    val result: State<MuntinCalculator.MuntinResult?> = _result

    fun calculate(
        sashWidth: Int,
        sashHeight: Int,
        profileHeight: Int,
        beadHeight: Int,
        beadAngle: Double,
        muntinWidth: Int,
        muntinGap: Double,
        overlap: Double,
        verticalFields: Int,
        horizontalFields: Int,
        isHalvingJoint: Boolean,
        externalOffset: Double
    ) {
        val request = MuntinCalculator.MuntinRequest(
            sashWidthMm = sashWidth,
            sashHeightMm = sashHeight,
            sashProfileHeightMm = profileHeight,
            beadHeightMm = beadHeight,
            beadAngleDeg = beadAngle,
            muntinWidthMm = muntinWidth,
            muntinGapMm = muntinGap,
            overlapBeadMm = overlap,
            isHalvingJoint = isHalvingJoint,
            externalOffsetMm = externalOffset
        )

        _result.value = MuntinCalculator.calculateRectangularGrid(
            request,
            verticalFields,
            horizontalFields
        )
    }
}

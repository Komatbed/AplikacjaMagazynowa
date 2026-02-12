package com.example.warehouse.ui.viewmodel

import com.example.warehouse.util.MuntinCalculatorV2.IntersectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuntinViewModelV2Test {

    @Test
    fun `test initial state`() {
        val viewModel = MuntinViewModelV2()
        val state = viewModel.uiState.value
        
        assertEquals("1000", state.sashWidth)
        assertEquals("1000", state.sashHeight)
        assertTrue(state.verticalMuntins.isEmpty())
        assertTrue(state.horizontalMuntins.isEmpty())
        assertTrue(state.cutList.isEmpty())
    }

    @Test
    fun `test add vertical muntin updates state and cut list`() {
        val viewModel = MuntinViewModelV2()
        
        viewModel.addVerticalMuntin()
        
        val state = viewModel.uiState.value
        assertEquals(1, state.verticalMuntins.size)
        assertEquals(500.0, state.verticalMuntins[0], 0.001)
        
        // Should have cuts now
        assertTrue(state.cutList.isNotEmpty())
        assertEquals(1, state.cutList.size) // 1 vertical muntin
    }

    @Test
    fun `test dimension change triggers recalculation`() {
        val viewModel = MuntinViewModelV2()
        viewModel.addVerticalMuntin() // 1 muntin at 500 (center of 1000)
        
        // Initial length: 1000 - offsets ~ 818
        val initialLen = viewModel.uiState.value.cutList[0].lengthMm
        
        // Change Height to 2000
        viewModel.updateSashDimensions("1000", "2000")
        
        val newState = viewModel.uiState.value
        val newLen = newState.cutList[0].lengthMm
        
        assertTrue(newLen > initialLen)
        // 2000 - offsets ~ 1818
        assertEquals(1818.0, newLen, 10.0) 
    }

    @Test
    fun `test mode switch to angular`() {
        val viewModel = MuntinViewModelV2()
        
        viewModel.setMode(true) // Switch to Angular
        
        assertTrue(viewModel.uiState.value.isAngularMode)
        
        // Add diagonal
        viewModel.addDiagonal(45.0)
        
        val state = viewModel.uiState.value
        assertEquals(1, state.diagonals.size)
        assertTrue(state.cutList.isNotEmpty())
    }

    @Test
    fun `test spider pattern toggle`() {
        val viewModel = MuntinViewModelV2()
        viewModel.setMode(true)
        
        viewModel.setSpiderPattern(true)
        
        val state = viewModel.uiState.value
        assertTrue(state.spiderPattern != null)
        assertTrue(state.cutList.isNotEmpty()) // Spider generates segments
        
        viewModel.setSpiderPattern(false)
        assertTrue(viewModel.uiState.value.spiderPattern == null)
    }
}

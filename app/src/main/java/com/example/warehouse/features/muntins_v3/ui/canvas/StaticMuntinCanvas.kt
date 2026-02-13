package com.example.warehouse.features.muntins_v3.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.warehouse.features.muntins_v3.geometry.Segment
import kotlin.math.min

@Composable
fun StaticMuntinCanvas(
    glassWidth: Double,
    glassHeight: Double,
    segments: List<Segment>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = maxWidth.value
        val canvasHeight = maxHeight.value
        
        // Avoid division by zero
        if (glassWidth <= 0 || glassHeight <= 0) return@BoxWithConstraints

        // Calculate scale to fit
        // We want to fit glassWidth into canvasWidth (with padding?)
        // and glassHeight into canvasHeight
        
        // Available size (assuming pixels approx or density handled by Canvas drawscope?)
        // BoxWithConstraints gives Dp. We need pixels for DrawScope? 
        // Actually DrawScope size is in pixels.
        // Let's do the scaling inside Canvas to be safe with densities.
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val drawW = size.width
            val drawH = size.height
            
            val scaleX = drawW / glassWidth.toFloat()
            val scaleY = drawH / glassHeight.toFloat()
            
            // Uniform scale to fit
            val scale = min(scaleX, scaleY) * 0.9f // 90% fill to leave margin
            
            // Center it
            val contentW = glassWidth.toFloat() * scale
            val contentH = glassHeight.toFloat() * scale
            val offsetX = (drawW - contentW) / 2f
            val offsetY = (drawH - contentH) / 2f
            
            withTransform({
                translate(left = offsetX, top = offsetY)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // 1. Draw Glass Bounds
                drawRect(
                    color = Color.White,
                    topLeft = Offset.Zero,
                    size = Size(glassWidth.toFloat(), glassHeight.toFloat())
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset.Zero,
                    size = Size(glassWidth.toFloat(), glassHeight.toFloat()),
                    style = Stroke(width = 2f / scale) // Maintain visible stroke width
                )

                // 2. Draw Segments
                segments.forEach { segment ->
                    drawLine(
                        color = Color.Black,
                        start = Offset(segment.startNode.x.toFloat(), segment.startNode.y.toFloat()),
                        end = Offset(segment.endNode.x.toFloat(), segment.endNode.y.toFloat()),
                        strokeWidth = 4f / scale // Constant visual width
                    )
                }
            }
        }
    }
}

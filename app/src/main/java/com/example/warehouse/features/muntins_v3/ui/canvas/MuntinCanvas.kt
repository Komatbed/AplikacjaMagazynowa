package com.example.warehouse.features.muntins_v3.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import kotlin.math.sqrt

@Composable
fun MuntinCanvas(
    glassWidth: Double,
    glassHeight: Double,
    segments: List<Segment>,
    selectedSegmentId: String? = null,
    onTap: (String?, Offset) -> Unit = { _, _ -> },
    clearanceGlobal: Double = 0.0,
    clearanceBead: Double = 0.0,
    clearanceMuntin: Double = 0.0,
    modifier: Modifier = Modifier
) {
    // Fixed view (zoom/pan wyłączone wg wymagań)
    val scale = 1f
    val offset = Offset.Zero

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // Convert screen tap to canvas coordinates
                    // Screen = Canvas * Scale + Offset
                    // Canvas = (Screen - Offset) / Scale
                    val canvasX = (tapOffset.x - offset.x) / scale
                    val canvasY = (tapOffset.y - offset.y) / scale
                    val canvasPoint = Offset(canvasX, canvasY)

                    // Find closest segment within threshold (e.g. 20 units)
                    // We check distance to the axis line of the segment
                    val touchThreshold = 30.0 // mm
                    
                    val closest = segments.minByOrNull { segment ->
                        distanceToSegment(canvasPoint, segment)
                    }

                    if (closest != null && distanceToSegment(canvasPoint, closest) < touchThreshold) {
                        onTap(closest.id, canvasPoint)
                    } else {
                        onTap(null, canvasPoint)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            withTransform({
                translate(offset.x, offset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // 1. Draw Glass Bounds (Light)
                // Assuming (0,0) is top-left of the glass
                drawRect(
                    color = Color.White,
                    topLeft = Offset.Zero,
                    size = Size(glassWidth.toFloat(), glassHeight.toFloat())
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset.Zero,
                    size = Size(glassWidth.toFloat(), glassHeight.toFloat()),
                    style = Stroke(width = 2f)
                )

                // 2. Draw Segments
                segments.forEach { segment ->
                    drawSegment(segment, segment.id == selectedSegmentId, clearanceGlobal, clearanceBead, clearanceMuntin)
                }
            }
        }
        val d = androidx.compose.ui.platform.LocalDensity.current
        segments.forEach { segment ->
            val sxDp = with(d) { segment.startNode.x.toFloat().toDp() }
            val syDp = with(d) { segment.startNode.y.toFloat().toDp() }
            val exDp = with(d) { segment.endNode.x.toFloat().toDp() }
            val eyDp = with(d) { segment.endNode.y.toFloat().toDp() }
            androidx.compose.material3.Text(
                text = "${kotlin.math.round(segment.angleStart * 10) / 10.0}°, g=${clearanceGlobal}, b=${clearanceBead}, m=${clearanceMuntin}",
                modifier = Modifier.offset(x = sxDp + 5.dp, y = syDp - 14.dp),
                color = Color.Black
            )
            androidx.compose.material3.Text(
                text = "${kotlin.math.round(segment.angleEnd * 10) / 10.0}°, g=${clearanceGlobal}, b=${clearanceBead}, m=${clearanceMuntin}",
                modifier = Modifier.offset(x = exDp + 5.dp, y = eyDp - 14.dp),
                color = Color.Black
            )
        }
    }
}

// Extension to draw a segment (simplified visualization)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegment(segment: Segment, isSelected: Boolean, clearanceGlobal: Double, clearanceBead: Double, clearanceMuntin: Double) {
    // Draw the axis line
    val color = if (isSelected) Color.Red else Color.Blue
    val width = if (isSelected) 8f else 4f // Thicker if selected (visual scale)

    // Draw main line
    drawLine(
        color = color,
        start = Offset(segment.startNode.x.toFloat(), segment.startNode.y.toFloat()),
        end = Offset(segment.endNode.x.toFloat(), segment.endNode.y.toFloat()),
        strokeWidth = width
    )

    // Labels are drawn via overlay Text composables
}

private fun distanceToSegment(p: Offset, s: Segment): Float {
    val x1 = s.startNode.x.toFloat()
    val y1 = s.startNode.y.toFloat()
    val x2 = s.endNode.x.toFloat()
    val y2 = s.endNode.y.toFloat()
    val px = p.x
    val py = p.y
    
    val dx = x2 - x1
    val dy = y2 - y1
    if (dx == 0f && dy == 0f) return sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1))
    
    val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
    
    val closestX: Float
    val closestY: Float
    
    if (t < 0) {
        closestX = x1
        closestY = y1
    } else if (t > 1) {
        closestX = x2
        closestY = y2
    } else {
        closestX = x1 + t * dx
        closestY = y1 + t * dy
    }
    
    val ddx = px - closestX
    val ddy = py - closestY
    return sqrt(ddx * ddx + ddy * ddy)
}

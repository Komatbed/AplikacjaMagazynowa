package com.example.warehouse.features.muntins_v3.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import kotlin.math.sqrt

@Composable
fun MuntinCanvas(
    glassWidth: Double,
    glassHeight: Double,
    segments: List<Segment>,
    selectedSegmentId: String? = null,
    onTap: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Zoom and Pan State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offset += pan
                }
            }
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
                    val threshold = 20f / scale // Scale threshold so it's consistent visually? Or fixed in canvas units?
                    // Better fixed in canvas units (e.g. half width of muntin + margin)
                    val touchThreshold = 30.0 // mm
                    
                    val closest = segments.minByOrNull { segment ->
                        distanceToSegment(canvasPoint, segment)
                    }

                    if (closest != null && distanceToSegment(canvasPoint, closest) < touchThreshold) {
                        onTap(closest.id)
                    } else {
                        onTap(null)
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
                    drawSegment(segment, segment.id == selectedSegmentId)
                }
            }
        }
    }
}

// Extension to draw a segment (simplified visualization)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSegment(segment: Segment, isSelected: Boolean) {
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

    // TODO: Draw the full profile width as a rotated rectangle/path
    // This requires calculating the corners based on width and angle
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

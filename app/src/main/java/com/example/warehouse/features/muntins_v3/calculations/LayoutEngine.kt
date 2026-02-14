package com.example.warehouse.features.muntins_v3.calculations

import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import java.util.UUID
import kotlin.math.abs

object LayoutEngine {
    
    /**
     * Generates a simple grid layout with vertical muntins as masters (continuous).
     * @param width Width of the glass (light)
     * @param height Height of the glass (light)
     * @param rows Number of horizontal spaces (rows - 1 horizontal lines)
     * @param cols Number of vertical spaces (cols - 1 vertical lines)
     * @param muntinWidth Width of the muntin profile
     */
    fun generateGrid(
        width: Double,
        height: Double,
        rows: Int,
        cols: Int,
        muntinWidth: Double
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        
        // Calculate step sizes
        val stepX = if (cols > 0) width / cols else width
        val stepY = if (rows > 0) height / rows else height

        // 1. Generate Vertical Muntins (Continuous - Masters)
        // x positions: stepX * i
        val verticalX = mutableListOf<Double>()
        for (i in 1 until cols) {
            val x = stepX * i
            verticalX.add(x)
            segments.add(
                Segment(
                    id = UUID.randomUUID().toString(),
                    startNode = Node(x, 0.0),
                    endNode = Node(x, height),
                    width = muntinWidth,
                    angleStart = 90.0,
                    angleEnd = 90.0
                )
            )
        }

        // 2. Generate Horizontal Muntins (Split by Verticals)
        // y positions: stepY * j
        for (j in 1 until rows) {
            val y = stepY * j
            
            if (verticalX.isEmpty()) {
                // No verticals, just one long horizontal
                segments.add(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        startNode = Node(0.0, y),
                        endNode = Node(width, y),
                        width = muntinWidth,
                        angleStart = 0.0,
                        angleEnd = 0.0
                    )
                )
            } else {
                // Split by verticals
                var currentX = 0.0
                
                // Add segments between verticals
                for (vX in verticalX) {
                    segments.add(
                        Segment(
                            id = UUID.randomUUID().toString(),
                            startNode = Node(currentX, y),
                            endNode = Node(vX, y),
                            width = muntinWidth,
                            angleStart = 0.0,
                            angleEnd = 0.0
                        )
                    )
                    currentX = vX
                }
                
                // Add last segment (from last vertical to edge)
                segments.add(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        startNode = Node(currentX, y),
                        endNode = Node(width, y),
                        width = muntinWidth,
                        angleStart = 0.0,
                        angleEnd = 0.0
                    )
                )
            }
        }

        return segments
    }
    
    /**
     * Grid with horizontal as masters (continuous), verticals split by horizontals.
     */
    fun generateGridHorizontalMaster(
        width: Double,
        height: Double,
        rows: Int,
        cols: Int,
        muntinWidth: Double
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        
        val stepX = if (cols > 0) width / cols else width
        val stepY = if (rows > 0) height / rows else height
        
        // Horizontal masters
        val horizontalY = mutableListOf<Double>()
        for (j in 1 until rows) {
            val y = stepY * j
            horizontalY.add(y)
            segments.add(
                Segment(
                    id = UUID.randomUUID().toString(),
                    startNode = Node(0.0, y),
                    endNode = Node(width, y),
                    width = muntinWidth,
                    angleStart = 0.0,
                    angleEnd = 0.0
                )
            )
        }
        
        // Vertical split by horizontals
        for (i in 1 until cols) {
            val x = stepX * i
            var startY = 0.0
            for (y in horizontalY) {
                segments.add(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        startNode = Node(x, startY),
                        endNode = Node(x, y),
                        width = muntinWidth,
                        angleStart = 90.0,
                        angleEnd = 90.0
                    )
                )
                startY = y
            }
            // Last segment to bottom edge
            segments.add(
                Segment(
                    id = UUID.randomUUID().toString(),
                    startNode = Node(x, startY),
                    endNode = Node(x, height),
                    width = muntinWidth,
                    angleStart = 90.0,
                    angleEnd = 90.0
                )
            )
        }
        
        return segments
    }

    /**
     * Generates a single X-Cross (Saint Andrew's Cross).
     * One diagonal is continuous (Master), the other is split.
     */
    fun generateCross(
        width: Double,
        height: Double,
        muntinWidth: Double
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        
        val centerX = width / 2.0
        val centerY = height / 2.0
        
        // 1. Master Diagonal (Top-Left to Bottom-Right)
        segments.add(
            Segment(
                id = UUID.randomUUID().toString(),
                startNode = Node(0.0, 0.0),
                endNode = Node(width, height),
                width = muntinWidth,
                angleStart = 45.0, // Approximate, will be calc'd
                angleEnd = 45.0
            )
        )

        // 2. Split Diagonal (Bottom-Left to Top-Right)
        // Part A: Bottom-Left to Center
        segments.add(
            Segment(
                id = UUID.randomUUID().toString(),
                startNode = Node(0.0, height),
                endNode = Node(centerX, centerY),
                width = muntinWidth,
                angleStart = 45.0,
                angleEnd = 45.0
            )
        )
        
        // Part B: Center to Top-Right
        segments.add(
            Segment(
                id = UUID.randomUUID().toString(),
                startNode = Node(centerX, centerY),
                endNode = Node(width, 0.0),
                width = muntinWidth,
                angleStart = 45.0,
                angleEnd = 45.0
            )
        )

        return segments
    }

    /**
     * Generates a Diamond (Rhombus) shape connecting the midpoints of the frame edges.
     */
    fun generateDiamond(
        width: Double,
        height: Double,
        muntinWidth: Double
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        
        val midX = width / 2.0
        val midY = height / 2.0
        
        // Points
        val top = Node(midX, 0.0)
        val right = Node(width, midY)
        val bottom = Node(midX, height)
        val left = Node(0.0, midY)

        // 4 Segments
        segments.add(Segment(UUID.randomUUID().toString(), top, right, muntinWidth))
        segments.add(Segment(UUID.randomUUID().toString(), right, bottom, muntinWidth))
        segments.add(Segment(UUID.randomUUID().toString(), bottom, left, muntinWidth))
        segments.add(Segment(UUID.randomUUID().toString(), left, top, muntinWidth))

        return segments
    }

    /**
     * Generates a Sunburst layout (Rays from bottom center).
     * @param numRays Number of rays (e.g. 3, 5, 7)
     */
    fun generateSunburst(
        width: Double,
        height: Double,
        muntinWidth: Double,
        numRays: Int = 3
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        val centerX = width / 2.0
        val centerY = height // Bottom center
        
        // Rays distributed over 180 degrees (0 to 180)
        // 0 is right (0 rad), 90 is up (PI/2 rad), 180 is left (PI rad)
        // But coordinate system Y is down.
        // So angle 0 (right) -> (1, 0)
        // Angle 90 (up) -> (0, -1)
        // Angle 180 (left) -> (-1, 0)
        
        val step = 180.0 / (numRays + 1)
        
        for (i in 1..numRays) {
            val angleDeg = step * i
            val angleRad = Math.toRadians(angleDeg)
            
            val dx = kotlin.math.cos(angleRad)
            val dy = -kotlin.math.sin(angleRad) // Negative because Y is down and we want to go UP
            
            // Find intersection with box boundaries (0,0, width, height)
            // Ray starts at (centerX, centerY)
            
            // 1. Try Top (y=0) intersection
            // 0 = centerY + t * dy => t = -centerY / dy
            var endNode: Node? = null
            
            if (abs(dy) > 0.001) {
                val t = -centerY / dy
                if (t > 0) {
                    val x = centerX + t * dx
                    if (x >= -0.001 && x <= width + 0.001) {
                        endNode = Node(x, 0.0)
                    }
                }
            }
            
            // 2. Try Right (x=width) intersection if not hit top
            if (endNode == null && dx > 0.001) {
                val t = (width - centerX) / dx
                if (t > 0) {
                    val y = centerY + t * dy
                    if (y >= -0.001 && y <= height + 0.001) { // height check is redundant as we go up
                        endNode = Node(width, y)
                    }
                }
            }
            
            // 3. Try Left (x=0) intersection if not hit top
            if (endNode == null && dx < -0.001) {
                val t = (0.0 - centerX) / dx
                if (t > 0) {
                    val y = centerY + t * dy
                    if (y >= -0.001 && y <= height + 0.001) {
                        endNode = Node(0.0, y)
                    }
                }
            }
            
            if (endNode != null) {
                segments.add(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        startNode = Node(centerX, centerY),
                        endNode = endNode,
                        width = muntinWidth,
                        angleStart = angleDeg,
                        angleEnd = angleDeg
                    )
                )
            }
        }
        return segments
    }

    /**
     * Generates a Web layout (Rays from center).
     * @param numRays Number of rays (e.g. 8)
     */
    fun generateWeb(
        width: Double,
        height: Double,
        muntinWidth: Double,
        numRays: Int = 8
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        val centerX = width / 2.0
        val centerY = height / 2.0
        
        val step = 360.0 / numRays
        
        for (i in 0 until numRays) {
            val angleDeg = step * i
            val angleRad = Math.toRadians(angleDeg)
            
            val dx = kotlin.math.cos(angleRad)
            val dy = -kotlin.math.sin(angleRad) // Y is down
            
            // Ray from center
            // Find intersection with box
            var endNode: Node? = null
            
            // 1. Top (y=0)
            if (dy < -0.001) {
                val t = -centerY / dy
                val x = centerX + t * dx
                if (x >= -0.001 && x <= width + 0.001) {
                     endNode = Node(x, 0.0)
                }
            }
            
            // 2. Bottom (y=height)
            if (endNode == null && dy > 0.001) {
                val t = (height - centerY) / dy
                val x = centerX + t * dx
                if (x >= -0.001 && x <= width + 0.001) {
                     endNode = Node(x, height)
                }
            }
            
            // 3. Right (x=width)
            if (endNode == null && dx > 0.001) {
                val t = (width - centerX) / dx
                val y = centerY + t * dy
                if (y >= -0.001 && y <= height + 0.001) {
                     endNode = Node(width, y)
                }
            }
            
            // 4. Left (x=0)
            if (endNode == null && dx < -0.001) {
                val t = -centerX / dx
                val y = centerY + t * dy
                if (y >= -0.001 && y <= height + 0.001) {
                     endNode = Node(0.0, y)
                }
            }
            
            if (endNode != null) {
                segments.add(
                    Segment(
                        id = UUID.randomUUID().toString(),
                        startNode = Node(centerX, centerY),
                        endNode = endNode,
                        width = muntinWidth,
                        angleStart = angleDeg,
                        angleEnd = angleDeg
                    )
                )
            }
        }
        return segments
    }

    /**
     * Generates a Gothic Arch layout (Pointed Arch).
     * Approximates curves with segments.
     */
    fun generateGothic(
        width: Double,
        height: Double,
        muntinWidth: Double
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        
        // Gothic Arch usually starts from some height (spring line) and goes to top center.
        // Let's assume the arch starts at 2/3 height (from top? or from bottom?).
        // Usually "Gothic" means the top part of the window.
        // Let's assume the spring line is at Y = height / 2.
        
        val springY = height / 2.0
        val centerX = width / 2.0
        val topY = 0.0 // Top of the arch
        
        // Left side arch: From (0, springY) to (centerX, topY)
        // Right side arch: From (width, springY) to (centerX, topY)
        // We approximate each with 3 segments.
        
        // Bezier-like points or circle arc?
        // Simple approximation: 3 segments.
        
        // Left Arch
        val leftPoints = listOf(
            Node(0.0, springY),
            Node(centerX * 0.2, springY * 0.6), // Control point approx
            Node(centerX * 0.6, topY * 0.4),
            Node(centerX, topY)
        )
        
        for (i in 0 until leftPoints.size - 1) {
            segments.add(Segment(UUID.randomUUID().toString(), leftPoints[i], leftPoints[i+1], muntinWidth))
        }

        // Right Arch
        val rightPoints = listOf(
            Node(width, springY),
            Node(width - centerX * 0.2, springY * 0.6),
            Node(width - centerX * 0.6, topY * 0.4),
            Node(centerX, topY)
        )
        
        for (i in 0 until rightPoints.size - 1) {
            segments.add(Segment(UUID.randomUUID().toString(), rightPoints[i], rightPoints[i+1], muntinWidth))
        }
        
        // Horizontal bar at spring line?
        segments.add(Segment(UUID.randomUUID().toString(), Node(0.0, springY), Node(width, springY), muntinWidth))
        
        // Vertical in center?
        segments.add(Segment(UUID.randomUUID().toString(), Node(centerX, springY), Node(centerX, height), muntinWidth))

        return segments
    }

    /**
     * Generates a Trapezoidal layout (Converging verticals).
     * @param numVerticals Number of vertical bars
     * @param topScale Scale factor for top width (0.0 to 1.0) relative to bottom width
     */
    fun generateTrapezoidLayout(
        width: Double,
        height: Double,
        muntinWidth: Double,
        numVerticals: Int = 3,
        topScale: Double = 0.6
    ): List<Segment> {
        val segments = mutableListOf<Segment>()
        
        // Bottom width is full width.
        // Top width is width * topScale, centered.
        
        val bottomStep = width / (numVerticals + 1)
        val topWidth = width * topScale
        val topStartX = (width - topWidth) / 2.0
        val topStep = topWidth / (numVerticals + 1)
        
        for (i in 1..numVerticals) {
            val bottomX = bottomStep * i
            val topX = topStartX + topStep * i
            
            segments.add(
                Segment(
                    id = UUID.randomUUID().toString(),
                    startNode = Node(topX, 0.0),
                    endNode = Node(bottomX, height),
                    width = muntinWidth
                )
            )
        }
        
        // Add one horizontal in the middle?
        segments.add(Segment(UUID.randomUUID().toString(), Node(0.0, height/2.0), Node(width, height/2.0), muntinWidth))

        return segments
    }
}

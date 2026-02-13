package com.example.warehouse.features.muntins_v3.calculations

import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CutItem(
    val length: Double,
    val angleStart: Double,
    val angleEnd: Double,
    val count: Int
)

object CutListCalculator {
    private const val EPSILON = 0.001

    fun calculateCutList(
        segments: List<Segment>,
        glassWidth: Double,
        glassHeight: Double
    ): List<CutItem> {
        val cuts = mutableListOf<Triple<Double, Double, Double>>() // length, angleStart, angleEnd

        for (segment in segments) {
            // Find start boundary
            val (startBoundary, startClearance) = findBoundary(segment.startNode, segments, segment, glassWidth, glassHeight)
            // Find end boundary
            val (endBoundary, endClearance) = findBoundary(segment.endNode, segments, segment, glassWidth, glassHeight)

            val result = SegmentCalculator.calculateRealLength(
                segment,
                startBoundary,
                endBoundary,
                startClearance,
                endClearance
            )
            cuts.add(Triple(result.finalLength, result.cutAngleStart, result.cutAngleEnd))
        }

        // Group by identical cuts (tolerance 0.1mm)
        // Normalize angles (smaller first) to group symmetric cuts
        return cuts.groupBy { 
            val len = Math.round(it.first * 10) / 10.0
            val a1 = Math.round(it.second * 10) / 10.0
            val a2 = Math.round(it.third * 10) / 10.0
            Triple(
                len,
                min(a1, a2),
                max(a1, a2)
            )
        }.map { (key, list) ->
            CutItem(key.first, key.second, key.third, list.size)
        }.sortedByDescending { it.length }
    }

    private fun findBoundary(
        node: Node,
        allSegments: List<Segment>,
        currentSegment: Segment,
        width: Double,
        height: Double
    ): Pair<Pair<Node, Node>, Double> {
        // 1. Check Glass Edges
        if (abs(node.y) < EPSILON) return Pair(Node(0.0, 0.0) to Node(width, 0.0), 1.0) // Top
        if (abs(node.y - height) < EPSILON) return Pair(Node(0.0, height) to Node(width, height), 1.0) // Bottom
        if (abs(node.x) < EPSILON) return Pair(Node(0.0, 0.0) to Node(0.0, height), 1.0) // Left
        if (abs(node.x - width) < EPSILON) return Pair(Node(width, 0.0) to Node(width, height), 1.0) // Right

        // 2. Check other segments
        for (other in allSegments) {
            if (other.id == currentSegment.id) continue
            if (isPointOnSegment(node, other)) {
                // Check if parallel (collinear)
                if (areParallel(currentSegment, other)) {
                    // Ignore parallel segments touching at endpoints (splice)
                    continue
                }
                
                // Found intersection with another muntin
                // Boundary is the axis of the other muntin
                // Clearance is half width + 1.0
                return Pair(other.startNode to other.endNode, other.width / 2.0 + 1.0)
            }
        }

        // 3. Loose End (No intersection)
        // Return boundary line perpendicular to current segment passing through node
        // Clearance = 0.0
        val dx = currentSegment.endNode.x - currentSegment.startNode.x
        val dy = currentSegment.endNode.y - currentSegment.startNode.y
        // Perpendicular vector (-dy, dx)
        val p1 = Node(node.x - dy, node.y + dx)
        val p2 = Node(node.x + dy, node.y - dx)
        return Pair(p1 to p2, 0.0)
    }

    private fun isPointOnSegment(p: Node, s: Segment): Boolean {
        // Check if p is on line segment s.startNode-s.endNode
        // Using cross product for collinearity and dot product for bounds
        val crossProduct = (p.y - s.startNode.y) * (s.endNode.x - s.startNode.x) - 
                           (p.x - s.startNode.x) * (s.endNode.y - s.startNode.y)
        if (abs(crossProduct) > EPSILON) return false

        val dotProduct = (p.x - s.startNode.x) * (s.endNode.x - s.startNode.x) + 
                         (p.y - s.startNode.y) * (s.endNode.y - s.startNode.y)
        if (dotProduct < 0) return false

        val squaredLength = (s.endNode.x - s.startNode.x) * (s.endNode.x - s.startNode.x) + 
                            (s.endNode.y - s.startNode.y) * (s.endNode.y - s.startNode.y)
        if (dotProduct > squaredLength + EPSILON) return false // Added EPSILON for tolerance

        return true
    }

    private fun areParallel(s1: Segment, s2: Segment): Boolean {
        val dx1 = s1.endNode.x - s1.startNode.x
        val dy1 = s1.endNode.y - s1.startNode.y
        val dx2 = s2.endNode.x - s2.startNode.x
        val dy2 = s2.endNode.y - s2.startNode.y
        val cross = dx1 * dy2 - dy1 * dx2
        return abs(cross) < EPSILON
    }
}

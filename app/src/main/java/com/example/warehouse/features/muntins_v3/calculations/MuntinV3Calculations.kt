package com.example.warehouse.features.muntins_v3.calculations

import com.example.warehouse.features.muntins_v3.geometry.Node
import com.example.warehouse.features.muntins_v3.geometry.Segment
import kotlin.math.*

object MuntinV3Calculations {

    data class GlassDimensions(
        val width: Double,
        val height: Double
    )

    /**
     * Calculates the visible glass dimensions (light) based on frame and profile settings.
     * 1. Oblicz światło szyby
     * glassWidth  = frameWidth - 2*(offset + beadOffset)
     * glassHeight = frameHeight - 2*(offset + beadOffset)
     */
    fun calculateGlassDimensions(
        frameWidth: Double,
        frameHeight: Double,
        profileGlassOffset: Double,
        beadEffectiveOffset: Double
    ): GlassDimensions {
        val totalOffset = profileGlassOffset + beadEffectiveOffset
        val width = frameWidth - 2 * totalOffset
        val height = frameHeight - 2 * totalOffset
        return GlassDimensions(max(0.0, width), max(0.0, height))
    }

    /**
     * Basic vector math and line intersection helpers.
     */
    object GeometryUtils {
        
        fun distance(n1: Node, n2: Node): Double {
            return sqrt((n2.x - n1.x).pow(2) + (n2.y - n1.y).pow(2))
        }

        /**
         * Returns the intersection point of two infinite lines defined by (p1-p2) and (p3-p4).
         * Returns null if parallel.
         */
        fun getLineIntersection(p1: Node, p2: Node, p3: Node, p4: Node): Node? {
            val det = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
            if (abs(det) < 1e-9) return null // Parallel

            val t = ((p1.x - p3.x) * (p3.y - p4.y) - (p1.y - p3.y) * (p3.x - p4.x)) / det
            
            // Intersection point
            val x = p1.x + t * (p2.x - p1.x)
            val y = p1.y + t * (p2.y - p1.y)
            return Node(x, y)
        }

        /**
         * Calculates the offset vector for a line segment (p1->p2) given a perpendicular distance (offset).
         * Used to find the "edge" lines of a profile with a certain width.
         */
        fun getOffsetVector(p1: Node, p2: Node, offset: Double): Node {
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1e-9) return Node(0.0, 0.0)
            
            // Perpendicular vector (-dy, dx) normalized * offset
            return Node((-dy / len) * offset, (dx / len) * offset)
        }
    }
}

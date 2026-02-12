package com.example.warehouse.util

import com.example.warehouse.model.*
import kotlin.math.*
import java.util.UUID

object MuntinCalculatorV2Angular {

    data class Point(val x: Double, val y: Double) {
        fun dist(other: Point) = sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
        operator fun plus(other: Point) = Point(x + other.x, y + other.y)
        operator fun minus(other: Point) = Point(x - other.x, y - other.y)
        operator fun times(scalar: Double) = Point(x * scalar, y * scalar)
    }

    data class Segment(val p1: Point, val p2: Point) {
        fun length(): Double = p1.dist(p2)
        fun vector() = p2 - p1
        fun direction() = vector().let { v -> 
            val len = sqrt(v.x * v.x + v.y * v.y)
            if (len > 0) Point(v.x / len, v.y / len) else Point(0.0, 0.0)
        }
    }

    data class AngularResult(
        val cutItems: List<CutItemV2>,
        val mountingMarks: List<MountMarkV2>,
        val debugSegments: List<Segment>
    )

    data class ProcessedSegment(
        val p1: Point,
        val p2: Point,
        val isContinuous: Boolean,
        val description: String,
        val angleDeg: Double,
        val profileWidthMm: Double
    ) {
        fun length() = p1.dist(p2)
    }

    private const val EPSILON = 0.001

    fun calculate(
        sashWidthMm: Int,
        sashHeightMm: Int,
        sashProfile: SashProfileV2,
        beadProfile: BeadProfileV2,
        muntinProfile: MuntinProfileV2,
        diagonals: List<DiagonalLineV2>,
        spider: SpiderPatternV2? = null,
        arch: ArchPatternV2? = null,
        settings: V2GlobalSettings
    ): AngularResult {
        // 1. Define Usable Area (Glass Area)
        // Correction: The muntins are on the GLASS. 
        // Glass dimensions = Sash Outer - 2 * (SashProfileHeight - GlassOverlap?)
        // Usually: Sash Outer - 2 * SashProfileHeight (approx) + Beads...
        // Prompt says: "startuje od wymiaru skrzydła i odejmuje: 2x wysokość profilu, 2x efektywny zabór listwy".
        
        val frameOffset = sashProfile.heightMm + beadProfile.heightMm // Simplified effective offset
        val minX = frameOffset.toDouble()
        val maxX = (sashWidthMm - frameOffset).toDouble()
        val minY = frameOffset.toDouble()
        val maxY = (sashHeightMm - frameOffset).toDouble()

        val rawSegments = mutableListOf<ProcessedSegment>()

        // 2. Generate Raw Geometry

        // Diagonals
        diagonals.forEach { diag ->
            // Logic: Line equation from (offset, 0) or (0, offset) with angle.
            // Normalize angle 0..180
            val angle = diag.angleDeg
            val rad = Math.toRadians(angle)
            val dx = cos(rad)
            val dy = sin(rad)

            // Origin logic:
            // 0-45 deg (Horizontal-ish): Offset is Y on Left Edge (x=0)
            // 45-135 deg (Vertical-ish): Offset is X on Top Edge (y=0)
            // 135-180 deg (Horizontal-ish): Offset is Y on Right Edge? Or Left?
            // Let's stick to user prompt implication or standard convention.
            // Defaulting to: 
            // If |tan(angle)| > 1 (Vertical-ish, 45..135): Start at (offset, 0)
            // Else (Horizontal-ish): Start at (0, offset)
            
            val isVerticalIsh = abs(tan(rad)) > 1.0
            
            val startP = if (isVerticalIsh) {
                Point(diag.offsetRefMm, 0.0) // Top edge
            } else {
                Point(0.0, diag.offsetRefMm) // Left edge
            }

            val clipped = clipLineToRect(startP, dx, dy, minX, minY, maxX, maxY)
            if (clipped != null) {
                rawSegments.add(
                    ProcessedSegment(
                        clipped.p1, clipped.p2, 
                        diag.isContinuous, 
                        "Ukośny ${diag.lineId}", 
                        angle,
                        muntinProfile.widthMm.toDouble()
                    )
                )
            }
        }

        // Spider Pattern
        if (spider != null) {
            val cx = if (spider.centerX == 0.0) (minX + maxX) / 2 else spider.centerX
            val cy = if (spider.centerY == 0.0) (minY + maxY) / 2 else spider.centerY
            val center = Point(cx, cy)
            
            val angleStep = 360.0 / spider.armCount
            
            // Arms
            for (i in 0 until spider.armCount) {
                val angle = spider.startAngleDeg + (i * angleStep)
                val rad = Math.toRadians(angle)
                val dx = cos(rad)
                val dy = sin(rad)
                
                val clipped = clipRayToRect(center, dx, dy, minX, minY, maxX, maxY)
                if (clipped != null) {
                    rawSegments.add(
                        ProcessedSegment(clipped.p1, clipped.p2, false, "Pajęczyna Ramię $i", angle, muntinProfile.widthMm.toDouble())
                    )
                }
            }
            
            // Rings
            for (r in 1..spider.ringCount) {
                val radius = r * spider.ringSpacingMm
                for (i in 0 until spider.armCount) {
                    val a1 = spider.startAngleDeg + (i * angleStep)
                    val a2 = spider.startAngleDeg + ((i + 1) % spider.armCount * angleStep)
                    
                    val p1 = Point(cx + radius * cos(Math.toRadians(a1)), cy + radius * sin(Math.toRadians(a1)))
                    val p2 = Point(cx + radius * cos(Math.toRadians(a2)), cy + radius * sin(Math.toRadians(a2)))
                    
                    val seg = clipSegmentToRect(p1, p2, minX, minY, maxX, maxY)
                    if (seg != null) {
                        val segAngle = Math.toDegrees(atan2(p2.y - p1.y, p2.x - p1.x))
                        rawSegments.add(
                            ProcessedSegment(seg.p1, seg.p2, false, "Pajęczyna Pierścień $r", segAngle, muntinProfile.widthMm.toDouble())
                        )
                    }
                }
            }
        }

        // Arch Pattern (Placeholder for future logic)
        if (arch != null) {
             // Simple implementation: Top Arch
             // Center at (Width/2, Height/2) just to show something
             val cx = (minX + maxX) / 2
             val cy = minY + (arch.radiusMm ?: 500.0)
             val radius = arch.radiusMm ?: 500.0
             
             // Generate segments approximating arch
             val steps = 20
             val startAngle = 180.0 + 45.0
             val sweep = 90.0
             val step = sweep / steps
             
             for(i in 0 until steps) {
                 val a1 = startAngle + i*step
                 val a2 = startAngle + (i+1)*step
                 val p1 = Point(cx + radius * cos(Math.toRadians(a1)), cy + radius * sin(Math.toRadians(a1)))
                 val p2 = Point(cx + radius * cos(Math.toRadians(a2)), cy + radius * sin(Math.toRadians(a2)))
                 
                 val seg = clipSegmentToRect(p1, p2, minX, minY, maxX, maxY)
                 if (seg != null) {
                     val angle = Math.toDegrees(atan2(p2.y - p1.y, p2.x - p1.x))
                     rawSegments.add(ProcessedSegment(seg.p1, seg.p2, false, "Łuk", angle, muntinProfile.widthMm.toDouble()))
                 }
             }
        }

        // 3. Resolve Intersections & Clearances
        val resolvedSegments = resolveIntersections(rawSegments, settings.assemblyClearanceMm)

        // 4. Generate Output
        val cutItems = resolvedSegments.mapIndexed { index, seg ->
            val len = seg.length()
            // Real angles calculation is complex, here using geometric angle
            // Left/Right angle defaults to 90 or actual if available
            // For angular cuts, we'd calculate the angle relative to the cut line.
            // Simplified:
            
            CutItemV2(
                sashNo = 1,
                muntinNo = index + 1,
                axis = if (abs(seg.angleDeg) % 180 in 45.0..135.0) Axis.VERTICAL else Axis.HORIZONTAL,
                lengthMm = max(0.0, len + settings.sawCorrectionMm),
                leftAngleDeg = 90.0, // Placeholder
                rightAngleDeg = 90.0, // Placeholder
                qty = 1,
                profileName = muntinProfile.profileNo,
                description = seg.description,
                notes = "Kąt ${"%.1f".format(seg.angleDeg)}"
            )
        }

        val marks = resolvedSegments.mapIndexed { index, seg ->
            val mid = Point((seg.p1.x + seg.p2.x)/2, (seg.p1.y + seg.p2.y)/2)
            // Reference from Outer Edge.
            // Our minX/minY are GLASS edges.
            // So Outer Top = 0. Glass Top = frameOffset.
            // Mark Y = mid.y (Glass coord) + frameOffset? 
            // Wait, minX is frameOffset. So mid.x is already relative to Outer (0,0)?
            // No, in step 1 we defined minX = frameOffset.
            // So (0,0) is the outer corner of Sash.
            // So mid.x, mid.y ARE relative to Outer Edge.
            
            MountMarkV2(
                itemId = "${index + 1}",
                referenceEdge = "Góra/Lewa",
                offsetMm = 0.0,
                axisDescription = "X=${"%.1f".format(mid.x)}, Y=${"%.1f".format(mid.y)}"
            )
        }

        return AngularResult(
            cutItems = cutItems,
            mountingMarks = marks,
            debugSegments = resolvedSegments.map { Segment(it.p1, it.p2) }
        )
    }

    // --- Core Logic: Intersection Resolution ---

    private fun resolveIntersections(segments: List<ProcessedSegment>, clearanceMm: Double): List<ProcessedSegment> {
        if (segments.isEmpty()) return emptyList()

        // 1. Find all intersection points for each segment
        // Map<SegmentIndex, List<IntersectionInfo>>
        // IntersectionInfo: (Point, OtherSegmentIndex)
        
        val intersections = mutableMapOf<Int, MutableList<Point>>()

        for (i in segments.indices) {
            for (j in i + 1 until segments.size) {
                val s1 = segments[i]
                val s2 = segments[j]
                
                val p = getIntersection(s1.p1, s1.p2, s2.p1, s2.p2)
                if (p != null) {
                    // Check logic: Who is continuous?
                    // Priority: Explicit isContinuous > Vertical (approx 90) > Horizontal (approx 0) > Diagonal
                    val s1Vertical = abs(s1.angleDeg - 90.0) < 10.0 || abs(s1.angleDeg - 270.0) < 10.0
                    val s2Vertical = abs(s2.angleDeg - 90.0) < 10.0 || abs(s2.angleDeg - 270.0) < 10.0
                    
                    val s1Cont = s1.isContinuous || (s1Vertical && !s2.isContinuous)
                    val s2Cont = s2.isContinuous || (s2Vertical && !s1.isContinuous)
                    
                    // If s1 is continuous, it CUTS s2. So s2 gets an intersection point.
                    // If s2 is continuous, it CUTS s1.
                    // If both or neither, we default to s1 continuous (Vertical wins).
                    
                    val s1Wins = if (s1Cont && !s2Cont) true 
                                 else if (!s1Cont && s2Cont) false 
                                 else s1Vertical // Default tie-breaker
                    
                    if (s1Wins) {
                        // s1 is continuous, s2 is cut
                        intersections.computeIfAbsent(j) { mutableListOf() }.add(p)
                    } else {
                        // s2 is continuous, s1 is cut
                        intersections.computeIfAbsent(i) { mutableListOf() }.add(p)
                    }
                }
            }
        }

        val result = mutableListOf<ProcessedSegment>()

        // 2. Process each segment
        for (i in segments.indices) {
            val seg = segments[i]
            val cuts = intersections[i]

            if (cuts.isNullOrEmpty()) {
                result.add(seg)
            } else {
                // Sort cuts by distance from p1
                val sortedCuts = cuts.sortedBy { seg.p1.dist(it) }
                
                // Create sub-segments
                var currentStart = seg.p1
                // We need to know IF currentStart is a "cut point" (needs gap) or "frame point" (no gap).
                // p1 is Frame (usually), unless we chained segments (not here).
                // But cuts ARE cuts.
                
                // Logic:
                // S --- cut1 --- cut2 --- E
                // Segments: S->cut1, cut1->cut2, cut2->E
                // At 'cut1':
                //   End of (S->cut1) needs gap.
                //   Start of (cut1->cut2) needs gap.
                // Gap = muntinWidth / 2 + clearance
                
                val gap = (seg.profileWidthMm / 2.0) + clearanceMm
                val dir = seg.p2 - seg.p1
                val len = seg.length()
                val uDir = if (len > 0) Point(dir.x / len, dir.y / len) else Point(0.0, 0.0)

                // Add points to a list: [Start, Cut1, Cut2, ..., End]
                val points = mutableListOf<Point>()
                points.add(seg.p1)
                points.addAll(sortedCuts)
                points.add(seg.p2)

                for (k in 0 until points.size - 1) {
                    val pStart = points[k]
                    val pEnd = points[k+1]
                    
                    // Is pStart a cut? Yes if k > 0.
                    // Is pEnd a cut? Yes if k < size - 2. (Last point is p2)
                    
                    val isStartCut = (k > 0)
                    val isEndCut = (k < points.size - 2) // Actually points.size is N+2 (S, c1...cN, E). 
                    // k=0: S->c1. End is cut.
                    // k=last: cN->E. Start is cut.
                    
                    // Correct indices:
                    // points[0] is Start (Frame) -> No gap
                    // points[1..N] are Cuts -> Gap
                    // points[N+1] is End (Frame) -> No gap
                    
                    var valid = true
                    var sP = pStart
                    var eP = pEnd
                    
                    if (k > 0) {
                        // pStart is a cut, move it forward by gap
                        sP = sP + (uDir * gap)
                    }
                    
                    if (k < points.size - 2) {
                        // pEnd is a cut, move it backward by gap
                        eP = eP - (uDir * gap)
                    }
                    
                    // Check if segment inverted (gap too big)
                    // Dot product check or length check
                    val segVec = eP - sP
                    val dot = segVec.x * uDir.x + segVec.y * uDir.y
                    if (dot <= 0) valid = false
                    
                    if (valid) {
                        result.add(seg.copy(p1 = sP, p2 = eP, description = "${seg.description} part ${k+1}"))
                    }
                }
            }
        }
        
        return result
    }

    // --- Geometry Helpers ---

    private fun getIntersection(p1: Point, p2: Point, p3: Point, p4: Point): Point? {
        val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (abs(d) < 1e-9) return null

        val t = ((p1.x - p3.x) * (p3.y - p4.y) - (p1.y - p3.y) * (p3.x - p4.x)) / d
        val u = -((p1.x - p2.x) * (p1.y - p3.y) - (p1.y - p2.y) * (p1.x - p3.x)) / d

        if (t in 0.0..1.0 && u in 0.0..1.0) {
            return Point(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
        }
        return null
    }

    private fun clipLineToRect(start: Point, dx: Double, dy: Double, minX: Double, minY: Double, maxX: Double, maxY: Double): Segment? {
        var t0 = Double.NEGATIVE_INFINITY
        var t1 = Double.POSITIVE_INFINITY

        // Clip against X
        if (abs(dx) < 1e-9) {
            if (start.x < minX || start.x > maxX) return null
        } else {
            val tMin = (minX - start.x) / dx
            val tMax = (maxX - start.x) / dx
            t0 = max(t0, min(tMin, tMax))
            t1 = min(t1, max(tMin, tMax))
        }

        // Clip against Y
        if (abs(dy) < 1e-9) {
            if (start.y < minY || start.y > maxY) return null
        } else {
            val tMin = (minY - start.y) / dy
            val tMax = (maxY - start.y) / dy
            t0 = max(t0, min(tMin, tMax))
            t1 = min(t1, max(tMin, tMax))
        }

        if (t1 < t0) return null

        return Segment(
            Point(start.x + t0 * dx, start.y + t0 * dy),
            Point(start.x + t1 * dx, start.y + t1 * dy)
        )
    }

    private fun clipRayToRect(start: Point, dx: Double, dy: Double, minX: Double, minY: Double, maxX: Double, maxY: Double): Segment? {
        var t0 = 0.0
        var t1 = Double.POSITIVE_INFINITY

        if (abs(dx) < 1e-9) {
            if (start.x < minX || start.x > maxX) return null
        } else {
            val tMin = (minX - start.x) / dx
            val tMax = (maxX - start.x) / dx
            t0 = max(t0, min(tMin, tMax))
            t1 = min(t1, max(tMin, tMax))
        }

        if (abs(dy) < 1e-9) {
            if (start.y < minY || start.y > maxY) return null
        } else {
            val tMin = (minY - start.y) / dy
            val tMax = (maxY - start.y) / dy
            t0 = max(t0, min(tMin, tMax))
            t1 = min(t1, max(tMin, tMax))
        }

        if (t1 < t0) return null

        return Segment(
            Point(start.x + t0 * dx, start.y + t0 * dy),
            Point(start.x + t1 * dx, start.y + t1 * dy)
        )
    }

    private fun clipSegmentToRect(p1: Point, p2: Point, minX: Double, minY: Double, maxX: Double, maxY: Double): Segment? {
        if (max(p1.x, p2.x) < minX || min(p1.x, p2.x) > maxX || max(p1.y, p2.y) < minY || min(p1.y, p2.y) > maxY) return null
        
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val seg = clipLineToRect(p1, dx, dy, minX, minY, maxX, maxY) ?: return null
        
        // Clamp to 0..1 relative to original segment length
        // clipLineToRect returns points on infinite line. We need to clamp to p1..p2.
        
        val minXSeg = min(p1.x, p2.x)
        val maxXSeg = max(p1.x, p2.x)
        val minYSeg = min(p1.y, p2.y)
        val maxYSeg = max(p1.y, p2.y)
        
        val cx1 = max(minXSeg, min(maxXSeg, seg.p1.x))
        val cy1 = max(minYSeg, min(maxYSeg, seg.p1.y))
        val cx2 = max(minXSeg, min(maxXSeg, seg.p2.x))
        val cy2 = max(minYSeg, min(maxYSeg, seg.p2.y))
        
        if (abs(cx1 - cx2) < 1e-9 && abs(cy1 - cy2) < 1e-9) return null
        
        return Segment(Point(cx1, cy1), Point(cx2, cy2))
    }
}

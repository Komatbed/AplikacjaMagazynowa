package com.example.warehouse.util

import com.example.warehouse.model.*
import kotlin.math.tan
import kotlin.math.PI

object MuntinCalculatorV2 {

    data class Topology(
        val verticalMuntins: List<MuntinNode>,
        val horizontalMuntins: List<MuntinNode>,
        val intersections: List<Intersection>
    )

    data class Intersection(
        val vertical: MuntinNode,
        val horizontal: MuntinNode,
        val type: IntersectionType
    )

    enum class IntersectionType {
        VERTICAL_CONTINUOUS, // Vertical runs through, Horizontal is cut
        HORIZONTAL_CONTINUOUS // Horizontal runs through, Vertical is cut
    }

    /**
     * Main calculation pipeline.
     */
    fun calculate(
        sashWidthMm: Int,
        sashHeightMm: Int,
        sashProfile: SashProfileV2,
        beadProfile: BeadProfileV2,
        muntinProfile: MuntinProfileV2,
        verticalPositions: List<Double>, // Relative positions or absolute from edge? Let's assume absolute from left edge of sash
        horizontalPositions: List<Double>, // Absolute from top edge of sash
        settings: V2GlobalSettings,
        defaultIntersectionRule: IntersectionType = IntersectionType.VERTICAL_CONTINUOUS
    ): List<CutItemV2> {
        
        // Let's stick to: Usable = Sash - 2*SashHeight - 2*BeadHeight.
        
        val openingWidth = sashWidthMm - (2 * sashProfile.heightMm) - (2 * beadProfile.heightMm)
        val openingHeight = sashHeightMm - (2 * sashProfile.heightMm) - (2 * beadProfile.heightMm)
        
        // 2. Build Topology
        // We accept positions relative to the SASH EDGE (as per prompt "wymiar od zewnętrznej krawędzi skrzydła").
        // We need to convert these to positions within the OPENING to check for validity?
        // Or just map them.
        
        // Offset to opening start: SashHeight + BeadHeight
        val offsetX = sashProfile.heightMm + beadProfile.heightMm
        val offsetY = sashProfile.heightMm + beadProfile.heightMm
        
        // Filter positions that are actually inside the opening
        val validVerticals = verticalPositions.filter { it > offsetX && it < (sashWidthMm - offsetX) }.sorted()
        val validHorizontals = horizontalPositions.filter { it > offsetY && it < (sashHeightMm - offsetY) }.sorted()
        
        val vNodes = validVerticals.mapIndexed { index, pos -> 
            MuntinNode("V${index + 1}", Axis.VERTICAL, pos, defaultIntersectionRule == IntersectionType.VERTICAL_CONTINUOUS)
        }
        val hNodes = validHorizontals.mapIndexed { index, pos -> 
            MuntinNode("H${index + 1}", Axis.HORIZONTAL, pos, defaultIntersectionRule == IntersectionType.HORIZONTAL_CONTINUOUS)
        }
        
        // 3. Resolve Joints & Generate Cut Items
        val cuts = mutableListOf<CutItemV2>()
        
        // --- Process Vertical Muntins ---
        // For each vertical line, find intersections with horizontal lines
        vNodes.forEach { vNode ->
            // Find all horizontal lines that intersect this vertical line
            // For a rectangular grid, all horizontals intersect all verticals.
            
            // Segments:
            // If Vertical is Continuous: It's one long bar (or multiple if we had blocking objects, but here it's just grid).
            // Length = Opening Height.
            // It touches Top Bead and Bottom Bead.
            
            // If Vertical is NOT Continuous (Cut): It is cut into segments between horizontals.
            
            if (defaultIntersectionRule == IntersectionType.VERTICAL_CONTINUOUS) {
                // Continuous Vertical
                // Length = Opening Height
                // Apply clearances: Top and Bottom
                val rawLen = openingHeight.toDouble()
                val finalLen = rawLen - (2 * settings.assemblyClearanceMm) + settings.sawCorrectionMm + settings.windowCorrectionMm
                
                cuts.add(CutItemV2(
                    sashNo = 1,
                    muntinNo = cuts.size + 1,
                    axis = Axis.VERTICAL,
                    lengthMm = kotlin.math.round(finalLen),
                    leftAngleDeg = 90.0,
                    rightAngleDeg = 90.0,
                    profileName = muntinProfile.profileNo,
                    description = "Pion ${vNode.id} (Cały)",
                    notes = "Pos: ${vNode.positionMm}"
                ))
            } else {
                // Cut Vertical (Horizontal is continuous)
                // Segments defined by horizontal lines.
                // Sorted horizontals: H1, H2, H3...
                // Segments: Top-H1, H1-H2, H2-H3, H3-Bottom.
                
                val intersections = validHorizontals
                val boundaries = listOf(offsetY.toDouble()) + intersections + listOf(sashHeightMm - offsetY.toDouble())
                
                for (i in 0 until boundaries.size - 1) {
                    val start = boundaries[i]
                    val end = boundaries[i+1]
                    
                    // Raw distance center-to-center (or edge-to-center)
                    val dist = end - start
                    
                    // Deductions:
                    // If 'start' is Top Bead (i==0): deduct clearance.
                    // If 'start' is a Muntin: deduct half muntin width + clearance.
                    
                    val startDeduction = if (i == 0) settings.assemblyClearanceMm else (muntinProfile.widthMm / 2.0 + settings.assemblyClearanceMm)
                    val endDeduction = if (i == boundaries.size - 2) settings.assemblyClearanceMm else (muntinProfile.widthMm / 2.0 + settings.assemblyClearanceMm)
                    
                    val len = dist - startDeduction - endDeduction + settings.sawCorrectionMm // Window correction maybe only for full bars? Assuming saw correction per cut.
                    
                    cuts.add(CutItemV2(
                        sashNo = 1,
                        muntinNo = cuts.size + 1,
                        axis = Axis.VERTICAL,
                        lengthMm = kotlin.math.round(len),
                        leftAngleDeg = 90.0,
                        rightAngleDeg = 90.0,
                        profileName = muntinProfile.profileNo,
                        description = "Pion ${vNode.id} - Seg ${i+1}",
                        notes = "Między ${if(i==0) "Góra" else "H$i"} a ${if(i==boundaries.size-2) "Dół" else "H${i+1}"}"
                    ))
                }
            }
        }
        
        // --- Process Horizontal Muntins ---
        hNodes.forEach { hNode ->
             if (defaultIntersectionRule == IntersectionType.HORIZONTAL_CONTINUOUS) {
                // Continuous Horizontal
                val rawLen = openingWidth.toDouble()
                val finalLen = rawLen - (2 * settings.assemblyClearanceMm) + settings.sawCorrectionMm + settings.windowCorrectionMm
                
                cuts.add(CutItemV2(
                    sashNo = 1,
                    muntinNo = cuts.size + 1,
                    axis = Axis.HORIZONTAL,
                    lengthMm = kotlin.math.round(finalLen),
                    leftAngleDeg = 90.0,
                    rightAngleDeg = 90.0,
                    profileName = muntinProfile.profileNo,
                    description = "Poziom ${hNode.id} (Cały)",
                    notes = "Pos: ${hNode.positionMm}"
                ))
            } else {
                // Cut Horizontal (Vertical is continuous)
                val intersections = validVerticals
                val boundaries = listOf(offsetX.toDouble()) + intersections + listOf(sashWidthMm - offsetX.toDouble())
                
                for (i in 0 until boundaries.size - 1) {
                    val start = boundaries[i]
                    val end = boundaries[i+1]
                    
                    val dist = end - start
                    
                    val startDeduction = if (i == 0) settings.assemblyClearanceMm else (muntinProfile.widthMm / 2.0 + settings.assemblyClearanceMm)
                    val endDeduction = if (i == boundaries.size - 2) settings.assemblyClearanceMm else (muntinProfile.widthMm / 2.0 + settings.assemblyClearanceMm)
                    
                    val len = dist - startDeduction - endDeduction + settings.sawCorrectionMm
                    
                    cuts.add(CutItemV2(
                        sashNo = 1,
                        muntinNo = cuts.size + 1,
                        axis = Axis.HORIZONTAL,
                        lengthMm = kotlin.math.round(len),
                        leftAngleDeg = 90.0,
                        rightAngleDeg = 90.0,
                        profileName = muntinProfile.profileNo,
                        description = "Poziom ${hNode.id} - Seg ${i+1}",
                        notes = "Między ${if(i==0) "Lewa" else "V$i"} a ${if(i==boundaries.size-2) "Prawa" else "V${i+1}"}"
                    ))
                }
            }
        }
        
        return cuts
    }
    
    // Helper to generate mounting marks
    fun generateMountingMarks(
        verticalPositions: List<Double>,
        horizontalPositions: List<Double>
    ): List<String> {
        val marks = mutableListOf<String>()
        verticalPositions.forEachIndexed { i, pos ->
            marks.add("Pion ${i+1} (V${i+1}): Oś = ${"%.1f".format(pos)} mm od lewej krawędzi zewn.")
        }
        horizontalPositions.forEachIndexed { i, pos ->
            marks.add("Poziom ${i+1} (H${i+1}): Oś = ${"%.1f".format(pos)} mm od górnej krawędzi zewn.")
        }
        return marks
    }
}

from typing import List, Dict, Optional, Tuple
from app.models.schema import OptimizationRequest, OptimizationResponse, OptimizedCut, WasteItem, OrderItem
import uuid

def optimize_batch_cutting(request: OptimizationRequest) -> OptimizationResponse:
    # 1. Group Orders by (Profile, Color)
    grouped_orders = {}
    for order in request.orders:
        key = (order.profile_code, order.color)
        if key not in grouped_orders:
            grouped_orders[key] = []
        grouped_orders[key].append(order)

    # 2. Prepare Waste Pool (Mutable)
    # Map: (Profile, Color) -> List[WasteItem]
    waste_pool: Dict[tuple, List[WasteItem]] = {}
    for waste in request.available_waste:
        key = (waste.profile_code, waste.color)
        if key not in waste_pool:
            waste_pool[key] = []
        waste_pool[key].append(waste)

    cuts: List[OptimizedCut] = []
    total_new_bars = 0
    total_scrap = 0
    waste_used_count = 0
    
    # 3. Process each group
    for key, orders in grouped_orders.items():
        profile_code, color = key
        
        # Sort orders: Priority (High to Low), then Length (Long to Short)
        # Longest items are hardest to fit, so place them first.
        orders.sort(key=lambda x: (-x.priority, -x.required_length_mm))
        
        # Get relevant waste
        available_waste = waste_pool.get(key, [])
        # Sort waste by length (Ascending) - Best Fit strategy
        available_waste.sort(key=lambda x: x.length_mm)
        
        # Track "Open" New Bars (Virtual waste created during this batch)
        # These are treated as high priority to use up immediately
        virtual_waste: List[WasteItem] = []

        for order in orders:
            matched_cut = None
            
            # --- STRATEGY A: Try Virtual Waste (Remnants from new bars in this batch) ---
            best_virtual_idx = -1
            min_remnant_v = float('inf')
            
            for i, item in enumerate(virtual_waste):
                if item.length_mm >= order.required_length_mm:
                    remnant = item.length_mm - order.required_length_mm
                    # We want to minimize remnant to pack tightly
                    if remnant < min_remnant_v:
                        min_remnant_v = remnant
                        best_virtual_idx = i
            
            if best_virtual_idx != -1:
                # Use Virtual Waste
                source_item = virtual_waste.pop(best_virtual_idx)
                remnant = source_item.length_mm - order.required_length_mm
                
                matched_cut = OptimizedCut(
                    order_id=order.order_id,
                    source_type="NEW_BAR_REMNANT", # Technically from a new bar opened in this batch
                    source_id=None, 
                    waste_created_mm=remnant,
                    is_scrap=(remnant < request.scrap_threshold_mm and remnant > 0)
                )
                
                # If remnant is usable, add back to virtual waste
                if remnant > 0:
                     if remnant >= request.scrap_threshold_mm:
                         # Keep using this bar if possible
                         new_virtual = WasteItem(
                             id=f"virt-{uuid.uuid4()}",
                             location="PRODUCTION_LINE",
                             length_mm=remnant,
                             profile_code=profile_code,
                             color=color
                         )
                         virtual_waste.append(new_virtual)
                     else:
                         total_scrap += remnant

            else:
                # --- STRATEGY B: Try Physical Waste (Warehouse) ---
                best_waste_idx = -1
                min_remnant_w = float('inf')
                best_is_bad_offcut = True # Initialize as worst case
                
                for i, item in enumerate(available_waste):
                    if item.length_mm >= order.required_length_mm:
                        remnant = item.length_mm - order.required_length_mm
                        
                        # Definition of "Bad Offcut":
                        # A piece that is too short to be usable, but too long to be just scrap (wasteful).
                        # Or strictly: usable > remnant > scrap_threshold (if we consider that range 'bad')
                        # Usually: 
                        #  - Scrap: 0 to scrap_threshold
                        #  - Usable: > min_usable_offcut
                        #  - Bad/Wasteful: scrap_threshold to min_usable_offcut
                        
                        is_bad = (remnant > request.scrap_threshold_mm and remnant < request.min_usable_offcut_mm)
                        
                        # We prefer:
                        # 1. Not Bad (Usable or Scrap)
                        # 2. Smallest remnant (Best Fit)
                        
                        if not is_bad:
                            if best_is_bad_offcut:
                                # Found our first "Good" match, take it
                                best_waste_idx = i
                                min_remnant_w = remnant
                                best_is_bad_offcut = False
                            else:
                                # Already have a good match, check if this is tighter
                                if remnant < min_remnant_w:
                                    best_waste_idx = i
                                    min_remnant_w = remnant
                        
                        else:
                            # It is bad. Only take it if we have no "Good" matches yet and it's the tightest bad one?
                            # Or maybe avoid bad matches entirely if possible?
                            # Let's say we avoid bad matches from WASTE unless forced? 
                            # Actually, for existing waste, getting rid of it is good.
                            # But creating a NEW bad offcut from existing waste is bad.
                            if best_is_bad_offcut:
                                if remnant < min_remnant_w:
                                    best_waste_idx = i
                                    min_remnant_w = remnant
                
                if best_waste_idx != -1:
                    # Use Physical Waste
                    source_item = available_waste.pop(best_waste_idx)
                    remnant = source_item.length_mm - order.required_length_mm
                    
                    matched_cut = OptimizedCut(
                        order_id=order.order_id,
                        source_type="WASTE",
                        source_id=source_item.id,
                        waste_created_mm=remnant,
                        is_scrap=(remnant < request.scrap_threshold_mm and remnant > 0)
                    )
                    waste_used_count += 1
                    
                    if remnant > 0:
                        if remnant < request.scrap_threshold_mm:
                            total_scrap += remnant
                        # If it's a large remnant, technically it goes back to stock, 
                        # but in this batch logic we assume it's output.
                        # We don't add it to virtual_waste usually because it's already "cut".
                        # But for optimization, we COULD treat it as available for next cuts?
                        # Let's assume physical waste is "One Shot" for simplicity in this version,
                        # or add it to virtual if you want to multi-cut a long waste piece.
                        # Let's add to virtual to allow multi-cut of long waste!
                        elif remnant >= request.min_usable_offcut_mm:
                             new_virtual = WasteItem(
                                 id=f"rem-{source_item.id}",
                                 location="PRODUCTION_LINE",
                                 length_mm=remnant,
                                 profile_code=profile_code,
                                 color=color
                             )
                             virtual_waste.append(new_virtual)

                else:
                    # --- STRATEGY C: Open New Bar ---
                    total_new_bars += 1
                    remnant = request.full_bar_length_mm - order.required_length_mm
                    
                    matched_cut = OptimizedCut(
                        order_id=order.order_id,
                        source_type="NEW_BAR",
                        source_id=None,
                        waste_created_mm=remnant,
                        is_scrap=(remnant < request.scrap_threshold_mm and remnant > 0)
                    )
                    
                    if remnant > 0:
                         if remnant >= request.scrap_threshold_mm:
                             # Add to virtual waste to be used by subsequent orders
                             new_virtual = WasteItem(
                                 id=f"newbar-{total_new_bars}",
                                 location="PRODUCTION_LINE",
                                 length_mm=remnant,
                                 profile_code=profile_code,
                                 color=color
                             )
                             virtual_waste.append(new_virtual)
                         else:
                             total_scrap += remnant

            if matched_cut:
                cuts.append(matched_cut)
            else:
                # Should not happen with New Bar strategy unless order > full_bar_length
                pass

    return OptimizationResponse(
        cuts=cuts,
        total_waste_used_count=waste_used_count,
        total_new_bars_count=total_new_bars,
        total_scrap_generated_mm=total_scrap
    )

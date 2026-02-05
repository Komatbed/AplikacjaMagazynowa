from app.algorithm.waste_optimizer import optimize_batch_cutting
from app.models.schema import OptimizationRequest, OrderItem, WasteItem

def test_optimization():
    # 1. Setup Data
    orders = [
        OrderItem(order_id="o1", profile_code="P1", color="White", required_length_mm=2000, priority=5),
        OrderItem(order_id="o2", profile_code="P1", color="White", required_length_mm=1500, priority=1),
        OrderItem(order_id="o3", profile_code="P1", color="White", required_length_mm=2500, priority=3),
    ]
    
    waste = [
        WasteItem(id="w1", location="A1", length_mm=2100, profile_code="P1", color="White"), # Good fit for o1 (100mm scrap)
        WasteItem(id="w2", location="A2", length_mm=1000, profile_code="P1", color="White"), # Too short
    ]
    
    request = OptimizationRequest(
        orders=orders,
        available_waste=waste,
        full_bar_length_mm=6500,
        min_usable_offcut_mm=500,
        scrap_threshold_mm=200
    )
    
    # 2. Run Optimization
    response = optimize_batch_cutting(request)
    
    # 3. Print Results
    print(f"Total New Bars: {response.total_new_bars_count}")
    print(f"Total Waste Used: {response.total_waste_used_count}")
    print(f"Total Scrap: {response.total_scrap_generated_mm}")
    
    for cut in response.cuts:
        print(f"Order {cut.order_id}: Source {cut.source_type} ({cut.source_id}), Waste: {cut.waste_created_mm}mm (Scrap: {cut.is_scrap})")

if __name__ == "__main__":
    test_optimization()

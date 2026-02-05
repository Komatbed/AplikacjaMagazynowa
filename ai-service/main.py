from fastapi import FastAPI, HTTPException
from app.models.schema import (
    ShortagePredictionRequest, ShortagePredictionResponse,
    WasteRecommendationRequest, WasteRecommendationResponse,
    OptimizationRequest, OptimizationResponse
)
from app.algorithm.waste_optimizer import optimize_batch_cutting

app = FastAPI(title="Warehouse Intelligence API", version="0.1.0")

@app.get("/")
def read_root():
    return {"status": "online", "service": "Warehouse Intelligence"}

@app.post("/predict/shortage", response_model=ShortagePredictionResponse)
def predict_shortage(request: ShortagePredictionRequest):
    # Dummy implementation for now
    # In real world: Load Prophet model, predict based on history
    import random
    from datetime import date, timedelta
    
    days_left = int(request.current_stock_mm / 1000) # Fake logic
    
    return ShortagePredictionResponse(
        profile_code=request.profile_code,
        predicted_stockout_date=date.today() + timedelta(days=days_left),
        risk_level="HIGH" if days_left < 7 else "LOW",
        daily_usage_trend=150.5
    )

@app.post("/recommend/waste", response_model=WasteRecommendationResponse)
def recommend_waste(request: WasteRecommendationRequest):
    # Simple logic: Find best fit from provided list
    best_item = None
    min_cutoff = float('inf')
    
    for item in request.available_waste:
        if (item.profile_code == request.profile_code and 
            item.color == request.color and 
            item.length_mm >= request.required_length_mm):
            
            cutoff = item.length_mm - request.required_length_mm
            # Prefer smaller cutoff (best fit)
            if cutoff < min_cutoff:
                min_cutoff = cutoff
                best_item = item
                
    if best_item:
        return WasteRecommendationResponse(
            recommended_item_id=best_item.id,
            waste_length_mm=best_item.length_mm,
            cutoff_waste_mm=int(min_cutoff),
            score=0.95, # Placeholder
            message=f"Użyj odpadu z lokalizacji {best_item.location}"
        )
    else:
        return WasteRecommendationResponse(
            recommended_item_id=None,
            waste_length_mm=0,
            cutoff_waste_mm=0,
            score=0.0,
            message="Brak pasującego odpadu. Użyj nowej sztangi."
        )

@app.post("/optimize/batch", response_model=OptimizationResponse)
def optimize_batch(request: OptimizationRequest):
    return optimize_batch_cutting(request)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

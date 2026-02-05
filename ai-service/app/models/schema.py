from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import date

# --- Shared Models ---

class ProfileSpec(BaseModel):
    profile_code: str
    color: str
    is_core_colored: bool = False

# --- Shortage Prediction Models ---

class ShortagePredictionRequest(BaseModel):
    profile_code: str
    current_stock_mm: int
    history_days: int = 90

class ShortagePredictionResponse(BaseModel):
    profile_code: str
    predicted_stockout_date: Optional[date]
    risk_level: str  # LOW, MEDIUM, HIGH, CRITICAL
    daily_usage_trend: float

# --- Waste Recommendation Models ---

class WasteItem(BaseModel):
    id: str
    location: str
    length_mm: int
    profile_code: str
    color: str

class WasteRecommendationRequest(BaseModel):
    profile_code: str
    required_length_mm: int
    color: str
    available_waste: List[WasteItem]

class WasteRecommendationResponse(BaseModel):
    recommended_item_id: Optional[str]
    waste_length_mm: int
    cutoff_waste_mm: int
    score: float
    message: str

# --- Advanced Optimization Models ---

class OrderItem(BaseModel):
    order_id: str
    profile_code: str
    color: str
    required_length_mm: int
    priority: int = 1  # 1 (Low) to 5 (High)

class OptimizationRequest(BaseModel):
    orders: List[OrderItem]
    available_waste: List[WasteItem]
    full_bar_length_mm: int = 6500
    min_usable_offcut_mm: int = 500  # Minimum length to keep as waste
    scrap_threshold_mm: int = 200    # Below this is trash

class OptimizedCut(BaseModel):
    order_id: str
    source_type: str  # WASTE or NEW_BAR
    source_id: Optional[str]  # ID of waste item or None for new bar
    waste_created_mm: int
    is_scrap: bool

class OptimizationResponse(BaseModel):
    cuts: List[OptimizedCut]
    total_waste_used_count: int
    total_new_bars_count: int
    total_scrap_generated_mm: int

# Warehouse Intelligence Service (AI Module)

Usługa oparta na Python/FastAPI, dostarczająca zaawansowaną analitykę i optymalizację dla systemu magazynowego PVC.

## Funkcjonalności

1.  **Optymalizacja Cięcia (Waste Optimization)**:
    *   Algorytm "Best Fit" z priorytetami.
    *   Obsługa odpadów wirtualnych (cięcie z tej samej nowej sztangi w jednej partii).
    *   Minimalizacja odpadów nieużytecznych ("Bad Offcuts").

2.  **Predykcja Braków (Shortage Prediction)**:
    *   Analiza historycznego zużycia.
    *   Prognozowanie daty wyczerpania zapasów (Prophet).

3.  **Rekomendacja Odpadów**:
    *   Sugestie użycia konkretnych ścinków dla pojedynczych zleceń.

## Uruchomienie

### Lokalnie
```bash
pip install -r requirements.txt
uvicorn main:app --reload
```

### Docker
```bash
docker build -t warehouse-ai-service .
docker run -p 8000:8000 warehouse-ai-service
```

## API Endpointy

*   `POST /optimize/batch` - Optymalizacja listy zleceń.
*   `POST /predict/shortage` - Predykcja braków magazynowych.
*   `POST /recommend/waste` - Rekomendacja odpadu dla jednego profilu.

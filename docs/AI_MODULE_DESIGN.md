# Projekt Modułu AI - Warehouse Intelligence

## 1. Przegląd
Moduł "Warehouse Intelligence" to warstwa analityczno-predykcyjna systemu magazynowego, mająca na celu minimalizację odpadów PVC i optymalizację stanów magazynowych. Działa jako asystent kierownika produkcji oraz system rekomendacji dla pracowników.

## 2. Architektura
Moduł jest realizowany jako osobny mikroserwis (Python) komunikujący się z głównym Backendem (Spring Boot).

*   **Język/Framework**: Python 3.11 + FastAPI.
*   **Biblioteki ML**: Scikit-learn, Pandas, Prophet (do szeregów czasowych).
*   **Baza Danych**: Dostęp do PostgreSQL (Read-Replica) lub pobieranie danych przez API Backendu (Batch).
*   **Komunikacja**: REST API (JSON).

## 3. Funkcjonalności i Modele

### A. Analiza Strat i Ranking (Loss Analysis)
Identyfikacja profili generujących najwięcej odpadu nieużytecznego.

*   **Cel**: Wskazanie "Top 10" profili/kolorów generujących straty finansowe.
*   **Dane wejściowe**:
    *   `operation_logs` (typ: `WASTE_CREATED`, `TAKEN`).
    *   `inventory_items` (długość, ilość).
*   **Algorytm**:
    *   **KPI: Współczynnik Odpadu (Waste Ratio)** = `Suma Długości Odpadu` / `Suma Długości Pobranych Pełnych Sztang`.
    *   **Analiza ABC**: Klasyfikacja profili na grupy A (największy obrót/strata), B, C.
*   **Wynik**: Raport JSON dla Dashboardu Kierownika (Ranking profili wg % strat).

### B. Predykcja Ryzyka Braków (Shortage Prediction)
Przewidywanie, kiedy skończy się zapas danego profilu, uwzględniając historię pobrań.

*   **Cel**: Alert "Za 3 dni braknie profilu X, jeśli tempo produkcji się utrzyma".
*   **Dane wejściowe**:
    *   Historia pobrań (`operation_logs` gdzie `type=TAKEN`) z ostatnich 90 dni.
    *   Aktualny stan magazynowy (`inventory_items`).
    *   Czas dostawy (parametr stały konfigurowalny, np. 7 dni).
*   **Model ML**:
    *   **Prophet (Facebook)** lub **ARIMA**: Analiza szeregów czasowych (Time Series Forecasting).
    *   Model uczy się sezonowości (np. więcej pobrań w piątki) i trendu rosnącego.
*   **Trening**: Batchowy, raz w tygodniu (w nocy).
*   **Wynik**: Data wyczerpania zapasu (Stockout Date) dla każdego profilu.

### C. Rekomendacja Użycia Odpadu (Waste Optimization)
Inteligentne sugerowanie użycia istniejącego odpadu zamiast cięcia nowej sztangi.

*   **Cel**: Pracownik potrzebuje odcinka 1200mm. System mówi: "Weź odpad 1250mm z lokalizacji 01B" zamiast "Weź nową sztangę 6500mm".
*   **Dane wejściowe**:
    *   Żądana długość (Input z produkcji).
    *   Lista dostępnych odpadów (`inventory_items` gdzie `status=AVAILABLE` i `is_full_bar=FALSE`).
*   **Algorytm (Heurystyka + Ranking)**:
    1.  Filtruj odpady: `Długość Odpadu >= Żądana Długość`.
    2.  Oblicz "Resztę" (Cut-off): `Reszta = Długość Odpadu - Żądana Długość`.
    3.  **Score**: Im mniejsza Reszta (bliżej 0), tym lepiej. Preferuj Resztę < 50mm (złom) lub Resztę > 500mm (użyteczny odpad), unikaj "śmieciowych" resztek (np. 150mm).
*   **Wynik**: Lista posortowanych lokalizacji odpadów do pobrania.

## 4. Cechy (Features) i Inżynieria Danych

| Nazwa Cechy | Źródło | Opis |
| :--- | :--- | :--- |
| `daily_usage_mm` | `operation_logs` | Suma pobranych mm danego dnia dla profilu. |
| `waste_generated_mm` | `operation_logs` | Suma mm odpadu wytworzonego danego dnia. |
| `days_since_last_movement` | `inventory_items` | `NOW() - updated_at` (wykrywanie zalegających towarów). |
| `correction_frequency` | `operation_logs` | Liczba operacji `CORRECTION` / Całkowita liczba operacji (wskaźnik błędów ludzkich). |
| `seasonality_index` | `operation_logs` | Dzień tygodnia / Miesiąc (cecha kategoryczna dla modelu). |

## 5. Integracja z Backendem (API Contract)

### Endpoint: `/predict/shortage`
*   **Metoda**: `GET`
*   **Response**:
    ```json
    [
      {
        "profile_code": "504010",
        "current_stock_mm": 150000,
        "predicted_daily_usage_mm": 5000,
        "days_until_stockout": 30,
        "risk_level": "LOW"
      },
      {
        "profile_code": "504020",
        "days_until_stockout": 2,
        "risk_level": "CRITICAL"
      }
    ]
    ```

### Endpoint: `/recommend/waste`
*   **Metoda**: `POST`
*   **Request**: `{"profile_code": "504010", "required_length_mm": 1200, "color": "A01"}`
*   **Response**:
    ```json
    {
      "recommended_item_id": "uuid-...",
      "location": "01B",
      "waste_length_mm": 1250,
      "cutoff_waste_mm": 50,
      "score": 0.98
    }
    ```

## 6. Plan Wdrożenia (Roadmap)
1.  **Faza 1 (Reguły)**: Implementacja algorytmu rekomendacji odpadów (C) bezpośrednio w Kotlinie (bez Pythona). To daje największy zysk natychmiast.
2.  **Faza 2 (Analiza)**: Skrypt Python generujący raporty statyczne (A) z bazy danych.
3.  **Faza 3 (ML)**: Wdrożenie modelu Prophet (B) jako mikroserwisu, gdy zbierzemy min. 3 miesiące danych historycznych.

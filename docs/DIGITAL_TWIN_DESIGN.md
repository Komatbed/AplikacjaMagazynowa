# Cyfrowy Model Hali (Digital Twin Design)

## 1. Koncepcja Wizualna (UI)

### Widok Główny: Mapa Cieplna (Heatmap)
Ekran przedstawia rzut z góry hali magazynowej w układzie siatki (Grid).
*   **Wiersze (Rows)**: Numery 1-25 (oś Y).
*   **Palety (Palettes)**: Litery A, B, C (oś X).
*   Każda komórka siatki reprezentuje jedną fizyczną lokalizację (np. `12B`).

### Kodowanie Kolorami (Tryby Widoku)
Użytkownik może przełączać warstwy informacyjne:

1.  **Tryb "Zapełnienie" (Occupancy)**:
    *   🟩 **Zielony**: Pusta lub prawie pusta (< 20%).
    *   🟨 **Żółty**: Umiarkowane obłożenie (20-80%).
    *   🟥 **Czerwony**: Pełna (> 80%).
    *   ⚫ **Czarny**: Przeciążenie (Overload > 100% wagi/ilości).

2.  **Tryb "Kolory Profili" (Content)**:
    *   Komórka przyjmuje dominujący kolor profili składowanych na niej (np. Biały, Brązowy, Antracyt).
    *   Jeśli mix kolorów -> Paski lub Szary.

3.  **Tryb "Alerty"**:
    *   Migające na czerwono komórki wymagające uwagi (np. "Leżak magazynowy > 90 dni").

### Interakcja (Tooltips & Drill-down)
*   **Hover/Tap**: Dymek z szybkim info: "Lokalizacja 12B: 150 sztuk, Profil 504010".
*   **Kliknięcie**: Otwiera panel boczny (Sidebar) ze szczegółami:
    *   Lista wszystkich paczek na palecie.
    *   Historia operacji (kto ostatnio brał).
    *   Przycisk "Drukuj Etykietę Lokalizacyjną".

## 2. Model Danych (Backend Extension)

### Tabela `locations` (Rozszerzenie)
Istniejąca tabela `locations` musi zostać wzbogacona o metadane wizualne/fizyczne lub obliczane dynamicznie.

```sql
-- Dodatkowe pola (opcjonalnie, lub w osobnej tabeli location_stats)
ALTER TABLE locations ADD COLUMN max_capacity_kg INT DEFAULT 1000;
ALTER TABLE locations ADD COLUMN current_weight_kg INT DEFAULT 0; -- Cache
ALTER TABLE locations ADD COLUMN last_audit_date TIMESTAMP;
```

### Agregat DTO (Data Transfer Object)
Obiekt przesyłany do Frontendu (React/Android) w celu narysowania mapy.

```json
{
  "grid_dimensions": {"rows": 25, "cols": 3},
  "cells": [
    {
      "label": "01A",
      "row": 1,
      "col": 1,
      "occupancy_percentage": 45,
      "dominant_color_hex": "#FFFFFF",
      "alert_level": "NONE", // NONE, WARNING, CRITICAL
      "items_count": 12
    },
    {
      "label": "01B",
      "row": 1,
      "col": 2,
      "occupancy_percentage": 110,
      "dominant_color_hex": "#333333",
      "alert_level": "CRITICAL", // Overload
      "items_count": 50
    }
  ]
}
```

## 3. Aktualizacja w Czasie Rzeczywistym (Real-time)

### Mechanizm
Zamiast ciągłego odpytywania (Polling), użyjemy **WebSockets** (STOMP over WebSocket) w Spring Boot.

1.  **Zdarzenie**: Magazynier odkłada towar (Android -> API `POST /inventory/waste`).
2.  **Backend**:
    *   Aktualizuje bazę danych.
    *   Przelicza nowe obłożenie dla lokalizacji `01B`.
    *   Publikuje wiadomość na temat `/topic/warehouse/map`.
3.  **Frontend (Dashboard Kierownika)**:
    *   Nasłuchuje na `/topic/warehouse/map`.
    *   Otrzymuje JSON: `{"label": "01B", "occupancy": 55, ...}`.
    *   Odświeża tylko jedną komórkę na mapie (React State update).

## 4. Integracja (Przepływ)

1.  **Inicjalizacja**:
    *   Aplikacja pobiera pełny stan mapy (`GET /api/warehouse/map`).
2.  **Operacje Magazynowe**:
    *   Każde "Pobranie" i "Odłożenie" zmienia stan licznika w bazie.
3.  **Alerty Przeciążenia**:
    *   Backend sprawdza: `IF current_quantity > max_capacity THEN alert = CRITICAL`.
    *   Alert jest wysyłany do Kierownika (Push/Socket) i Magazyniera (Toast w Androidzie przy próbie odłożenia).

## 5. Role Użytkowników

### Kierownik (Web Dashboard)
*   **Widok**: Pełna mapa na dużym ekranie.
*   **Akcje**: Zmiana limitów palet, blokowanie lokalizacji (np. "Paleta uszkodzona"), analiza historyczna.

### Magazynier (Android Tablet)
*   **Widok**: Uproszczona "Minimapa" przy wyborze lokalizacji.
*   **Akcje**: Tylko podgląd "Gdzie jest wolne miejsce?" podczas odkładania odpadu. System sam sugeruje: "Jedź do 05C (Wolne: 80%)".

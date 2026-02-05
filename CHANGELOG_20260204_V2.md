# Dokumentacja Zmian Systemowych (v2.0)
**Data:** 2026-02-04
**Autor:** Assistant AI

## 1. Rozszerzona Konfiguracja Systemu
Zaktualizowano skrypt `setup_config.py` o nowe sekcje konfiguracyjne:
*   **Optymalizacja Cięcia:** Wybór algorytmu (Max Yield / Prefer Waste), rezerwacja odpadów (np. 1200mm).
*   **Szprosy Wiedeńskie:** Definicja luzów montażowych, grubości taśm.
*   **Infrastruktura:** Rozszerzona o porty drukarek i ustawienia sieciowe.

## 2. Zarządzanie Danymi Bazowymi
Stworzono narzędzie `manage_data.py` (Python) umożliwiające szybkie wprowadzanie danych do bazy:
*   **Kolory:** Obsługa `numerZKolornika`, `numerWewnętrznyVeka`.
*   **Profile:** Obsługa szczegółowych wymiarów (wysokość, szerokość, kąty listew) wymaganych do obliczeń.

## 3. Moduł Optymalizacji Cięcia (Backend)
Wdrożono nowy serwis optymalizacyjny (`OptimizationService`):
*   **Algorytm:** Best Fit Decreasing (Najlepsze dopasowanie malejąco).
*   **Logika:** 
    1. Sortuje wymagane elementy od najdłuższego.
    2. Przeszukuje dostępne odpady w magazynie.
    3. Jeśli brak odpadu, dobiera nową sztangę (domyślnie 6500mm).
    4. Minimalizuje odpad globalny.
*   **API:** `POST /api/v1/optimization/calculate` - przyjmuje listę elementów, zwraca plan cięcia.

## 4. Modele Danych (Entity Update)
Zaktualizowano tabele bazy danych (PostgreSQL):
*   `color_definitions`: Dodano kolumny `palette_code`, `veka_code`, `name`.
*   `profile_definitions`: Dodano kolumny wymiarowe `height_mm`, `width_mm`, `bead_height_mm`, `bead_angle`.

## 5. Kalkulator Szprosów (Wstępny)
Dodano klasę `MuntinCalculator.kt` w aplikacji mobilnej:
*   Zawiera bazową logikę obliczania długości szprosa wiedeńskiego na podstawie wymiarów skrzydła i kątów listew.
*   Uwzględnia luzy montażowe zdefiniowane w konfiguracji.

## 6. Ustawienia Aplikacji (Android)
*   Dodano przełącznik "Preferuj Rezerwację Odpadów" w ustawieniach.
*   Zaktualizowano `SettingsDataStore` o nową flagę.

---

## Instrukcja Aktualizacji
1. Uruchom `setup_config.py` aby wygenerować nowy plik `.env`.
2. Zrestartuj Backend (automatyczna migracja bazy danych doda nowe kolumny).
3. Użyj `manage_data.py` aby uzupełnić brakujące dane profili i kolorów.

# Plan Rozwoju Aplikacji Warehouse (Future Development Plan)

## 1. Propozycje Nowych Modułów Funkcjonalnych

### A. Moduł Raportowania i Analityki (Reporting & Analytics)
**Cel:** Umożliwienie kierownikom i brygadzistom analizy wydajności produkcji oraz zużycia materiałów.
**Funkcjonalności:**
- **Wykresy zużycia:** Wizualizacja ilości zużytych profili w czasie (dziennie/tygodniowo).
- **Analiza odpadów:** Raport procentowy odpadu użytecznego vs. bezużytecznego.
- **Eksport PDF/XLS:** Generowanie raportów zmianowych do plików PDF lub Excel.
- **Ranking wydajności:** Statystyki dla poszczególnych stanowisk cięcia (opcjonalnie).

### B. Moduł Powiadomień i Alertów (Notifications System)
**Cel:** Szybka reakcja na sytuacje krytyczne w magazynie i produkcji.
**Funkcjonalności:**
- **Niski stan magazynowy:** Powiadomienie Push/Email, gdy stan profilu spadnie poniżej minimum.
- **Błędy optymalizacji:** Alert, gdy plik .ct500txt zawiera błędy lub niemożliwe do wykonania cięcia.
- **Aktualizacje:** Informacja o nowej wersji aplikacji (wymuszenie aktualizacji).
- **Zadania:** Przypomnienia o zaplanowanej inwentaryzacji.

### C. Moduł Integracji ERP (External API Integration)
**Cel:** Automatyczna synchronizacja stanów magazynowych z głównym systemem ERP firmy.
**Funkcjonalności:**
- **REST API Client:** Dwukierunkowa komunikacja z systemem ERP (np. SAP, Symfonia, Comarch).
- **Pobieranie zamówień:** Automatyczne pobieranie plików produkcyjnych z serwera.
- **Synchronizacja stanów:** Aktualizacja stanów magazynowych w ERP po wykonaniu cięcia (zdejmowanie materiału).
- **Obsługa trybu Offline:** Kolejkowanie żądań w przypadku braku sieci (SyncAdapter/WorkManager).

---

## 2. Propozycje Usprawnień Technicznych

### A. Implementacja Zaawansowanego Cache’owania (Caching Strategy)
- **Problem:** Częste pobieranie tych samych danych konfiguracyjnych z bazy/sieci.
- **Rozwiązanie:** Wdrożenie `Store` (Dropbox) lub własnej warstwy Repository z polityką "Cache-First".
- **Technologia:** Room Database jako Single Source of Truth + in-memory cache dla słowników.

### B. Migracja na Mikroserwisy (Backend Evolution)
- **Problem:** Obecny backend (jeśli istnieje) lub przyszły monolit może być trudny w skalowaniu.
- **Rozwiązanie:** Wydzielenie usług: `AuthService` (logowanie), `InventoryService` (stany), `OptimizationService` (obliczenia).
- **Korzyść:** Niezależne skalowanie modułu optymalizacji (CPU intensive) od reszty systemu.

### C. Wdrożenie CI/CD (Continuous Integration/Deployment)
- **Problem:** Ręczne budowanie i testowanie aplikacji.
- **Rozwiązanie:** GitHub Actions lub GitLab CI.
- **Pipeline:**
  1. Linting (Ktlint/Detekt).
  2. Unit Tests (JUnit).
  3. UI Tests (Espresso/Compose Test).
  4. Build APK (Debug/Release).
  5. Upload to App Distribution (Firebase/Play Store Internal).

### D. Automatyzacja Testów Regresyjnych
- **Problem:** Ryzyko błędów przy zmianach w UI.
- **Rozwiązanie:** Rozbudowa testów E2E (End-to-End) przy użyciu Maestro lub Appium.
- **Zakres:** Automatyczne przejście ścieżki: Import pliku -> Optymalizacja -> Wynik -> Zapis do bazy.

### E. Refaktoring Warstwy Dostępu do Danych (Data Layer)
- **Problem:** `InventoryRepository` jest "God Object", miesza logikę biznesową z dostępem do danych.
- **Rozwiązanie:**
  - Podział na mniejsze repozytoria: `ProfileRepository`, `WasteRepository`, `ConfigRepository`.
  - Wprowadzenie wzorca `UseCase` (Clean Architecture) dla logiki biznesowej (np. `CalculateOptimizationUseCase`).
  - Użycie Hilt do wstrzykiwania zależności.

---

## 3. Harmonogram Rozwoju (Roadmap)

### Iteracja 1: Stabilizacja i Dług Techniczny (2 tygodnie)
- Refaktoring `InventoryRepository`.
- Konfiguracja CI/CD (GitHub Actions).
- Uzupełnienie testów jednostkowych do 80%.
- Poprawki UI (Material 3 migration complete).

### Iteracja 2: Nowe Funkcjonalności - Raportowanie (3 tygodnie)
- Projekt bazy danych dla historii operacji.
- Implementacja ekranu Statystyk (Wykresy - MPAndroidChart lub Vico).
- Eksport danych do CSV/PDF.

### Iteracja 3: Integracja i Powiadomienia (4 tygodnie)
- Stworzenie modułu `SyncManager` (WorkManager).
- Integracja z próbnym API ERP.
- Implementacja powiadomień lokalnych i Push (Firebase FCM).

### Iteracja 4: Bezpieczeństwo i Optymalizacja (2 tygodnie)
- Audyt bezpieczeństwa (SQLCipher, ProGuard/R8 rules).
- Profilowanie pamięci i CPU (Android Profiler).
- Optymalizacja algorytmu cięcia (cache wyników).

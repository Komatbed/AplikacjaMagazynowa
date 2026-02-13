# PEŁNY KONTEKST PROJEKTU: SYSTEM MAGAZYNOWY PVC

## 1. Tożsamość Projektu
**Nazwa:** Warehouse Management System (WMS) dla Produkcji PVC
**Cel:** Zarządzanie magazynem profili okiennych, optymalizacja cięcia, kalkulacja szprosów oraz integracja z maszynami produkcyjnymi.
**Główny Fokus (Obecny):** Rozbudowa modułu **Kalkulator Szprosów V2** w aplikacji mobilnej.

---

## 2. Stos Technologiczny (Tech Stack)

### A. Aplikacja Mobilna (Android)
*   **Język:** Kotlin
*   **UI:** Jetpack Compose (Material3)
*   **Architektura:** MVVM (Model-View-ViewModel)
*   **Baza Danych:** Room (SQLite) - wersja schematu: 9 (ostatnia migracja: dodanie tabel Muntin V2)
*   **Sieć:** Retrofit2 + OkHttp3
*   **Kluczowe Biblioteki:** ZXing (skanowanie kodów), Navigation Compose, Coroutines.

### B. Backend (Serwer API)
*   **Framework:** Spring Boot 3.2.0 (Kotlin)
*   **Baza Danych:** PostgreSQL 15+ (zarządzana przez Docker)
*   **Migracje:** Flyway
*   **Bezpieczeństwo:** Spring Security + JWT
*   **Monitoring:** Spring Actuator + Prometheus + Grafana

### C. Narzędzia Pomocnicze (Python)
*   **AI Service:** FastAPI (Python 3.11) - moduł przewidywania braków i optymalizacji odpadów.
*   **Config Wizard:** Skrypt `config_wizard.py` - generator plików konfiguracyjnych JSON (`profiles.json`, `colors.json`).

---

## 3. Architektura i Przepływ Danych

### Zarządzanie Konfiguracją (Nowość)
1.  **Inicjalizacja:** Użytkownik uruchamia `python config_wizard.py` -> generuje pliki JSON w `backend/src/main/resources/initial_data`.
2.  **Backend Load:** Endpoint `POST /api/v1/config/reload-defaults` wczytuje te pliki do bazy PostgreSQL (Hot Reload).
3.  **Android Sync:** Aplikacja mobilna wywołuje ten endpoint (przycisk "Wymuś Import"), a następnie pobiera zaktualizowane dane do lokalnej bazy Room.

### Kalkulator Szprosów V2 (Kluczowy Moduł)
*   **Logika:** Obliczenia geometryczne w `MuntinCalculatorV2` (prosty) i `MuntinCalculatorV2Angular` (skośny/łuki).
*   **UI:** Interaktywny Canvas (`MuntinScreenV2`), obsługa gestów (Tap to add/remove).
*   **Symetria:** Algorytm wymuszający równe odstępy między szprosami (Step-based division).
*   **Optymalizacja:** Wbudowany algorytm **1D Bin Packing** (`CuttingOptimizer.kt`) - dopasowuje odcinki do sztang 6000mm.

---

## 4. Szczegółowy Status Funkcjonalności

### ✅ ZROBIONE (Działające i Zweryfikowane)
1.  **Symetryczny Podział Szprosów**:
    *   Dodanie szprosu (przycisk/klik) automatycznie przelicza pozycje wszystkich istniejących szprosów w danej osi, aby zachować równe pola.
2.  **Zaokrąglanie Wymiarów**:
    *   Wszystkie wyniki cięcia (długości profili) są zaokrąglane do pełnych milimetrów (`kotlin.math.round`).
3.  **Optymalizacja Cięcia (Native Kotlin)**:
    *   Algorytm First Fit Decreasing zaimplementowany w `CuttingOptimizer.kt`.
    *   Uwzględnia szerokość rzazu piły (domyślnie 3.0mm, konfigurowalne).
4.  **Integracja "Config Wizard"**:
    *   Backend potrafi przeładować dane z plików bez restartu.
    *   Aplikacja mobilna posiada UI do wymuszenia tej operacji.
5.  **CRUD Profili w Aplikacji**:
    *   Możliwość dodawania/edycji/usuwania definicji profili bezpośrednio w telefonie (z synchronizacją do backendu).
6.  **UI/UX V2**:
    *   Scalone menu ustawień.
    *   Wybór trybu Prosty/Skośny w pasku górnym.
    *   Wizualizacja przekroju B-B (schematyczna).

### 🚧 W TRAKCIE / DO ZROBIENIA
1.  **Szablony Szprosów (UI)**:
    *   Logika szablonów (Słoneczko, Szachownica) jest gotowa w backendzie obliczeniowym, ale wymaga dopracowania UI (Dialog wyboru szablonu).
2.  **Testy End-to-End**:
    *   Weryfikacja fizyczna na urządzeniu (zwłaszcza czy "Force Import" poprawnie odświeża listy rozwijane).

---

## 5. Kluczowe Lokalizacje Plików

| Moduł | Ścieżka | Opis |
| :--- | :--- | :--- |
| **Android UI** | `app/.../ui/screens/MuntinScreenV2.kt` | Główny ekran kalkulatora V2. |
| **Android VM** | `app/.../ui/viewmodel/MuntinViewModelV2.kt` | Logika stanu, obsługa CRUD i obliczeń. |
| **Android Logic** | `app/.../util/MuntinCalculatorV2Angular.kt` | Silnik obliczeniowy dla skosów i łuków. |
| **Android Repo** | `app/.../data/repository/ConfigRepository.kt` | Synchronizacja danych z backendem. |
| **Backend API** | `backend/.../controller/ConfigurationController.kt` | Endpointy przeładowania konfiguracji. |
| **Config Tool** | `plikikonfiguracyjne/config_wizard.py` | Skrypt generujący dane startowe. |

---

## 6. Znane Problemy i Ostrzeżenia
*   **Błędy w `error.txt`**: Dotyczą zewnętrznego środowiska Python (`Optymalizacje/venv_32bit`) i **nie wpływają** na działanie głównej aplikacji Android/Backend.
*   **Kompilacja**: Należy używać `./gradlew assembleDebug`. Build Release może wymagać wyłączenia Lint (zostało to już skonfigurowane w `build.gradle.kts`).

## 7. Instrukcja dla AI (Jak kontynuować)
1.  **Załaduj ten plik kontekstu.**
2.  Przyjmij, że środowisko jest skonfigurowane i build działa.
3.  Skup się na weryfikacji funkcjonalnej (czy to co napisaliśmy działa logicznie poprawnie).
4.  Wszelkie zmiany w kodzie Androida **muszą** być weryfikowane przez kompilację (`./gradlew assembleDebug`).

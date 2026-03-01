# PEŁNY KONTEKST PROJEKTU: SYSTEM MAGAZYNOWY PVC

> Uwaga dla modeli AI: w razie konfliktu między opisem w tym pliku a
> faktycznym kodem lub konfiguracją w repozytorium, **repozytorium jest
> źródłem prawdy**. Twoim zadaniem jest korygowanie założeń z dokumentu
> tak, aby zawsze odzwierciedlały aktualny stan projektu.

## 1. Tożsamość Projektu
**Nazwa:** Ferplast-magazyn (system zarządzania magazynem PVC)
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

---

## 8. Architektura systemu (skrócona wersja)

1.  **Klienci:**
    - Android (Kotlin, Jetpack Compose, offline-first z Room).
    - Web UI (React / JS) dla zarządzania i raportów.
2.  **Core System:**
    - Backend API (Spring Boot, Kotlin) z modułami: Inventory, Production, Reporting.
    - Baza danych PostgreSQL (Docker) z migracjami Flyway.
3.  **Usługi brzegowe:**
    - OCR Service (Python, Computer Vision) – rozpoznawanie naklejek/etykiet.
    - Print Service – generowanie i wysyłka ZPL do drukarek Zebra.
4.  **Analytics & AI:**
    - AI Service (Python, FastAPI) – optymalizacje, predykcje, symulacje odpadów.

Kluczowe decyzje architektoniczne:
- Android jest **offline-first** – lokalna baza Room jest źródłem prawdy na urządzeniu, synchronizacja z backendem następuje w tle.
- AI i OCR są osobnymi mikrousługami, aby umożliwić niezależny rozwój i skalowanie.
- Drukowanie etykiet może odbywać się przez lokalnego agenta lub bezpośrednio po sieci – konfiguracja zależy od instalacji.

---

## 9. Architektura wdrożeń (Docker + VPS + vps_manager)

### 9.1. Wdrożenie Docker / Compose

- Główne środowisko uruchomieniowe korzysta z `docker-compose.yml`:
  - Backend (Spring Boot, JDK 17).
  - Baza PostgreSQL.
  - AI Service (FastAPI).
  - Web UI (Nginx + statyczne pliki).
- Konfiguracja środowiskowa:
  - Hasła i sekrety trzymane w `.env` na serwerze (poza repozytorium).
  - Spring Profiles (`SPRING_PROFILES_ACTIVE=prod`) przełączają konfiguracje.
- CI/CD:
  - GitHub Actions dla backendu, frontendu i Androida (workflows w `.github/workflows`).
  - Publikacja obrazów do rejestru (np. Docker Hub) + deploy na VPS przez SSH.

### 9.2. Wdrożenie bez Dockera – rola w vps_manager

W przypadku uruchamiania wybranego backendu bez Dockera, używana jest integracja z `vps_manager`:

- Aplikacja działa na **VPS z Ubuntu**, zarządzana przez:
  - systemd (usługi `.service`),
  - reverse proxy Nginx,
  - moduł `vps_manager` (YAML-e roli + bot Telegram).
- Kluczowe wymagania aplikacji:
  - Nasłuch na `127.0.0.1:APP_PORT`.
  - Endpoint `GET /health` zwracający `{"status": "ok"}` przy poprawnym działaniu.
  - Konfiguracja wyłącznie przez zmienne środowiskowe (`APP_ENV`, `APP_DB_*`, `APP_SECRET_KEY`, itp.).
- Pliki roli (YAML + env) są generowane automatycznie (np. przez `scripts/deploy_role.ps1`) i umieszczane w:
  - `/etc/vps_manager/roles/<system_user>.yml`,
  - `/etc/vps_manager/role_definitions/<system_user>_full.yml`,
  - `/etc/Ferplast-magazyn.env` (lub analogiczny `*.env`).

W razie konfliktu między tym opisem a zawartością `scripts/master_prompt_vps_manager.md`, traktuj `scripts/master_prompt_vps_manager.md` oraz aktualny kod skryptów deploy jako nadrzędne źródło prawdy.

---

## 10. Kierunki rozwoju i usprawnienia (skrót)

### 10.1. Nowe moduły biznesowe

- **Reporting & Analytics:**
  - Wykresy zużycia profili w czasie.
  - Analiza odpadów (użyteczny vs bezużyteczny).
  - Eksport raportów (PDF / XLS / CSV).
- **System Powiadomień:**
  - Alerty o niskim stanie magazynowym.
  - Powiadomienia o błędach optymalizacji.
  - Informacje o nowych wersjach aplikacji i wymuszenie aktualizacji.
- **Integracja z ERP:**
  - Dwukierunkowa synchronizacja stanów magazynowych.
  - Automatyczne pobieranie zleceń produkcyjnych.
  - Kolejkowanie żądań w trybie offline.

### 10.2. Usprawnienia techniczne

- Warstwa danych:
  - Podział dużych repozytoriów na mniejsze (Profile, Waste, Config).
  - Wprowadzenie wzorca UseCase (Clean Architecture) dla kluczowych operacji.
- CI/CD:
  - Rozbudowa pipeline’ów o lint, testy jednostkowe, testy UI i automatyczny deploy.
- Testy:
  - E2E scenariusze: Import pliku -> Optymalizacja -> Rezultat -> Zapis do bazy.
- Backend:
  - Stopniowa ewolucja monolitu w mikroserwisy (Auth, Inventory, Optimization), jeśli skala systemu tego wymaga.

### 10.3. Zalecenia dla dalszej pracy z AI

1.  Przy proponowaniu nowych funkcji zawsze sprawdzaj istniejące moduły, aby unikać dublowania logiki.
2.  Preferuj rozwiązania, które wykorzystują obecny stos technologiczny (Kotlin/Spring/Python).
3.  Przy każdej większej zmianie sugeruj:
    - aktualizację migracji bazy (Flyway),
    - dopisanie testów jednostkowych / integracyjnych,
    - aktualizację tego pliku (sekcje 4–10), aby utrzymać spójny kontekst.

---

## 11. Inspiracje i notatki z wcześniejszych dokumentów

Poniższa lista syntetyzuje pomysły i ustalenia z usuniętych plików `.md`.
W razie wątpliwości zawsze kieruj się aktualnym kodem i konfiguracją repozytorium.

### 11.1. Wdrożenie, testy i diagnostyka

- Utrzymuj jeden spójny sposób wdrożenia:
  - dla środowisk konteneryzowanych preferuj `docker-compose` + `.env` na serwerze,
  - dla ról zarządzanych przez `vps_manager` używaj skryptów z katalogu `scripts/` (np. `deploy_role.ps1`).
- CI/CD:
  - backend: build `bootJar`, testy, budowa obrazu Docker, publikacja do rejestru, deploy przez SSH,
  - frontend: build statycznych plików, obraz Nginx, deploy analogicznie,
  - Android: `assembleRelease`, podpisywanie z użyciem sekretów w CI, opcjonalny upload do App Distribution.
- Diagnostyka:
  - backend: korzystaj z Spring Actuator (`/actuator/health`, `/actuator/prometheus`),
  - monitorowanie kontenerów i hosta: Prometheus + Grafana,
  - regularne backupy bazy (pg_dump, retencja min. 7 dni).

### 11.2. Reguły biznesowe i API

- Projektuj API w sposób:
  - zgodny z REST (czytelne zasoby, sensowne kody HTTP),
  - oparty na identyfikatorach biznesowych (np. kod profilu, kolor, długość),
  - przyjazny dla pracy offline (możliwość kolejkowania żądań, idempotencja tam, gdzie to możliwe).
- Reguły biznesowe:
  - operacje magazynowe (pobranie, zwrot, korekta) powinny mieć jasno zdefiniowane eventy,
  - każda zmiana stanu materiału powinna zostawiać ślad historii (podstawa do raportów).

### 11.3. Optymalizacja odpadów i moduły AI

- Moduł optymalizacji:
  - rozwijaj istniejący algorytm 1D Bin Packing, pamiętając o kosztach rzazu i ograniczeniach długości sztang,
  - rozważ wprowadzenie wariantów strategii: minimalizacja liczby sztang vs minimalizacja odpadu.
- Moduły AI i „digital twin”:
  - traktuj bazę danych i zdarzenia produkcyjne jako podstawę wirtualnego modelu hali (digital twin),
  - typowe przypadki użycia:
    - przewidywanie braków magazynowych,
    - symulacje wpływu zmian parametrów produkcji na odpady,
    - wykrywanie anomalii (nietypowe zużycie materiału).

### 11.4. OCR i druk etykiet

- OCR:
  - zdjęcia etykiet są wysyłane z Androida do dedykowanego serwisu OCR,
  - wynik powinien być zawsze edytowalny przez pracownika przed zapisem do systemu.
- Druk etykiet:
  - generuj komendy ZPL po stronie backendu albo lokalnego agenta,
  - zadbaj o standardowy format etykiet (profil, kolor, długość, kod zamówienia),
  - projektuj API tak, aby w przyszłości łatwo zmieniać layout etykiet bez modyfikacji aplikacji mobilnej.

### 11.5. Konfiguracja, palety i wymiary

- Konfiguracje palet i wymiarów:
  - trzymaj parametry logistyczne (maks. długości, wysokości, ciężary) w bazie lub w plikach konfiguracyjnych ładowanych przez backend,
  - unikaj „magicznych liczb” w kodzie Android/Backend; zamiast tego korzystaj z centralnej konfiguracji.
- Kalkulacja wymiarów:
  - wszelkie kalkulatory geometrii (szprosy, pakowanie na palecie) powinny:
    - pracować w milimetrach,
    - jasno opisywać zaokrąglanie (w górę/w dół/do najbliższej wartości).

### 11.6. Roadmapa i strategia rozwoju

- Priorytety rozwoju:
  - najpierw stabilizacja i redukcja długu technicznego (refaktoring repozytoriów, testy),
  - następnie moduły raportowania i powiadomień,
  - na końcu integracje z systemami zewnętrznymi (ERP, inne API).
- Przy każdej większej zmianie:
  - upewnij się, że aplikacja Android zachowuje tryb offline-first,
  - wprowadzaj odpowiednie migracje bazy i testy regresyjne,
  - aktualizuj ten plik, aby kolejne osoby (i modele AI) miały spójny obraz systemu.

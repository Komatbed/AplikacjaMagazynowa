# Status Techniczny Projektu

Dokument opisuje aktualny stan wdrożenia systemu, zrealizowane moduły oraz plany rozwoju.

## 1. Zrealizowane Moduły (Gotowe do Testów)

### A. Aplikacja Mobilna (Android)
*   **Technologia**: Kotlin, Jetpack Compose, MVVM.
*   **Funkcje**:
    *   Skanowanie kodów QR/Kreskowych (lokalizacje, profile).
    *   OCR (ML Kit) do odczytu etykiet dostawców.
    *   Integracja z drukarkami Zebra (ZPL) po TCP/IP.
    *   Tryb Offline (Room Database + WorkManager Sync).
    *   Dynamiczna konfiguracja (Adres API, IP Drukarki).
    *   Ekrany: Home, Kamera, Ustawienia, Pobranie Ręczne (szkielet).

### B. Backend (API)
*   **Technologia**: Java/Kotlin, Spring Boot 3.2, Hibernate/JPA.
*   **Funkcje**:
    *   REST API do zarządzania inwentarzem (`/take`, `/waste`).
    *   WebSocket (`/topic/warehouse/map`) do aktualizacji mapy w czasie rzeczywistym.
    *   Integracja z bazą PostgreSQL.
    *   Logika biznesowa rejestracji odpadów.

### C. Baza Danych
*   **Technologia**: PostgreSQL 15.
*   **Struktura**:
    *   `locations`: Magazyn, rzędy, palety.
    *   `inventory_items`: Stany magazynowe (długość, profil, status).

### D. AI Service (Intelligence)
*   **Technologia**: Python, FastAPI.
*   **Funkcje**:
    *   Algorytm optymalizacji cięcia (Best Fit + Virtual Waste).
    *   Endpointy predykcyjne (szkielet).
    *   Konteneryzacja (Docker).

## 2. Częściowo Zaimplementowane / Do Rozbudowy

1.  **Ekran Pobierania Ręcznego (Android)**:
    *   UI jest gotowe, podpięte pod ViewModel.
    *   Należy dodać bardziej zaawansowane filtrowanie i wybór konkretnej sztangi z listy wyników.

2.  **Cyfrowy Bliźniak (Dashboard WWW)**:
    *   Istnieje tylko prosty plik `index.html`.
    *   Wymaga implementacji pełnego wizualizatora 2D/3D (np. w React/Three.js) odbierającego dane z WebSocket.

3.  **Zaawansowana Predykcja (AI)**:
    *   Obecnie zwraca dane losowe/mockowe.
    *   Należy podpiąć bibliotekę Prophet pod rzeczywiste dane historyczne z bazy.

## 3. Sugestie Rozwoju (Roadmap)

1.  **Autoryzacja Użytkowników**:
    *   Dodanie logowania w Androidzie i zabezpieczenie endpointów (Spring Security + JWT).
    *   Role: Magazynier, Kierownik, Administrator.

2.  **Historia Operacji**:
    *   Pełny log (audit) kto, kiedy i co pobrał.
    *   Raportowanie zużycia materiału.

3.  **Integracja z ERP**:
    *   Automatyczne pobieranie zleceń produkcyjnych z systemu nadrzędnego (np. SAP, Symfonia).

4.  **Powiadomienia Push**:
    *   Alerty na telefon magazyniera o niskim stanie lub pilnym zleceniu (FCM).

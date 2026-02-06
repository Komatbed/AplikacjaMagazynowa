# Roadmapa Rozwoju Systemu Magazynowo-Produkcyjnego
**Wersja:** 1.0.0
**Autor:** Lead Architect
**Data:** 2026-02-06

---

## FAZA 1: MVP (Minimum Viable Product)
*Cel: Uruchomienie core'owej funkcjonalności z nową logiką kolorów i niezależnymi profilami.*

### 1. Backend & Baza Danych (Obszar F, A)
* **[WYSOKI] / Backend / Migracja Schematu Bazy / Rozdzielenie encji Profil i Kolor**
  * **Opis:** Utworzenie tabel `technology_profiles` (system, producent, wymiary) oraz `technology_colors` (kod, nazwa, typ powierzchni). Usunięcie sztywnych relacji 1:1. Tabela `core_color_rules` lub plik konfiguracyjny dla rdzeni.
  * **Kryteria akceptacji:**
    * Tabela `technology_profiles` istnieje i przyjmuje dane niezależnie od kolorów.
    * Tabela `technology_colors` istnieje (słownik).
    * Brak kluczy obcych wymuszających parowanie przy definicji.

* **[WYSOKI] / Konfiguracja / Obsługa Plików Konfiguracyjnych / Implementacja `core_color_map.json`**
  * **Opis:** Stworzenie mechanizmu wczytywania mapowania kolorów rdzenia dla oklein dwustronnych.
  * **Format:** `{"ext_color_code": "winchester", "core_color": "caramel"}`.
  * **Kryteria akceptacji:**
    * System ładuje plik JSON przy starcie.
    * Zmiana w pliku (hot-reload lub restart) aktualizuje logikę.

### 2. Logika Biznesowa & UI (Obszar B)
* **[WYSOKI] / Android UI / Nowy Selektor Kolorów / Implementacja widoku wyboru**
  * **Opis:** Przebudowa formularza dodawania elementu.
    1. Dropdown "Kolor Zewnętrzny" (z bazy `technology_colors`).
    2. Sekcja "Kolor Wewnętrzny": dwa przyciski [BIAŁY] | [TAKI SAM JAK ZEWN].
  * **Kryteria akceptacji:**
    * Wybranie "TAKI SAM" blokuje wartość na zgodną z zewnętrznym.
    * Wybranie "BIAŁY" ustawia kod koloru białego.

* **[WYSOKI] / Logika / Algorytm Doboru Rdzenia / Implementacja `CoreColorCalculator`**
  * **Opis:** Logika backendowa/mobilna wyliczająca kolor rdzenia w czasie rzeczywistym.
  * **Reguła:**
    * IF (Ext == White OR Int == White) THEN Core = White.
    * ELSE Core = Lookup(Ext_Color) from `core_color_map.json`.
  * **Kryteria akceptacji:**
    * Testy jednostkowe pokrywają przypadki: jednostronny (biały rdzeń), dwustronny (rdzeń z mapy), brak w mapie (fallback/błąd).

### 3. Konfiguracja Profili (Obszar A)
* **[ŚREDNI] / Import / CSV Importer / Masowy import definicji**
  * **Opis:** Endpoint i obsługa CSV do ładowania listy profili i kolorów.
  * **Kryteria akceptacji:**
    * Import 1000 rekordów profili w < 2s.
    * Walidacja duplikatów kodów.

---

## FAZA 2: WDROŻENIE (Deployment & Stabilization)
*Cel: Operacyjne narzędzia dla składacza, audytowalność i edukacja.*

### 4. Funkcje dla Składacza (Obszar C)
* **[WYSOKI] / Składacz / Inteligentny Szperacz v2 / Integracja z nową logiką**
  * **Opis:** Aktualizacja "Smart Waste Finder" o filtrowanie po nowym modelu (Profil X + Kolor Y).
  * **Kryteria akceptacji:**
    * Wyszukiwanie uwzględnia: System Profilu + Kolor Zew + Kolor Wew.
    * Sugestie odpadów pasujących (np. odpad dwustronny pasuje do zlecenia jednostronnego? - do decyzji biznesowej, na razie strict match).

* **[ŚREDNI] / Składacz / Presety "Często Używane" / Szybki wybór**
  * **Opis:** Możliwość zapisu bieżącej konfiguracji (Profil + Kolor) jako "Ulubione".
  * **Kryteria akceptacji:**
    * Kliknięcie w gwiazdkę zapisuje preset.
    * Lista presetów dostępna na górze ekranu dodawania.

### 5. Konfiguracja i Audyt (Obszar A)
* **[ŚREDNI] / Backend / Audit Log / Śledzenie zmian w konfiguracji**
  * **Opis:** Rejestrowanie kto i kiedy zmienił definicję profilu lub mapę rdzeni.
  * **Kryteria akceptacji:**
    * Tabela `config_audit_log` zawiera: timestamp, user_id, action, old_value, new_value.

* **[ŚREDNI] / Konfiguracja / Wersjonowanie / Historia zmian konfiguracji**
  * **Opis:** Każdy import konfiguracji tworzy nową wersję (v1, v2...). Możliwość rollbacku.
  * **Kryteria akceptacji:**
    * API pozwala pobrać konfigurację dla konkretnej wersji.

### 6. Moduł Szkoleniowy (Obszar E)
* **[NISKI] / Edukacja / WindowsMaking 101 / Silnik kursów**
  * **Opis:** Moduł w aplikacji wyświetlający treści edukacyjne (JSON/Markdown + obrazki).
  * **Struktura:**
    1. Teoria (Systemy profili).
    2. Praktyka (Jak mierzyć, jak ciąć).
    3. Quiz (Test wiedzy).
  * **Kryteria akceptacji:**
    * Działa offline (treści zcache'owane).
    * Zaliczenie quizu zapisuje wynik w profilu lokalnym pracownika.

---

## FAZA 3: ROZWÓJ (Development & Scaling)
*Cel: Skalowanie, AI i zarządzanie wieloma zakładami.*

### 7. Roadmap i Perspektywa (Obszar D)
* **[NISKI] / AI / Predykcja Odpadów / Moduł analityczny**
  * **Opis:** Analiza historycznych danych cięcia, aby sugerować, które długości odpadów są "martwe" i należy je utylizować, a które rotują.
  * **Kryteria akceptacji:**
    * Raport "Top 10 zalegających wymiarów".

* **[ŚREDNI] / Architektura / Multi-tenancy / Obsługa wielu lokalizacji**
  * **Opis:** Dodanie `location_id` do wszystkich zasobów magazynowych i konfiguracyjnych.
  * **Kryteria akceptacji:**
    * Użytkownik z Zakładu A nie widzi stanów Zakładu B (chyba że ma rolę Global Admin).
    * Konfiguracja profili może być globalna lub per zakład.

* **[ŚREDNI] / Dashboard / KPI Produkcji / Wizualizacja danych**
  * **Opis:** Webowy dashboard dla kierownika produkcji (Ilość przyjęć, % odpadu, Stan magazynu).

---

## WYMAGANIA TECHNICZNE (Obszar F - Szczegóły)

### Backend (Spring Boot / Kotlin)
* **Modele:**
  * `ProfileDefinition` (id, code, system, manufacturer, integration_code)
  * `ColorDefinition` (id, code, name, foil_manufacturer, type [wood/smooth/mat])
  * `CoreColorRule` (ext_color_id, core_color_code)
* **API:**
  * `GET /api/v1/config/profiles`
  * `GET /api/v1/config/colors`
  * `POST /api/v1/inventory` (z walidacją spójności logicznej)

### Android (Kotlin / Jetpack Compose)
* **Baza danych (Room):**
  * Lokalne kopie tabel definicyjnych do pracy offline.
  * `SyncWorker` pobierający nową konfigurację co X minut.
* **UI/UX:**
  * Wybór koloru z wizualnym podglądem (kółka z kolorem).
  * Walidacja formularza w czasie rzeczywistym (np. blokada zapisu, jeśli brak reguły dla rdzenia, a wybrano niestandardowy zestaw - warning).

### Konfiguracja Plikowa (Przykłady)
* **Lokalizacja:** `/config/` w kontenerze Docker lub zasób w aplikacji Android.
* **`core_color_map.json`**:
```json
{
  "winchester": "caramel",
  "anthracite_smooth": "grey",
  "golden_oak": "caramel"
}
```

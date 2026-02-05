# Android UI/UX Design - Warehouse Worker App

## Filozofia Designu
*   **"Rękawica Robocza" (Glove-first)**: Elementy interaktywne min. 64dp wysokości. Duże odstępy.
*   **Wysoki Kontrast**: Ciemnoszary podkład, Pomarańczowe akcje (Safety Orange).
*   **Minimalizm Informacyjny**: Tylko to, co niezbędne w danym momencie. Brak "rozpraszaczy".
*   **Feedback**: Wyraźne sygnały dźwiękowe i wibracyjne przy sukcesie/błędzie.

## Paleta Kolorów
*   **Primary (Akcja)**: `#FF6D00` (Safety Orange)
*   **Background**: `#212121` (Dark Grey)
*   **Surface (Karty)**: `#424242` (Lighter Grey)
*   **Text Primary**: `#FFFFFF` (White)
*   **Error**: `#CF6679` (Soft Red - widoczny na ciemnym)
*   **Success**: `#43A047` (Green)

## Mapa Ekranów (Flow)

### 1. Ekran Główny (Dashboard)
Dostępny natychmiast po uruchomieniu (Kiosk Mode).
*   **Widget Aktualności**: Wyświetla ostatnie 5 komunikatów (np. dostawy, awarie).
*   **Kafelki Statystyczne**: 
    *   Stan Całkowity, Rezerwacje, Wolne Palety, Zajętość.
    *   Wartości liczbowe + wskaźnik zmiany procentowej.
    *   Responsywny układ (4 kolumny desktop / 2 kolumny tablet).
*   **Menu Nawigacyjne (Kafelki)**:
    *   SKANUJ (Kamera/OCR).
    *   POBIERZ RĘCZNIE.
    *   ODŁÓŻ ODPAD.
    *   MAPA MAGAZYNU.
    *   REZERWACJE (Nowość).
    *   KALKULATOR SZPROSÓW (Nowość).
    *   INWENTARYZACJA.
    *   KALKULATOR OKIEN.
    *   USTAWIENIA / KONFIGURACJA.

### 2. Ekran Skanowania (OCR/Camera)
*   Podgląd z kamery na pełny ekran.
*   Nakładka celownika (Ramka).
*   Przycisk migawki (jeśli nie auto-scan).
*   **Wynik Skanowania (Overlay)**:
    *   Wyświetla rozpoznane: Profil, Kolor, Długość.
    *   Dwa wielkie przyciski na dole: "ZATWIERDŹ (TAK)" (Zielony) / "POPRAW (NIE)" (Czerwony).

### 3. Ekran Akcji (Po zidentyfikowaniu towaru)
Kontekstowy, zależny od tego co zeskanowano.
*   **Nagłówek**: Co to jest? (np. "Rama 70mm, Biała").
*   **Sekcja Lokalizacji**: Wielki tekst "IDŹ DO: **01A**".
*   **Input Ilości**:
    *   Duże przyciski `[-]` `[+]` po bokach licznika.
    *   Możliwość wpisania z klawiatury numerycznej (rzadziej).
*   **Przycisk Akcji**: "POBIERAM" (Slide-to-confirm, aby uniknąć przypadkowych kliknięć w kieszeni? Nie, w rękawicach slide jest trudny. Lepiej Long Press lub fizyczny przycisk głośności).
    *   *Decyzja*: Zwykły duży przycisk z lekkim opóźnieniem (debounce).

### 4. Ekran Kalkulatora Szprosów (Nowość)
*   **Wybór Typu**: Szybkie przyciski (1 Pion, 1 Poziom, Krzyż, Niestandardowy).
*   **Interaktywny Schemat**:
    *   Wizualizacja okna.
    *   Kliknięcie w obszar dodaje/usuwa szpros (inteligentne wykrywanie pion/poziom).
*   **Parametry**: Szerokość/Wysokość skrzydła, Szerokość szprosa, Kąt listwy.
*   **Wyniki**: Automatyczne przeliczanie długości szprosów i segmentów (wewn./zewn.).

### 5. Ekran Rezerwacji (Nowość)
*   Lista sztang zarezerwowanych ("RESERVED", "IN_PROGRESS").
*   Filtrowanie po dacie i użytkowniku.
*   Możliwość podglądu szczegółów.

### 6. Ekran Ustawień
*   Konfiguracja adresu API (domyślnie 51.77.59.105).
*   **Status Backend**: Wskaźnik Online/Offline z czasem ostatniego pinga i opóźnieniem.
*   Konfiguracja drukarki etykiet.

### 7. Ekran Błędu / Ostrzeżenia
*   Tło zmienia kolor na Żółty (Ostrzeżenie) lub Czerwony (Błąd).
*   Komunikat wielką czcionką: "NIE TA PALETA!".
*   Wymaga potwierdzenia fizycznego (np. kliknięcie "Rozumiem").

## Stany Błędów
1.  **Brak Sieci**:
    *   Ikona Wi-Fi przekreślona.
    *   Pasek na górze: "TRYB OFFLINE - Dane zapisywane lokalnie".
    *   Aplikacja NIE blokuje pracy.
2.  **Błąd OCR**:
    *   Komunikat: "Nieczytelne".
    *   Propozycja: "Wpisz ręcznie" lub "Zrób zdjęcie ponownie".

## Komunikaty (Tone of Voice)
*   Krótkie, żołnierskie komendy.
*   Zamiast "Proszę udać się do lokalizacji 01A", piszemy: "**IDŹ DO: 01A**".
*   Zamiast "Operacja zakończona sukcesem", piszemy: "**GOTOWE**".

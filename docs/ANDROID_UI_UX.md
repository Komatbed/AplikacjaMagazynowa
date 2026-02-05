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
*   **Duży Przycisk 1**: "SKANUJ" (Kamera/OCR) - zajmuje 50% ekranu.
*   **Przycisk 2**: "POBIERZ RĘCZNIE" (Lista).
*   **Przycisk 3**: "ODŁÓŻ ODPAD".
*   **Status Bar**: Ikona Wi-Fi (Zielona/Czerwona), Stan baterii.

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

### 4. Ekran Błędu / Ostrzeżenia
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

# Changelog - 2026-02-04 V3

## Nowe Funkcjonalności (Backend & Mobile)

### 1. Zaawansowana Optymalizacja Cięcia
- **Algorytm**: Zaktualizowano algorytm Best Fit Decreasing.
- **Sortowanie**: Detale są teraz sortowane od najdłuższych.
- **Grupowanie**: Podobne długości są grupowane obok siebie w planie cięcia.
- **Priorytetyzacja Odpadów**:
  - **Idealne dopasowanie (0mm)**: Najwyższy priorytet.
  - **Zarezerwowane wymiary**: Bardzo wysoki priorytet (np. 1200mm zdefiniowane w ustawieniach).
  - **Preferowany odpad (50-250mm)**: Wysoki priorytet ("mały odpad").
  - **Standardowy odpad**: Normalny priorytet.
  - **Niepożądany odpad (<50mm)**: Unikany (kara punktowa).
- **Konfiguracja**: Dodano obsługę "Zarezerwowanych długości odpadów" w Ustawieniach Aplikacji.

### 2. Kalkulator Szprosów (Muntins)
- **Logika**: Dodano obsługę siatki prostokątnej (szprosy krzyżowe).
- **Połączenia**:
  - Obsługa łączenia "na wpust" (Halving Joint) - szprosy krzyżują się.
  - Obsługa łączenia "doczołowego" (Butt Joint) - szprosy są dzielone na segmenty (skrajne i środkowe).
- **Wymiary**:
  - Automatyczne uwzględnianie luzu (domyślnie 1mm na stronę).
  - Obliczanie nachodzenia na listwy przyszybowe.
  - Osobne wyliczanie szprosów wewnętrznych i zewnętrznych z offsetem.

### 3. Poprawki Techniczne
- **Baza Danych**: Naprawiono błąd `null value in column "internal_color"` poprzez restart schematu bazy danych.
- **Backend**: Uruchomiono serwer backendu na porcie 8080 (dostępny dla aplikacji i skryptów).
- **Skrypty**: Zweryfikowano łączność skryptu `manage_data.py`.

### 4. Ustawienia
- **Numpad**: Pola numeryczne w aplikacji wymuszają teraz klawiaturę numeryczną.
- **IP**: Zaktualizowano domyślny adres IP serwera na adres PC.

## Instrukcja Aktualizacji
1. Zainstaluj nową wersję APK (V3).
2. Upewnij się, że backend jest uruchomiony (`gradlew bootRun`).
3. W ustawieniach wprowadź ewentualne "Zarezerwowane odpady" (np. "1200,850").

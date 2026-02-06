# Dokumentacja Techniczna Modułu Optymalizacji (CT500 -> DCX)

## Przegląd
Moduł `FileProcessingOptimizer` służy do przetwarzania plików zleceń cięcia w formacie `.ct500txt` na pliki sterujące maszyny `.dcxtxt`.
Głównym celem jest optymalizacja rozkroju profili aluminiowych/PVC w celu minimalizacji odpadów oraz obsługa specyficznych wymagań maszynowych.

## Kluczowe Funkcjonalności
1. **Parsowanie Wejścia**: Obsługa formatu `*.ct500txt` (rozdzielany znakiem `*`).
2. **Grupowanie**: Automatyczne grupowanie elementów według typu profilu i koloru.
3. **Optymalizacja (FFD)**: Algorytm First Fit Decreasing z uwzględnieniem rzazu piły (10mm).
4. **Zarządzanie Odpadem**:
   - Wykrywanie odpadów użytecznych (>500mm).
   - Oznaczanie odpadów w pliku wynikowym (`odpad=XXXXX`).
5. **Tryby Pracy**:
   - `MIN_WASTE`: Priorytet minimalizacji odpadu (Best Fit).
   - `DEFINED_WASTE`: (Zarezerwowany) Zachowanie odpadów zdefiniowanych.
   - `LONGEST_FIRST`: Priorytet dla najdłuższych elementów (agresywna optymalizacja).

## Specyfikacja Techniczna

### Parametry Stałe
- **Szerokość rzazu (Kerf)**: 10mm (100 jednostek 0.1mm)
- **Długość sztangi**: 6500mm
- **Min. odpad użyteczny**: 500mm

### Struktura Klas
- `FileProcessingOptimizer`: Główna klasa logiczna (Singleton/Object).
- `InputRecord`: Reprezentacja wiersza wejściowego.
- `Bar`: Reprezentacja sztangi materiału.
- `CutAssignment`: Przypisanie cięcia do sztangi.
- `OptimizationResult`: Wynik zawierający linie wyjściowe, logi i podsumowanie.

### Algorytm
1. Wczytanie pliku i walidacja linii.
2. Podział na grupy (Profil + Kolor).
3. Dla każdej grupy:
   - Sortowanie elementów zgodnie z wybranym trybem.
   - Iteracja po elementach i próba umieszczenia w istniejących sztangach (Bin Packing).
   - Jeśli brak miejsca -> utworzenie nowej sztangi.
4. Generowanie pliku wynikowego z zachowaniem formatowania DCX.

## Instrukcja Użytkownika (Android)
1. Przejdź do ekranu **Optymalizacja**.
2. Kliknij przycisk **"TRYB PLIKOWY"** w prawym górnym rogu.
3. Wybierz tryb optymalizacji (np. "Min Odpad").
4. Kliknij **"WYBIERZ PLIK .CT500TXT"** i wskaż plik z urządzenia.
5. Po przetworzeniu, wynik pojawi się na ekranie.
6. Użyj przycisków na dole, aby **Skopiować** treść lub **Udostępnić** plik `.dcxtxt`.

## Format Plików
**Wejście (.ct500txt)**:
`ID*Profil*Kolor*DlugoscSztangi*DlugoscElementu*KatL*KatP*...`

**Wyjście (.dcxtxt)**:
`NrSztangi*Profil*Kolor*Zlecenie*Stojak*Pozycja*Komentarz*Dlugosc*KatL*KatP*...`

Komentarz może zawierać informację o odpadzie, np. `  odpad=02500  ` (2500mm).

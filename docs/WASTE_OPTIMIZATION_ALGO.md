# Algorytm Optymalizacji Cięcia (Waste Optimization Algorithm)

## 1. Cel
Algorytm ma na celu minimalizację zużycia nowych sztang (6.5m) poprzez maksymalne wykorzystanie istniejących odpadów magazynowych oraz odpadów powstających na bieżąco podczas realizacji zlecenia (Batch).

## 2. Dane Wejściowe
*   **Zlecenia (Orders)**: Lista odcinków do ucięcia.
    *   `ProfileCode`, `Color` (Grupowanie).
    *   `Length` (Długość).
    *   `Priority` (Priorytet 1-5).
*   **Dostępne Odpady (Available Waste)**: Lista odpadów na magazynie.
    *   `Length`.
    *   `Location`.
*   **Parametry Globalne**:
    *   `FullBarLength`: 6500mm (Standardowa sztanga).
    *   `MinUsableOffcut`: 500mm (Poniżej tego odpad jest trudny do użycia).
    *   `ScrapThreshold`: 200mm (Poniżej tego to śmieć/złom).

## 3. Logika Decyzyjna (Krok po Kroku)

### Krok 1: Grupowanie i Sortowanie
Algorytm dzieli zlecenia na grupy według unikalnych par `(Profil, Kolor)`. Optymalizacja zachodzi niezależnie w każdej grupie.

Wewnątrz grupy, zlecenia są sortowane:
1.  **Priorytet (Malejąco)**: Najważniejsze zlecenia są obsługiwane pierwsze.
2.  **Długość (Malejąco)**: Najdłuższe odcinki są "najtrudniejsze" do dopasowania, więc szukamy dla nich materiału najpierw (Strategia *First Fit Decreasing* / *Best Fit Decreasing*).

### Krok 2: Przygotowanie Puli Materiału
Tworzone są dwie pule materiału:
1.  **Magazyn Odpadów**: Istniejące fizycznie odpady (posortowane rosnąco wg długości - strategia *Best Fit*).
2.  **Wirtualne Odpady (Virtual Pool)**: Pusta na starcie. Tu trafiają resztki powstałe po cięciu w trakcie trwania tego procesu.

### Krok 3: Pętla Alokacji (Greedy Best-Fit)
Dla każdego zlecenia w posortowanej liście:

**A. Sprawdzenie Puli Wirtualnej (Priorytet Najwyższy)**
*   Sprawdź, czy mamy świeży odpad (z właśnie otwartej sztangi), który pasuje długością.
*   Jeśli tak -> Użyj go (Unikamy odkładania go na półkę, tniemy "z ręki").

**B. Sprawdzenie Magazynu Odpadów**
*   Jeśli nie ma wirtualnego, szukaj w magazynie.
*   Wybierz odpad, który generuje **najmniejszą resztę** (Best Fit).
*   *Cel*: Pozbycie się krótkich, zalegających odpadów, które pasują "na styk".

**C. Użycie Nowej Sztangi**
*   Jeśli żaden odpad nie pasuje, pobierz nową sztangę (6500mm).
*   Zwiększ licznik zużycia nowych sztang.
*   Powstałą resztę (np. 6500 - 2000 = 4500mm) dodaj do **Puli Wirtualnej**. Będzie ona dostępna dla kolejnych zleceń w tym samym przebiegu (Krok 3A).

### 4. Obsługa Resztek (Offcuts)
Przy każdym cięciu powstaje reszta (`SourceLength - RequiredLength`).
*   **Złom (Scrap)**: Jeśli `Reszta < 200mm` -> Oznacz jako odpad do utylizacji. Nie dodawaj do puli.
*   **Użyteczny (Usable)**: Jeśli `Reszta >= 200mm` -> Dodaj do Puli Wirtualnej.

## 5. Przykład Działania

**Zlecenia**:
1.  2000mm (Prio 5)
2.  1500mm (Prio 3)
3.  4000mm (Prio 1)

**Magazyn**:
*   Odpad A: 2100mm
*   Odpad B: 1000mm

**Przebieg**:
1.  **Zlecenie 4000mm**:
    *   Odpad A (2100) za krótki.
    *   Odpad B (1000) za krótki.
    *   **Decyzja**: Nowa Sztanga (6500).
    *   Powstaje reszta: 2500mm -> Trafia do Puli Wirtualnej.
2.  **Zlecenie 2000mm**:
    *   Sprawdź Wirtualne: Mamy 2500mm. Pasuje!
    *   **Decyzja**: Użyj Wirtualnego (2500).
    *   Powstaje reszta: 500mm -> Trafia do Puli Wirtualnej.
3.  **Zlecenie 1500mm**:
    *   Sprawdź Wirtualne: Mamy 500mm. Za krótki.
    *   Sprawdź Magazyn: Mamy Odpad A (2100). Pasuje!
    *   **Decyzja**: Użyj Odpadu A (2100).
    *   Powstaje reszta: 600mm -> Trafia do Puli Wirtualnej (lub Magazynu).

**Wynik**:
*   Zużyto: 1 Nowa Sztanga, 1 Odpad Magazynowy.
*   Zostało: Odpad B (1000) na półce + Nowy Odpad 500mm + Nowy Odpad 600mm.

## 6. Integracja
System zwraca listę cięć (`Cuts`) z instrukcjami dla operatora:
*   "WEŹ NOWĄ SZTANGĘ"
*   "WEŹ ODPAD Z LOKALIZACJI 01A"
*   "DOTNIJ Z TEGO CO TRZYMASZ W RĘKU" (Source: NEW_BAR_REMNANT)

# Logika Biznesowa Magazynu - Zestaw Reguł

Dokument definiuje zasady działania systemu magazynowo-produkcyjnego.

---

## 1. Definicje Podstawowe
*   **Cała Sztanga (Full Bar)**: Materiał o standardowej długości produkcyjnej (zwykle 6000mm lub 6500mm).
*   **Odpad Użyteczny (Usable Waste)**: Kawałek profilu pozostały po cięciu, który jest dłuższy niż minimalny próg (np. > 300mm) i nadaje się do ponownego użycia.
*   **Ścinek (Scrap)**: Odpad poniżej minimalnej długości -> Utylizacja.
*   **Paleta**: Fizyczna lokalizacja (Rząd + Numer) przypisana na sztywno do typu materiału.

---

## 2. Reguły Przypisania Palety (Slotting Rules)

System automatycznie wskazuje pracownikowi, gdzie odłożyć materiał.

**Tabela Decyzyjna: Gdzie odłożyć?**

| Typ Materiału | Długość (L) | Czy Paleta Pełna? | Akcja Systemu | Komunikat dla Pracownika |
| :--- | :--- | :--- | :--- | :--- |
| Nowa Dostawa | = Standard | NIE | Wskaż stałą paletę (np. 01A) | "Odłóż na 01A" |
| Nowa Dostawa | = Standard | TAK | Szukaj pustej palety "buforowej" | "Paleta 01A pełna! Odłóż na BUFOR B1" |
| Produkcja (Zwrot) | < Standard, > Min | - | Wskaż paletę na odpady (np. 01B) | "To jest ODPAD. Odłóż na 01B" |
| Produkcja (Zwrot) | < Min (np. 300mm) | - | Utylizacja | "Do kosza (Zbyt krótkie)" |

**Pseudokod - Funkcja `findLocationForProfile`**
```kotlin
fun findLocationForItem(profileCode: String, length: Int): Location {
    val standardLength = getStandardLength(profileCode)
    val config = getLocationConfig(profileCode)

    // 1. Sprawdź czy to odpad
    if (length < standardLength) {
        if (length < MIN_USABLE_LENGTH) return Location.TRASH
        return config.wasteLocation // np. Paleta obok głównej
    }

    // 2. Sprawdź miejsce na głównej palecie
    val currentQty = inventoryService.getQuantity(config.mainLocation)
    if (currentQty < config.maxCapacity) {
        return config.mainLocation
    }

    // 3. Fallback - znajdź bufor
    return inventoryService.findNearestEmptyBuffer(config.mainLocation)
}
```

---

## 3. Reguły Zapełnienia i Pobierania

### Reguła FIFO (First In, First Out) - "Najpierw Odpady"
System zawsze sugeruje pobranie najkrótszego możliwego kawałka, który wystarczy na zlecenie, aby czyścić magazyn z odpadów.

**Algorytm Sugestii:**
1.  Pracownik potrzebuje: `X` mm.
2.  Szukaj w odpadach (lokalizacja WASTE): kawałek gdzie `L >= X`.
3.  Jeśli znaleziono -> Sugeruj "Weź odpad z 01B (Długość L)".
4.  Jeśli nie -> Sugeruj "Weź całą sztangę z 01A".

### Reguła Ostatniej Sztangi (The "Last Bar" Rule)
Fizycznie magazyn jest chaotyczny. System może pokazywać 1 sztukę, której fizycznie nie ma (zgubiona/zniszczona).

**Scenariusz:**
*   System: Stan = 1.
*   Pracownik: "Pusta paleta".
*   Akcja: Pracownik klika "Zgłoś brak" -> Stan zmienia się na 0 -> Alert do Kierownika -> Pracownik bierze z innej palety (jeśli jest).

---

## 4. Ostrzeżenia i Blokady

| Zdarzenie | Warunek | Reakcja Systemu | Typ |
| :--- | :--- | :--- | :--- |
| Pobranie poniżej minimum | `Stan Po < MinThreshold` | Wyświetl Toast/Popup na żółto | Ostrzeżenie (Można pominąć) |
| Pobranie zera | `Stan Aktualny <= 0` | Blokada przycisku "Pobierz" | Błąd Krytyczny (Wymaga korekty stanu) |
| Zła lokalizacja | Skan palety != Sugerowana | "Błędna paleta! Odkładasz X na Y?" | Ostrzeżenie (Wymaga potwierdzenia) |

---

## 5. Edge Cases (Sytuacje Graniczne)

1.  **"Cudowne Rozmnożenie"**
    *   *Sytuacja*: System pokazuje 0, pracownik znajduje sztangę na hali.
    *   *Rozwiązanie*: Funkcja "Szybkie Przyjęcie" (Quick Add). Pracownik skanuje kod palety -> Wpisuje ilość 1 -> System loguje "Znaleziono na hali" (+1).

2.  **Błędny OCR**
    *   *Sytuacja*: OCR odczytał "Biały" zamiast "Biały Struktura".
    *   *Rozwiązanie*: Pracownik widzi podgląd danych PRZED zatwierdzeniem. Musi mieć duże przyciski "+" / "-" lub listę wyboru do szybkiej korekty koloru/długości.

3.  **Awaria Sieci (Offline)**
    *   *Reguła*: W trybie offline system pozwala pobierać "na minus" (lokalnie).
    *   *Sync*: Po powrocie sieci, jeśli stan spadł poniżej 0, tworzony jest raport "Konflikt Magazynowy" dla kierownika.

---

## 6. Sugestie Zamówień (Dla Kierownika)

*   **Trigger**: `Stan Całkowity (Sztangi + Odpady przeliczone na metry) < Safety Stock`.
*   **Akcja**: Dodaj do listy "Do Zamówienia".
*   **Priorytet**:
    *   CRITICAL: Stan < 3 dni produkcji.
    *   NORMAL: Stan < 14 dni produkcji.

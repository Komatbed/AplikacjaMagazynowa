# OCR System Design - Sticker Recognition

## Założenia
*   **Brak kodów QR/Kreskowych**: Bazujemy wyłącznie na tekście (alfanumerycznym).
*   **Różnorodność Etykiet**: Różni producenci = różne formaty (layouty).
*   **Jakość Obrazu**: Często słabe oświetlenie, zdjęcia pod kątem, zabrudzone naklejki.

## Pipeline Przetwarzania

```mermaid
graph TD
    Input[Zdjęcie z Kamery] --> Preprocess[Preprocessing (OpenCV)]
    Preprocess --> Detection[Text Detection (EAST/DB)]
    Detection --> Recognition[Text Recognition (Tesseract/PaddleOCR)]
    Recognition --> RawText[Surowy Tekst]
    RawText --> Parsing[Parser (Regex & Heurystyka)]
    Parsing --> Validation[Walidacja Biznesowa]
    Validation --> Output[JSON: Profil, Kolor, Długość]
```

### 1. Preprocessing (OpenCV)
Kluczowy etap dla poprawy skuteczności.
*   **Grayscale**: Konwersja do skali szarości.
*   **Adaptive Thresholding**: Usuwanie cieni i nierównomiernego oświetlenia.
*   **Deskewing**: Wyprostowanie tekstu (jeśli naklejka jest krzywo).

### 2. Silnik OCR
Zalecane rozwiązanie: **Google ML Kit (On-device)** dla Androida lub **PaddleOCR** (Server-side).
*   *Decyzja*: **ML Kit na Androidzie** jako pierwsza linia (szybkość, offline). Jeśli wynik jest słaby -> Upload na Backend (PaddleOCR - dokładniejszy).

### 3. Parsing (Logika "Inteligentna")
Tekst z naklejki jest "zupą słowną". Musimy wyłowić sens.

**Strategia Regex + Słowniki:**

#### A. Wykrywanie Producenta
Szukamy słów kluczowych ze słownika `producers`:
*   "ALUPLAST", "VEKA", "SALAMANDER", "SCHUCO".

#### B. Wykrywanie Profila (Kod Artykułu)
*   Wzorce (zależne od producenta):
    *   Aluplast: `^\d{6}$` (np. 140001)
    *   Veka: `^\d{5}$` lub `^\d{2}.\d{3}$`
*   *Heurystyka*: Szukaj ciągu cyfr w pobliżu słowa "Art.Nr" lub "Profil".

#### C. Wykrywanie Długości
*   Wzorzec: `\d{3,4}\s?(mm|MM)?`
*   *Logika*: Wartości w zakresie 300-6500. Często największa liczba na etykiecie to długość.

#### D. Wykrywanie Koloru
*   Najtrudniejsze. Często skróty np. "ZD", "W", "AP05".
*   *Rozwiązanie*: Fuzzy Matching (Levenshtein distance) względem bazy kolorów.
    *   OCR: "Zloty Dab" -> Baza: "Złoty Dąb" (Match 90%).

## Struktura Danych (Wynik)

```json
{
  "rawText": "ALUPLAST Sp. z o.o.\nArt: 140001\nKolor: Złoty Dąb / Biały\nDł: 6500 mm\nData: 2023-10-12",
  "parsedData": {
    "producer": "ALUPLAST",
    "profileCode": "140001",
    "lengthMm": 6500,
    "colors": {
      "raw": "Złoty Dąb / Biały",
      "innerCode": "W", // Zmapowane
      "outerCode": "ZD" // Zmapowane
    }
  },
  "confidence": 0.88,
  "needsManualCorrection": false
}
```

## Uczenie się na Błędach (Feedback Loop)
Jeśli pracownik poprawi dane (np. OCR: 6000 -> Pracownik: 6500):
1.  System zapisuje parę: `(Zdjęcie, Poprawne Dane)`.
2.  Zdjęcie trafia do folderu `dataset/failed_ocr`.
3.  Raz w tygodniu dotrenowujemy model lub poprawiamy Regexy na podstawie tych przypadków.

# Label Printing System Design - Zebra ZT410 (ZPL II)

## Architektura Druku
*   **Model**: Zebra ZT410 (Przemysłowa).
*   **Protokół**: ZPL II (Zebra Programming Language).
*   **Komunikacja**: TCP/IP (Port 9100). Drukarki są sieciowe, dostępne w LAN.

## Szablony Etykiet (ZPL)

### 1. Etykieta Odpadu (Waste Label)
Etykieta naklejana na kawałek, który wraca na magazyn.
*   **Rozmiar**: 100mm x 50mm (przykładowo).
*   **Kluczowe dane**: Wielki numer Długości, Kod Profila, Kod kreskowy do szybkiego skanowania.

**Kod ZPL (Szablon):**
```zpl
^XA
^PW800
^LL400

// Nagłówek: Typ Profila
^FO50,50^A0N,50,50^FD{profileCode}^FS

// Wielka Długość (Najważniejsze dla pracownika)
^FO50,120^A0N,150,150^FD{lengthMm} mm^FS

// Kolory
^FO50,280^A0N,40,40^FD{colorName}^FS

// Kod Kreskowy (Code 128) - zawiera unikalne ID odpadu
^FO450,50^BY3,3,100^BCN,100,Y,N,N
^FD{wasteId}^FS

// Lokalizacja docelowa
^FO450,280^A0N,40,40^FDPaleta: {location}^FS

^XZ
```

### 2. Etykieta Paletowa (Lokalizacyjna)
Etykieta na regał.
*   **Treść**: Duży kod lokalizacji (np. "01A") + Kod QR do weryfikacji przez Appkę.

## Obsługa Błędów i Kolejkowanie

### Problem: Drukarka zajęta lub offline
Jeśli drukarka nie odpowiada na porcie 9100, nie możemy zablokować pracy magazyniera.

### Rozwiązanie: Print Queue (Backend)
1.  Android wysyła żądanie druku (`POST /api/print`).
2.  Backend zapisuje zadanie w tabeli `print_jobs` (status: `PENDING`).
3.  Osobny wątek (`PrintJobWorker`) pobiera zadania i próbuje wysłać na IP drukarki.
4.  **Retry Policy**: 3 próby co 5 sekund.
5.  **Fallback**: Jeśli po 3 próbach fail -> Alert do Kierownika ("Sprawdź papier/sieć w drukarce X").

### Bezpośredni Druk (Opcja Android)
W razie awarii backendu, aplikacja Android może (jeśli jest w tej samej sieci Wi-Fi) wysłać ZPL bezpośrednio na IP drukarki.
*   *Zaleta*: Działa offline (jeśli LAN działa).
*   *Wada*: Trudniejsze zarządzanie szablonami (muszą być zaszyte w Appce).
*   *Decyzja*: **Hybryda**. Domyślnie Backend. Przycisk "Drukuj Awaryjnie" w Appce wysyła bezpośrednio.

## Status Drukarki (ZPL Host Status)
System co 5 minut pyta drukarkę o status (`~HS`).
Odpowiedź analizujemy pod kątem:
*   `Paper Out` (Brak papieru).
*   `Ribbon Out` (Brak taśmy).
*   `Head Open` (Otwarta głowica).

Wynik wyświetlamy na Dashboardzie Kierownika.

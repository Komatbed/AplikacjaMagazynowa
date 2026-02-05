# REST API Design - Warehouse System

## Przegląd
API służy do komunikacji aplikacji mobilnej (Android) oraz webowej (React) z backendem.
Format danych: JSON.
Autoryzacja: JWT Bearer Token.

## Role
- `ROLE_WORKER`: Pracownik hali (dostęp do skanowania, pobierania, zgłaszania braków).
- `ROLE_MANAGER`: Kierownik (dostęp do raportów, edycji stanów, konfiguracji).
- `ROLE_SYSTEM`: Systemy zewnętrzne (np. automatyczne skrypty).

## Obsługa Błędów
Standardowy format błędu:
```json
{
  "timestamp": "2024-05-20T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Niewystarczający stan magazynowy. Próba pobrania 5, dostępne 2.",
  "code": "INSUFFICIENT_STOCK",
  "path": "/api/v1/inventory/take"
}
```

---

## Endpointy

### 1. Inventory (Magazyn)

#### `GET /api/v1/inventory/items`
Pobiera stan magazynowy (z filtrowaniem).
**Query Params:** `location`, `profileCode`, `minQuantity`
**Response:**
```json
[
  {
    "id": "uuid-...",
    "locationLabel": "01A",
    "profileCode": "P-1234",
    "lengthMm": 6500,
    "quantity": 50,
    "isWaste": false,
    "colors": { "inner": "Biały", "outer": "Złoty Dąb" }
  }
]
```

#### `POST /api/v1/inventory/take`
Pobranie materiału (operacja krytyczna).
**Role:** `WORKER`
**Request:**
```json
{
  "locationLabel": "01A",
  "profileCode": "P-1234", // Opcjonalne weryfikacja
  "lengthMm": 6500,
  "quantity": 1,
  "reason": "PRODUCTION", // PRODUCTION, DAMAGE, SAMPLE
  "force": false // Jeśli true, pozwala na pobranie "ostatniej sztangi" mimo ostrzeżenia
}
```
**Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "newQuantity": 49,
  "warning": null
}
```
**Response (409 Conflict - Ostatnia Sztanga / Minimum):**
```json
{
  "status": "WARNING",
  "code": "LOW_STOCK_WARNING",
  "message": "Uwaga! Osiągnięto stan minimalny. Potwierdź pobranie.",
  "currentQuantity": 10
}
```
*Frontend musi ponowić request z `force: true` jeśli użytkownik potwierdzi.*

#### `POST /api/v1/inventory/waste`
Zgłoszenie odpadu (odłożenie ścinka).
**Request:**
```json
{
  "sourceProfileId": "uuid-...", // Z której sztangi powstał odpad (opcjonalne)
  "lengthMm": 1200,
  "locationLabel": "01B-ODPADY",
  "quantity": 1
}
```

### 2. OCR (Rozpoznawanie)

#### `POST /api/v1/ocr/recognize`
Przesłanie zdjęcia naklejki do rozpoznania.
**Request:** `Multipart/form-data` (file: image)
**Response:**
```json
{
  "rawText": "ALUPLAST 140001 ZD/B 6500",
  "parsed": {
    "producer": "ALUPLAST",
    "profileCode": "140001",
    "colors": "ZD/B",
    "length": 6500
  },
  "confidence": 0.85
}
```

### 3. Labels (Drukowanie)

#### `POST /api/v1/print/label`
Zlecenie wydruku etykiety.
**Request:**
```json
{
  "printerId": "ZEBRA-HALA-1",
  "template": "STANDARD_PROFILE",
  "data": {
    "profile": "P-1234",
    "color": "Złoty Dąb",
    "length": 2300,
    "palette": "05C"
  }
}
```

### 4. Reports & Alerts (Manager)

#### `GET /api/v1/reports/loss`
Raport strat (odpady vs produkcja).

#### `POST /api/v1/webhooks/subscribe`
Rejestracja na powiadomienia (np. SMS gdy stan < 5).
**Request:**
```json
{
  "eventType": "LOW_STOCK",
  "channel": "SMS",
  "target": "+48123456789",
  "conditions": { "minQuantity": 5 }
}
```

---

## Kluczowe Logiki Biznesowe w API

1.  **Blokada Minimalnego Stanu**: Jeśli `quantity - requested < min_threshold`, API zwraca 409 (Conflict) z kodem `LOW_STOCK_WARNING`. Pracownik musi jawnie potwierdzić (parametr `force=true`), że wie co robi.
2.  **Logika Ostatniej Sztangi**: Jeśli `quantity - requested < 0`, API zwraca 400 (Bad Request), chyba że jest to tryb korekty inwentaryzacyjnej przez Managera. W fizycznym świecie nie można wziąć czegoś, czego nie ma, ale system może mieć błędny stan. Worker może użyć flagi `correction=true` (jeśli ma uprawnienia) lub zgłosić "Rozbieżność".
3.  **Idempotentność**: Kluczowe operacje (Pobranie) powinny wspierać `Idempotency-Key` w nagłówku, aby przy słabym Wi-Fi nie pobrać materiału 2 razy, gdy request poleci ponownie.

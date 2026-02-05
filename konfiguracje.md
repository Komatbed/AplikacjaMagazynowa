# Pliki Konfiguracyjne Systemu Magazynowego

Poniżej znajduje się zestawienie wszystkich kluczowych plików konfiguracyjnych systemu wraz z instrukcjami i przykładami.

## 1. Ankieta Wdrożeniowa (`docs/CONFIGURATION_QUESTIONNAIRE.md`)
Ten plik definiuje fizyczne i biznesowe parametry Twojego magazynu. Służy do dostosowania logiki aplikacji.

**Kluczowe zmienne:**
*   `WAREHOUSE_ROWS`: Liczba rzędów regałów.
*   `SHELF_CAPACITY`: Maksymalna liczba palet w rzędzie.
*   `STD_PROFILE_LENGTH`: Długość nowej sztangi (mm).
*   `USEFUL_WASTE_LIMIT`: Minimalna długość odpadu do zachowania (mm).
*   `SCRAP_LIMIT`: Próg złomowania (mm).

**Przykład:**
```properties
WAREHOUSE_ROWS=25
SHELF_CAPACITY=3
STD_PROFILE_LENGTH=6500
USEFUL_WASTE_LIMIT=500
SCRAP_LIMIT=200
```

---

## 2. Konfiguracja Backendu (`backend/src/main/resources/application.properties`)
Główny plik sterujący serwerem, bazą danych i logowaniem.

**Lokalizacja:** `backend/src/main/resources/application.properties`

**Instrukcja:**
Edytuj ten plik, aby zmienić adres bazy danych lub port serwera.

**Przykład:**
```properties
spring.application.name=warehouse-backend
server.port=8080

# Baza danych (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5433/warehouse
spring.datasource.username=postgres
spring.datasource.password=twoje_haslo

# Automatyczna aktualizacja schematu bazy (uwaga na produkcji!)
spring.jpa.hibernate.ddl-auto=update
```

---

## 3. Konfiguracja Drukarek Etykiet (`docs/LABEL_PRINTING.md`)
Definiuje szablony etykiet w języku ZPL (Zebra Programming Language) oraz parametry sieciowe drukarek.

**Szablon ZPL (Etykieta Odpadu):**
```zpl
^XA
^PW800
^LL400
^FO50,50^A0N,50,50^FD{profileCode}^FS
^FO50,120^A0N,150,150^FD{lengthMm} mm^FS
^FO450,50^BY3,3,100^BCN,100,Y,N,N^FD{wasteId}^FS
^XZ
```

**Parametry:**
*   **Port:** 9100 (Standard Zebra TCP/IP)
*   **IP:** Należy ustawić statyczne IP dla drukarki (np. `192.168.1.200`).

---

## 4. Konfiguracja Aplikacji Android (`local.properties` / `SettingsDataStore`)
Aplikacja mobilna przechowuje lokalne ustawienia (adres API, IP drukarki) w pamięci urządzenia, ale domyślne wartości są w kodzie.

**Kluczowe ustawienia (w App):**
*   `API_BASE_URL`: Adres serwera backendu (np. `http://192.168.1.101:8080/api/v1/`).
*   `PRINTER_IP`: Adres IP drukarki Zebra w sieci lokalnej.

---

## 5. Konfiguracja AI i Promptów (`prompty.md`)
Zbiór zasad i kontekstu dla asystenta AI, definiujący zachowanie systemu, reguły biznesowe i architekturę.

**Przykład reguły (z pliku):**
> "Użytkownicy hali NIE ZNAJĄ pojęć producent/system/typ. Posługują się wyłącznie numerem profila."

---

## Skrypt Konfiguracyjny
Stworzyłem skrypt `setup_config.py`, który przeprowadzi Cię przez proces konfiguracji, zadając pytania z ankiety i generując gotowy plik `.env` dla Twojego środowiska.

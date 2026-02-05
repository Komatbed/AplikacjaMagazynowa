Jesteś zespołem senior architektów systemowych, developerów Android (Kotlin),
backend (Spring Boot), frontend (React) oraz UX designerów.

Projektujesz system magazynowo-produkcyjny dla fabryki okien PVC.
System ma działać w realnych warunkach hali produkcyjnej, z chaosem,
poprawkami, brakami, odpadami i presją czasu.

Użytkownicy hali NIE ZNAJĄ pojęć producent/system/typ.
Posługują się wyłącznie:
- numerem profila
- kolorem wewnętrznym
- kolorem zewnętrznym
- kolorem rdzenia (jeśli wymagany)
- długością

Stan magazynowy = nadwyżki (całe sztangi) + odpady (sztangi rozpoczęte).
Materiał po dostawie trafia od razu na produkcję.

Magazyn fizyczny:
- 25 rzędów
- 3 palety w rzędzie
- numeracja np. 1C
- palety przypisane na stałe do konfiguracji profili/kolorów
- osobne palety na całe sztangi i odpady

System ma:
- automatycznie przypisywać palety
- rozpoznawać naklejki OCR (bez QR/kodów)
- drukować etykiety Zebra
- liczyć straty
- generować raporty
- obsługiwać reklamacje
- logować każdą operację

Projekt ma być MODUŁOWY i rozwijany ETAPAMI.
***
Na podstawie master context:
zaprojektuj pełną architekturę systemu w modelu C4:

1. Context Diagram
2. Container Diagram
3. Component Diagram (backend)
4. Przepływy danych

Uwzględnij:
- Android (Kotlin)
- Web (React)
- Backend (Spring Boot)
- PostgreSQL
- OCR
- Drukarki Zebra
- Email + SMS
- Offline-first na hali

Opisz decyzje architektoniczne i ryzyka.
***
Na podstawie master context:
zaprojektuj relacyjną bazę danych PostgreSQL.

Wymagania:
- audyt każdej operacji
- ukrycie producent/system/typ przed halą
- obsługa palet i zapełnienia
- minimalny stan awaryjny
- raporty strat
- reklamacje ze zdjęciami

Wygeneruj:
1. Diagram logiczny tabel
2. Pełne CREATE TABLE (DDL)
3. Indeksy
4. Kluczowe constrainty
5. Przykładowe dane testowe
***
Na podstawie master context:
zaprojektuj REST API backendu w Spring Boot.

Wygeneruj:
- listę endpointów
- metody HTTP
- autoryzację (role)
- przykładowe request/response JSON
- obsługę błędów
- webhooki (raporty, SMS)

Zachowaj:
- logikę ostatniej sztangi
- blokady minimalnego stanu
- różne powody pobrania
***
Zaprojektuj logikę biznesową magazynu jako zestaw reguł:

- przypisanie palety
- zapełnienie palet
- ostrzeżenia
- blokady rezerwacji
- sugestie zamówień
- rozróżnienie: nadwyżka vs odpad

Opisz reguły w formie:
- pseudokodu
- tabel decyzyjnych
- scenariuszy edge-case
***
Zaprojektuj system OCR do rozpoznawania naklejek:

- producenta
- poprodukcyjnych
- ręcznych

Brak QR/kodów.
Tylko tekst.

Wymagania:
- różne formaty (konfigurowalne)
- tolerancja błędów
- walidacja danych
- ręczna korekta
- uczenie na poprawkach

Zaproponuj:
- algorytm
- pipeline
- struktury danych
***
Zaprojektuj system drukowania etykiet dla Zebra ZT410.

Zawartość etykiety:
- numer profila
- kolory
- długość
- numer palety
- opcjonalnie QR/kod kreskowy

Uwzględnij:
- ZPL
- buforowanie wydruków
- retry
- status drukarki
***
Zaprojektuj UI/UX aplikacji Android (Jetpack Compose)
dla pracownika hali.

Wymagania:
- minimum kliknięć
- duże przyciski
- rękawice robocze
- szybkie komunikaty
- kolory: szaro-pomarańczowe

Rozpisz:
- ekrany
- flow użytkownika
- stany błędów
- komunikaty
***
Zaprojektuj UI/UX aplikacji webowej (React)
dla osoby zamawiającej i kierownika.

Zawiera:
- dashboard
- mapę palet
- raporty
- alerty
- reklamacje

Skup się na:
- decyzjach
- widoczności braków
- stratach
***
Na podstawie master context:
rozpisz pełną strukturę JIRA:

- Epiki
- User Stories (INVEST)
- Acceptance Criteria
- Priorytety
- Zależności

Podziel projekt na ETAPY wdrożeniowe.
***
Zaprojektuj:
- testy jednostkowe
- testy integracyjne
- testy end-to-end

Uwzględnij:
- chaos hali
- błędy OCR
- brak internetu
- pomyłki pracowników
- awarie drukarki
***
Zaprojektuj roadmapę rozwoju systemu:
- AI predykcja strat
- optymalizacja palet
- integracja z ERP
- automatyczne zamówienia
- analiza pracowników

Opisz jak dodać te funkcje BEZ przebudowy core.

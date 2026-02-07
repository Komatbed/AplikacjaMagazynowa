# Instrukcja Instalacji i Uruchomienia Systemu

Niniejszy dokument opisuje krok po kroku proces instalacji, konfiguracji i uruchomienia kompletnego systemu magazynowego (Backend, AI Service, Baza Danych, Aplikacja Mobilna).

## Wymagania Systemowe

*   **System Operacyjny**: Windows 10/11, Linux (Ubuntu 20.04+), macOS.
*   **Docker Desktop**: Wymagany do uruchomienia serwerów (Backend + Baza + AI).
*   **Java JDK 17+**: Wymagane do budowania backendu (jeśli nie używamy Dockera) i aplikacji Android.
*   **Android Studio**: Do budowania i instalacji aplikacji mobilnej.
*   **Python 3.11+**: Do lokalnego uruchamiania serwisu AI (opcjonalnie).

## 1. Szybki Start (Docker Compose)

Najprostszy sposób na uruchomienie części serwerowej.

1.  Upewnij się, że Docker Desktop działa.
2.  Otwórz terminal w głównym katalogu projektu.
3.  Uruchom komendę:
    ```bash
    docker-compose up --build
    ```
4.  Poczekaj, aż wszystkie serwisy wystartują.
    *   **Backend API**: Dostępne pod `http://localhost:8080/api/v1/...`
    *   **Panel Webowy**: Dostępny pod `http://localhost:8080`
    *   **AI Service**: Dostępne pod `http://localhost:8000/docs` (Swagger UI)
    *   **Baza Danych**: Port 5432 (użytkownik: postgres, hasło: postgres)

## 1a. Uruchomienie Backendu (Bez Dockera)

Jeśli chcesz uruchomić backend bezpośrednio (np. do developmentu):

1.  Upewnij się, że masz zainstalowaną bazę PostgreSQL (lub użyj bazy z Dockera).
2.  Skonfiguruj połączenie do bazy w `backend/src/main/resources/application.properties`.
3.  Uruchom komendę w głównym katalogu:
    ```bash
    ./gradlew :backend:bootRun
    ```
    Lub zbuduj plik JAR:
    ```bash
    ./gradlew :backend:build
    java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
    ```

## 2. Instalacja Aplikacji Mobilnej (Android)

1.  Otwórz projekt w **Android Studio** (wskazując katalog `app` lub główny katalog projektu).
2.  Poczekaj na synchronizację Gradle.
3.  Podłącz telefon z włączonym trybem debugowania USB lub uruchom Emulator.
4.  W pliku `SettingsDataStore.kt` lub w Ustawieniach aplikacji po uruchomieniu, skonfiguruj adres IP serwera.
    *   Domyślnie emulator widzi hosta pod `10.0.2.2`.
    *   Na fizycznym urządzeniu musisz podać adres IP swojego komputera w sieci LAN (np. `192.168.1.15:8080`).
5.  Kliknij "Run" (zielony trójkąt).

## 3. Konfiguracja

### Backend (`backend/src/main/resources/application.properties`)
Edytuj ten plik, aby zmienić ustawienia bazy danych lub porty, jeśli uruchamiasz lokalnie bez Dockera. Zobacz `application-example.properties` po szczegóły.

### AI Service (`ai-service/.env`)
Utwórz plik `.env` na podstawie `.env.example`, aby dostosować parametry algorytmu cięcia (np. długość sztangi).

## 4. Rozwiązywanie Problemów

### Błąd: "Port is already in use"
Jeśli port 8080 lub 5432 jest zajęty, wyłącz inne usługi (np. inny lokalny Postgres) lub zmień mapowanie portów w `docker-compose.yml`.

### Błąd: Aplikacja Android nie łączy się z serwerem
1.  Sprawdź, czy telefon i komputer są w tej samej sieci WiFi.
2.  Upewnij się, że zapora sieciowa (Firewall) Windows nie blokuje portu 8080 dla Javy/Dockera.
3.  W ustawieniach aplikacji Android wpisz poprawny adres IP (sprawdź poleceniem `ipconfig` w terminalu Windows).

### Błąd: Baza danych jest pusta
Przy pierwszym uruchomieniu Docker powinien załadować plik `docs/DATABASE_SCHEMA.sql`. Jeśli to nie nastąpiło:
1.  Zatrzymaj kontenery: `docker-compose down -v` (usuwa wolumeny!).
2.  Uruchom ponownie: `docker-compose up --build`.

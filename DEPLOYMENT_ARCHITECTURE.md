# Architektura Wdrożeniowa i Plan Konfiguracji CI/CD

Dokument ten opisuje szczegółowy plan konfiguracji procesów Continuous Integration / Continuous Deployment (CI/CD) oraz architektury wdrożeniowej dla systemu "Aplikacja Magazynowa".

System składa się z czterech głównych warstw:
1.  **Frontend (Web UI)** - Aplikacja przeglądarkowa dla zarządzania.
2.  **Backend (Spring Boot)** - Logika biznesowa i API.
3.  **Infrastruktura (VPS/Docker)** - Baza danych, konteneryzacja, proxy.
4.  **Aplikacja Mobilna (Android)** - Narzędzie dla pracowników magazynu.

---

## 1. Frontend (Web UI)

### System Kontroli Wersji i Webhooki
*   **Repozytorium:** GitHub (obecne).
*   **Branching Strategy:** `main` (produkcja), `develop` (testy), `feature/*` (rozwój).
*   **Webhooki:** Skonfigurowane w GitHub Settings -> Webhooks, celujące w endpoint na VPS (opcjonalnie, do triggerowania `deploy.sh`) lub wykorzystujące GitHub Actions Runner.

### Pipeline CI/CD (GitHub Actions)
Plik: `.github/workflows/frontend_deploy.yml`
1.  **Trigger:** Push do `webApplication/**` na branchu `main`.
2.  **Build:**
    *   `docker build` - tworzenie obrazu kontenera z aplikacją (Nginx + pliki statyczne).
    *   Tagowanie obrazu wersją (SHA commit) oraz tagiem `latest`.
3.  **Publish:**
    *   Logowanie do Docker Hub (używając `secrets.DOCKERHUB_USERNAME` i `TOKEN`).
    *   `docker push` - wysłanie obrazu do rejestru.
4.  **Deploy (Strategia VPS):**
    *   Logowanie po SSH do VPS (używając `secrets.VPS_SSH_KEY`).
    *   Uruchomienie skryptu `deploy.sh` lub komendy `docker compose pull && docker compose up -d`.

### Strategia Deploymentu
*   **Środowisko:** VPS z Docker Compose.
*   **Cache Invalidation:** Konfiguracja Nginx (`expires`, `Cache-Control`) dla plików statycznych. Wersjonowanie plików (hash w nazwie) jest zalecane przy budowie SPA (np. Vite/Webpack), tutaj (czysty JS/HTML) polegamy na konfiguracji serwera.
*   **Zmienne Środowiskowe:**
    *   Plik `.env` na serwerze (nie w repozytorium!).
    *   Wstrzykiwanie `API_URL` podczas startu kontenera (np. przez `subst` w Nginx lub zmienną globalną `window.env` w `index.html`).

---

## 2. Backend (Spring Boot)

### Automatyzacja Builda
Plik: `.github/workflows/backend_ci.yml`
1.  **Build Aplikacji:**
    *   `setup-java` (JDK 17/21).
    *   `./gradlew bootJar` - kompilacja i testy jednostkowe.
2.  **Docker Build:**
    *   `Dockerfile` wieloetapowy (builder -> runtime).
    *   Optymalizacja warstw (osobno zależności, osobno kod).

### Orchestracja i Registry
*   **Registry:** Docker Hub (prywatne repozytorium).
*   **Orchestracja:** Docker Compose (obecna skala).
    *   Dla zapewnienia **Zero Downtime** (Rolling Update) w przyszłości: przejście na Docker Swarm lub Kubernetes (K8s), albo prosta implementacja Blue-Green Deployment na poziomie Nginx (dwa kontenery backendu, przełączanie ruchu).

### Migracje Bazy Danych
*   **Narzędzie:** Flyway (zalecane dla Spring Boot).
*   **Proces:**
    *   Skrypty SQL w `src/main/resources/db/migration`.
    *   Uruchamianie migracji automatycznie przy starcie aplikacji (`spring.flyway.enabled=true`).
    *   Weryfikacja: Testy integracyjne z bazą Testcontainers w CI.

### Konfiguracja Środowiskowa
*   **Secrets:** Hasła do DB, klucze JWT przechowywane w pliku `.env` na serwerze, mapowane w `docker-compose.yml`.
*   **Spring Profiles:** `application-prod.properties` aktywowany przez `SPRING_PROFILES_ACTIVE=prod`.

---

## 3. Baza Danych i Infrastruktura

### Infrastructure as Code (IaC)
*   **Docker Compose:** Definicja całej infrastruktury w `docker-compose.yml` (wersjonowana w Git).
*   **Setup Serwera:** Skrypty Bash (`setup_vps.sh`) do wstępnej konfiguracji (instalacja Dockera, Firewalla, użytkowników).

### Baza Danych (PostgreSQL)
*   **Wersjonowanie Schematu:** Flyway (zarządzane przez aplikację Backend).
*   **Backup Automation:**
    *   Kontener `backup-service` (prosty obraz z klientem postgres).
    *   Cron job: `pg_dump` codziennie w nocy.
    *   Retencja: Lokalne kopie z ostatnich 7 dni + upload do chmury (np. AWS S3 / Google Cloud Storage) przy użyciu AWS CLI.

### Monitoring i Alerting
*   **Stack:** Prometheus + Grafana.
*   **Metryki:**
    *   **Backend:** Spring Boot Actuator (`/actuator/prometheus`) - metryki JVM, HTTP, DB pool.
    *   **Host:** Node Exporter (CPU, RAM, Disk).
    *   **Docker:** cAdvisor (zasoby kontenerów).
*   **Health Checks:**
    *   Nginx sprawdza dostępność backendu (`/actuator/health`).
    *   Docker `healthcheck` w `docker-compose.yml` (restartuje kontenery w razie awarii).

---

## 4. Aplikacja Mobilna (Android)

### Build Automation
Plik: `.github/workflows/android_ci.yml`
1.  **Trigger:** Push do `app/**` lub tag `v*`.
2.  **Environment:** `ubuntu-latest` z Java i Android SDK.
3.  **Build:**
    *   `./gradlew assembleRelease` - budowanie pliku APK/AAB.
    *   Obsługa `local.properties` i `google-services.json` poprzez GitHub Secrets (zakodowane base64 i dekodowane w locie).

### Code Signing (Podpisywanie)
*   **Keystore:** Plik `.jks` przechowywany jako Secret (base64).
*   **Zmienne:** `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` w Secrets.
*   Gradle skonfigurowany do pobierania tych wartości ze zmiennych środowiskowych CI.

### Dystrybucja i Testy
*   **Firebase App Distribution:**
    *   Dla testerów i QA.
    *   Automatyczny upload po udanym buildzie na branchu `develop`.
    *   Release Notes generowane z commitów.
*   **Testy Automatyczne:**
    *   Unit Testy (`./gradlew test`).
    *   Instrumented Tests (UI): Firebase Test Lab (uruchamianie na fizycznych urządzeniach w chmurze Google).

### OTA (Over-The-Air Updates)
*   Dla aplikacji natywnej (Kotlin) pełne OTA kodu nie jest możliwe (wymaga aktualizacji APK).
*   **Rozwiązanie:** Wymuszanie aktualizacji. Backend wystawia endpoint `/api/version/android`. Aplikacja przy starcie sprawdza wersję i jeśli jest starsza niż `min_supported_version`, blokuje działanie i kieruje do pobrania nowej wersji (np. z linku bezpośredniego lub App Distribution).

---

## Podsumowanie Zadań Konfiguracyjnych

### Do Wykonania (TODO):
1.  [ ] **Backend:** Dodać `Flyway` do zależności i stworzyć pierwsze skrypty migracyjne.
2.  [ ] **Backend:** Skonfigurować `Spring Actuator` dla monitoringu.
3.  [ ] **Mobile:** Wygenerować `Keystore` dla produkcji i dodać do GitHub Secrets.
4.  [ ] **Infra:** Uzupełnić `docker-compose.yml` o kontenery `prometheus` i `grafana`.
5.  [ ] **Infra:** Skonfigurować skrypt backupu bazy danych.
6.  [ ] **CI/CD:** Utworzyć workflow dla Androida (`android_ci.yml`).

# Instrukcja Instalacji i Wdrożenia Web UI

## Wymagania Systemowe

- **System Operacyjny:** Linux (Ubuntu 20.04/22.04 LTS zalecane), Windows 10/11 Pro (z WSL2), lub macOS
- **Oprogramowanie:**
  - Docker (v20.10+)
  - Docker Compose (v2.0+)
  - Git
- **Sprzęt (Minimalne):**
  - CPU: 2 vCPU
  - RAM: 4 GB
  - Dysk: 20 GB wolnego miejsca

## Konfiguracja Środowiska

1.  **Sklonuj repozytorium:**
    ```bash
    git clone https://github.com/twoja-organizacja/aplikacja-magazynowa.git
    cd aplikacja-magazynowa/webApplication
    ```

2.  **Zmienne Środowiskowe:**
    Aplikacja webowa (frontend) w obecnej konfiguracji używa `js/api.js` do komunikacji.
    Adres API jest konfigurowany w `js/api.js`. W środowisku produkcyjnym zaleca się użycie reverse proxy (Nginx) skonfigurowanego w `nginx/default.conf` do przekierowywania zapytań `/api` do backendu, co eliminuje problemy z CORS i sztywnym kodowaniem adresów URL.

## Uruchomienie Produkcyjne (Docker)

1.  **Zbuduj i uruchom kontenery:**
    ```bash
    docker-compose up -d --build
    ```

2.  **Weryfikacja:**
    Otwórz przeglądarkę i wejdź na adres `http://localhost` (lub adres IP serwera).

    Domyślne konto (jeśli backend nie jest podłączony, działa tryb Demo):
    - Login: `admin`
    - Hasło: `admin`

## Konfiguracja Nginx i SSL/TLS

W pliku `nginx/default.conf` znajduje się podstawowa konfiguracja. Aby włączyć SSL (HTTPS):

1.  Wygeneruj certyfikaty (np. Let's Encrypt) lub kup komercyjne.
2.  Zmontuj certyfikaty do kontenera w `docker-compose.yml`:
    ```yaml
    volumes:
      - ./certs:/etc/nginx/certs
    ```
3.  Zaktualizuj `nginx/default.conf`:
    ```nginx
    server {
        listen 443 ssl;
        server_name twoja-domena.com;
        ssl_certificate /etc/nginx/certs/fullchain.pem;
        ssl_certificate_key /etc/nginx/certs/privkey.pem;
        # ... reszta konfiguracji
    }
    ```

## CI/CD (GitHub Actions)

W katalogu `.github/workflows` (należy utworzyć w root repozytorium) przykładowy plik `deploy.yml`:

```yaml
name: Deploy Web UI

on:
  push:
    branches: [ "main" ]
    paths: [ "webApplication/**" ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Docker Image
        run: |
          cd webApplication
          docker build -t my-registry/web-app:latest .
      
      # Krok logowania do rejestru (np. Docker Hub, GHCR)
      # - name: Login to Docker Hub
      #   uses: docker/login-action@v2
      #   ...

      # Krok pushowania obrazu
      # - name: Push Image
      #   run: docker push my-registry/web-app:latest

      # Krok deploymentu (np. przez SSH na serwer)
      # - name: Deploy to Server
      #   uses: appleboy/ssh-action@master
      #   ...
```

## Procedury Backup i Rollback

### Backup
Ponieważ Web UI jest bezstanowy (statyczne pliki HTML/JS), backup dotyczy głównie konfiguracji i kodu (Git).
Dane są przechowywane w bazie danych backendu, która powinna mieć własną politykę backupu (np. pg_dump dla PostgreSQL).

### Rollback
W przypadku problemów po wdrożeniu:
1.  Cofnij zmiany w Git: `git revert HEAD`
2.  Przebuduj i zrestartuj kontenery: `docker-compose up -d --build`
3.  Lub jeśli używasz tagowania obrazów, zmień wersję w `docker-compose.yml` na poprzednią działającą.

## Security Audit & Testy

Przed wdrożeniem wykonano:
- **Testy Integracyjne:** Sprawdzenie komunikacji z API (mock/demo fallback).
- **Security:**
  - Nginx skonfigurowany z nagłówkami bezpieczeństwa (X-Frame-Options, X-XSS-Protection).
  - Brak wrażliwych danych w kodzie frontendowym (hasła, klucze API).
  - Zalecane włączenie HTTPS w produkcji.

## Skrypt Automatycznej Instalacji i Aktualizacji

W katalogu projektu znajduje się skrypt `deploy.sh`, który automatyzuje proces instalacji i aktualizacji aplikacji na serwerze VPS (Ubuntu/Debian).

### Użycie:

1.  Skopiuj skrypt na serwer lub sklonuj repozytorium.
2.  Nadaj uprawnienia wykonywania:
    ```bash
    chmod +x deploy.sh
    ```
3.  Uruchom skrypt (wymaga uprawnień root):
    ```bash
    sudo ./deploy.sh
    ```

### Co robi skrypt?
1.  Aktualizuje pakiety systemowe.
2.  Instaluje Docker i Docker Compose (jeśli nie są obecne).
3.  Klonuje repozytorium (lub pobiera najnowsze zmiany `git pull`).
4.  Buduje i uruchamia kontenery (`docker compose up -d --build`).
5.  Czyści nieużywane obrazy Dockera.

**Uwaga:** Przed pierwszym uruchomieniem edytuj zmienną `REPO_URL` w pliku `deploy.sh`, aby wskazywała na twoje repozytorium Git.


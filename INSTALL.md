# Instrukcja Instalacji i Konfiguracji Systemu Magazynowego (Production)

Dokument opisuje proces instalacji kompletnego systemu (Backend + Web UI + Baza Danych + Monitoring) na środowisku produkcyjnym (VPS).

## 1. Wymagania Systemowe
- **System Operacyjny:** Ubuntu 22.04 LTS (zalecane) lub inny Linux z obsługą Dockera.
- **CPU:** Minimum 2 vCPU.
- **RAM:** Minimum 4 GB RAM (dla Spring Boot + Postgres + kontenery pomocnicze).
- **Dysk:** 20 GB wolnego miejsca.
- **Porty:** 
  - 80 (HTTP)
  - 443 (HTTPS)
  - 22 (SSH)

## 2. Przygotowanie Środowiska (VPS)

1. **Aktualizacja systemu:**
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

2. **Instalacja Dockera:**
   ```bash
   # Dodanie kluczy GPG i repozytorium Dockera
   sudo apt-get install -y ca-certificates curl gnupg lsb-release
   sudo mkdir -p /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   sudo apt-get update
   sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
   ```

3. **Utworzenie użytkownika `deployer` (opcjonalne, zalecane dla bezpieczeństwa):**
   ```bash
   sudo useradd -m -s /bin/bash deployer
   sudo usermod -aG docker deployer
   sudo su - deployer
   ```

## 3. Instalacja Aplikacji

1. **Klonowanie repozytorium:**
   ```bash
   mkdir -p ~/warehouse
   git clone https://github.com/twoja-organizacja/aplikacja-magazynowa.git ~/warehouse
   cd ~/warehouse
   ```

2. **Konfiguracja zmiennych środowiskowych:**
   Utwórz plik `.env` w katalogu głównym:
   ```bash
   cp .env.example .env
   nano .env
   ```
   **Wymagane zmienne:**
   ```env
   DB_USER=warehouse_user
   DB_PASSWORD=silne_haslo_produkcyjne
   SERVER_PORT=8080
   JWT_SECRET=bardzo_dlugi_i_losowy_klucz_jwt
   ```

3. **Uruchomienie skryptu instalacyjnego:**
   Użyj przygotowanego menedżera deploymentu:
   ```bash
   chmod +x scripts/deploy_manager.sh
   ./scripts/deploy_manager.sh
   ```
   Skrypt automatycznie:
   - Zbuduje obrazy Dockera (Backend i Web UI).
   - Uruchomi kontenery zdefiniowane w `docker-compose.prod.yml`.
   - Przeprowadzi Health Check.

## 4. Konfiguracja Automatycznych Aktualizacji (CI/CD & Cron)

### Opcja A: GitHub Actions (Push-based)
Skonfiguruj workflow `.github/workflows/deploy.yml` (znajduje się w repozytorium), aby łączył się z VPS przez SSH i uruchamiał `deploy_manager.sh` po każdym merge'u do `main`.

### Opcja B: Polling (Pull-based)
Jeśli VPS nie jest dostępny z zewnątrz dla GitHub Actions, skonfiguruj CRON do sprawdzania zmian:

1. Edytuj crontab:
   ```bash
   crontab -e
   ```
2. Dodaj wpis (sprawdzanie co 5 minut):
   ```cron
   */5 * * * * /home/deployer/warehouse/scripts/monitor_repo.sh >> /home/deployer/warehouse/logs/cron.log 2>&1
   ```

## 5. Backup i Rollback

### Backup
Skrypt `deploy_manager.sh` automatycznie wykonuje zrzut bazy danych przed każdą aktualizacją.
Pliki backupu znajdują się w: `~/warehouse/backups/app/YYYYMMDD_HHMMSS/db_backup.sql`.

Ręczne wymuszenie backupu:
```bash
./scripts/backup_db.sh
```

### Rollback (Wycofanie zmian)
W przypadku awarii po aktualizacji (np. błąd Health Check), skrypt `deploy_manager.sh` automatycznie spróbuje przywrócić poprzednią wersję.
Ręczny rollback:
1. Cofnij kod:
   ```bash
   git reset --hard HEAD@{1}
   ```
2. Przebuduj:
   ```bash
   ./scripts/deploy_manager.sh
   ```

## 6. Monitoring i Logi

- **Status kontenerów:** `docker compose -f docker-compose.prod.yml ps`
- **Logi backendu:** `docker logs -f warehouse-backend-prod`
- **Logi Nginx:** `docker logs -f warehouse-nginx-prod`
- **Metryki (Prometheus/Grafana):** Dostępne na portach 9090 (Prometheus) i 3000 (Grafana), jeśli odblokowane w firewallu (zalecane tunelowanie SSH lub VPN).

## 7. Web UI - Konfiguracja Specyficzna
Frontend jest serwowany przez Nginx. Plik konfiguracyjny `env-config.js` jest generowany dynamicznie przy starcie kontenera (przez `docker-entrypoint.sh`), aby wstrzyknąć zmienne środowiskowe do statycznych plików JS.
Aby zmienić adres API na froncie, edytuj `.env` lub zmienne kontenera `warehouse-web-prod`.

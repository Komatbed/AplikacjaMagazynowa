#!/bin/bash

# Ustawienia skryptu
APP_DIR="/opt/aplikacja-magazynowa/webApplication"
REPO_URL="https://github.com/twoja-organizacja/aplikacja-magazynowa.git" # Zmień na właściwy URL
BRANCH="main"

# Kolory do logów
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Funkcja logowania
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARN: $1${NC}"
}

err() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

# Sprawdzenie czy skrypt uruchomiono jako root
if [ "$EUID" -ne 0 ]; then
  err "Ten skrypt musi być uruchomiony jako root (sudo)."
  exit 1
fi

log "Rozpoczynanie procesu instalacji/aktualizacji..."

# 1. Aktualizacja systemu i instalacja zależności
log "Aktualizacja pakietów systemowych..."
apt-get update && apt-get upgrade -y
apt-get install -y git curl ca-certificates gnupg lsb-release

# 2. Instalacja Docker i Docker Compose (jeśli brak)
if ! command -v docker &> /dev/null; then
    log "Instalacja Docker..."
    mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
else
    log "Docker jest już zainstalowany."
fi

# 3. Przygotowanie katalogu aplikacji
if [ ! -d "$APP_DIR" ]; then
    log "Klonowanie repozytorium do $APP_DIR..."
    mkdir -p "$(dirname "$APP_DIR")"
    git clone -b "$BRANCH" "$REPO_URL" "$(dirname "$APP_DIR")" || {
        err "Nie udało się sklonować repozytorium. Sprawdź URL i uprawnienia."
        # Fallback: Jeśli skrypt jest uruchamiany wewnątrz repozytorium, użyj bieżącego katalogu
        if [ -f "docker-compose.yml" ]; then
            warn "Wykryto uruchomienie wewnątrz projektu. Używam bieżącego katalogu."
            APP_DIR=$(pwd)
        else
            exit 1
        fi
    }
else
    log "Repozytorium istnieje. Pobieranie zmian..."
    cd "$APP_DIR" || exit
    git fetch origin
    git pull origin "$BRANCH"
fi

# Przejdź do katalogu aplikacji (na wypadek gdybyśmy tam nie byli)
cd "$APP_DIR" || exit

# 4. Obsługa konfiguracji (.env)
if [ ! -f .env ]; then
    warn "Brak pliku .env. Tworzenie domyślnego..."
    # Tutaj można zdefiniować domyślne zmienne
    echo "# Auto-generated .env" > .env
fi

# 5. Uruchomienie aplikacji przez Docker Compose
log "Przebudowa i restart kontenerów..."
docker compose down --remove-orphans || true
docker compose up -d --build

# 6. Weryfikacja
if [ $? -eq 0 ]; then
    log "Sukces! Aplikacja została zaktualizowana i uruchomiona."
    log "Sprawdź status: docker compose ps"
else
    err "Wystąpił błąd podczas uruchamiania docker compose."
    exit 1
fi

# 7. Czyszczenie (opcjonalne)
docker image prune -f

log "Gotowe."

# 8. Konfiguracja automatycznych aktualizacji (CRON)
# Uruchamia skrypt codziennie o 3:00 rano
SCRIPT_PATH=$(readlink -f "$0")
CRON_JOB="0 3 * * * root $SCRIPT_PATH >> /var/log/app_deploy.log 2>&1"

if [ -f /etc/cron.d/app_updates ]; then
    log "CRON już skonfigurowany."
else
    log "Konfiguracja automatycznych aktualizacji (CRON)..."
    echo "$CRON_JOB" > /etc/cron.d/app_updates
    chmod 0644 /etc/cron.d/app_updates
    log "Utworzono zadanie CRON: /etc/cron.d/app_updates"
fi


#!/bin/bash

# ==============================================================================
# Deploy Manager - Warehouse System
# ==============================================================================
# Skrypt zarządzający procesem aktualizacji, backupu i rollbacku.
# Uruchamiany przez monitor_repo.sh lub ręcznie.
# ==============================================================================

# Konfiguracja
APP_DIR="/home/deployer/warehouse"
DOCKER_COMPOSE_FILE="docker-compose.prod.yml"
BACKUP_DIR="$APP_DIR/backups/app"
LOG_FILE="$APP_DIR/logs/deploy.log"
DATE_TAG=$(date +'%Y%m%d_%H%M%S')

# Funkcja logowania
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Funkcja powiadomień (Mock - do uzupełnienia o curl do Slack/Discord)
notify() {
    local message="$1"
    local type="$2" # success, error, info
    log "NOTIFICATION [$type]: $message"
    # Przykład Discord:
    # curl -H "Content-Type: application/json" -X POST -d "{\"content\": \"$message\"}" $DISCORD_WEBHOOK_URL
}

# 1. Sprawdzenie wymagań
cd "$APP_DIR" || { log "Nie znaleziono katalogu aplikacji!"; exit 1; }

log "=== Rozpoczynanie Deploymentu: $DATE_TAG ==="

# 2. Backup obecnej wersji (prosty backup docker images + db dump)
log "Tworzenie backupu..."
mkdir -p "$BACKUP_DIR/$DATE_TAG"

# Backup bazy danych (jeśli kontener działa)
if docker ps | grep -q warehouse-db-prod; then
    docker exec warehouse-db-prod pg_dump -U postgres warehouse > "$BACKUP_DIR/$DATE_TAG/db_backup.sql"
    log "Baza danych zrzucona."
else
    log "Kontener bazy nie działa, pomijanie zrzutu."
fi

# 3. Pull najnowszego kodu
log "Pobieranie kodu z repozytorium..."
git fetch origin
git reset --hard origin/main # Lub inny branch produkcyjny

# 4. Budowanie nowych obrazów
log "Budowanie kontenerów..."
if docker compose -f "$DOCKER_COMPOSE_FILE" build; then
    log "Budowanie zakończone sukcesem."
else
    notify "Błąd budowania obrazów! Przerywanie aktualizacji." "error"
    exit 1
fi

# 5. Aktualizacja (Rolling Update / Recreate)
log "Restartowanie usług..."
# Pobranie ID starego kontenera backendu do weryfikacji
OLD_CONTAINER_ID=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q backend)

if docker compose -f "$DOCKER_COMPOSE_FILE" up -d; then
    log "Kontenery uruchomione."
else
    notify "Błąd uruchamiania kontenerów!" "error"
    # Tutaj można dodać logikę rollbacku do poprzednich obrazów
    exit 1
fi

# 6. Health Check (Weryfikacja Live)
log "Oczekiwanie na start aplikacji (Health Check)..."
sleep 15 # Czas na start Spring Boota

HEALTH_URL="http://localhost:8080/actuator/health" # Bezpośrednio do backendu lub przez Nginx
MAX_RETRIES=10
RETRY_COUNT=0
HEALTHY=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL")
    if [ "$HTTP_CODE" == "200" ]; then
        HEALTHY=true
        break
    fi
    log "Próba $RETRY_COUNT: Status $HTTP_CODE. Czekam..."
    sleep 5
    ((RETRY_COUNT++))
done

if [ "$HEALTHY" = true ]; then
    log "Health Check OK. Aplikacja działa."
    
    # Czyszczenie starych obrazów (prune)
    docker image prune -f
    
    notify "Deployment zakończony sukcesem! Wersja: $DATE_TAG" "success"
else
    log "Health Check FAILED! Rozpoczynanie Rollbacku..."
    notify "Health Check nie powiódł się. Wycofywanie zmian..." "error"
    
    # --- ROLLBACK STRATEGY ---
    # W prostym Docker Compose rollback to zazwyczaj revert kodu + rebuild lub użycie otagowanych obrazów
    # Tutaj zrobimy prosty revert gita (zakładając że poprzedni commit był OK)
    
    git reset --hard HEAD@{1}
    docker compose -f "$DOCKER_COMPOSE_FILE" up -d --build
    
    notify "Rollback wykonany. Sprawdź logi." "info"
    exit 1
fi

log "=== Deployment Zakończony ==="

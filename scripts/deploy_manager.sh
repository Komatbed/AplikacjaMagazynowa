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

# Konfiguracja Email (Bramka HTTP)
EMAIL_RECIPIENT="mateuszbednarczyk99@gmail.com"
GATEWAY_URL="https://formsubmit.co/$EMAIL_RECIPIENT"

# Funkcja logowania
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Funkcja wysyłania emaila z logami przez bramkę
send_error_email() {
    local subject="[CRITICAL] Warehouse Deployment FAILED"
    
    log "Wysyłanie powiadomienia do bramki email ($EMAIL_RECIPIENT)..."
    
    # Pobranie ostatnich 50 linii logów
    local logs=$(tail -n 50 "$LOG_FILE")
    
    # Wysłanie POST przez curl
    # -F form-data dla lepszej obsługi multiline
    curl -s -X POST "$GATEWAY_URL" \
        -F "_subject=$subject" \
        -F "message=Deployment failed at $(date). Logs below:" \
        -F "logs=$logs" \
        -F "_captcha=false" \
        > /dev/null
        
    if [ $? -eq 0 ]; then
        log "Powiadomienie wysłane do bramki."
    else
        log "Błąd wysyłania do bramki."
    fi
}

# Funkcja powiadomień
notify() {
    local message="$1"
    local type="$2" # success, error, info
    log "NOTIFICATION [$type]: $message"
    
    if [ "$type" == "error" ]; then
        send_error_email
    fi
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

# 5. Aktualizacja (Rolling Update / Recreate) z mechanizmem "Start za wszelką cenę"
log "Restartowanie usług (Retry Mode)..."

start_services() {
    docker compose -f "$DOCKER_COMPOSE_FILE" up -d
}

MAX_START_RETRIES=3
START_RETRY=0
STARTED=false

while [ $START_RETRY -lt $MAX_START_RETRIES ]; do
    if start_services; then
        STARTED=true
        log "Kontenery uruchomione poprawnie."
        break
    else
        log "BŁĄD startu (Próba $((START_RETRY+1))/$MAX_START_RETRIES). Przystępuję do procedury naprawczej..."
        
        # Procedura "Start za wszelką cenę"
        log ">>> FORCE CLEANUP <<<"
        
        # 1. Zatrzymaj wszystko
        docker compose -f "$DOCKER_COMPOSE_FILE" down --remove-orphans
        
        # 2. Usuń potencjalnie zablokowane kontenery ręcznie
        docker ps -aq | xargs -r docker rm -f
        
        # 3. Wyczyść sieć (może być zajęta)
        docker network prune -f
        
        # 4. Zwolnij porty (Ostrożnie! Tylko jeśli jesteśmy pewni)
        # fuser -k 80/tcp 443/tcp 8080/tcp
        
        log "Cleanup zakończony. Ponowna próba startu za 5 sekund..."
        sleep 5
        ((START_RETRY++))
    fi
done

if [ "$STARTED" = false ]; then
    notify "KRYTYCZNY BŁĄD: Nie udało się uruchomić kontenerów po $MAX_START_RETRIES próbach!" "error"
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

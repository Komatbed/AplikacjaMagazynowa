#!/bin/bash

# ==============================================================================
# Git Monitor - Warehouse System
# ==============================================================================
# Skrypt monitorujący repozytorium. Jeśli wykryje zmiany, uruchamia deploy_manager.sh.
# Dodaj do crontab: */5 * * * * /home/deployer/warehouse/scripts/monitor_repo.sh
# ==============================================================================

APP_DIR="/home/deployer/warehouse"
SCRIPT_DIR="$APP_DIR/scripts"
LOG_FILE="$APP_DIR/logs/monitor.log"

cd "$APP_DIR" || exit 1

# Pobranie zmian bez merge'owania
git fetch origin main

# Porównanie lokalnego HEAD z zdalnym main
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/main)

if [ "$LOCAL" != "$REMOTE" ]; then
    echo "[$(date)] Wykryto zmiany w repozytorium. Uruchamianie deploymentu..." >> "$LOG_FILE"
    echo "[$(date)] Local: $LOCAL | Remote: $REMOTE" >> "$LOG_FILE"
    
    # Uruchomienie skryptu deploymentu
    chmod +x "$SCRIPT_DIR/deploy_manager.sh"
    "$SCRIPT_DIR/deploy_manager.sh" >> "$APP_DIR/logs/deploy_full.log" 2>&1
    
else
    # Opcjonalnie loguj brak zmian (tylko debug)
    # echo "[$(date)] Brak zmian." >> "$LOG_FILE"
    :
fi

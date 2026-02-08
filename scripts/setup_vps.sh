#!/bin/bash

# ==============================================================================
# VPS Setup Script - Warehouse System
# ==============================================================================
# Ten skrypt przygotowuje czysty serwer Ubuntu 22.04/24.04 do uruchomienia aplikacji.
# Uruchom jako root: sudo ./setup_vps.sh
# ==============================================================================

set -e

# Kolory do logowania
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

LOG_FILE="/var/log/warehouse_setup.log"

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
    echo "[ERROR] $1" >> "$LOG_FILE"
    exit 1
}

# Sprawdzenie uprawnień roota
if [ "$EUID" -ne 0 ]; then
  error "Proszę uruchomić jako root"
fi

log "Rozpoczynanie konfiguracji VPS..."

# 1. Aktualizacja systemu
log "Aktualizacja pakietów systemowych..."
apt-get update && apt-get upgrade -y
apt-get install -y curl wget git unzip htop software-properties-common ca-certificates gnupg lsb-release jq

# 2. Konfiguracja Firewalla (UFW)
log "Konfiguracja UFW..."
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
# Otwieramy porty dla aplikacji jeśli nie używamy Nginx jako proxy (opcjonalne)
# ufw allow 8080/tcp 
ufw --force enable
log "UFW włączony."

# 3. Instalacja Fail2Ban
log "Instalacja Fail2Ban..."
apt-get install -y fail2ban
systemctl enable fail2ban
systemctl start fail2ban

# 4. Instalacja Docker i Docker Compose
log "Instalacja Docker..."
if ! command -v docker &> /dev/null; then
    mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
else
    log "Docker już zainstalowany."
fi

# 5. Tworzenie użytkownika deployer
log "Konfiguracja użytkownika 'deployer'..."
if id "deployer" &>/dev/null; then
    log "Użytkownik deployer już istnieje."
else
    useradd -m -s /bin/bash deployer
    usermod -aG sudo deployer
    usermod -aG docker deployer
    
    # Generowanie losowego hasła dla użytkownika deployer
    RANDOM_PASS=$(date +%s | sha256sum | base64 | head -c 16)
    echo "deployer:$RANDOM_PASS" | chpasswd
    
    log "Użytkownik deployer utworzony."
    log "!!! WAŻNE !!! Wygenerowane hasło dla 'deployer': $RANDOM_PASS"
    echo -e "${YELLOW}!!! WAŻNE !!! Wygenerowane hasło dla 'deployer': $RANDOM_PASS${NC}"
    echo -e "${YELLOW}Zapisz je, aby móc się zalogować!${NC}"
fi

# 5a. Dodanie obecnego użytkownika (jeśli nie root) do grupy docker
if [ "$SUDO_USER" ]; then
    REAL_USER=$SUDO_USER
    if [ "$REAL_USER" != "deployer" ]; then
        log "Dodawanie użytkownika '$REAL_USER' do grupy docker..."
        usermod -aG docker "$REAL_USER"
    fi
fi

# 6. Przygotowanie katalogów aplikacji
APP_DIR="/home/deployer/warehouse"
log "Tworzenie struktury katalogów w $APP_DIR..."
mkdir -p "$APP_DIR/logs"
mkdir -p "$APP_DIR/backups/db"
mkdir -p "$APP_DIR/backups/app"
mkdir -p "$APP_DIR/nginx"
chown -R deployer:deployer "$APP_DIR"

log "Struktura katalogów gotowa."

# 7. Konfiguracja Autostartu (Systemd)
log "Konfiguracja serwisu systemd..."
SERVICE_FILE="/etc/systemd/system/warehouse.service"

cat > "$SERVICE_FILE" <<EOF
[Unit]
Description=Warehouse System Docker Compose Service
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$APP_DIR
# Use 'docker compose' (v2) or 'docker-compose' (v1) depending on availability
ExecStart=/bin/bash -c 'if docker compose version >/dev/null 2>&1; then docker compose -f docker-compose.prod.yml up -d; else docker-compose -f docker-compose.prod.yml up -d; fi'
ExecStop=/bin/bash -c 'if docker compose version >/dev/null 2>&1; then docker compose -f docker-compose.prod.yml down; else docker-compose -f docker-compose.prod.yml down; fi'
User=deployer
Group=docker
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

chmod 644 "$SERVICE_FILE"
systemctl daemon-reload
systemctl enable warehouse.service

log "Serwis warehouse.service został utworzony i włączony."

log "============================================================"
log " INSTALACJA ZAKOŃCZONA SUKCESEM"

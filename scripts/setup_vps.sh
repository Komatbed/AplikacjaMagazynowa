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
    # Hasło zostanie ustawione ręcznie lub klucz SSH zostanie dodany później
    log "Użytkownik deployer utworzony. Pamiętaj o dodaniu klucza SSH!"
fi

# 6. Przygotowanie katalogów aplikacji
APP_DIR="/home/deployer/warehouse"
log "Tworzenie struktury katalogów w $APP_DIR..."
mkdir -p "$APP_DIR/logs"
mkdir -p "$APP_DIR/backups/db"
mkdir -p "$APP_DIR/backups/app"
mkdir -p "$APP_DIR/nginx"
mkdir -p "$APP_DIR/certbot/conf"
mkdir -p "$APP_DIR/certbot/www"

# Nadanie uprawnień
chown -R deployer:deployer /home/deployer/warehouse

# 7. Instalacja dodatkowych narzędzi (opcjonalnie: Node, Python)
# W kontenerach nie jest to wymagane na hoście, ale może się przydać do skryptów pomocniczych
log "Instalacja Python3 i pip..."
apt-get install -y python3 python3-pip

# 8. Utwardzanie SSH (Ostrożnie!)
SSH_CONFIG="/etc/ssh/sshd_config"
log "Utwardzanie SSH..."
if grep -q "PermitRootLogin yes" "$SSH_CONFIG"; then
    sed -i 's/PermitRootLogin yes/PermitRootLogin no/' "$SSH_CONFIG"
    log "Zablokowano logowanie roota przez SSH."
fi
# Opcjonalnie wyłącz logowanie hasłem (wymaga wgranego klucza!)
# sed -i 's/PasswordAuthentication yes/PasswordAuthentication no/' "$SSH_CONFIG"

systemctl restart ssh

log "Konfiguracja zakończona sukcesem! Przejdź do użytkownika deployer: 'su - deployer'"

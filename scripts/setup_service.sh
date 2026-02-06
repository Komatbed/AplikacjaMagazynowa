#!/bin/bash

# ==============================================================================
# Warehouse System - Auto-start Configuration (Systemd)
# ==============================================================================
# This script creates a systemd service to automatically start the Warehouse 
# application stack on server boot.
# Run as root: sudo ./setup_service.sh
# ==============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

LOG_FILE="/var/log/warehouse_service_setup.log"

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
    exit 1
}

if [ "$EUID" -ne 0 ]; then
  error "Please run as root (sudo ./setup_service.sh)"
fi

APP_DIR="/home/deployer/warehouse"
SERVICE_FILE="/etc/systemd/system/warehouse.service"

log "Configuring Warehouse System auto-start..."

# 1. Create Systemd Service File
log "Creating systemd service file at $SERVICE_FILE..."

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

# 2. Set permissions
chmod 644 "$SERVICE_FILE"

# 3. Reload Systemd
log "Reloading systemd daemon..."
systemctl daemon-reload

# 4. Enable and Start Service
log "Enabling warehouse.service..."
systemctl enable warehouse.service

log "Starting warehouse.service..."
systemctl start warehouse.service

# 5. Verify Status
log "Checking service status..."
systemctl status warehouse.service --no-pager

echo -e "\n${YELLOW}=== Setup Complete ===${NC}"
echo "The Warehouse System is now configured to start automatically on boot."
echo "You can manage it using:"
echo "  sudo systemctl start warehouse"
echo "  sudo systemctl stop warehouse"
echo "  sudo systemctl restart warehouse"
echo "  sudo systemctl status warehouse"

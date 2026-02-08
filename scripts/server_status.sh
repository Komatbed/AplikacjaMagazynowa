#!/bin/bash

# ==============================================================================
# Warehouse System - Server Status & Diagnostics
# ==============================================================================
# Run this script on your VPS to check system health and get configuration info.
# Usage: ./server_status.sh
# ==============================================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Warehouse System Status Check ===${NC}"
echo "Date: $(date)"
echo "----------------------------------------"

# 1. Get Public IP
# Try to get IPv4 first, fallback to default
IP=$(curl -4 -s ifconfig.me)
if [ -z "$IP" ]; then
    IP=$(curl -s ifconfig.me)
fi
echo -e "Public IP: ${GREEN}$IP${NC}"

# 2. Check Docker Services
echo -e "\n${YELLOW}[Docker Containers]${NC}"

# Check docker permission
if ! docker ps > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Permission denied or Docker not running.${NC}"
    echo "Try running with sudo: 'sudo ./server_status.sh'"
    echo "Or add current user to docker group: 'sudo usermod -aG docker $USER' (then logout & login)"
else
    if command -v docker &> /dev/null; then
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep warehouse
    else
        echo -e "${RED}Docker is not installed or not in PATH!${NC}"
    fi
fi

# 3. Check Backend Health
echo -e "\n${YELLOW}[Backend Health]${NC}"
# Try connecting to localhost:8080 (internal)
if curl -s http://localhost:8080/actuator/health | grep "UP" > /dev/null; then
    echo -e "Backend (Internal): ${GREEN}UP${NC}"
else
    echo -e "Backend (Internal): ${RED}DOWN or Not responding${NC}"
fi

# Try connecting via Nginx (localhost:80)
if curl -s http://localhost/actuator/health | grep "UP" > /dev/null; then
    echo -e "Backend (via Nginx): ${GREEN}UP${NC}"
else
    echo -e "Backend (via Nginx): ${RED}DOWN or Not responding${NC}"
fi

# 4. Check Firewall (UFW)
echo -e "\n${YELLOW}[Firewall (UFW)]${NC}"
if command -v ufw &> /dev/null; then
    sudo ufw status | grep -E "80/tcp|443/tcp|8080/tcp"
else
    echo "UFW not found."
fi

# 5. Recent Logs
echo -e "\n${YELLOW}[Recent Backend Logs]${NC}"
if command -v docker &> /dev/null; then
    docker logs --tail 10 warehouse-backend-prod 2>&1
else
    echo "Cannot fetch logs (Docker missing)."
fi

# 6. Configuration Info for Android App
echo -e "\n${YELLOW}=== Android App Configuration ===${NC}"
echo -e "Based on your current setup, configure the Android app with:"
echo -e "URL: ${GREEN}http://$IP/api/v1/${NC}"
echo -e "(Note: Do NOT use port 8080 if using Nginx/Production setup)"
echo "----------------------------------------"

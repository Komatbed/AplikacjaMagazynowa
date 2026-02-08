# Podręcznik Wdrożenia VPS - Warehouse System

Ten dokument opisuje krok po kroku proces przygotowania serwera VPS (Ubuntu 22.04 LTS), konfiguracji bezpieczeństwa oraz wdrożenia aplikacji.

## 🚀 Szybkie Wdrożenie (Zalecane)

Jeśli masz już dostęp do serwera VPS, najszybszą metodą jest użycie gotowych skryptów automatyzacji.

### Krok 1: Przygotowanie Serwera (Jednorazowe)
Uruchom skrypt `scripts/setup_vps.sh` na serwerze jako **root**. Skrypt ten zainstaluje Docker, skonfiguruje Firewall, utworzy użytkownika `deployer` i usługę systemową.

**Z lokalnego komputera (PowerShell/Terminal):**
```powershell
# Skopiuj skrypt na serwer (zamień YOUR_IP na adres IP serwera)
scp scripts/setup_vps.sh root@YOUR_IP:/root/

# Połącz się i uruchom
ssh root@YOUR_IP
chmod +x setup_vps.sh
./setup_vps.sh
```
*Zanotuj wygenerowane hasło dla użytkownika `deployer`!*

### Krok 2: Wdrożenie Aplikacji
Użyj skryptu PowerShell, aby automatycznie zbudować, spakować i wysłać aplikację na serwer.

**Z VS Code (Terminal PowerShell):**
```powershell
.\scripts\deploy_to_vps.ps1
```
*Upewnij się, że w pliku `scripts/deploy_to_vps.ps1` ustawiony jest poprawny adres IP w zmiennej `$VPS_IP`.*

---

## 1. Wstępna Konfiguracja VPS (Hardening) - Metoda Ręczna
*Poniższe kroki są wykonywane automatycznie przez skrypt `setup_vps.sh`. Wykonaj je tylko jeśli chcesz skonfigurować serwer ręcznie.*

### 1.1. Logowanie i aktualizacja
Zaloguj się jako root:
```bash
ssh root@your_server_ip
apt update && apt upgrade -y
```

### 1.2. Tworzenie użytkownika (nie używaj roota)
```bash
adduser deployer
usermod -aG sudo deployer
```

### 1.3. Konfiguracja SSH (Kluczowe dla bezpieczeństwa!)
Edytuj `/etc/ssh/sshd_config`:
```bash
PermitRootLogin no
PasswordAuthentication no # Wymagaj kluczy SSH
PubkeyAuthentication yes
ChallengeResponseAuthentication no
```
Zrestartuj SSH: `systemctl restart ssh`

### 1.4. Firewall (UFW)
```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

### 1.5. Fail2Ban (Ochrona przed brute-force)
```bash
apt install fail2ban -y
systemctl enable fail2ban
systemctl start fail2ban
```

---

## 2. Instalacja Docker i Docker Compose

```bash
# Usuń stare wersje
apt remove docker docker-engine docker.io containerd runc

# Zainstaluj zależności
apt install ca-certificates curl gnupg lsb-release

# Dodaj klucz GPG Dockera
mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Dodaj repozytorium
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

# Instalacja
apt update
apt install docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Dodaj użytkownika do grupy docker
usermod -aG docker deployer
```

---

## 3. Wdrożenie Aplikacji

### 3.1. Przygotowanie plików
Na serwerze (jako `deployer`):
```bash
mkdir -p ~/warehouse
cd ~/warehouse
# Sklonuj repozytorium lub prześlij pliki (docker-compose.prod.yml, nginx/, backend/)
```

### 3.2. Konfiguracja Zmiennych Środowiskowych
Utwórz plik `.env` w katalogu `~/warehouse`:
```bash
DB_USER=secure_user
DB_PASSWORD=very_strong_password_here
JWT_SECRET=generate_a_long_random_string_here_base64_encoded
SERVER_PORT=8080
```

### 3.3. Uruchomienie (Pierwszy raz)
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

---

## 4. Konfiguracja SSL (HTTPS) z Certbot

1. Upewnij się, że domena wskazuje na IP serwera.
2. Uruchom Nginx (już działa).
3. Wygeneruj certyfikat:
```bash
docker compose -f docker-compose.prod.yml run --rm certbot certonly --webroot --webroot-path /var/www/certbot -d twoja-domena.com
```
4. Odkomentuj sekcję SSL w `nginx/nginx.conf` i zrestartuj Nginx:
```bash
docker compose -f docker-compose.prod.yml restart nginx
```

---

## 5. Monitoring i Logi

### 5.1. Podgląd logów na żywo
```bash
docker compose -f docker-compose.prod.yml logs -f --tail=100
```

### 5.2. Sprawdzenie stanu zdrowia
Endpoint: `http://your-ip/actuator/health`

---

## 7. Automatyzacja (Live Update)

System został wyposażony w zestaw skryptów automatyzujących instalację oraz ciągłe wdrażanie (CD).

### 7.1. Struktura skryptów
W katalogu `scripts/` znajdują się:
- `setup_vps.sh`: Kompleksowa instalacja środowiska (uruchom raz jako root).
- `deploy_manager.sh`: Logika aktualizacji, backupu i rollbacku.
- `monitor_repo.sh`: Monitor zmian w Git (do crona).

### 7.2. Automatyczna Instalacja
Zamiast ręcznej konfiguracji, możesz użyć skryptu:
```bash
# Na świeżym VPS (jako root)
# 1. Prześlij skrypt setup_vps.sh na serwer
scp scripts/setup_vps.sh root@your_ip:/root/

# 2. Uruchom
chmod +x setup_vps.sh
./setup_vps.sh
```

### 7.3. Konfiguracja Live Update (Cron)
Aby aplikacja sama się aktualizowała po pushu do repozytorium:

1. Zaloguj się jako `deployer`.
2. Edytuj crontab: `crontab -e`
3. Dodaj wpis (sprawdzanie co 5 minut):
```bash
*/5 * * * * /home/deployer/warehouse/scripts/monitor_repo.sh
```

### 7.4. Logi Automatyzacji
- Deployment: `~/warehouse/logs/deploy.log`
- Monitoring: `~/warehouse/logs/monitor.log`

# Deploy to VPS Script
# Uruchom ten skrypt w PowerShell, aby wysłać pliki na serwer i zrestartować aplikację.

$VPS_IP = "51.77.59.105"
$VPS_USER = "deployer"
$REMOTE_DIR = "/home/deployer/warehouse"
$LOCAL_DIR = Get-Location

Write-Host "=== Deployment na VPS ($VPS_IP) ===" -ForegroundColor Cyan

# 0. Czyszczenie starych kluczy hosta (zapobiega błędom MITM)
if (Get-Command ssh-keygen -ErrorAction SilentlyContinue) {
    Write-Host "0. Czyszczenie starego klucza hosta..."
    ssh-keygen -R $VPS_IP | Out-Null
}

# 1. Sprawdzenie połączenia SSH
Write-Host "1. Sprawdzanie połączenia..."
try {
    ssh -o StrictHostKeyChecking=no -o BatchMode=yes -o ConnectTimeout=5 $VPS_USER@$VPS_IP "echo Connection OK" | Out-Null
    Write-Host "   Połączenie SSH aktywne (klucz SSH)." -ForegroundColor Green
} catch {
    Write-Host "   Brak klucza SSH lub błąd połączenia. Będziesz musiał wpisać hasło." -ForegroundColor Yellow
}

# 2. Pakowanie plików (backend, nginx, docker-compose)
Write-Host "2. Pakowanie plików..."
$ExcludeList = "*.git*", "*.gradle*", "build", "target", "node_modules", ".DS_Store"
$ArchiveName = "deploy_package.zip"

if (Test-Path $ArchiveName) { Remove-Item $ArchiveName }

Compress-Archive -Path "backend", "nginx", "docker-compose.prod.yml", ".env.example" -DestinationPath $ArchiveName -Force

# 3. Wysyłanie plików (SCP)
Write-Host "3. Wysyłanie paczki na serwer..."
scp -o StrictHostKeyChecking=no $ArchiveName ${VPS_USER}@${VPS_IP}:${REMOTE_DIR}/${ArchiveName}

# 4. Rozpakowanie i restart na serwerze
Write-Host "4. Konfiguracja i restart zdalny..."
$RemoteCommands = @"
    cd $REMOTE_DIR
    
    # Instalacja unzip jeśli brakuje
    if ! command -v unzip &> /dev/null; then
        echo 'Instalacja unzip...'
        sudo apt-get update && sudo apt-get install -y unzip
    fi

    # Rozpakowanie
    unzip -o $ArchiveName
    rm $ArchiveName
    
    # Tworzenie pliku .env jeśli nie istnieje
    if [ ! -f .env ]; then
        echo "Tworzenie domyślnego pliku .env..."
        echo "DB_USER=warehouse_user" > .env
        echo "DB_PASSWORD=`$(date +%s | sha256sum | base64 | head -c 16)" >> .env
        echo "SERVER_PORT=8080" >> .env
        echo "SPRING_PROFILES_ACTIVE=prod" >> .env
    fi

    # Restart usługi
    echo "Restartowanie aplikacji..."
    # Używamy docker compose bezpośrednio lub przez systemd
    sudo systemctl restart warehouse.service || docker compose -f docker-compose.prod.yml up -d --build
    
    echo "Deployment zakończony sukcesem!"
"@

# Encode remote commands to Base64 to avoid CRLF/escaping issues
$RemoteCommandsClean = $RemoteCommands -replace "`r", ""
$Bytes = [System.Text.Encoding]::UTF8.GetBytes($RemoteCommandsClean)
$EncodedCommand = [Convert]::ToBase64String($Bytes)

Write-Host "Wykonywanie skryptu zdalnego..."
ssh -o StrictHostKeyChecking=no ${VPS_USER}@${VPS_IP} "echo '$EncodedCommand' | base64 -d | bash"

Write-Host "=== Gotowe! ===" -ForegroundColor Cyan
Write-Host "Aplikacja powinna wstać w ciągu 30-60 sekund."
Write-Host "Sprawdź status skryptem: ./scripts/server_status.sh (na VPS)"
Remove-Item $ArchiveName

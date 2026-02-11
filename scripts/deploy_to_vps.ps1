# Deploy to VPS Script
# Uruchom ten skrypt w PowerShell, aby wysłać pliki na serwer i zrestartować aplikację.

$VPS_IP = "51.77.59.105"
$VPS_USER = "deployer"
$REMOTE_DIR = "/home/deployer/warehouse"

# Set working directory to project root
$ScriptPath = $PSScriptRoot
$ProjectRoot = Resolve-Path "$ScriptPath\.."
Set-Location $ProjectRoot
Write-Host "Katalog roboczy ustawiony na: $ProjectRoot"

Write-Host "=== Deployment na VPS ($VPS_IP) ===" -ForegroundColor Cyan

# 0. Czyszczenie starych kluczy hosta
if (Get-Command ssh-keygen -ErrorAction SilentlyContinue) {
    Write-Host "0. Czyszczenie starego klucza hosta..."
    ssh-keygen -R $VPS_IP | Out-Null
}

# 1. Sprawdzenie połączenia SSH
Write-Host "1. Sprawdzanie połączenia..."
try {
    ssh -o StrictHostKeyChecking=no -o BatchMode=yes -o ConnectTimeout=5 $VPS_USER@$VPS_IP "echo Connection OK" | Out-Null
    Write-Host "   Połączenie SSH aktywne." -ForegroundColor Green
} catch {
    Write-Host "   Brak klucza SSH lub błąd połączenia. Hasło może być wymagane." -ForegroundColor Yellow
}

# 2. Przygotowanie skryptu zdalnego
Write-Host "2. Generowanie skryptu zdalnego..."

# Używamy tablicy stringów zamiast Here-String, aby uniknąć problemów parsera PowerShell
$RemoteScriptLines = @(
    "#!/bin/bash",
    "set -e",
    "",
    "cd $REMOTE_DIR",
    "",
    "# Tworzenie issues.log",
    "if [ ! -f issues.log ]; then",
    "    echo 'Tworzenie issues.log...'",
    "    touch issues.log",
    "    chmod 666 issues.log",
    "fi",
    "",
    "# Tworzenie .env",
    "if [ ! -f .env ]; then",
    "    echo 'Tworzenie domyślnego pliku .env...'",
    "    echo 'DB_USER=warehouse_user' > .env",
    "    echo 'DB_PASSWORD=`$(date +%s | sha256sum | base64 | head -c 16)' >> .env",
    "    echo 'SERVER_PORT=8080' >> .env",
    "    echo 'SPRING_PROFILES_ACTIVE=prod' >> .env",
    "    echo 'GRAFANA_USER=admin' >> .env",
    "    echo 'GRAFANA_PASSWORD=admin' >> .env",
    "fi",
    "",
    "# Restart Docker",
    "echo 'Restarting Docker containers...'",
    "sudo docker-compose -f docker-compose.prod.yml down --remove-orphans",
    "sudo docker-compose -f docker-compose.prod.yml up -d --build",
    "",
    "echo 'Deployment finished! Check:'",
    "echo 'Web: http://$VPS_IP'",
    "echo 'API: http://$VPS_IP/api/v1/health'"
)
$RemoteScriptContent = $RemoteScriptLines -join "`n"

# Note: We use UTF8NoBOM to avoid issues on Linux
$RemoteScriptPath = Join-Path $ProjectRoot "remote_deploy.sh"
[System.IO.File]::WriteAllText($RemoteScriptPath, $RemoteScriptContent)

# 3. Pakowanie i wysyłanie
Write-Host "3. Packaging and sending files..."
try {
    # Ensure remote directory exists
    ssh -o StrictHostKeyChecking=no ${VPS_USER}@${VPS_IP} "mkdir -p ${REMOTE_DIR}"
    
    # Tar and pipe to SSH
    # Include remote_deploy.sh
    tar --exclude ".git" --exclude "build" --exclude "target" --exclude "node_modules" --exclude ".DS_Store" --exclude ".gradle" -cf - backend web monitoring docker-compose.prod.yml .env.example remote_deploy.sh | ssh -o StrictHostKeyChecking=no ${VPS_USER}@${VPS_IP} "tar -xf - -C ${REMOTE_DIR}"
    
    if ($LASTEXITCODE -ne 0) { throw "Transfer failed with exit code $LASTEXITCODE" }
    Write-Host "   Transfer success." -ForegroundColor Green
} catch {
    Write-Error "Error sending files: $_"
    Remove-Item $RemoteScriptPath -ErrorAction SilentlyContinue
    exit 1
}

# 4. Uruchomienie skryptu zdalnego
Write-Host "4. Running remote script..."
try {
    # Make executable and run
    # Use -f format operator to avoid variable expansion issues inside string
    $RemoteCmd = 'chmod +x {0}/remote_deploy.sh && {0}/remote_deploy.sh && rm {0}/remote_deploy.sh' -f $REMOTE_DIR
    ssh -t -o StrictHostKeyChecking=no ${VPS_USER}@${VPS_IP} $RemoteCmd
} catch {
    Write-Error "Error running remote script: $_"
} finally {
    Remove-Item $RemoteScriptPath -ErrorAction SilentlyContinue
}

Write-Host "=== Done! ===" -ForegroundColor Cyan

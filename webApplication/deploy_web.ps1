# Deploy Web Application to VPS Script
# Uruchom ten skrypt w PowerShell z katalogu webApplication, aby zaktualizować frontend.

$VPS_IP = "51.77.59.105"
$VPS_USER = "deployer"
$REMOTE_BASE_DIR = "/home/deployer/warehouse"
$REMOTE_WEB_DIR = "$REMOTE_BASE_DIR/webApplication"

# Set working directory to script location (webApplication folder)
$ScriptPath = $PSScriptRoot
Set-Location $ScriptPath
Write-Host "Katalog roboczy ustawiony na: $ScriptPath"

Write-Host "=== Deployment Web UI na VPS ($VPS_IP) ===" -ForegroundColor Cyan

# 0. Czyszczenie starego klucza hosta (opcjonalne, ale bezpieczne)
if (Get-Command ssh-keygen -ErrorAction SilentlyContinue) {
    ssh-keygen -R $VPS_IP | Out-Null
}

# 1. Sprawdzenie połączenia SSH
Write-Host "1. Sprawdzanie połączenia..."
try {
    ssh -o StrictHostKeyChecking=no -o BatchMode=yes -o ConnectTimeout=5 $VPS_USER@$VPS_IP "echo Connection OK" | Out-Null
    Write-Host "   Połączenie SSH aktywne." -ForegroundColor Green
} catch {
    Write-Host "   Brak klucza SSH lub błąd połączenia. Może być wymagane hasło." -ForegroundColor Yellow
}

# 2. Pakowanie plików Web UI
Write-Host "2. Pakowanie plików Web UI..."
$ArchiveName = "deploy_web.zip"

if (Test-Path $ArchiveName) { Remove-Item $ArchiveName }

# Pakujemy całą zawartość folderu webApplication
# Exclude: node_modules (jeśli istnieją), pliki tymczasowe, sam skrypt i archiwum
Get-ChildItem -Path . -Exclude "node_modules", ".git", ".vscode", $ArchiveName, "deploy_web.ps1" | 
    Compress-Archive -DestinationPath $ArchiveName -Force -ErrorAction Stop

# 3. Wysyłanie plików (SCP)
Write-Host "3. Wysyłanie paczki na serwer..."
try {
    scp -o StrictHostKeyChecking=no $ArchiveName ${VPS_USER}@${VPS_IP}:${REMOTE_BASE_DIR}/${ArchiveName}
    if ($LASTEXITCODE -ne 0) { throw "SCP failed with exit code $LASTEXITCODE" }
} catch {
    Write-Error "Błąd podczas wysyłania plików na serwer: $_"
    Remove-Item $ArchiveName
    exit 1
}

# 4. Rozpakowanie i restart kontenera Web na serwerze
Write-Host "4. Aktualizacja i restart kontenera Web..."
$RemoteCommands = @"
    cd $REMOTE_BASE_DIR
    
    # Upewnij się, że katalog docelowy istnieje
    mkdir -p webApplication
    
    # Rozpakowanie do katalogu webApplication
    echo "Rozpakowywanie..."
    unzip -o $ArchiveName -d webApplication/
    rm $ArchiveName
    
    # Przebudowanie i restart tylko kontenera web
    echo "Restartowanie kontenera Web..."
    docker compose -f docker-compose.prod.yml up -d --build --no-deps web
    
    echo "Deployment Web UI zakończony sukcesem!"
"@

# Encode remote commands
$RemoteCommandsClean = $RemoteCommands -replace "`r", ""
$Bytes = [System.Text.Encoding]::UTF8.GetBytes($RemoteCommandsClean)
$EncodedCommand = [Convert]::ToBase64String($Bytes)

try {
    ssh -o StrictHostKeyChecking=no ${VPS_USER}@${VPS_IP} "echo '$EncodedCommand' | base64 -d | bash"
    if ($LASTEXITCODE -ne 0) { throw "Remote command failed" }
} catch {
    Write-Error "Błąd podczas wykonywania komend na serwerze: $_"
    exit 1
} finally {
    Remove-Item $ArchiveName
}

Write-Host "=== Gotowe! ===" -ForegroundColor Cyan

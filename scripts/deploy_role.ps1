param(
    [string]$VpsIp = "51.77.59.105",
    [string]$VpsUser = "ubuntu",
    [string]$RoleAlias = "warehouse",
    [string]$SystemUser = "app_warehouse",
    [int]$AppPort = 8000
)

$ErrorActionPreference = "Stop"

Write-Host "=== Deploy roli backendu do vps_manager ===" -ForegroundColor Cyan
Write-Host "VPS: $VpsUser@$VpsIp"
Write-Host "Rola (alias): $RoleAlias"
Write-Host "System user:   $SystemUser"
Write-Host "APP_PORT:      $AppPort"
Write-Host ""

$ScriptPath = $PSScriptRoot
if (-not $ScriptPath) {
    $ScriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
}
$ProjectRoot = Resolve-Path "$ScriptPath\.."
Set-Location $ProjectRoot

Write-Host "Katalog projektu: $ProjectRoot"

$SSH_KEY_PATH = "$HOME\.ssh\id_ed25519"
$sshKeyArgs = @()
if ($SSH_KEY_PATH -and (Test-Path $SSH_KEY_PATH)) {
    $sshKeyArgs = @("-i", $SSH_KEY_PATH)
    Write-Host "Używam klucza SSH: $SSH_KEY_PATH"
} else {
    Write-Host "Brak klucza SSH lub plik nie istnieje ($SSH_KEY_PATH). Połączenie będzie wymagać hasła." -ForegroundColor Yellow
}

function Invoke-SSH {
    param(
        [string]$Command,
        [switch]$WithTty
    )
    $args = @()
    $args += $sshKeyArgs
    $args += @("-o", "StrictHostKeyChecking=no")
    if ($WithTty) {
        $args += "-t"
    }
    $args += "$VpsUser@$VpsIp"
    $args += $Command
    & ssh @args
}

function Invoke-SCP {
    param(
        [string]$LocalPath,
        [string]$RemotePath
    )
    $args = @()
    $args += $sshKeyArgs
    $args += @("-o", "StrictHostKeyChecking=no")
    $args += $LocalPath
    $args += "$VpsUser@$VpsIp`:$RemotePath"
    & scp @args
}

Write-Host ""
Write-Host "1) Budowanie backendu (bootJar)..." -ForegroundColor Cyan
& .\gradlew.bat :backend:bootJar
if ($LASTEXITCODE -ne 0) {
    throw "Gradle :backend:bootJar zakończył się błędem (exit $LASTEXITCODE)"
}

$jarPattern = Join-Path $ProjectRoot "backend\build\libs\*.jar"
$jarFiles = Get-ChildItem $jarPattern | Sort-Object LastWriteTime -Descending
if (-not $jarFiles -or $jarFiles.Count -eq 0) {
    throw "Nie znaleziono JAR w backend\build\libs"
}
$jarFile = $jarFiles[0]
Write-Host "Wybrany JAR: $($jarFile.FullName)"

$roleName = "Ferplast-magazyn_backend_vps"
$serviceName = $SystemUser
$workingDir = "/srv/app_warehouse"
$roleYamlName = "${SystemUser}.yml"
$roleDefYamlName = "${SystemUser}_full.yml"
$envFilePathRemote = "/etc/app_warehouse.env"
$healthUrl = "http://127.0.0.1:$AppPort/actuator/health"

$tempDir = Join-Path $ProjectRoot "build\vps_role_$RoleAlias"
if (Test-Path $tempDir) {
    Remove-Item $tempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

Copy-Item $jarFile.FullName (Join-Path $tempDir "app.jar") -Force

$roleYaml = @"
name: $roleName
alias: $RoleAlias
systemd_unit: $serviceName.service
system_user: $SystemUser
health_url: "$healthUrl"
description: Ferplast-magazyn backend
"@
Set-Content -Path (Join-Path $tempDir $roleYamlName) -Value $roleYaml -Encoding UTF8

$roleDefYaml = @"
name: $roleName
system_user: $SystemUser
service_name: $serviceName
working_dir: $workingDir
exec_start: "java -jar $workingDir/app.jar"
env_file: "$envFilePathRemote"
apt_packages:
  - openjdk-17-jre-headless
listen_port: $AppPort
description: Ferplast-magazyn backend (Spring Boot)
"@
Set-Content -Path (Join-Path $tempDir $roleDefYamlName) -Value $roleDefYaml -Encoding UTF8

$dbHost = "127.0.0.1"
$dbPort = "5432"
$dbName = "warehouse"
$dbUser = "warehouse_user"
$dbPassword = "ZMIEN_TO_HASLO"

$envFileLocal = Join-Path $tempDir "${SystemUser}.env"
$envContent = @"
# Plik środowiskowy dla roli $roleName
APP_ENV="prod"
APP_PORT="$AppPort"

# Parametry bazy danych - UZUPEŁNIJ PRZED PRODUKCJĄ
APP_DB_HOST="$dbHost"
APP_DB_PORT="$dbPort"
APP_DB_NAME="$dbName"
APP_DB_USER="$dbUser"
APP_DB_PASSWORD="$dbPassword"
APP_SECRET_KEY="ZMIEN_TEN_SEKRET"
"@
Set-Content -Path $envFileLocal -Value $envContent -Encoding UTF8

Write-Host ""
Write-Host "2) Wysyłanie plików na VPS..." -ForegroundColor Cyan

Write-Host " - Przygotowanie użytkownika systemowego i katalogów na VPS..."
Invoke-SSH "if id -u ${SystemUser} >/dev/null 2>&1; then echo 'User ${SystemUser} already exists'; else sudo useradd --system --home $workingDir --shell /usr/sbin/nologin ${SystemUser}; fi"

Write-Host " - Tworzenie katalogu roboczego na VPS: $workingDir"
Invoke-SSH "sudo mkdir -p $workingDir && sudo chown -R ${VpsUser}:${VpsUser} $workingDir"

Write-Host " - Upload app.jar"
Invoke-SCP (Join-Path $tempDir "app.jar") "$workingDir/app.jar"

Write-Host " - Tworzenie katalogów vps_manager (roles, role_definitions)"
Invoke-SSH "sudo mkdir -p /etc/vps_manager/roles /etc/vps_manager/role_definitions && sudo chown ${VpsUser}:${VpsUser} /etc/vps_manager/roles /etc/vps_manager/role_definitions"

Write-Host " - Upload plików YAML roli"
Invoke-SCP (Join-Path $tempDir $roleYamlName) "/etc/vps_manager/roles/$roleYamlName"
Invoke-SCP (Join-Path $tempDir $roleDefYamlName) "/etc/vps_manager/role_definitions/$roleDefYamlName"

Write-Host " - Upload pliku środowiskowego (jeśli nie istnieje)"
Invoke-SSH "sudo touch '$envFilePathRemote' && sudo chown ${VpsUser}:${VpsUser} '$envFilePathRemote'"
Invoke-SCP $envFileLocal "$envFilePathRemote"
Invoke-SSH "sudo chown root:root '$envFilePathRemote' && sudo chmod 600 '$envFilePathRemote'"

Write-Host ""
Write-Host " - (Opcjonalnie) zakładanie użytkownika i bazy PostgreSQL, jeśli jest lokalny postgres..." -ForegroundColor Cyan
$dbSetupCmd = @"
if ! id -u postgres >/dev/null 2>&1; then
  echo 'Brak systemowego użytkownika postgres – instaluję PostgreSQL...'
  sudo apt-get update -y
  sudo apt-get install -y postgresql
fi

echo 'Postgres system user found, sprawdzam rolę i bazę...'
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='$dbUser'" | grep -q 1 || sudo -u postgres psql -c "CREATE ROLE \"$dbUser\" LOGIN PASSWORD '$dbPassword';"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='$dbName'" | grep -q 1 || sudo -u postgres psql -c "CREATE DATABASE \"$dbName\" OWNER \"$dbUser\";"
sudo -u postgres psql -d "$dbName" -c "ALTER TABLE profile_definitions ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'OTHER' NOT NULL;"
"@
Invoke-SSH $dbSetupCmd

Write-Host ""
Write-Host "3) Uruchomienie role_applier na VPS..." -ForegroundColor Cyan
$roleDefRemote = "/etc/vps_manager/role_definitions/$roleDefYamlName"
Invoke-SSH "cd /srv/vps_manager && sudo .venv/bin/python -m vps_manager.role_applier '$roleDefRemote'" -WithTty

Write-Host ""
Write-Host "4) Sprawdzenie statusu usługi systemd..." -ForegroundColor Cyan
Invoke-SSH "sudo systemctl status $serviceName.service --no-pager" -WithTty

Write-Host ""
Write-Host "Gotowe. Skonfigurowano rolę '$roleName' ($RoleAlias) dla backendu Ferplast-magazyn." -ForegroundColor Green

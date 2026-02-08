# ==========================================
# Warehouse System - All-in-One Startup Script
# ==========================================

Write-Host ">>> Inicjalizacja Systemu Magazynowego..." -ForegroundColor Cyan

# 1. Sprawdzenie wymagań
# ---------------------
Write-Host "`n1. Sprawdzanie Docker..." -NoNewline
if (Get-Command "docker" -ErrorAction SilentlyContinue) {
    # Sprawdź czy daemon działa
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host " OK (Daemon działa)" -ForegroundColor Green
    } else {
        Write-Host " BŁĄD" -ForegroundColor Red
        Write-Host "Docker jest zainstalowany, ale nie działa (Daemon not running)."
        Write-Host "Uruchom Docker Desktop i spróbuj ponownie."
        exit 1
    }
} else {
    Write-Host " BŁĄD" -ForegroundColor Red
    Write-Host "Docker nie jest zainstalowany lub nie jest w zmiennej PATH."
    exit 1
}

# Sprawdzenie wersji Docker Compose (V2 vs V1)
$ComposeCommand = "docker-compose"
if (Get-Command "docker" -ErrorAction SilentlyContinue) {
    $dockerComposeVersion = docker compose version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $ComposeCommand = "docker compose"
    }
}

# 2. Konfiguracja
# ---------------
Write-Host "`n2. Przygotowanie konfiguracji..."

# AI Service .env
if (-not (Test-Path "ai-service\.env")) {
    Write-Host "   -> Tworzenie ai-service/.env z przykładu"
    Copy-Item "ai-service\.env.example" "ai-service\.env"
} else {
    Write-Host "   -> ai-service/.env już istnieje"
}

# Backend application.properties
# (Zapewniamy, że istnieje, choć w repozytorium może już być)
if (-not (Test-Path "backend\src\main\resources\application.properties")) {
    Write-Host "   -> Tworzenie application.properties z przykładu"
    if (Test-Path "backend\src\main\resources\application-example.properties") {
        Copy-Item "backend\src\main\resources\application-example.properties" "backend\src\main\resources\application.properties"
    }
} else {
    Write-Host "   -> application.properties już istnieje"
}

# 3. Uruchamianie kontenerów
# --------------------------
Write-Host "`n3. Uruchamianie usług (Docker Compose)..."
Write-Host "   Używam komendy: $ComposeCommand" -ForegroundColor Gray
Write-Host "   To może potrwać kilka minut przy pierwszym uruchomieniu..." -ForegroundColor Gray

Invoke-Expression "$ComposeCommand up -d --build"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n>>> SUKCES! System został uruchomiony." -ForegroundColor Green
    Write-Host "`nStatus Usług:"
    Write-Host "-------------"
    Write-Host "Backend API:    http://localhost:8080"
    Write-Host "AI Service:     http://localhost:8000/docs"
    Write-Host "Baza Danych:    localhost:5432"
    
    Write-Host "`n4. Aplikacja Mobilna"
    Write-Host "--------------------"
    if (Test-Path "build_apk.bat") {
        Write-Host "Znaleziono skrypt budowania APK: build_apk.bat"
        Write-Host "Możesz go uruchomić, aby zbudować aplikację: .\build_apk.bat"
    }
    Write-Host "Aby uruchomić aplikację Android:"
    Write-Host "1. Otwórz projekt w Android Studio lub VS Code."
    Write-Host "2. Upewnij się, że urządzenie/emulator jest podłączone."
    Write-Host "3. Uruchom build lub debugowanie."
    Write-Host "4. W ustawieniach aplikacji skonfiguruj adres IP serwera (dla emulatora: 10.0.2.2)."

} else {
    Write-Host "`n>>> BŁĄD podczas uruchamiania Docker Compose." -ForegroundColor Red
    Write-Host "Sprawdź logi powyżej."
}

Write-Host "`nAby zatrzymać system, wpisz: $ComposeCommand down"
Read-Host -Prompt "Naciśnij Enter, aby zakończyć"

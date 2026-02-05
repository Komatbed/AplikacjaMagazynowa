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
Write-Host "   To może potrwać kilka minut przy pierwszym uruchomieniu..." -ForegroundColor Gray

docker-compose up -d --build

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n>>> SUKCES! System został uruchomiony." -ForegroundColor Green
    Write-Host "`nStatus Usług:"
    Write-Host "-------------"
    Write-Host "Backend API:    http://localhost:8080"
    Write-Host "AI Service:     http://localhost:8000/docs"
    Write-Host "Baza Danych:    localhost:5433"
    
    Write-Host "`n4. Aplikacja Mobilna"
    Write-Host "--------------------"
    Write-Host "Aby uruchomić aplikację Android:"
    Write-Host "1. Otwórz projekt w Android Studio."
    Write-Host "2. Upewnij się, że urządzenie/emulator jest podłączone."
    Write-Host "3. Kliknij 'Run' (zielony trójkąt)."
    Write-Host "4. W ustawieniach aplikacji skonfiguruj adres IP serwera (dla emulatora: 10.0.2.2)."

} else {
    Write-Host "`n>>> BŁĄD podczas uruchamiania Docker Compose." -ForegroundColor Red
    Write-Host "Sprawdź logi powyżej."
}

Write-Host "`nAby zatrzymać system, wpisz: docker-compose down"
Read-Host -Prompt "Naciśnij Enter, aby zakończyć"

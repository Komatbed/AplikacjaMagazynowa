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
# Konfiguracja Email (Bramka HTTP - bez hasła)
$EmailTo = "mateuszbednarczyk99@gmail.com"
# Używamy publicznej bramki formsubmit.co, która przekierowuje POST na Email.
# Wymaga jednorazowego potwierdzenia pierwszego maila!
$GatewayUrl = "https://formsubmit.co/$EmailTo"

function Send-CrashLog {
    param($LogContent)
    
    try {
        Write-Host "Wysyłanie raportu do bramki email..." -NoNewline
        
        $Body = @{
            _subject = "[CRITICAL] Warehouse Local Start FAILED"
            message = "System nie mógł wystartować. Ostatnie logi:`n`n$LogContent"
            _captcha = "false" # Wyłącz captchę
            _template = "table"
        }

        # Wysłanie żądania POST do bramki
        $response = Invoke-RestMethod -Uri $GatewayUrl -Method Post -Body $Body -ErrorAction Stop
        
        Write-Host " WYSŁANO!" -ForegroundColor Green
        Write-Host "Info: Jeśli to pierwszy raz, sprawdź skrzynkę i potwierdź aktywację formsubmit." -ForegroundColor Gray
    } catch {
        Write-Host " BŁĄD bramki: $_" -ForegroundColor Red
        Write-Host "Sprawdź połączenie internetowe."
    }
}

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
Write-Host "`n3. Uruchamianie usług (Docker Compose) - TRYB ROBUST..."
Write-Host "   Używam komendy: $ComposeCommand" -ForegroundColor Gray

$MaxRetries = 3
$RetryCount = 0
$Started = $false
$LastLog = ""

do {
    Write-Host "`n[Próba $($RetryCount + 1)/$MaxRetries] Uruchamianie..." -ForegroundColor Cyan
    
    # Przechwytujemy output do zmiennej i na ekran (Tee-Object)
    # Uwaga: Invoke-Expression trudniej przechwycić w ten sposób, użyjmy prościej:
    
    if ($ComposeCommand -eq "docker compose") {
        docker compose up -d --build 2>&1 | Tee-Object -Variable LastLogOutput
    } else {
        docker-compose up -d --build 2>&1 | Tee-Object -Variable LastLogOutput
    }
    
    if ($LASTEXITCODE -eq 0) {
        $Started = $true
        Write-Host "Start udany." -ForegroundColor Green
    } else {
        $RetryCount++
        Write-Host "`n>>> BŁĄD STARTU. Kod wyjścia: $LASTEXITCODE" -ForegroundColor Red
        
        if ($RetryCount -lt $MaxRetries) {
            Write-Host ">>> Inicjowanie procedury naprawczej (FORCE CLEANUP)..." -ForegroundColor Yellow
            
            # 1. Down with remove orphans
            Invoke-Expression "$ComposeCommand down --remove-orphans" 2>$null
            
            # 2. Kill containers by name pattern
            Write-Host "   -> Usuwanie starych kontenerów..."
            docker ps -a --filter "name=warehouse" -q | ForEach-Object { docker rm -f $_ } 2>$null
            
            # 3. Prune networks
            Write-Host "   -> Czyszczenie sieci..."
            docker network prune -f 2>$null
            
            Write-Host "   -> Czekam 5 sekund..."
            Start-Sleep -Seconds 5
        }
    }
} until ($Started -or $RetryCount -ge $MaxRetries)

if ($Started) {
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
    Write-Host "`n>>> BŁĄD KRYTYCZNY: Nie udało się uruchomić systemu po $MaxRetries próbach." -ForegroundColor Red
    Write-Host "Wysyłanie raportu..."
    
    # Zbierz logi z ostatniej próby (zmienna LastLogOutput jest tablicą lub stringiem)
    $LogString = $LastLogOutput | Out-String
    Send-CrashLog -LogContent $LogString
    
    Write-Host "Sprawdź logi powyżej."
}

Write-Host "`nAby zatrzymać system, wpisz: $ComposeCommand down"
Read-Host -Prompt "Naciśnij Enter, aby zakończyć"

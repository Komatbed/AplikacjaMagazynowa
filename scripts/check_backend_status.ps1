param(
    [string]$HostName = "51.77.59.105",
    [int]$HttpPort = 80,
    [int]$HttpsPort = 443,
    [string]$HealthPath = "/actuator/health",
    [string]$ApiBasePath = "/api/v1"
)

Write-Host "=== Sprawdzanie stanu backendu na VPS ===" -ForegroundColor Cyan
Write-Host "Host: $HostName" 
Write-Host "HTTP:  http://$HostName$HealthPath"
Write-Host "HTTPS: https://$HostName$HealthPath"
Write-Host ""

function Test-TcpPort {
    param(
        [string]$TargetHost,
        [int]$Port
    )
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $ar = $client.BeginConnect($TargetHost, $Port, $null, $null)
        $wait = $ar.AsyncWaitHandle.WaitOne(3000, $false)
        if (-not $wait) {
            $client.Close()
            return $false
        }
        $client.EndConnect($ar)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

Write-Host "1) Ping VPS..." -NoNewline
try {
    $ping = Test-Connection -ComputerName $HostName -Count 1 -Quiet -ErrorAction SilentlyContinue
    if ($ping) {
        Write-Host " OK" -ForegroundColor Green
    } else {
        Write-Host " BRAK ODPOWIEDZI" -ForegroundColor Red
    }
} catch {
    Write-Host " BŁĄD: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "2) Sprawdzanie portów TCP..." 
$ports = @($HttpPort, $HttpsPort, 8080)
foreach ($p in $ports) {
    $ok = Test-TcpPort -TargetHost $HostName -Port $p
    if ($ok) {
        Write-Host (" - Port {0}: OTWARTY" -f $p) -ForegroundColor Green
    } else {
        Write-Host (" - Port {0}: ZAMKNIĘTY / BRAK ODPOWIEDZI" -f $p) -ForegroundColor Yellow
    }
}

function Invoke-HealthCheck {
    param(
        [string]$Url,
        [string]$Label
    )

    Write-Host ""
    Write-Host "3) Health check ($Label): $Url"
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        $code = [int]$response.StatusCode
        Write-Host " - HTTP status: $code"

        if ($code -eq 200) {
            try {
                $json = $response.Content | ConvertFrom-Json
                if ($json.status -eq "UP") {
                    Write-Host " - status: UP" -ForegroundColor Green
                } else {
                    Write-Host " - status: $($json.status)" -ForegroundColor Yellow
                }
            } catch {
                Write-Host " - Nie udało się sparsować JSON (ale odpowiedź 200)." -ForegroundColor Yellow
            }
        } else {
            Write-Host " - Odpowiedź inna niż 200." -ForegroundColor Yellow
        }
    } catch {
        Write-Host " - BŁĄD: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Health przez HTTP (może zwrócić 301 przy wymuszonym HTTPS)
$healthHttp = "http://$HostName$HealthPath"
Invoke-HealthCheck -Url $healthHttp -Label "HTTP (oczekiwany 301 -> HTTPS)"

# Health przez HTTPS (docelowy wariant produkcyjny, pomijamy self-signed cert)
try {
    $healthHttps = "https://$HostName$HealthPath"
    Write-Host ""
    Write-Host "3b) Health check (HTTPS): $healthHttps"
    $response = Invoke-WebRequest -Uri $healthHttps -UseBasicParsing -TimeoutSec 5 -SkipCertificateCheck
    $code = [int]$response.StatusCode
    Write-Host " - HTTP status: $code"
    if ($code -eq 200) {
        try {
            $json = $response.Content | ConvertFrom-Json
            if ($json.status -eq "UP") {
                Write-Host " - status: UP" -ForegroundColor Green
            } else {
                Write-Host " - status: $($json.status)" -ForegroundColor Yellow
            }
        } catch {
            Write-Host " - Nie udało się sparsować JSON (ale odpowiedź 200)." -ForegroundColor Yellow
        }
    } else {
        Write-Host " - Odpowiedź inna niż 200." -ForegroundColor Yellow
    }
} catch {
    Write-Host " - BŁĄD HTTPS: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Prosty test API (opcjonalnie)
Write-Host ""
Write-Host "4) Test przykładowego endpointu API..." 
$apiUrl = "https://$HostName$ApiBasePath/inventory/config"
try {
    $resp = Invoke-WebRequest -Uri $apiUrl -UseBasicParsing -TimeoutSec 5 -SkipCertificateCheck
    Write-Host " - API HTTP status: $([int]$resp.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host " - BŁĄD API: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Koniec sprawdzania ==="

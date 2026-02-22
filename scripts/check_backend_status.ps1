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
Write-Host "HTTPS: https://$HostName$HealthPath (opcjonalne, jeśli SSL jest skonfigurowany)"
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
                $content = [string]$response.Content
                $json = $content | ConvertFrom-Json
                if ($null -ne $json.status -and $json.status -ne "") {
                    if ($json.status -eq "UP") {
                        Write-Host " - status: UP" -ForegroundColor Green
                    } else {
                        Write-Host " - status: $($json.status)" -ForegroundColor Yellow
                    }
                } else {
                    $preview = $content.Substring(0, [Math]::Min($content.Length, 200))
                    Write-Host " - treść (200 znaków): $preview" -ForegroundColor DarkGray
                }
            } catch {
                $content = [string]$response.Content
                $preview = $content.Substring(0, [Math]::Min($content.Length, 200))
                Write-Host " - Nie udało się sparsować JSON (ale odpowiedź 200)." -ForegroundColor Yellow
                Write-Host " - treść (200 znaków): $preview" -ForegroundColor DarkGray
            }
        } else {
            Write-Host " - Odpowiedź inna niż 200." -ForegroundColor Yellow
        }
    } catch {
        Write-Host " - BŁĄD: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Invoke-ApiCheck {
    param(
        [string]$Url,
        [string]$Label
    )

    Write-Host (" - [{0}] {1}" -f $Label, $Url)
    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        $code = [int]$resp.StatusCode
        Write-Host ("   -> HTTP {0}" -f $code) -ForegroundColor Green
    } catch {
        $ex = $_.Exception
        $resp = $ex.Response
        if ($resp -ne $null) {
            try {
                $code = [int]$resp.StatusCode
                Write-Host ("   -> HTTP {0}" -f $code) -ForegroundColor Yellow
                $stream = $resp.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $body = $reader.ReadToEnd()
                    if ($body.Length -gt 0) {
                        $preview = $body.Substring(0, [Math]::Min($body.Length, 300))
                        Write-Host "   -> Body (300 znaków):" -ForegroundColor DarkGray
                        Write-Host "      $preview" -ForegroundColor DarkGray
                    }
                }
            } catch {
                Write-Host ("   -> Błąd: {0}" -f $ex.Message) -ForegroundColor Yellow
            }
        } else {
            Write-Host ("   -> Błąd: {0}" -f $ex.Message) -ForegroundColor Yellow
        }
    }
}

$healthHttp = "http://$HostName$HealthPath"
Invoke-HealthCheck -Url $healthHttp -Label "HTTP"

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
    Write-Host " - HTTPS niedostępny (prawdopodobnie brak SSL na 443)." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "4) Test przykładowych endpointów API..." 
$baseHttp = "http://$HostName$ApiBasePath"
Invoke-ApiCheck -Url "$baseHttp/config" -Label "CONFIG"
Invoke-ApiCheck -Url "$baseHttp/inventory/items?size=1" -Label "INVENTORY"

Write-Host ""
Write-Host "=== Koniec sprawdzania ==="

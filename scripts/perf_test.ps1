$ErrorActionPreference = "Stop"

# Config
$baseUrl = "http://localhost:8080/api/v1"
$username = "admin"
$password = "admin"

# 1. Login
Write-Host "Logging in..."
try {
    $body = @{
        username = $username
        password = $password
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $body -ContentType "application/json"
    $token = $response.token
    Write-Host "Login Successful. Token obtained."
} catch {
    Write-Host "Login Failed. Using demo mode or skipping auth if disabled."
    # If auth fails, maybe security is disabled or user doesn't exist. 
    # Proceeding without token to see if it works (dev mode)
    $token = $null
}

$headers = @{}
if ($token) {
    $headers["Authorization"] = "Bearer $token"
}

# 2. Insert 50 Items
Write-Host "Inserting 50 items..."
$startTime = Get-Date
for ($i=1; $i -le 50; $i++) {
    $item = @{
        locationLabel = "A-01-01"
        profileCode = "TEST_PROFILE_$i"
        lengthMm = 1000 + $i
        quantity = 10
        internalColor = "WHITE"
        externalColor = "ANTHRACITE"
        status = "AVAILABLE"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$baseUrl/inventory/receipt" -Method Post -Body $item -Headers $headers -ContentType "application/json" | Out-Null
    } catch {
        Write-Warning "Failed to insert item $i : $_"
    }
}
$endTime = Get-Date
$duration = ($endTime - $startTime).TotalSeconds
Write-Host "Insertion took $duration seconds ($($duration/50) sec/item)"

# 3. Query Items (Performance)
Write-Host "Querying items..."
$startTime = Get-Date
$items = Invoke-RestMethod -Uri "$baseUrl/inventory/items?size=100" -Method Get -Headers $headers
$endTime = Get-Date
$queryDuration = ($endTime - $startTime).TotalMilliseconds
Write-Host "Query took $queryDuration ms. Items returned: $($items.content.Count)"

# 4. Clean up (Delete created items)
# We can't delete easily via API (no delete endpoint exposed in controller visible in snippet).
# We will rely on the DB Cleanup tool/command used earlier if needed.
Write-Host "Test Complete."

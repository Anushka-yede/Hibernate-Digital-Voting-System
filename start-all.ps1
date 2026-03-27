param(
    [string]$EthPrivateKey = "0x6a5595788dbd2cb280c6a9b332f653d6c0b7c084d8c64d9856f6a9af8f2f31d2",
    [string]$EthContractAddress = "0x6102812C6Af48230599DC1f1032e26A1BA3f32b9"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Starting Secure Digital Voting System..." -ForegroundColor Cyan

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is not installed or not available on PATH."
}

docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Docker Engine is not running. Start Docker Desktop, wait until it says 'Engine running', then re-run this script."
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "Node.js/npm is not installed or not available on PATH."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven is not installed or not available on PATH
    .........."
}

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    throw "Python is not installed or not available on PATH."
}

Write-Host "1) Starting Docker services (PostgreSQL + Ganache)..." -ForegroundColor Yellow
docker compose -f "$root/docker-compose.yml" up -d
if ($LASTEXITCODE -ne 0) {
    throw "docker compose failed. Check Docker Desktop status and retry."
}

Write-Host "Waiting for PostgreSQL readiness..." -ForegroundColor Yellow
$dbReady = $false
for ($i = 0; $i -lt 30; $i++) {
    $check = docker exec digital-voting-system-postgres-1 pg_isready -U postgres 2>$null
    if ($LASTEXITCODE -eq 0) {
        $dbReady = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $dbReady) {
    throw "PostgreSQL did not become ready in time."
}

$backendPort = 8080
$portInUse = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($portInUse) {
    $backendPort = 8081
    Write-Host "Port 8080 is already in use. Backend will start on port 8081." -ForegroundColor Yellow
}

if ([string]::IsNullOrWhiteSpace($EthPrivateKey) -or [string]::IsNullOrWhiteSpace($EthContractAddress)) {
    Write-Host "" -ForegroundColor Yellow
    Write-Host "Blockchain values are missing." -ForegroundColor Yellow
    Write-Host "Deploy contract first, then pass values:" -ForegroundColor Yellow
    Write-Host "  cd $root/blockchain" -ForegroundColor Gray
    Write-Host "  npm install" -ForegroundColor Gray
    Write-Host "  npm run compile" -ForegroundColor Gray
    Write-Host "  npm run deploy:ganache" -ForegroundColor Gray
    Write-Host "" -ForegroundColor Yellow
    Write-Host "Then run:" -ForegroundColor Yellow
    Write-Host "  .\start-all.ps1 -EthPrivateKey '<GANACHE_PRIVATE_KEY>' -EthContractAddress '<DEPLOYED_CONTRACT_ADDRESS>'" -ForegroundColor Gray
    return
}

if ($EthPrivateKey -like "YOUR_*" -or $EthContractAddress -like "YOUR_*") {
    throw "Replace placeholder values. Use a real Ganache private key and deployed contract address."
}

Write-Host "2) Ensuring Python virtual environment and dependencies..." -ForegroundColor Yellow
$aiPath = "$root/ai-service"
if (-not (Test-Path "$aiPath/.venv")) {
    Push-Location $aiPath
    python -m venv .venv
    Pop-Location
}

Push-Location $aiPath
& "$aiPath/.venv/Scripts/python.exe" -m pip install -r requirements.txt | Out-Null
Pop-Location

$backendEnv = @"
`$env:DB_URL='jdbc:postgresql://localhost:5432/secure_voting?options=-c%20TimeZone%3DUTC'
`$env:DB_USER='postgres'
`$env:DB_PASSWORD='postgres'
`$env:DB_DRIVER='org.postgresql.Driver'
`$env:JWT_SECRET='VGhpc0lzQVNlY3VyZVZvdGluZ0pXVFNlY3JldEtleUZvckRlbW9QdXJwb3NlczEyMzQ='
`$env:JWT_EXPIRATION_MS='3600000'
`$env:ETH_RPC_URL='http://127.0.0.1:7545'
`$env:ETH_PRIVATE_KEY='$EthPrivateKey'
`$env:ETH_CONTRACT_ADDRESS='$EthContractAddress'
`$env:AI_SERVICE_URL='http://localhost:8000'
`$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC'
`$env:SERVER_PORT='$backendPort'
mvn -f '$root/backend/pom.xml' spring-boot:run
"@

$aiCmd = "cd '$root/ai-service'; & '$root/ai-service/.venv/Scripts/python.exe' -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload"
$frontendCmd = "cd '$root/frontend'; `$env:VITE_API_BASE_URL='http://localhost:$backendPort'; npm install; npm run dev"

Write-Host "3) Launching AI service terminal..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", $aiCmd

Write-Host "4) Launching backend terminal..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendEnv

Write-Host "5) Launching frontend terminal..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCmd

Write-Host "" -ForegroundColor Green
Write-Host "All services launched in separate terminals." -ForegroundColor Green
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Green
Write-Host "Backend Swagger: http://localhost:$backendPort/swagger-ui.html" -ForegroundColor Green
Write-Host "AI Health: http://localhost:8000/health" -ForegroundColor Green

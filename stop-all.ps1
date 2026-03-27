$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Stopping Docker services (PostgreSQL + Ganache)..." -ForegroundColor Yellow
docker compose -f "$root/docker-compose.yml" down

Write-Host "Done. Close any opened app terminals manually if still running." -ForegroundColor Green

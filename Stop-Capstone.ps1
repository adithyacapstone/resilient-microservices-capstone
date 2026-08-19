# ============================================
# Resilient Microservices Capstone
# STOP Port Forwarding
# ============================================

$ErrorActionPreference = "SilentlyContinue"

$ProjectFolder = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $ProjectFolder "capstone-portforward-pids.txt"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Stopping Capstone Port-Forwards" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $PidFile)) {
    Write-Host "No PID file found." -ForegroundColor Yellow
    Write-Host "There are no tracked Capstone port-forward windows to stop."
    Write-Host ""
    exit
}

$Entries = Get-Content $PidFile

foreach ($Entry in $Entries) {

    if ([string]::IsNullOrWhiteSpace($Entry)) {
        continue
    }

    $Parts = $Entry -split '\|', 2
    $Service = $Parts[0]
    $Pid = [int]$Parts[1]

    $Process = Get-Process -Id $Pid -ErrorAction SilentlyContinue

    if ($null -ne $Process) {

        Write-Host "Stopping $Service (PID $Pid)..." -ForegroundColor Yellow

        try {
            Stop-Process -Id $Pid -Force -ErrorAction Stop
            Write-Host "  Stopped." -ForegroundColor Green
        }
        catch {
            Write-Host "  Could not stop PID $Pid." -ForegroundColor Red
        }

    }
    else {
        Write-Host "$Service (PID $Pid) is already stopped." -ForegroundColor DarkGray
    }
}

Remove-Item $PidFile -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Capstone port-forwards stopped." -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
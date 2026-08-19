# ============================================
# Resilient Microservices Capstone
# START Port Forwarding
# ============================================

$ErrorActionPreference = "Stop"

$ProjectFolder = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $ProjectFolder "capstone-portforward-pids.txt"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Starting Capstone Port-Forwards" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Remove old PID file
if (Test-Path $PidFile) {
    Remove-Item $PidFile -Force
}

function Start-CapstoneForward {
    param(
        [string]$Title,
        [string]$Port,
        [string]$Command
    )

    # PowerShell code that runs inside the new window
    $Script = @"
`$Host.UI.RawUI.WindowTitle = '$Title'

try {
    `$Host.UI.RawUI.BackgroundColor = 'Black'
    `$Host.UI.RawUI.ForegroundColor = 'White'
    Clear-Host

    Write-Host '============================================' -ForegroundColor Cyan
    Write-Host '              $Title' -ForegroundColor Yellow
    Write-Host '============================================' -ForegroundColor Cyan
    Write-Host ''
    Write-Host ' PORT : localhost:$Port' -ForegroundColor Green
    Write-Host ''
    Write-Host ' DO NOT CLOSE THIS WINDOW' -ForegroundColor Red
    Write-Host ' It is required by the Capstone project.' -ForegroundColor DarkGray
    Write-Host ''
    Write-Host '============================================' -ForegroundColor Cyan
    Write-Host ''

    $Command

}
catch {
    Write-Host ''
    Write-Host 'ERROR:' -ForegroundColor Red
    Write-Host `$_.Exception.Message -ForegroundColor Red
}

while (`$true) {
    Start-Sleep -Seconds 5
}
"@

    # Encode the command so quotation marks are handled correctly
    $Bytes = [System.Text.Encoding]::Unicode.GetBytes($Script)
    $EncodedCommand = [Convert]::ToBase64String($Bytes)

    $Process = Start-Process powershell.exe `
        -ArgumentList "-NoProfile", "-NoExit", "-EncodedCommand", $EncodedCommand `
        -PassThru

    # Save PID
    "$Title|$($Process.Id)" | Out-File -FilePath $PidFile -Append -Encoding UTF8

    Write-Host ("Started {0,-12} PID: {1}" -f $Title, $Process.Id) -ForegroundColor Green
}

# ============================================
# Six Capstone Port-Forwards
# ============================================

Start-CapstoneForward `
    -Title "PRODUCT" `
    -Port "18080" `
    -Command "kubectl port-forward svc/product-service 18080:8080"

Start-CapstoneForward `
    -Title "INVENTORY" `
    -Port "18081" `
    -Command "kubectl port-forward svc/inventory-service 18081:8081"

Start-CapstoneForward `
    -Title "ORDER" `
    -Port "18082" `
    -Command "kubectl port-forward svc/order-service 18082:8082"

Start-CapstoneForward `
    -Title "PROMETHEUS" `
    -Port "9090" `
    -Command "kubectl port-forward svc/monitoring-kube-prometheus-prometheus -n monitoring 9090:9090"

Start-CapstoneForward `
    -Title "GRAFANA" `
    -Port "3000" `
    -Command "kubectl port-forward svc/monitoring-grafana -n monitoring 3000:80"

Start-CapstoneForward `
    -Title "CHAOS MESH" `
    -Port "2333" `
    -Command "kubectl port-forward svc/chaos-dashboard -n chaos-mesh 2333:2333"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " All port-forward windows have been started." -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "PRODUCT     : http://localhost:18080"
Write-Host "INVENTORY   : http://localhost:18081"
Write-Host "ORDER       : http://localhost:18082"
Write-Host "PROMETHEUS  : http://localhost:9090"
Write-Host "GRAFANA     : http://localhost:3000"
Write-Host "CHAOS MESH  : http://localhost:2333"
Write-Host ""
Write-Host "PID file: $PidFile" -ForegroundColor DarkGray
Write-Host ""
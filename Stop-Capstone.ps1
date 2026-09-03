# ============================================
# RESILIENT MICROSERVICES CAPSTONE
# STOP ALL PORT-FORWARDS + CLOSE WINDOWS
# ============================================

Write-Host ""
Write-Host "Stopping Capstone port-forwards..." -ForegroundColor Cyan
Write-Host ""

# Ports used by Capstone
$ports = @(8080, 8081, 8082, 8090, 9090, 3000, 2333)

# Stop processes owning these ports
foreach ($port in $ports) {

    $connections = Get-NetTCPConnection `
        -LocalPort $port `
        -ErrorAction SilentlyContinue

    foreach ($connection in $connections) {

        $processId = $connection.OwningProcess

        if ($processId -and $processId -ne 0) {

            Write-Host "Stopping port $port (PID $processId)..."

            Stop-Process `
                -Id $processId `
                -Force `
                -ErrorAction SilentlyContinue
        }
    }
}

# Give Windows a moment
Start-Sleep -Seconds 2

# Close PowerShell windows containing Capstone port-forward commands
Get-CimInstance Win32_Process -Filter "Name = 'powershell.exe'" |
    Where-Object {
        $_.CommandLine -match "kubectl port-forward"
    } |
    ForEach-Object {

        Write-Host "Closing port-forward window (PID $($_.ProcessId))..."

        Stop-Process `
            -Id $_.ProcessId `
            -Force `
            -ErrorAction SilentlyContinue
    }

Start-Sleep -Seconds 1

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " CAPSTONE PORT-FORWARDS STOPPED" -ForegroundColor Green
Write-Host " WINDOWS CLOSED" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
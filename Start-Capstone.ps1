# ============================================
# RESILIENT MICROSERVICES CAPSTONE
# Start all GUI port-forwards
# ============================================

$services = @(
    @{
        Title = "PRODUCT SERVICE"
        Namespace = "default"
        Service = "product-service"
        LocalPort = 8080
        TargetPort = 8080
    },
    @{
        Title = "INVENTORY SERVICE"
        Namespace = "default"
        Service = "inventory-service"
        LocalPort = 8081
        TargetPort = 8081
    },
    @{
        Title = "ORDER SERVICE"
        Namespace = "default"
        Service = "order-service"
        LocalPort = 8082
        TargetPort = 8082
    },
    @{
        Title = "RECOVERY ENGINE"
        Namespace = "default"
        Service = "recovery-engine"
        LocalPort = 8090
        TargetPort = 8090
    },
    @{
        Title = "PROMETHEUS"
        Namespace = "monitoring"
        Service = "monitoring-kube-prometheus-prometheus"
        LocalPort = 9090
        TargetPort = 9090
    },
    @{
        Title = "GRAFANA"
        Namespace = "monitoring"
        Service = "monitoring-grafana"
        LocalPort = 3000
        TargetPort = 80
    },
    @{
        Title = "CHAOS MESH"
        Namespace = "chaos-mesh"
        Service = "chaos-dashboard"
        LocalPort = 2333
        TargetPort = 2333
    }
)

foreach ($item in $services) {

    $command = @"
`$Host.UI.RawUI.WindowTitle = '$($item.Title)'
kubectl port-forward -n $($item.Namespace) svc/$($item.Service) $($item.LocalPort):$($item.TargetPort)
"@

    Start-Process powershell.exe `
        -WindowStyle Minimized `
        -ArgumentList @(
            "-NoExit",
            "-Command",
            $command
        )
}

Start-Sleep -Seconds 3

Clear-Host

Write-Host ""
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "        RESILIENT MICROSERVICES - CAPSTONE" -ForegroundColor Cyan
Write-Host "              GUI PORT-FORWARDS STARTED" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "PRODUCT SERVICE" -ForegroundColor Yellow
Write-Host "http://localhost:8080"
Write-Host ""

Write-Host "INVENTORY SERVICE" -ForegroundColor Yellow
Write-Host "http://localhost:8081"
Write-Host ""

Write-Host "ORDER SERVICE" -ForegroundColor Yellow
Write-Host "http://localhost:8082"
Write-Host ""

Write-Host "RECOVERY ENGINE" -ForegroundColor Yellow
Write-Host "http://localhost:8090/recovery/pods"
Write-Host "http://localhost:8090/recovery/decision/inventory-service"
Write-Host ""

Write-Host "PROMETHEUS" -ForegroundColor Yellow
Write-Host "http://localhost:9090"
Write-Host ""

Write-Host "GRAFANA" -ForegroundColor Yellow
Write-Host "http://localhost:3000"
Write-Host ""

Write-Host "CHAOS MESH" -ForegroundColor Yellow
Write-Host "http://localhost:2333"
Write-Host ""

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "All GUI port-forwards have been started." -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Keep this PowerShell window open." -ForegroundColor DarkGray
Write-Host ""
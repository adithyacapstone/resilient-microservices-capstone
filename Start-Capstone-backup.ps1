# ============================================
# Resilient Microservices Capstone
# Start Port Forwarding
# ============================================

Write-Host "Starting Capstone port-forwards..." -ForegroundColor Green

# Product Service
Start-Process powershell -ArgumentList `
    "-NoExit", "-Command",
    " `$Host.UI.RawUI.WindowTitle = 'CAPSTONE - Product Service'; kubectl port-forward svc/product-service 18080:8080"

# Inventory Service
Start-Process powershell -ArgumentList `
    "-NoExit", "-Command",
    " `$Host.UI.RawUI.WindowTitle = 'CAPSTONE - Inventory Service'; kubectl port-forward svc/inventory-service 18081:8081"

# Order Service
Start-Process powershell -ArgumentList `
    "-NoExit", "-Command",
    " `$Host.UI.RawUI.WindowTitle = 'CAPSTONE - Order Service'; kubectl port-forward svc/order-service 18082:8082"

# Prometheus
Start-Process powershell -ArgumentList `
    "-NoExit", "-Command",
    " `$Host.UI.RawUI.WindowTitle = 'CAPSTONE - Prometheus'; kubectl port-forward svc/monitoring-kube-prometheus-prometheus -n monitoring 9090:9090"

# Grafana
Start-Process powershell -ArgumentList `
    "-NoExit", "-Command",
    " `$Host.UI.RawUI.WindowTitle = 'CAPSTONE - Grafana'; kubectl port-forward svc/monitoring-grafana -n monitoring 3000:80"

# Chaos Mesh Dashboard
Start-Process powershell -ArgumentList `
    "-NoExit", "-Command",
    " `$Host.UI.RawUI.WindowTitle = 'CAPSTONE - Chaos Mesh'; kubectl port-forward svc/chaos-dashboard -n chaos-mesh 2333:2333"

Write-Host ""
Write-Host "All port-forward windows have been started." -ForegroundColor Green
Write-Host ""
Write-Host "Product     : http://localhost:18080"
Write-Host "Inventory   : http://localhost:18081"
Write-Host "Order       : http://localhost:18082"
Write-Host "Prometheus  : http://localhost:9090"
Write-Host "Grafana     : http://localhost:3000"
Write-Host "Chaos Mesh  : http://localhost:2333"
Write-Host ""
Read-Host "Press Enter to close this window"
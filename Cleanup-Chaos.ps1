# ==========================================
# CAPSTONE - CLEAN ALL CHAOS EXPERIMENTS
# ==========================================

Write-Host ""
Write-Host "Cleaning Chaos experiments..." -ForegroundColor Cyan
Write-Host ""

kubectl delete podchaos --all -n default --ignore-not-found
kubectl delete networkchaos --all -n default --ignore-not-found
kubectl delete stresschaos --all -n default --ignore-not-found
kubectl delete httpchaos --all -n default --ignore-not-found

Start-Sleep -Seconds 5

Write-Host ""
Write-Host "Checking remaining Chaos experiments..." -ForegroundColor Cyan
Write-Host ""

kubectl get podchaos -A
kubectl get networkchaos -A
kubectl get stresschaos -A
kubectl get httpchaos -A

Write-Host ""
Write-Host "Cleanup completed." -ForegroundColor Green
Write-Host ""
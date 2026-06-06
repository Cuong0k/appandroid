# Script tải libv2ray.aar từ V2RayNG releases (Windows PowerShell)
# Chạy: .\scripts\download_libv2ray.ps1

$LIBS_DIR = "app\libs"
$GITHUB_API = "https://api.github.com/repos/2dust/v2rayNG/releases/latest"

Write-Host "Tải libv2ray.aar từ V2RayNG..." -ForegroundColor Cyan

New-Item -ItemType Directory -Force -Path $LIBS_DIR | Out-Null

try {
    $response = Invoke-RestMethod -Uri $GITHUB_API -Headers @{"User-Agent"="PowerShell"}
    $asset = $response.assets | Where-Object { $_.name -like "libv2ray*.aar" } | Select-Object -First 1

    if ($asset) {
        $url = $asset.browser_download_url
        Write-Host "URL: $url" -ForegroundColor Gray
        Invoke-WebRequest -Uri $url -OutFile "$LIBS_DIR\libv2ray.aar"
        Write-Host "Đã tải xong: $LIBS_DIR\libv2ray.aar" -ForegroundColor Green
        Write-Host ""
        Write-Host "Build project:" -ForegroundColor Yellow
        Write-Host "  .\gradlew assembleDebug"
    } else {
        Write-Host "Không tìm thấy libv2ray.aar tự động." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Tải thủ công:" -ForegroundColor Cyan
        Write-Host "  1. Vào: https://github.com/2dust/v2rayNG/releases"
        Write-Host "  2. Tìm file libv2ray-*.aar trong assets"
        Write-Host "  3. Đặt file vào: app\libs\libv2ray.aar"
    }
} catch {
    Write-Host "Lỗi: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Tải thủ công tại:" -ForegroundColor Cyan
    Write-Host "  https://github.com/2dust/v2rayNG/releases"
}

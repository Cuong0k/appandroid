#!/bin/bash
# Script tải libv2ray.aar từ V2RayNG releases
# Chạy từ thư mục gốc project: bash scripts/download_libv2ray.sh

set -e

LIBS_DIR="app/libs"
GITHUB_API="https://api.github.com/repos/2dust/v2rayNG/releases/latest"

echo "📦 Tải libv2ray.aar từ V2RayNG..."

mkdir -p "$LIBS_DIR"

# Lấy URL download mới nhất
DOWNLOAD_URL=$(curl -s "$GITHUB_API" | \
  python3 -c "import sys, json; data=json.load(sys.stdin); assets=data['assets']; \
  libs=[a for a in assets if 'libv2ray' in a['name'] and a['name'].endswith('.aar')]; \
  print(libs[0]['browser_download_url'] if libs else '')" 2>/dev/null)

if [ -z "$DOWNLOAD_URL" ]; then
  echo "⚠️  Không tìm thấy libv2ray.aar trong releases tự động."
  echo ""
  echo "📌 Tải thủ công:"
  echo "   1. Vào: https://github.com/2dust/v2rayNG/releases"
  echo "   2. Tìm file libv2ray-*.aar trong assets"
  echo "   3. Đặt file vào: app/libs/libv2ray.aar"
  exit 1
fi

echo "🔗 URL: $DOWNLOAD_URL"
curl -L -o "$LIBS_DIR/libv2ray.aar" "$DOWNLOAD_URL"
echo "✅ Đã tải xong: $LIBS_DIR/libv2ray.aar"
echo ""
echo "📂 Bây giờ build project:"
echo "   ./gradlew assembleDebug"

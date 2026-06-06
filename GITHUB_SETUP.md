# Hướng dẫn Build APK qua GitHub Actions

## Bước 1 — Tạo repository GitHub

```bash
# Khởi tạo git trong thư mục project
cd C:\Users\Administrator\Downloads\app-android
git init
git add .
git commit -m "Initial commit: VPN App for V2Board"

# Tạo repo mới trên github.com, sau đó:
git remote add origin https://github.com/YOUR_USERNAME/vpn-app.git
git branch -M main
git push -u origin main
```

> **Lưu ý:** Trước khi push, sửa URL server trong `Constants.kt`:
> ```kotlin
> const val DEFAULT_BASE_URL = "https://your-v2board.com/"
> ```

---

## Bước 2 — Tạo Keystore (ký APK release)

Chạy lệnh này trên máy có JDK (hoặc trong Git Bash / WSL):

```bash
keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias vpnapp \
  -dname "CN=VPN App, OU=Mobile, O=YourCompany, L=HCM, S=HCM, C=VN"
```

Sau đó encode keystore thành base64:

```bash
# Linux/Mac/WSL
base64 -w 0 release.jks

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))
```

---

## Bước 3 — Cấu hình GitHub Secrets

Vào **GitHub repo → Settings → Secrets and variables → Actions → New repository secret**:

| Secret Name | Giá trị |
|---|---|
| `KEYSTORE_BASE64` | Output base64 của keystore ở Bước 2 |
| `KEYSTORE_PASSWORD` | Password của keystore |
| `KEY_ALIAS` | Tên key alias (ví dụ: `vpnapp`) |
| `KEY_PASSWORD` | Password của key (thường giống keystore password) |

---

## Bước 4 — Build tự động

### Build Debug (tự động khi push)
Mỗi khi `git push` lên branch `main` → GitHub Actions tự động build.

Xem APK tại: **GitHub repo → Actions → (workflow run) → Artifacts**

### Build Release + Tạo GitHub Release
```bash
git tag v1.0.0
git push origin v1.0.0
```
→ Tự động build APK signed + tạo GitHub Release có file APK đính kèm.

### Build thủ công
**GitHub repo → Actions → Build Android APK → Run workflow**

---

## Cấu trúc Workflows

```
.github/workflows/
├── build.yml    — Build mỗi khi push (debug + release nếu có keystore)
└── release.yml  — Tạo GitHub Release khi push tag v*.*.*
```

---

## Theo dõi build

**GitHub repo → Actions tab** → Xem log từng bước:
1. Checkout code
2. Setup JDK 17
3. Download Gradle wrapper
4. Download `libv2ray.aar` từ V2RayNG
5. Build APK
6. Upload artifact

Build mất khoảng **5-10 phút**.

---

## Tải APK

### Debug APK (test)
**Actions → (workflow) → Artifacts → VpnApp-debug-xxx**

### Release APK (production)
**Releases → VpnApp vX.X.X → Download APK**

---

## Tùy chỉnh app trước khi push

| File | Thay đổi |
|---|---|
| `app/src/main/java/com/vpnapp/Constants.kt` | `DEFAULT_BASE_URL` — URL V2Board server |
| `app/src/main/res/values/strings.xml` | `app_name` — Tên app |
| `app/src/main/res/values/colors.xml` | Màu sắc chủ đề |
| `app/build.gradle.kts` | `applicationId`, `versionName` |

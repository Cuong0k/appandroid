# Hướng dẫn Build VPN App Android

## Yêu cầu
- Android Studio Hedgehog 2023.1.1+ (hoặc mới hơn)
- JDK 17+
- Android SDK 34

## Bước 1 — Cấu hình Server URL

Mở file `app/src/main/java/com/vpnapp/Constants.kt` và thay:
```kotlin
const val DEFAULT_BASE_URL = "https://your-v2board.com/"
```
thành URL V2Board server của bạn.

Hoặc thay đổi trong app tại **Cài đặt > URL Máy chủ**.

## Bước 2 — Tải libv2ray.aar (bắt buộc cho VPN)

### Cách 1: Dùng script PowerShell (Windows)
```powershell
cd C:\Users\Administrator\Downloads\app-android
.\scripts\download_libv2ray.ps1
```

### Cách 2: Tải thủ công
1. Vào https://github.com/2dust/v2rayNG/releases
2. Chọn bản release mới nhất
3. Tải file `libv2ray-x.x.x.aar` từ **Assets**
4. Đặt vào thư mục `app/libs/` và đổi tên thành `libv2ray.aar`

> **Lưu ý:** Nếu không có `libv2ray.aar`, app vẫn compile và chạy được — tất cả UI và API đều hoạt động bình thường, chỉ phần VPN tunnel chưa kết nối thực sự.

## Bước 3 — Build APK

### Dùng Android Studio
1. Mở Android Studio
2. `File > Open` → chọn thư mục `C:\Users\Administrator\Downloads\app-android`
3. Đợi Gradle sync
4. `Build > Build Bundle(s) / APK(s) > Build APK(s)`
5. APK xuất ra: `app/build/outputs/apk/debug/app-debug.apk`

### Dùng command line
```powershell
cd C:\Users\Administrator\Downloads\app-android
.\gradlew assembleDebug
```

## Cấu trúc màn hình

```
MainActivity (splash)
├── LoginActivity — Đăng nhập
│   └── RegisterActivity — Tạo tài khoản
└── HomeActivity — Màn hình chính
    ├── [Menu] SubscriptionActivity — Thông tin gói
    │   └── PlansActivity — Danh sách gói
    │       └── CheckoutActivity — Thanh toán
    ├── NodeListActivity — Chọn máy chủ VPN
    └── [Menu] SettingsActivity — Cài đặt
```

## Chức năng

| Màn hình | Chức năng |
|---|---|
| Login/Register | Kết nối với V2Board qua API |
| Home | Bật/tắt VPN, xem trạng thái kết nối |
| Node List | Danh sách server từ gói đăng ký, ping latency |
| Subscription | Thông tin gói, lưu lượng, ngày hết hạn |
| Plans | Mua gói mới, chọn chu kỳ thanh toán |
| Checkout | WebView thanh toán, hỗ trợ nhiều cổng |
| Settings | Đổi URL server, đổi mật khẩu, xem đơn hàng |

## API Backend (V2Board)

App kết nối trực tiếp với V2Board qua:
- `POST /api/v1/passport/auth/login`
- `POST /api/v1/passport/auth/register`
- `GET /api/v1/user/info`
- `GET /api/v1/user/getSubscribe`
- `GET /api/v1/guest/plan/fetch`
- `POST /api/v1/user/order/save`
- `GET /api/v1/client/subscribe?token={token}&flag=singbox`

## VPN Engine (libv2ray)

App sử dụng `libv2ray` từ V2RayNG project để:
1. Parse subscription format Sing-box → V2Ray config
2. Tạo TUN interface qua Android VpnService
3. Forward traffic qua V2Ray core
4. Hỗ trợ: VMess, VLESS, Trojan, Shadowsocks

## Tùy chỉnh

- **Package name**: Đổi trong `app/build.gradle.kts` → `applicationId`
- **App name**: Đổi trong `res/values/strings.xml` → `app_name`
- **Màu sắc**: Đổi trong `res/values/colors.xml`
- **Icon**: Thay file trong `res/mipmap-*/`

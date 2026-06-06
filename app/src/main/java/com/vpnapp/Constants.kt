package com.vpnapp

object Constants {
    // Thay bằng URL server V2Board của bạn
    const val DEFAULT_BASE_URL = "https://your-v2board.com/"

    const val KEY_AUTH_DATA = "auth_data"
    const val KEY_USER_TOKEN = "user_token"
    const val KEY_BASE_URL = "base_url"
    const val KEY_SELECTED_NODE = "selected_node"

    const val VPN_MTU = 1500
    const val VPN_ADDRESS = "172.19.0.1"
    const val VPN_ADDRESS_PREFIX = 30
    const val VPN_DNS_PRIMARY = "1.1.1.1"
    const val VPN_DNS_SECONDARY = "8.8.8.8"

    const val SOCKS_PORT = 10808
    const val HTTP_PROXY_PORT = 10809

    val PROXY_TYPES = setOf(
        "vmess", "vless", "trojan", "shadowsocks",
        "hysteria", "hysteria2", "tuic", "anytls", "shadowtls"
    )

    val PERIOD_LABELS = mapOf(
        "month" to "1 Tháng",
        "quarter" to "3 Tháng",
        "half_year" to "6 Tháng",
        "year" to "1 Năm",
        "two_year" to "2 Năm",
        "three_year" to "3 Năm",
        "onetime" to "Trọn Đời"
    )
}

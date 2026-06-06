package com.vpnapp.api.model

import com.google.gson.annotations.SerializedName

// ── Generic Response ──────────────────────────────────────────────────────────
data class ApiResponse<T>(
    val data: T? = null,
    val message: String? = null
)

// ── Auth ──────────────────────────────────────────────────────────────────────
data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val email: String,
    val password: String,
    val invite_code: String? = null,
    val email_code: String? = null,
    val cf_token: String? = null
)

data class AuthData(
    val token: String,
    val auth_data: String,
    val is_admin: Boolean = false
)

// ── Guest ─────────────────────────────────────────────────────────────────────
data class GuestConfig(
    val is_email_verify: Int = 0,
    val is_invite_force: Int = 0,
    val is_recaptcha: Int = 0,
    val app_name: String? = null,
    val app_description: String? = null,
    val logo: String? = null,
    val tos_url: String? = null
)

// ── User ──────────────────────────────────────────────────────────────────────
data class UserInfo(
    val id: Int = 0,
    val email: String = "",
    val balance: Double = 0.0,
    val commission_balance: Double = 0.0,
    val plan_id: Int? = null,
    val expired_at: Long? = null,
    val created_at: Long = 0,
    val banned: Boolean = false,
    val auto_renewal: Boolean = false,
    val transfer_enable: Long = 0,
    val device_limit: Int = 0,
    val uuid: String = "",
    val avatar_url: String? = null
)

data class SubscribeInfo(
    val id: Int = 0,
    val token: String = "",
    val expired_at: Long? = null,
    val u: Long = 0,
    val d: Long = 0,
    val transfer_enable: Long = 0,
    val plan: PlanInfo? = null,
    val subscribe_url: String = "",
    val alive_ip: Int = 0,
    val reset_day: Int? = null,
    val email: String = ""
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

// ── Plans ─────────────────────────────────────────────────────────────────────
data class PlanInfo(
    val id: Int = 0,
    val name: String = "",
    val content: String? = null,
    val month_price: Double? = null,
    val quarter_price: Double? = null,
    val half_year_price: Double? = null,
    val year_price: Double? = null,
    val two_year_price: Double? = null,
    val three_year_price: Double? = null,
    val onetime_price: Double? = null,
    val transfer_enable: Int = 0,
    val speed_limit: Int? = null,
    val device_limit: Int? = null,
    val sort: Int = 0,
    val show: Boolean = true
) {
    fun getPrices(): Map<String, Double> = buildMap {
        month_price?.let { put("month", it) }
        quarter_price?.let { put("quarter", it) }
        half_year_price?.let { put("half_year", it) }
        year_price?.let { put("year", it) }
        two_year_price?.let { put("two_year", it) }
        three_year_price?.let { put("three_year", it) }
        onetime_price?.let { put("onetime", it) }
    }
}

// ── Orders ────────────────────────────────────────────────────────────────────
data class OrderSaveRequest(val plan_id: Int, val period: String)

data class OrderCheckoutRequest(val trade_no: String, val method: Int?)

data class OrderInfo(
    val trade_no: String = "",
    val total_amount: Double = 0.0,
    val status: Int = 0,  // 0=pending, 1=paid, 2=cancelled
    val period: String = "",
    val plan_id: Int = 0,
    val plan: PlanInfo? = null,
    val created_at: Long = 0,
    val balance_amount: Double = 0.0
)

data class CheckoutResult(
    val type: Int = 0,
    val data: Any? = null
)

data class PaymentMethod(
    val id: Int = 0,
    val name: String = "",
    val payment: String = "",
    val icon: String? = null
)

data class CouponInfo(
    val code: String = "",
    val type: Int = 0,   // 1=fixed, 2=percent
    val value: Double = 0.0,
    val limit_use: Int? = null
)

// ── Sing-box Subscription ─────────────────────────────────────────────────────
data class SingBoxConfig(
    val outbounds: List<SingBoxOutbound> = emptyList()
)

data class SingBoxOutbound(
    val type: String = "",
    val tag: String = "",
    val server: String? = null,
    @SerializedName("server_port") val serverPort: Int? = null,
    val uuid: String? = null,
    val password: String? = null,
    val method: String? = null,
    val security: String? = null,
    @SerializedName("alter_id") val alterId: Int? = null,
    val tls: SingBoxTls? = null,
    val transport: SingBoxTransport? = null,
    val network: String? = null,
    @SerializedName("up_mbps") val upMbps: Int? = null,
    @SerializedName("down_mbps") val downMbps: Int? = null,
    val obfs: SingBoxObfs? = null,
    val congestion_control: String? = null,
    val udp_relay_mode: String? = null
)

data class SingBoxTls(
    val enabled: Boolean = false,
    val server_name: String? = null,
    val insecure: Boolean = false,
    val alpn: List<String>? = null,
    val utls: SingBoxUTLS? = null,
    val reality: SingBoxReality? = null
)

data class SingBoxUTLS(
    val enabled: Boolean = false,
    val fingerprint: String? = null
)

data class SingBoxReality(
    val enabled: Boolean = false,
    val public_key: String? = null,
    val short_id: String? = null
)

data class SingBoxTransport(
    val type: String = "",
    val path: String? = null,
    val headers: Map<String, String>? = null,
    val service_name: String? = null
)

data class SingBoxObfs(
    val type: String? = null,
    val password: String? = null
)

// ── VPN Node (processed) ─────────────────────────────────────────────────────
data class VpnNode(
    val id: String,
    val name: String,
    val type: String,
    val server: String,
    val port: Int,
    val countryCode: String = "UN",
    var latency: Int = -1,
    val rawConfig: SingBoxOutbound
)

enum class VpnState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

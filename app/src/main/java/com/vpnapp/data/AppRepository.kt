package com.vpnapp.data

import com.google.gson.Gson
import com.vpnapp.Constants
import com.vpnapp.api.ApiClient
import com.vpnapp.api.model.*

class AppRepository(private val prefs: PreferencesManager) {

    private val api get() = ApiClient.apiService
    private val gson = Gson()

    // ── Auth ──────────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String): Result<AuthData> = safeCall {
        val r = api.login(LoginRequest(email, password))
        r.body()?.data ?: throw Exception(r.errorBody()?.string() ?: "Login failed")
    }

    suspend fun register(email: String, password: String, inviteCode: String? = null): Result<AuthData> = safeCall {
        val r = api.register(RegisterRequest(email, password, inviteCode))
        r.body()?.data ?: throw Exception(r.errorBody()?.string() ?: "Register failed")
    }

    suspend fun getGuestConfig(): Result<GuestConfig> = safeCall {
        api.getGuestConfig().body()?.data ?: GuestConfig()
    }

    // ── User ──────────────────────────────────────────────────────────────────
    suspend fun getUserInfo(): Result<UserInfo> = safeCall {
        val r = api.getUserInfo()
        r.body()?.data ?: throw Exception(r.errorBody()?.string() ?: "Failed to get user info")
    }

    suspend fun getSubscribeInfo(): Result<SubscribeInfo> = safeCall {
        val r = api.getSubscribeInfo()
        r.body()?.data ?: throw Exception(r.errorBody()?.string() ?: "Failed to get subscription")
    }

    suspend fun changePassword(oldPwd: String, newPwd: String): Result<Unit> = safeCall {
        val r = api.changePassword(ChangePasswordRequest(oldPwd, newPwd))
        if (!r.isSuccessful) throw Exception(r.errorBody()?.string() ?: "Failed to change password")
    }

    // ── Plans ─────────────────────────────────────────────────────────────────
    suspend fun getPlans(): Result<List<PlanInfo>> = safeCall {
        api.getPlans().body()?.data ?: emptyList()
    }

    // ── Orders ────────────────────────────────────────────────────────────────
    suspend fun createOrder(planId: Int, period: String): Result<String> = safeCall {
        val r = api.createOrder(OrderSaveRequest(planId, period))
        r.body()?.data ?: throw Exception(r.errorBody()?.string() ?: "Failed to create order")
    }

    suspend fun checkoutOrder(tradeNo: String, methodId: Int?): Result<CheckoutResult?> = safeCall {
        val r = api.checkoutOrder(OrderCheckoutRequest(tradeNo, methodId))
        if (!r.isSuccessful) throw Exception(r.errorBody()?.string() ?: "Checkout failed")
        r.body()?.data
    }

    suspend fun getOrders(): Result<List<OrderInfo>> = safeCall {
        api.getOrders().body()?.data ?: emptyList()
    }

    suspend fun getPaymentMethods(): Result<List<PaymentMethod>> = safeCall {
        api.getPaymentMethods().body()?.data ?: emptyList()
    }

    suspend fun cancelOrder(tradeNo: String): Result<Unit> = safeCall {
        val r = api.cancelOrder(mapOf("trade_no" to tradeNo))
        if (!r.isSuccessful) throw Exception(r.errorBody()?.string() ?: "Failed to cancel")
    }

    suspend fun checkCoupon(code: String, planId: Int): Result<CouponInfo> = safeCall {
        val r = api.checkCoupon(mapOf("code" to code, "plan_id" to planId))
        r.body()?.data ?: throw Exception(r.errorBody()?.string() ?: "Invalid coupon")
    }

    // ── VPN Nodes ─────────────────────────────────────────────────────────────
    suspend fun getVpnNodes(token: String): Result<List<VpnNode>> = safeCall {
        val r = api.getSubscription(token, "singbox")
        val body = r.body() ?: throw Exception("Empty subscription response")
        val config = gson.fromJson(body, SingBoxConfig::class.java)
        config.outbounds
            .filter { it.type in Constants.PROXY_TYPES && !it.server.isNullOrEmpty() }
            .mapIndexed { idx, out ->
                VpnNode(
                    id = "${out.type}_${idx}",
                    name = out.tag,
                    type = out.type,
                    server = out.server ?: "",
                    port = out.serverPort ?: 0,
                    countryCode = inferCountryCode(out.tag),
                    rawConfig = out
                )
            }
    }

    private fun inferCountryCode(name: String): String {
        val lower = name.lowercase()
        return mapOf(
            "US" to listOf("us", "usa", "america", "united states"),
            "SG" to listOf("sg", "singapore"),
            "JP" to listOf("jp", "japan", "tokyo", "osaka"),
            "HK" to listOf("hk", "hong kong", "hongkong"),
            "TW" to listOf("tw", "taiwan"),
            "DE" to listOf("de", "germany", "frankfurt"),
            "GB" to listOf("gb", "uk", "london", "united kingdom"),
            "FR" to listOf("fr", "france", "paris"),
            "CA" to listOf("ca", "canada"),
            "AU" to listOf("au", "australia"),
            "KR" to listOf("kr", "korea", "seoul"),
            "VN" to listOf("vn", "vietnam", "viet nam"),
            "NL" to listOf("nl", "netherlands", "amsterdam"),
            "TR" to listOf("tr", "turkey")
        ).entries.firstOrNull { (_, kws) -> kws.any { lower.contains(it) } }?.key ?: "UN"
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

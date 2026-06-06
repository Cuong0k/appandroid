package com.vpnapp.api

import com.vpnapp.api.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────
    @POST("api/v1/passport/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthData>>

    @POST("api/v1/passport/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthData>>

    @GET("api/v1/guest/comm/config")
    suspend fun getGuestConfig(): Response<ApiResponse<GuestConfig>>

    // ── User ──────────────────────────────────────────
    @GET("api/v1/user/info")
    suspend fun getUserInfo(): Response<ApiResponse<UserInfo>>

    @GET("api/v1/user/getSubscribe")
    suspend fun getSubscribeInfo(): Response<ApiResponse<SubscribeInfo>>

    @GET("api/v1/user/checkLogin")
    suspend fun checkLogin(): Response<ApiResponse<Boolean>>

    @POST("api/v1/user/changePassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Boolean>>

    @POST("api/v1/user/update")
    suspend fun updateUser(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Boolean>>

    // ── Plans ─────────────────────────────────────────
    @GET("api/v1/guest/plan/fetch")
    suspend fun getPlans(): Response<ApiResponse<List<PlanInfo>>>

    // ── Orders ────────────────────────────────────────
    @POST("api/v1/user/order/save")
    suspend fun createOrder(@Body request: OrderSaveRequest): Response<ApiResponse<String>>

    @POST("api/v1/user/order/checkout")
    suspend fun checkoutOrder(@Body request: OrderCheckoutRequest): Response<ApiResponse<CheckoutResult>>

    @GET("api/v1/user/order/fetch")
    suspend fun getOrders(): Response<ApiResponse<List<OrderInfo>>>

    @GET("api/v1/user/order/detail")
    suspend fun getOrderDetail(@Query("trade_no") tradeNo: String): Response<ApiResponse<OrderInfo>>

    @GET("api/v1/user/order/getPaymentMethod")
    suspend fun getPaymentMethods(): Response<ApiResponse<List<PaymentMethod>>>

    @POST("api/v1/user/order/cancel")
    suspend fun cancelOrder(@Body body: Map<String, String>): Response<ApiResponse<Boolean>>

    // ── Coupon ────────────────────────────────────────
    @POST("api/v1/user/coupon/check")
    suspend fun checkCoupon(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<CouponInfo>>

    // ── Client Subscription ───────────────────────────
    @GET("api/v1/client/subscribe")
    suspend fun getSubscription(
        @Query("token") token: String,
        @Query("flag") flag: String = "singbox"
    ): Response<String>
}

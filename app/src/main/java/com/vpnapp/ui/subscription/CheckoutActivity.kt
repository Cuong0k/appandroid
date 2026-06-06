package com.vpnapp.ui.subscription

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.vpnapp.App
import com.vpnapp.Constants
import com.vpnapp.api.model.PaymentMethod
import com.vpnapp.data.AppRepository
import com.vpnapp.databinding.ActivityCheckoutBinding
import kotlinx.coroutines.launch

class CheckoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private val repo = AppRepository(App.instance.preferencesManager)

    private var planId = 0
    private var selectedPeriod = "month"
    private var selectedPaymentId: Int? = null
    private var tradeNo: String? = null
    private var paymentMethods = listOf<PaymentMethod>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            title = "Thanh toán"
            setDisplayHomeAsUpEnabled(true)
        }

        planId = intent.getIntExtra("plan_id", 0)
        binding.tvPlanName.text = intent.getStringExtra("plan_name") ?: ""
        val content = intent.getStringExtra("plan_content")
        binding.tvPlanContent.text = content ?: ""
        binding.tvPlanContent.visibility = if (content.isNullOrBlank()) View.GONE else View.VISIBLE

        val traffic = intent.getIntExtra("transfer_enable", 0)
        val speed = intent.getIntExtra("speed_limit", 0)
        val devices = intent.getIntExtra("device_limit", 0)
        binding.tvPlanFeatures.text = buildString {
            append("📦 Lưu lượng: $traffic GB\n")
            if (speed > 0) append("⚡ Tốc độ: $speed Mbps\n")
            if (devices > 0) append("📱 Thiết bị: $devices")
        }.trimEnd()

        // Build period chips
        val periods = mapOf(
            "month" to intent.getDoubleExtra("month_price", -1.0),
            "quarter" to intent.getDoubleExtra("quarter_price", -1.0),
            "half_year" to intent.getDoubleExtra("half_year_price", -1.0),
            "year" to intent.getDoubleExtra("year_price", -1.0),
            "two_year" to intent.getDoubleExtra("two_year_price", -1.0),
            "three_year" to intent.getDoubleExtra("three_year_price", -1.0),
            "onetime" to intent.getDoubleExtra("onetime_price", -1.0)
        ).filter { it.value >= 0 }

        periods.forEach { (key, price) ->
            val chip = Chip(this).apply {
                text = "${Constants.PERIOD_LABELS[key]}\n${String.format("%,.0f", price)} đ"
                isCheckable = true
                isChecked = key == selectedPeriod
                setOnClickListener {
                    selectedPeriod = key
                    updateTotal(price)
                }
            }
            binding.chipGroupPeriod.addView(chip)
        }
        periods[selectedPeriod]?.let { updateTotal(it) }

        loadPaymentMethods()

        binding.btnCheckout.setOnClickListener { createOrder() }
    }

    private fun updateTotal(price: Double) {
        binding.tvTotal.text = "Tổng: ${String.format("%,.0f", price)} đ"
    }

    private fun loadPaymentMethods() {
        lifecycleScope.launch {
            repo.getPaymentMethods()
                .onSuccess { methods ->
                    paymentMethods = methods
                    binding.layoutPayment.visibility = if (methods.isEmpty()) View.GONE else View.VISIBLE
                    methods.forEach { method ->
                        val chip = Chip(this@CheckoutActivity).apply {
                            text = method.name
                            isCheckable = true
                            setOnClickListener { selectedPaymentId = method.id }
                        }
                        binding.chipGroupPayment.addView(chip)
                    }
                    if (methods.isNotEmpty()) {
                        selectedPaymentId = methods[0].id
                        (binding.chipGroupPayment.getChildAt(0) as? Chip)?.isChecked = true
                    }
                }
        }
    }

    private fun createOrder() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCheckout.isEnabled = false
        lifecycleScope.launch {
            repo.createOrder(planId, selectedPeriod)
                .onSuccess { no ->
                    tradeNo = no
                    checkout(no)
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnCheckout.isEnabled = true
                    Snackbar.make(binding.root, e.message ?: "Lỗi tạo đơn hàng", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun checkout(no: String) {
        lifecycleScope.launch {
            repo.checkoutOrder(no, selectedPaymentId)
                .onSuccess { result ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnCheckout.isEnabled = true
                    val data = result?.data
                    when {
                        data == null -> showSuccess()
                        data is String && (data.startsWith("http://") || data.startsWith("https://")) ->
                            openPaymentWebView(data)
                        else -> showSuccess()
                    }
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnCheckout.isEnabled = true
                    Snackbar.make(binding.root, e.message ?: "Lỗi thanh toán", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun openPaymentWebView(url: String) {
        binding.webView.apply {
            visibility = View.VISIBLE
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl(url)
        }
        binding.layoutCheckout.visibility = View.GONE
    }

    private fun showSuccess() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Đặt hàng thành công")
            .setMessage("Đơn hàng đã được tạo thành công. Vui lòng kiểm tra và thanh toán.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

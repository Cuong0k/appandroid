package com.vpnapp.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.vpnapp.App
import com.vpnapp.Constants
import com.vpnapp.api.ApiClient
import com.vpnapp.data.AppRepository
import com.vpnapp.databinding.ActivitySettingsBinding
import com.vpnapp.ui.auth.LoginActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val prefs = App.instance.preferencesManager
    private val repo = AppRepository(prefs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            title = "Cài đặt"
            setDisplayHomeAsUpEnabled(true)
        }

        loadCurrentValues()
        setupListeners()
    }

    private fun loadCurrentValues() {
        lifecycleScope.launch {
            val email = prefs.userEmail.first()
            val url = prefs.baseUrl.first()
            binding.tvEmail.text = email ?: "Chưa đăng nhập"
            binding.etServerUrl.setText(url ?: Constants.DEFAULT_BASE_URL)
        }
    }

    private fun setupListeners() {
        binding.btnSaveUrl.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isBlank()) {
                Snackbar.make(binding.root, "URL không được để trống", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                prefs.saveBaseUrl(url)
                ApiClient.reset()
                Snackbar.make(binding.root, "Đã lưu URL máy chủ", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất") { _, _ -> logout() }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.btnViewOrders.setOnClickListener { showOrders() }
    }

    private fun showChangePasswordDialog() {
        val view = layoutInflater.inflate(
            android.R.layout.simple_list_item_2, null, false
        )
        val inputOld = TextInputEditText(this).apply { hint = "Mật khẩu cũ" }
        val inputNew = TextInputEditText(this).apply { hint = "Mật khẩu mới" }
        val inputConfirm = TextInputEditText(this).apply { hint = "Xác nhận mật khẩu mới" }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(inputOld)
            addView(inputNew)
            addView(inputConfirm)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Đổi mật khẩu")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                val old = inputOld.text.toString()
                val new = inputNew.text.toString()
                val confirm = inputConfirm.text.toString()
                when {
                    old.isBlank() || new.isBlank() ->
                        Snackbar.make(binding.root, "Vui lòng điền đầy đủ", Snackbar.LENGTH_SHORT).show()
                    new != confirm ->
                        Snackbar.make(binding.root, "Mật khẩu không khớp", Snackbar.LENGTH_SHORT).show()
                    new.length < 8 ->
                        Snackbar.make(binding.root, "Mật khẩu ít nhất 8 ký tự", Snackbar.LENGTH_SHORT).show()
                    else -> changePassword(old, new)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun changePassword(old: String, new: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repo.changePassword(old, new)
                .onSuccess {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "Đổi mật khẩu thành công", Snackbar.LENGTH_SHORT).show()
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, e.message ?: "Thất bại", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun showOrders() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repo.getOrders()
                .onSuccess { orders ->
                    binding.progressBar.visibility = View.GONE
                    if (orders.isEmpty()) {
                        MaterialAlertDialogBuilder(this@SettingsActivity)
                            .setTitle("Lịch sử đơn hàng")
                            .setMessage("Chưa có đơn hàng nào.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@onSuccess
                    }
                    val items = orders.map { order ->
                        val status = when (order.status) {
                            0 -> "Chờ thanh toán"
                            1 -> "Đã thanh toán"
                            2 -> "Đã hủy"
                            else -> "Không rõ"
                        }
                        "#${order.trade_no.take(8)}... - ${String.format("%,.0f", order.total_amount)} đ - $status"
                    }.toTypedArray()
                    MaterialAlertDialogBuilder(this@SettingsActivity)
                        .setTitle("Lịch sử đơn hàng")
                        .setItems(items, null)
                        .setPositiveButton("Đóng", null)
                        .show()
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, e.message ?: "Lỗi", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            prefs.clearAuth()
            ApiClient.reset()
            startActivity(Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

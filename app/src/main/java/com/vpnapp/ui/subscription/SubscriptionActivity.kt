package com.vpnapp.ui.subscription

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vpnapp.App
import com.vpnapp.data.AppRepository
import com.vpnapp.databinding.ActivitySubscriptionBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySubscriptionBinding
    private val repo = AppRepository(App.instance.preferencesManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            title = "Thông tin đăng ký"
            setDisplayHomeAsUpEnabled(true)
        }

        binding.btnBuyPlan.setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }

        loadSubscription()
    }

    private fun loadSubscription() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repo.getSubscribeInfo()
                .onSuccess { sub ->
                    binding.progressBar.visibility = View.GONE
                    if (sub.plan != null) {
                        binding.layoutHasPlan.visibility = View.VISIBLE
                        binding.layoutNoPlan.visibility = View.GONE

                        binding.tvPlanName.text = sub.plan.name
                        binding.tvPlanContent.text = sub.plan.content ?: ""
                        binding.tvPlanContent.visibility =
                            if (sub.plan.content.isNullOrEmpty()) View.GONE else View.VISIBLE

                        val used = sub.u + sub.d
                        val total = sub.transfer_enable
                        binding.tvTrafficUsed.text = formatBytes(used)
                        binding.tvTrafficTotal.text = formatBytes(total)
                        binding.tvTrafficRemain.text = "Còn lại: ${formatBytes(maxOf(0, total - used))}"
                        binding.progressTraffic.progress =
                            if (total > 0) ((used.toDouble() / total) * 100).toInt() else 0

                        binding.tvExpiry.text = if (sub.expired_at != null) {
                            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            df.format(Date(sub.expired_at * 1000))
                        } else "Không giới hạn"

                        val speed = sub.plan.speed_limit
                        binding.tvSpeed.text = if (speed != null && speed > 0) "${speed} Mbps" else "Không giới hạn"
                        binding.tvDevices.text = "${sub.plan.device_limit ?: "Không giới hạn"} thiết bị"

                        binding.tvSubscribeUrl.text = sub.subscribe_url
                        binding.btnCopyUrl.setOnClickListener {
                            val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("Subscribe URL", sub.subscribe_url)
                            )
                            com.google.android.material.snackbar.Snackbar
                                .make(binding.root, "Đã sao chép URL đăng ký", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        binding.layoutHasPlan.visibility = View.GONE
                        binding.layoutNoPlan.visibility = View.VISIBLE
                    }
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, e.message ?: "Lỗi tải thông tin", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
                }
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

package com.vpnapp.ui.subscription

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.vpnapp.App
import com.vpnapp.api.model.PlanInfo
import com.vpnapp.data.AppRepository
import com.vpnapp.databinding.ActivityPlansBinding
import kotlinx.coroutines.launch

class PlansActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlansBinding
    private val repo = AppRepository(App.instance.preferencesManager)
    private lateinit var adapter: PlanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            title = "Chọn gói dịch vụ"
            setDisplayHomeAsUpEnabled(true)
        }

        adapter = PlanAdapter { plan -> openCheckout(plan) }
        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(this@PlansActivity)
            adapter = this@PlansActivity.adapter
        }

        loadPlans()
    }

    private fun loadPlans() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repo.getPlans()
                .onSuccess { plans ->
                    binding.progressBar.visibility = View.GONE
                    val visible = plans.filter { it.show }
                    if (visible.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        adapter.submitList(visible.sortedBy { it.sort })
                    }
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, e.message ?: "Lỗi tải gói", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun openCheckout(plan: PlanInfo) {
        startActivity(Intent(this, CheckoutActivity::class.java).apply {
            putExtra("plan_id", plan.id)
            putExtra("plan_name", plan.name)
            putExtra("plan_content", plan.content)
            putExtra("transfer_enable", plan.transfer_enable)
            putExtra("month_price", plan.month_price ?: -1.0)
            putExtra("quarter_price", plan.quarter_price ?: -1.0)
            putExtra("half_year_price", plan.half_year_price ?: -1.0)
            putExtra("year_price", plan.year_price ?: -1.0)
            putExtra("two_year_price", plan.two_year_price ?: -1.0)
            putExtra("three_year_price", plan.three_year_price ?: -1.0)
            putExtra("onetime_price", plan.onetime_price ?: -1.0)
            putExtra("speed_limit", plan.speed_limit ?: 0)
            putExtra("device_limit", plan.device_limit ?: 0)
        })
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

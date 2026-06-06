package com.vpnapp.ui.subscription

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vpnapp.api.model.PlanInfo
import com.vpnapp.databinding.ItemPlanBinding

class PlanAdapter(
    private val onPlanClick: (PlanInfo) -> Unit
) : ListAdapter<PlanInfo, PlanAdapter.PlanViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlanViewHolder(
        ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: PlanViewHolder, pos: Int) = holder.bind(getItem(pos))

    inner class PlanViewHolder(private val b: ItemPlanBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(plan: PlanInfo) {
            b.tvPlanName.text = plan.name
            b.tvPlanContent.text = buildString {
                append("📦 Lưu lượng: ${plan.transfer_enable} GB\n")
                plan.speed_limit?.let { if (it > 0) append("⚡ Tốc độ: $it Mbps\n") }
                plan.device_limit?.let { append("📱 Thiết bị: $it\n") }
                if (!plan.content.isNullOrBlank()) append("\n${plan.content}")
            }.trimEnd()

            val prices = plan.getPrices()
            val minPrice = prices.values.minOrNull()
            if (minPrice != null) {
                val unit = prices.entries.minByOrNull { it.value }
                b.tvPlanPrice.text = "Từ ${String.format("%,.0f", minPrice)} đ/${getPeriodLabel(unit?.key)}"
            } else {
                b.tvPlanPrice.text = "Liên hệ"
            }

            b.root.setOnClickListener { onPlanClick(plan) }
        }

        private fun getPeriodLabel(key: String?) = when (key) {
            "month" -> "tháng"
            "quarter" -> "quý"
            "half_year" -> "6 tháng"
            "year" -> "năm"
            "two_year" -> "2 năm"
            "three_year" -> "3 năm"
            "onetime" -> "lần"
            else -> "?"
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PlanInfo>() {
            override fun areItemsTheSame(a: PlanInfo, b: PlanInfo) = a.id == b.id
            override fun areContentsTheSame(a: PlanInfo, b: PlanInfo) = a == b
        }
    }
}

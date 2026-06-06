package com.vpnapp.ui.nodes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vpnapp.R
import com.vpnapp.api.model.VpnNode
import com.vpnapp.databinding.ItemNodeBinding

class NodeAdapter(
    private val selectedId: String?,
    private val onNodeClick: (VpnNode) -> Unit
) : ListAdapter<VpnNode, NodeAdapter.NodeViewHolder>(DIFF) {

    private var currentSelectedId: String? = selectedId

    fun updateSelected(id: String?) {
        val old = currentItems().indexOfFirst { it.id == currentSelectedId }
        val new = currentItems().indexOfFirst { it.id == id }
        currentSelectedId = id
        if (old >= 0) notifyItemChanged(old)
        if (new >= 0) notifyItemChanged(new)
    }

    private fun currentItems() = (0 until itemCount).map { getItem(it) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeViewHolder(
        ItemNodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).id == currentSelectedId)
    }

    inner class NodeViewHolder(private val b: ItemNodeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(node: VpnNode, selected: Boolean) {
            b.tvNodeName.text = node.name
            b.tvNodeType.text = node.type.uppercase()
            b.tvNodeServer.text = "${node.server}:${node.port}"
            b.tvFlag.text = flagEmoji(node.countryCode)

            b.tvLatency.text = when {
                node.latency < 0 -> "-- ms"
                node.latency < 150 -> "${node.latency} ms"
                node.latency < 300 -> "${node.latency} ms"
                else -> "${node.latency} ms"
            }
            b.tvLatency.setTextColor(
                ContextCompat.getColor(b.root.context, when {
                    node.latency < 0 -> android.R.color.darker_gray
                    node.latency < 150 -> android.R.color.holo_green_dark
                    node.latency < 300 -> android.R.color.holo_orange_dark
                    else -> android.R.color.holo_red_dark
                })
            )

            b.ivSelected.visibility = if (selected) android.view.View.VISIBLE else android.view.View.GONE
            b.root.isSelected = selected

            b.root.setOnClickListener {
                onNodeClick(node)
                updateSelected(node.id)
            }
        }
    }

    private fun flagEmoji(code: String): String {
        if (code.length != 2) return "🌐"
        val f = 0x1F1E6 - 65 + code[0].uppercaseChar().code
        val s = 0x1F1E6 - 65 + code[1].uppercaseChar().code
        return String(Character.toChars(f)) + String(Character.toChars(s))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<VpnNode>() {
            override fun areItemsTheSame(a: VpnNode, b: VpnNode) = a.id == b.id
            override fun areContentsTheSame(a: VpnNode, b: VpnNode) = a == b
        }
    }
}

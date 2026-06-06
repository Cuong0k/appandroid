package com.vpnapp.ui.nodes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.vpnapp.App
import com.vpnapp.api.model.VpnNode
import com.vpnapp.data.AppRepository
import com.vpnapp.databinding.ActivityNodeListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class NodeListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNodeListBinding
    private val prefs = App.instance.preferencesManager
    private val repo = AppRepository(prefs)
    private val gson = Gson()

    private lateinit var adapter: NodeAdapter
    private var allNodes = listOf<VpnNode>()
    private var pingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNodeListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            title = "Chọn máy chủ"
            setDisplayHomeAsUpEnabled(true)
        }

        val savedNodeJson = android.os.Bundle().let {
            lifecycleScope.launch { prefs.selectedNode.first() }
            null
        }

        adapter = NodeAdapter(null) { node -> selectNode(node) }

        binding.rvNodes.apply {
            layoutManager = LinearLayoutManager(this@NodeListActivity)
            adapter = this@NodeListActivity.adapter
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterNodes(newText ?: "")
                return true
            }
        })

        binding.btnPingAll.setOnClickListener { pingAllNodes() }

        loadNodes()
    }

    private fun loadNodes() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val token = prefs.userToken.first() ?: run {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Không tìm thấy token. Vui lòng đăng nhập lại."
                return@launch
            }
            val savedJson = prefs.selectedNode.first()
            val savedId = try { gson.fromJson(savedJson, VpnNode::class.java)?.id } catch (_: Exception) { null }

            repo.getVpnNodes(token)
                .onSuccess { nodes ->
                    allNodes = nodes
                    binding.progressBar.visibility = View.GONE
                    if (nodes.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "Không có node nào. Vui lòng kiểm tra gói đăng ký."
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        adapter = NodeAdapter(savedId) { n -> selectNode(n) }
                        binding.rvNodes.adapter = adapter
                        adapter.submitList(nodes)
                    }
                }
                .onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Lỗi tải node: ${e.message}"
                    Snackbar.make(binding.root, e.message ?: "Error", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun filterNodes(query: String) {
        val filtered = if (query.isBlank()) allNodes
        else allNodes.filter {
            it.name.contains(query, true) ||
            it.server.contains(query, true) ||
            it.countryCode.contains(query, true) ||
            it.type.contains(query, true)
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun pingAllNodes() {
        pingJob?.cancel()
        pingJob = lifecycleScope.launch {
            allNodes.forEach { node ->
                launch {
                    val latency = measureLatency(node.server, node.port)
                    val index = allNodes.indexOfFirst { it.id == node.id }
                    if (index >= 0) {
                        allNodes[index].latency = latency
                        withContext(Dispatchers.Main) { adapter.notifyItemChanged(index) }
                    }
                }
            }
        }
    }

    private suspend fun measureLatency(host: String, port: Int): Int = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), 3000)
            }
            (System.currentTimeMillis() - start).toInt()
        } catch (_: Exception) { 9999 }
    }

    private fun selectNode(node: VpnNode) {
        lifecycleScope.launch {
            prefs.saveSelectedNode(gson.toJson(node))
            setResult(Activity.RESULT_OK, Intent().putExtra("selected_node", gson.toJson(node)))
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() { super.onDestroy(); pingJob?.cancel() }
}

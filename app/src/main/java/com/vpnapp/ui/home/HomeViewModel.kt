package com.vpnapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.vpnapp.App
import com.vpnapp.api.model.SubscribeInfo
import com.vpnapp.api.model.UserInfo
import com.vpnapp.api.model.VpnNode
import com.vpnapp.api.model.VpnState
import com.vpnapp.data.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {
    private val prefs = App.instance.preferencesManager
    private val repo = AppRepository(prefs)
    private val gson = Gson()

    private val _userInfo = MutableLiveData<UserInfo?>()
    val userInfo: LiveData<UserInfo?> = _userInfo

    private val _subscribeInfo = MutableLiveData<SubscribeInfo?>()
    val subscribeInfo: LiveData<SubscribeInfo?> = _subscribeInfo

    private val _selectedNode = MutableLiveData<VpnNode?>()
    val selectedNode: LiveData<VpnNode?> = _selectedNode

    private val _vpnState = MutableLiveData<VpnState>(VpnState.DISCONNECTED)
    val vpnState: LiveData<VpnState> = _vpnState

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadData() {
        viewModelScope.launch {
            _loading.value = true
            repo.getUserInfo()
                .onSuccess { _userInfo.value = it }
                .onFailure { _error.value = it.message }

            repo.getSubscribeInfo()
                .onSuccess { _subscribeInfo.value = it }

            // Restore selected node
            val nodeJson = prefs.selectedNode.first()
            if (!nodeJson.isNullOrEmpty()) {
                runCatching { _selectedNode.value = gson.fromJson(nodeJson, VpnNode::class.java) }
            }
            _loading.value = false
        }
    }

    fun setVpnState(state: VpnState) { _vpnState.value = state }

    fun setSelectedNode(node: VpnNode) {
        _selectedNode.value = node
        viewModelScope.launch { prefs.saveSelectedNode(gson.toJson(node)) }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }

    fun formatExpiry(epochSec: Long?): String {
        if (epochSec == null) return "Không giới hạn"
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochSec * 1000))
        val now = System.currentTimeMillis() / 1000
        val daysLeft = ((epochSec - now) / 86400).toInt()
        return "Hết hạn: $date" + if (daysLeft >= 0) " (còn $daysLeft ngày)" else " (đã hết hạn)"
    }

    fun trafficPercent(used: Long, total: Long) =
        if (total <= 0) 0 else ((used.toDouble() / total) * 100).toInt().coerceIn(0, 100)
}

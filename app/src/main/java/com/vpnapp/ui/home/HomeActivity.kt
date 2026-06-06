package com.vpnapp.ui.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.vpnapp.R
import com.vpnapp.api.model.VpnState
import com.vpnapp.databinding.ActivityHomeBinding
import com.vpnapp.ui.nodes.NodeListActivity
import com.vpnapp.ui.settings.SettingsActivity
import com.vpnapp.ui.subscription.SubscriptionActivity
import com.vpnapp.vpn.VpnController

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val vm: HomeViewModel by viewModels()

    private val vpnPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) startVpn()
        else Snackbar.make(binding.root, "Quyền VPN bị từ chối", Snackbar.LENGTH_SHORT).show()
    }

    private val nodePickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.loadData()
    }

    private val vpnStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra("state")?.let { s ->
                runCatching { vm.setVpnState(VpnState.valueOf(s)) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(vpnStateReceiver, IntentFilter(VpnController.ACTION_VPN_STATE_CHANGED))

        vm.loadData()
        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        vm.loading.observe(this) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        vm.userInfo.observe(this) { user ->
            user ?: return@observe
            binding.tvEmail.text = user.email
            binding.tvBalance.text = "Số dư: ${String.format("%,.0f", user.balance)} VND"
        }

        vm.subscribeInfo.observe(this) { sub ->
            if (sub?.plan != null) {
                val used = sub.u + sub.d
                binding.tvPlanName.text = sub.plan.name
                binding.tvTraffic.text = "${vm.formatBytes(used)} / ${vm.formatBytes(sub.transfer_enable)}"
                binding.progressTraffic.progress = vm.trafficPercent(used, sub.transfer_enable)
                binding.tvExpiry.text = vm.formatExpiry(sub.expired_at)
                binding.cardSubscription.setOnClickListener {
                    startActivity(Intent(this, SubscriptionActivity::class.java))
                }
            } else {
                binding.tvPlanName.text = "Chưa có gói"
                binding.tvTraffic.text = "0 / 0"
                binding.progressTraffic.progress = 0
                binding.tvExpiry.text = "Mua gói để sử dụng"
                binding.cardSubscription.setOnClickListener {
                    startActivity(Intent(this, SubscriptionActivity::class.java))
                }
            }
        }

        vm.selectedNode.observe(this) { node ->
            if (node != null) {
                binding.tvNodeName.text = node.name
                binding.tvNodeMeta.text = "${node.type.uppercase()} • ${node.server}:${node.port}"
                binding.tvNodeFlag.text = flagEmoji(node.countryCode)
            } else {
                binding.tvNodeName.text = "Chọn máy chủ"
                binding.tvNodeMeta.text = "Nhấn để chọn node kết nối"
                binding.tvNodeFlag.text = "🌐"
            }
        }

        vm.vpnState.observe(this) { updateVpnUi(it) }

        vm.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty())
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        binding.btnVpnToggle.setOnClickListener {
            when (vm.vpnState.value) {
                VpnState.DISCONNECTED, VpnState.ERROR -> requestVpnPermissionThenConnect()
                VpnState.CONNECTED -> VpnController.stopVpn(this)
                else -> {}
            }
        }
        binding.cardNode.setOnClickListener {
            nodePickLauncher.launch(Intent(this, NodeListActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { vm.loadData() }
    }

    private fun requestVpnPermissionThenConnect() {
        if (vm.selectedNode.value == null) {
            Snackbar.make(binding.root, "Hãy chọn máy chủ trước", Snackbar.LENGTH_SHORT).show()
            nodePickLauncher.launch(Intent(this, NodeListActivity::class.java))
            return
        }
        val intent = VpnService.prepare(this)
        if (intent != null) vpnPermLauncher.launch(intent) else startVpn()
    }

    private fun startVpn() {
        val node = vm.selectedNode.value ?: return
        vm.setVpnState(VpnState.CONNECTING)
        VpnController.startVpn(this, node)
    }

    private fun updateVpnUi(state: VpnState) {
        when (state) {
            VpnState.DISCONNECTED -> {
                binding.btnVpnToggle.text = "Kết nối"
                binding.btnVpnToggle.isEnabled = true
                binding.ivVpnStatus.setImageResource(R.drawable.ic_vpn_off)
                binding.tvVpnStatus.text = "Đã ngắt kết nối"
                binding.tvVpnStatus.setTextColor(getColor(android.R.color.darker_gray))
                binding.vpnIndicator.setBackgroundResource(R.drawable.bg_indicator_off)
            }
            VpnState.CONNECTING -> {
                binding.btnVpnToggle.text = "Đang kết nối..."
                binding.btnVpnToggle.isEnabled = false
                binding.tvVpnStatus.text = "Đang kết nối..."
                binding.tvVpnStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                binding.vpnIndicator.setBackgroundResource(R.drawable.bg_indicator_connecting)
            }
            VpnState.CONNECTED -> {
                binding.btnVpnToggle.text = "Ngắt kết nối"
                binding.btnVpnToggle.isEnabled = true
                binding.ivVpnStatus.setImageResource(R.drawable.ic_vpn_on)
                binding.tvVpnStatus.text = "Đã kết nối"
                binding.tvVpnStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                binding.vpnIndicator.setBackgroundResource(R.drawable.bg_indicator_on)
            }
            VpnState.DISCONNECTING -> {
                binding.btnVpnToggle.text = "Đang ngắt..."
                binding.btnVpnToggle.isEnabled = false
                binding.tvVpnStatus.text = "Đang ngắt kết nối..."
                binding.tvVpnStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
            VpnState.ERROR -> {
                binding.btnVpnToggle.text = "Thử lại"
                binding.btnVpnToggle.isEnabled = true
                binding.ivVpnStatus.setImageResource(R.drawable.ic_vpn_off)
                binding.tvVpnStatus.text = "Lỗi kết nối"
                binding.tvVpnStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                binding.vpnIndicator.setBackgroundResource(R.drawable.bg_indicator_off)
            }
        }
    }

    private fun flagEmoji(code: String): String {
        if (code.length != 2) return "🌐"
        val first = 0x1F1E6 - 65 + code[0].uppercaseChar().code
        val second = 0x1F1E6 - 65 + code[1].uppercaseChar().code
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        R.id.action_subscription -> { startActivity(Intent(this, SubscriptionActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() { super.onResume(); vm.loadData() }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vpnStateReceiver)
    }
}

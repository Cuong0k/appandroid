package com.vpnapp.vpn

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.vpnapp.api.model.VpnNode

object VpnController {
    const val ACTION_VPN_STATE_CHANGED = "com.vpnapp.VPN_STATE_CHANGED"
    const val ACTION_START = "com.vpnapp.START_VPN"
    const val ACTION_STOP = "com.vpnapp.STOP_VPN"
    const val EXTRA_NODE = "vpn_node_json"

    private val gson = Gson()

    fun startVpn(context: Context, node: VpnNode) {
        val intent = Intent(context, V2RayVpnService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_NODE, gson.toJson(node))
        }
        context.startForegroundService(intent)
    }

    fun stopVpn(context: Context) {
        val intent = Intent(context, V2RayVpnService::class.java).apply {
            action = ACTION_STOP
        }
        context.startService(intent)
    }
}

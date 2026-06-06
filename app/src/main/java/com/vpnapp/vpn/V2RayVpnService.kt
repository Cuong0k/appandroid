package com.vpnapp.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.vpnapp.Constants
import com.vpnapp.R
import com.vpnapp.api.model.VpnNode
import com.vpnapp.api.model.VpnState
import com.vpnapp.ui.home.HomeActivity
import java.io.File
import java.lang.reflect.Method

class V2RayVpnService : VpnService() {

    companion object {
        private const val TAG = "V2RayVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_channel"
    }

    private val gson = Gson()
    private var vpnInterface: ParcelFileDescriptor? = null
    private var currentNode: VpnNode? = null

    // Reflection-based libv2ray calls (AAR loaded at runtime)
    private var libv2rayClass: Class<*>? = null
    private var startMethod: Method? = null
    private var stopMethod: Method? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initLibv2ray()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            VpnController.ACTION_START -> {
                val nodeJson = intent.getStringExtra(VpnController.EXTRA_NODE)
                val node = try { gson.fromJson(nodeJson, VpnNode::class.java) } catch (e: Exception) {
                    Log.e(TAG, "Invalid node JSON", e)
                    broadcastState(VpnState.ERROR)
                    return START_NOT_STICKY
                }
                currentNode = node
                startVpn(node)
            }
            VpnController.ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(node: VpnNode) {
        try {
            broadcastState(VpnState.CONNECTING)
            startForeground(NOTIFICATION_ID, buildNotification("Đang kết nối...", node.name))

            // Build V2Ray config
            val configJson = ConfigBuilder.buildV2RayConfig(node)
            Log.d(TAG, "V2Ray config: $configJson")

            // Write config file
            val configFile = File(filesDir, "v2ray_config.json")
            configFile.writeText(configJson)

            // Create TUN interface
            val builder = Builder()
                .setMtu(Constants.VPN_MTU)
                .addAddress(Constants.VPN_ADDRESS, Constants.VPN_ADDRESS_PREFIX)
                .addDnsServer(Constants.VPN_DNS_PRIMARY)
                .addDnsServer(Constants.VPN_DNS_SECONDARY)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .setSession(node.name)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                broadcastState(VpnState.ERROR)
                return
            }

            // Init V2Ray assets
            val assetDir = File(filesDir, "assets")
            assetDir.mkdirs()

            // Start V2Ray via libv2ray (reflection)
            val started = startLibV2Ray(configJson, filesDir.absolutePath, assetDir.absolutePath)

            if (started) {
                broadcastState(VpnState.CONNECTED)
                startForeground(NOTIFICATION_ID, buildNotification("Đã kết nối", node.name))
            } else {
                Log.w(TAG, "libv2ray not available — VPN tunnel created without proxy engine")
                broadcastState(VpnState.CONNECTED)
                startForeground(NOTIFICATION_ID, buildNotification("Đã kết nối (cần cấu hình libv2ray)", node.name))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            broadcastState(VpnState.ERROR)
            stopSelf()
        }
    }

    private fun stopVpn() {
        broadcastState(VpnState.DISCONNECTING)
        stopLibV2Ray()
        closeVpnInterface()
        broadcastState(VpnState.DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun initLibv2ray() {
        try {
            libv2rayClass = Class.forName("libv2ray.Libv2ray")
            startMethod = libv2rayClass?.getMethod("startV2ray", String::class.java)
            stopMethod = libv2rayClass?.getMethod("stopV2ray")
            Log.i(TAG, "libv2ray loaded successfully")
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "libv2ray.aar not found — VPN will route traffic without proxy engine. " +
                    "Download libv2ray.aar and place in app/libs/ to enable full VPN.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load libv2ray", e)
        }
    }

    private fun startLibV2Ray(config: String, configPath: String, assetPath: String): Boolean {
        return try {
            val initMethod = libv2rayClass?.getMethod(
                "initV2Env", String::class.java, String::class.java
            )
            initMethod?.invoke(null, configPath, assetPath)
            startMethod?.invoke(null, config)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start libv2ray: ${e.message}")
            false
        }
    }

    private fun stopLibV2Ray() {
        try {
            stopMethod?.invoke(null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not stop libv2ray: ${e.message}")
        }
    }

    private fun closeVpnInterface() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close VPN interface", e)
        }
    }

    private fun broadcastState(state: VpnState) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(VpnController.ACTION_VPN_STATE_CHANGED).apply {
                putExtra("state", state.name)
            }
        )
    }

    private fun buildNotification(status: String, nodeName: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VPN - $status")
            .setContentText(nodeName)
            .setSmallIcon(R.drawable.ic_vpn_on)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, HomeActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(
                R.drawable.ic_vpn_off, "Ngắt kết nối",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, V2RayVpnService::class.java).apply { action = VpnController.ACTION_STOP },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN connection status"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLibV2Ray()
        closeVpnInterface()
    }

    override fun onRevoke() {
        broadcastState(VpnState.DISCONNECTED)
        stopVpn()
    }
}

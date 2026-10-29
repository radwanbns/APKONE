package com.example.myvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class VpnTun : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var payloadProxy: PayloadProxy? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        val serverAddress = intent?.getStringExtra("serverAddress") ?: "127.0.0.1"
        val serverPort = intent?.getIntExtra("serverPort", 443) ?: 443
        val payload = intent?.getStringExtra("payload") ?: ""
        val sni = intent?.getStringExtra("sni") ?: ""
        val protocol = intent?.getStringExtra("protocol") ?: "OPEN_VPN_WS"

        if (protocol == "OPEN_VPN_WS") {
            val wsUrl = "wss://\$serverAddress:\$serverPort/ws"
            payloadProxy = PayloadProxy(localPort = 8989, wsUrl = wsUrl, payloadText = payload, sniHost = sni)
            payloadProxy?.start()
        }

        startVpnTunnel()
        return START_STICKY
    }

    private fun startVpnTunnel() {
        try {
            val builder = Builder()
                .setSession("MyVPN")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
            Log.d("VpnTun", "VPN Tunnel established successfully.")
        } catch (e: Exception) {
            Log.e("VpnTun", "Failed to establish VPN Interface", e)
        }
    }

    private fun stopVpn() {
        try {
            payloadProxy?.stop()
            vpnInterface?.close()
            vpnInterface = null
            stopSelf()
            Log.d("VpnTun", "VPN Service Stopped.")
        } catch (e: Exception) {
            Log.e("VpnTun", "Error stopping VPN", e)
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
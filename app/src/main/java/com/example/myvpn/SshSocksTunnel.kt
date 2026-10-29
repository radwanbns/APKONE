package com.example.myvpn

import android.util.Log

class SshSocksTunnel {
    fun connect(config: AppConfig) {
        Log.d("SshSocksTunnel", "Connecting SSH SOCKS Tunnel to \${config.serverAddress}:\${config.serverPort}")
    }

    fun disconnect() {
        Log.d("SshSocksTunnel", "SSH Disconnected")
    }
}
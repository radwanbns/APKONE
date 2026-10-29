package com.example.myvpn

import com.google.gson.Gson

enum class VpnProtocol {
    OPEN_VPN_WS,
    SSH_SOCKS,
    V2RAY
}

data class AppConfig(
    var protocol: VpnProtocol = VpnProtocol.OPEN_VPN_WS,
    var serverAddress: String = "127.0.0.1",
    var serverPort: Int = 443,
    var payload: String = "GET / HTTP/1.1[crlf]Host: example.com[crlf]Upgrade: websocket[crlf][crlf]",
    var sni: String = "example.com",
    var openVpnConfig: String = "",
    var sshUser: String = "",
    var sshPass: String = ""
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): AppConfig? {
            return try {
                Gson().fromJson(json, AppConfig::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}

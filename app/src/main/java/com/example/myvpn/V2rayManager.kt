package com.example.myvpn

import android.util.Log

class V2rayManager {
    fun startV2ray(config: AppConfig) {
        Log.d("V2rayManager", "Starting V2Ray with SNI: \${config.sni}")
    }

    fun stopV2ray() {
        Log.d("V2rayManager", "V2Ray stopped.")
    }
}
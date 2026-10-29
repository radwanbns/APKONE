package com.example.myvpn

import android.util.Base64

object SubParser {
    fun parseSubscription(urlOrConfig: String): AppConfig? {
        return try {
            if (urlOrConfig.startsWith("{")) {
                AppConfig.fromJson(urlOrConfig)
            } else {
                val decoded = String(Base64.decode(urlOrConfig, Base64.DEFAULT))
                AppConfig.fromJson(decoded)
            }
        } catch (e: Exception) {
            null
        }
    }
}
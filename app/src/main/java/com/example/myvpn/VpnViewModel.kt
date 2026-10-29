package com.example.myvpn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VpnViewModel : ViewModel() {
    private val _vpnStatus = MutableLiveData<Boolean>(false)
    val vpnStatus: LiveData<Boolean> = _vpnStatus

    private val _config = MutableLiveData<AppConfig>(AppConfig())
    val config: LiveData<AppConfig> = _config

    fun setConnected(connected: Boolean) {
        _vpnStatus.value = connected
    }

    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
    }
}
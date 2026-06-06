package com.vpnapp

import android.app.Application
import com.vpnapp.data.PreferencesManager

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

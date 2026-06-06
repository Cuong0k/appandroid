package com.vpnapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vpnapp.ui.auth.LoginActivity
import com.vpnapp.ui.home.HomeActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val authData = App.instance.preferencesManager.authData.first()
            val dest = if (authData.isNullOrEmpty()) {
                LoginActivity::class.java
            } else {
                HomeActivity::class.java
            }
            startActivity(Intent(this@MainActivity, dest).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}

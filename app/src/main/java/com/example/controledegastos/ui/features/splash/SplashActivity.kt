package com.example.controledegastos.ui.features.splash

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.controledegastos.ui.configureSystemInsets
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.controledegastos.ui.features.home.MainActivity
import com.example.controledegastos.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        configureSystemInsets()

        lifecycleScope.launch {
            delay(1_000.milliseconds)
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}

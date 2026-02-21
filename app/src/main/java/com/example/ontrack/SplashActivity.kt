package com.example.ontrack

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nu mai gonflăm niciun layout aici; folosim direct windowBackground-ul din Theme.Splash,
        // și ținem doar o mică pauză ca să se vadă splash-ul.

        scope.launch {
            val application = application as OnTrackApplication
            val prefs = application.userPreferences

            val isFirstLaunch = prefs.isFirstLaunch.first()
            val skipOnboarding = prefs.skipOnboardingEnabled.first()

            delay(2_000)

            val intent = when {
                skipOnboarding -> Intent(this@SplashActivity, MainActivity::class.java).apply {
                    putExtra("start_page", 0)
                }
                isFirstLaunch -> Intent(this@SplashActivity, OnboardingActivity::class.java)
                else -> Intent(this@SplashActivity, OnboardingSecondActivity::class.java)
            }

            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

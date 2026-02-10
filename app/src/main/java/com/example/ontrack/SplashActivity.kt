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

            // Citim dacă este prima lansare înainte de a naviga mai departe
            val isFirstLaunch = prefs.isFirstLaunch.first()

            // Afișăm splash-ul aproximativ 2 secunde înainte de ecranul de welcome.
            delay(2_000)

            val nextActivity = if (isFirstLaunch) {
                OnboardingActivity::class.java
            } else {
                OnboardingSecondActivity::class.java
            }

            startActivity(Intent(this@SplashActivity, nextActivity))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

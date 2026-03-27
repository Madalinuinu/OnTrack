package com.example.ontrack

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
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
    private var pulseAnim: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_splash)

        val icon = findViewById<ImageView>(R.id.splash_icon)

        // Icon entrance: scale 0.6→1.0 cu spring + fade
        icon.alpha = 0f
        icon.scaleX = 0.60f
        icon.scaleY = 0.60f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f).apply {
                    duration = 650
                    interpolator = DecelerateInterpolator(1.4f)
                },
                ObjectAnimator.ofFloat(icon, "scaleX", 0.60f, 1f).apply {
                    duration = 700
                    interpolator = OvershootInterpolator(1.8f)
                },
                ObjectAnimator.ofFloat(icon, "scaleY", 0.60f, 1f).apply {
                    duration = 700
                    interpolator = OvershootInterpolator(1.8f)
                }
            )
            start()
        }

        // Puls subtil dupa intrare (acelasi ca la butonul GET STARTED)
        icon.postDelayed({
            if (!isFinishing) startIconPulse(icon)
        }, 900)

        // Navigare dupa 2s (logica originala)
        scope.launch {
            val prefs = (application as OnTrackApplication).userPreferences
            val isFirstLaunch   = prefs.isFirstLaunch.first()
            val skipOnboarding  = prefs.skipOnboardingEnabled.first()

            delay(2_000)

            pulseAnim?.cancel()

            val intent = when {
                skipOnboarding -> Intent(this@SplashActivity, MainActivity::class.java).apply {
                    putExtra("start_page", 0)
                }
                isFirstLaunch  -> Intent(this@SplashActivity, OnboardingActivity::class.java)
                else           -> Intent(this@SplashActivity, OnboardingSecondActivity::class.java)
            }

            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun startIconPulse(icon: ImageView) {
        val breathe = AccelerateDecelerateInterpolator()
        val px = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.06f).apply {
            duration    = 1200
            repeatCount = ObjectAnimator.INFINITE
            repeatMode  = ObjectAnimator.REVERSE
            interpolator = breathe
        }
        val py = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.06f).apply {
            duration    = 1200
            repeatCount = ObjectAnimator.INFINITE
            repeatMode  = ObjectAnimator.REVERSE
            interpolator = breathe
        }
        pulseAnim = AnimatorSet().also { it.playTogether(px, py); it.start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnim?.cancel()
        scope.cancel()
    }
}

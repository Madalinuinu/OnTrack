package com.example.ontrack

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingSecondActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var welcomeSubtext: TextView
    private lateinit var taglineText: TextView
    private lateinit var continueButton: Button
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_onboarding_2nd)

        welcomeText = findViewById(R.id.welcome_text)
        welcomeSubtext = findViewById(R.id.welcome_subtext)
        taglineText = findViewById(R.id.tagline_text)
        continueButton = findViewById(R.id.continue_button)

        // Setăm numele salvat după "Hi, Welcome"
        val application = application as OnTrackApplication
        scope.launch {
            val name = application.userPreferences.userName.first()
            if (name.isNotBlank()) {
                welcomeText.text = "Hi, Welcome $name"
            } else {
                welcomeText.text = "Hi, Welcome"
            }
        }

        animateTextIn()

        continueButton.setOnClickListener {
            val intent = Intent(this@OnboardingSecondActivity, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun animateTextIn() {
        welcomeText.alpha = 0f
        welcomeText.translationY = 30f
        welcomeSubtext.alpha = 0f
        welcomeSubtext.translationY = 30f
        taglineText.alpha = 0f
        taglineText.translationY = 30f
        continueButton.alpha = 0f
        continueButton.translationY = 30f

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            animateView(welcomeText, 0)
            animateView(welcomeSubtext, 100)
            animateView(taglineText, 200)
            animateView(continueButton, 300)
        }, 300)
    }

    private fun animateView(view: View, delay: Long) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val fadeIn = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            val translateY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 30f, 0f)
            fadeIn.duration = 500
            translateY.duration = 500
            fadeIn.interpolator = DecelerateInterpolator()
            translateY.interpolator = DecelerateInterpolator()
            fadeIn.start()
            translateY.start()
        }, delay)
    }
}


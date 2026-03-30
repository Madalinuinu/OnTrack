package com.example.ontrack

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
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

        welcomeText    = findViewById(R.id.welcome_text)
        welcomeSubtext = findViewById(R.id.welcome_subtext)
        taglineText    = findViewById(R.id.tagline_text)
        continueButton = findViewById(R.id.continue_button)

        scope.launch {
            val name = (application as OnTrackApplication).userPreferences.userName.first()
            welcomeText.text = if (name.isNotBlank()) "Hi, welcome $name" else "Hi, welcome"
        }

        playEntrance()

        continueButton.setOnClickListener {
            startActivity(Intent(this@OnboardingSecondActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ─── Entrance ─────────────────────────────────────────────────────────────

    private fun playEntrance() {
        listOf(welcomeText, welcomeSubtext, taglineText).forEach {
            it.alpha = 0f
            it.translationY = 64f
            it.scaleX = 0.92f
            it.scaleY = 0.92f
        }
        continueButton.alpha = 0f
        continueButton.translationY = 56f
        continueButton.scaleX = 0.82f
        continueButton.scaleY = 0.82f

        val h = Handler(Looper.getMainLooper())
        h.postDelayed({ animateText(welcomeText) },      200)
        h.postDelayed({ animateText(welcomeSubtext) },   390)
        h.postDelayed({ animateText(taglineText) },      560)
        h.postDelayed({ animateButtonIn() },             760)
    }

    private fun animateText(view: View) {
        val interp = DecelerateInterpolator(1.6f)
        AnimatorSet().apply {
            playTogether(
                anim(view, View.ALPHA,         0f,           1f,   620, interp),
                anim(view, View.TRANSLATION_Y, view.translationY, 0f, 620, interp),
                anim(view, View.SCALE_X,       view.scaleX,  1f,   620, interp),
                anim(view, View.SCALE_Y,       view.scaleY,  1f,   620, interp)
            )
            start()
        }
    }

    private fun animateButtonIn() {
        val decel  = DecelerateInterpolator(1.4f)
        AnimatorSet().apply {
            playTogether(
                anim(continueButton, View.ALPHA,         0f,    1f,  420, decel),
                anim(continueButton, View.TRANSLATION_Y, 56f,   0f,  420, decel),
                anim(continueButton, View.SCALE_X,       0.94f, 1f,  420, decel),
                anim(continueButton, View.SCALE_Y,       0.94f, 1f,  420, decel)
            )
            start()
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun anim(
        view: View,
        property: android.util.Property<View, Float>,
        from: Float,
        to: Float,
        duration: Long,
        interpolator: android.view.animation.Interpolator
    ) = ObjectAnimator.ofFloat(view, property, from, to).apply {
        this.duration     = duration
        this.interpolator = interpolator
    }
}

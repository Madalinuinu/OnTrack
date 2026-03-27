package com.example.ontrack

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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

    private var pulseAnim: AnimatorSet? = null
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
            pulseAnim?.cancel()
            startActivity(Intent(this@OnboardingSecondActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnim?.cancel()
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
        h.postDelayed({ startButtonPulse() },           1700)
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
        val spring = OvershootInterpolator(2.4f)
        val decel  = DecelerateInterpolator(1.2f)
        AnimatorSet().apply {
            playTogether(
                anim(continueButton, View.ALPHA,         0f,    1f,  700, decel),
                anim(continueButton, View.TRANSLATION_Y, 56f,   0f,  700, decel),
                anim(continueButton, View.SCALE_X,       0.82f, 1f,  750, spring),
                anim(continueButton, View.SCALE_Y,       0.82f, 1f,  750, spring)
            )
            start()
        }
    }

    private fun startButtonPulse() {
        val breathe = AccelerateDecelerateInterpolator()
        val px = ObjectAnimator.ofFloat(continueButton, View.SCALE_X, 1f, 1.025f).apply {
            duration     = 1100
            repeatCount  = ObjectAnimator.INFINITE
            repeatMode   = ObjectAnimator.REVERSE
            interpolator = breathe
        }
        val py = ObjectAnimator.ofFloat(continueButton, View.SCALE_Y, 1f, 1.025f).apply {
            duration     = 1100
            repeatCount  = ObjectAnimator.INFINITE
            repeatMode   = ObjectAnimator.REVERSE
            interpolator = breathe
        }
        pulseAnim = AnimatorSet().also { it.playTogether(px, py); it.start() }
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

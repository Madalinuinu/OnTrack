package com.example.ontrack

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var welcomeSubtext: TextView
    private lateinit var taglineText: TextView
    private lateinit var getStartedButton: Button
    private lateinit var nameInput: EditText

    private var isNameStep = false
    private var pulseAnim: AnimatorSet? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_onboarding)

        welcomeText      = findViewById(R.id.welcome_text)
        welcomeSubtext   = findViewById(R.id.welcome_subtext)
        taglineText      = findViewById(R.id.tagline_text)
        getStartedButton = findViewById(R.id.get_started_button)
        nameInput        = findViewById(R.id.name_input)

        nameInput.visibility = View.GONE
        getStartedButton.text = "GET STARTED"
        getStartedButton.isEnabled = true

        nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val name = s?.toString()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    welcomeText.text = "Hi $name, welcome"
                    if (isNameStep) { getStartedButton.isEnabled = true; getStartedButton.alpha = 1f }
                } else {
                    welcomeText.text = "Hi, welcome"
                    if (isNameStep) { getStartedButton.isEnabled = false; getStartedButton.alpha = 0.62f }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        playEntrance()

        getStartedButton.setOnClickListener {
            pulseAnim?.cancel()
            pulseAnim = null
            scope.launch {
                if (!isNameStep) {
                    isNameStep = true
                    revealNameInput()
                    getStartedButton.text = "CONTINUE"
                    getStartedButton.isEnabled = false
                    getStartedButton.alpha = 0.62f
                    return@launch
                }
                val prefs = (application as OnTrackApplication).userPreferences
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) return@launch
                prefs.setFirstLaunchComplete(name)
                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnim?.cancel()
        scope.cancel()
    }

    // ─── Entrance ─────────────────────────────────────────────────────────────

    private fun playEntrance() {
        // Fiecare text incepe scalat mic si transparent – va "materializa"
        listOf(welcomeText, welcomeSubtext, taglineText).forEach {
            it.alpha = 0f
            it.translationY = 64f
            it.scaleX = 0.92f
            it.scaleY = 0.92f
        }
        // Butonul pleaca mai de jos si mai scalat
        getStartedButton.alpha = 0f
        getStartedButton.translationY = 56f
        getStartedButton.scaleX = 0.82f
        getStartedButton.scaleY = 0.82f

        val h = Handler(Looper.getMainLooper())
        h.postDelayed({ animateText(welcomeText) },       200)
        h.postDelayed({ animateText(welcomeSubtext) },    390)
        h.postDelayed({ animateText(taglineText) },       560)
        h.postDelayed({ animateButtonIn() },              760)
        // Dupa ce toate elementele au aparut, butonul incepe sa pulseze subtil
        h.postDelayed({ startButtonPulse() },            1700)
    }

    /** Fade + slide-up + scale pentru texte — efect de "materializare" */
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

    /** Fade + slide-up + overshoot spring pe buton */
    private fun animateButtonIn() {
        val spring   = OvershootInterpolator(2.4f)
        val decel    = DecelerateInterpolator(1.2f)
        AnimatorSet().apply {
            playTogether(
                anim(getStartedButton, View.ALPHA,         0f,    1f,  700, decel),
                anim(getStartedButton, View.TRANSLATION_Y, 56f,   0f,  700, decel),
                anim(getStartedButton, View.SCALE_X,       0.82f, 1f,  750, spring),
                anim(getStartedButton, View.SCALE_Y,       0.82f, 1f,  750, spring)
            )
            start()
        }
    }

    /**
     * Puls subtil continuu pe buton — scale 1.0 ↔ 1.025 cu ritm de respiratie.
     * Invita utilizatorul sa apese fara sa deranjeze.
     */
    private fun startButtonPulse() {
        val breathe = AccelerateDecelerateInterpolator()
        val px = ObjectAnimator.ofFloat(getStartedButton, View.SCALE_X, 1f, 1.025f).apply {
            duration     = 1100
            repeatCount  = ObjectAnimator.INFINITE
            repeatMode   = ObjectAnimator.REVERSE
            interpolator = breathe
        }
        val py = ObjectAnimator.ofFloat(getStartedButton, View.SCALE_Y, 1f, 1.025f).apply {
            duration     = 1100
            repeatCount  = ObjectAnimator.INFINITE
            repeatMode   = ObjectAnimator.REVERSE
            interpolator = breathe
        }
        pulseAnim = AnimatorSet().also { it.playTogether(px, py); it.start() }
    }

    /** Reveal camp de nume cu fade + scale + slide */
    private fun revealNameInput() {
        val interp = DecelerateInterpolator(1.4f)
        nameInput.alpha = 0f
        nameInput.translationY = 32f
        nameInput.scaleX = 0.94f
        nameInput.scaleY = 0.94f
        nameInput.visibility = View.VISIBLE
        AnimatorSet().apply {
            playTogether(
                anim(nameInput, View.ALPHA,         0f,    1f,  450, interp),
                anim(nameInput, View.TRANSLATION_Y, 32f,   0f,  450, interp),
                anim(nameInput, View.SCALE_X,       0.94f, 1f,  450, interp),
                anim(nameInput, View.SCALE_Y,       0.94f, 1f,  450, interp)
            )
            start()
        }
        Handler(Looper.getMainLooper()).postDelayed({ nameInput.requestFocus() }, 470)
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

package com.example.ontrack

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
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

class OnboardingActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var welcomeSubtext: TextView
    private lateinit var taglineText: TextView
    private lateinit var getStartedButton: Button
    private lateinit var nameInput: EditText
    /** false = doar ecranul de design; true = suntem în pasul de introducere a numelui */
    private var isNameStep: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_onboarding)

        welcomeText = findViewById(R.id.welcome_text)
        welcomeSubtext = findViewById(R.id.welcome_subtext)
        taglineText = findViewById(R.id.tagline_text)
        getStartedButton = findViewById(R.id.get_started_button)
        nameInput = findViewById(R.id.name_input)

        // Stare inițială: doar design-ul, fără input de nume.
        nameInput.visibility = View.GONE
        getStartedButton.text = "GET STARTED"
        getStartedButton.isEnabled = true
        getStartedButton.alpha = 1f

        // Când scriem în câmpul de nume, actualizăm textul "Hi [nume], Welcome"
        // și controlăm butonul doar atunci când suntem în pasul de nume.
        nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val name = s?.toString()?.trim() ?: ""

                if (name.isNotEmpty()) {
                    welcomeText.text = "Hi $name, Welcome"
                    if (isNameStep) {
                        getStartedButton.isEnabled = true
                        getStartedButton.alpha = 1f
                    }
                } else {
                    welcomeText.text = "Hi, Welcome"
                    if (isNameStep) {
                        getStartedButton.isEnabled = false
                        getStartedButton.alpha = 0.5f
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Animate text elements
        animateTextIn()

        // Set button click listener
        getStartedButton.setOnClickListener {
            val application = application as OnTrackApplication

            scope.launch {
                // Prima apăsare: intrăm în pasul de nume (afișăm input-ul și schimbăm butonul în CONTINUE)
                if (!isNameStep) {
                    isNameStep = true
                    nameInput.visibility = View.VISIBLE
                    getStartedButton.text = "CONTINUE"
                    // Până nu e introdus nimic, butonul e dezactivat
                    getStartedButton.isEnabled = false
                    getStartedButton.alpha = 0.5f
                    return@launch
                }

                // De aici încolo suntem în pasul de nume și butonul este CONTINUE.
                val prefs = application.userPreferences
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    // Salvăm numele introdus și marcăm onboarding-ul ca fiind complet
                    prefs.setFirstLaunchComplete(name)
                } else {
                    // De siguranță: nu continuăm dacă numele e gol
                    return@launch
                }

                // Navigate to MainActivity
                val intent = Intent(this@OnboardingActivity, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
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
        getStartedButton.alpha = 0f
        getStartedButton.translationY = 30f

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            animateView(welcomeText, 0)
            animateView(welcomeSubtext, 100)
            animateView(taglineText, 200)
            animateView(getStartedButton, 300)
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

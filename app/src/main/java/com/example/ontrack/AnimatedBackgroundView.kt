package com.example.ontrack

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-screen animated background:
 *  - Base periwinkle gradient
 *  - 5 large soft-light orbs drifting on sin/cos paths
 *  - 20 small particles (dots + rings) floating upward
 *
 * Performance strategy:
 *  - Gradient + orb bitmaps built once in onSizeChanged
 *  - Per frame: drawRect + 5×drawBitmap (orbs) + 20×drawCircle (particles)
 *  - VSync-locked via postOnAnimation → silky 60fps, minimal battery impact
 */
class AnimatedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    // ══════════════════════════════════════════════════════════════════════════
    //  ORBS  –  large soft-light blobs that drift slowly
    // ══════════════════════════════════════════════════════════════════════════

    private data class Orb(
        val baseX: Float,   // 0..1 fraction of width
        val baseY: Float,   // 0..1 fraction of height
        val radius: Float,  // fraction of min(w,h)
        val color: Int,     // ARGB
        val phaseX: Float,
        val phaseY: Float,
        val freqX: Float,
        val freqY: Float,
        val ampX: Float = 0.07f,
        val ampY: Float = 0.055f
    )

    private val orbs = listOf(
        Orb(0.05f, 0.12f, 0.62f, 0x50FFFFFF.toInt(), 0.00f, 0.80f, 1.00f, 0.70f, 0.09f, 0.06f),
        Orb(0.90f, 0.20f, 0.56f, 0x42F0F2FF.toInt(), 1.80f, 0.00f, 0.75f, 1.10f, 0.06f, 0.08f),
        Orb(0.40f, 0.74f, 0.54f, 0x48F4F8FF.toInt(), 3.50f, 2.20f, 1.20f, 0.65f, 0.08f, 0.05f),
        Orb(0.08f, 0.92f, 0.40f, 0x38FFFFFF.toInt(), 2.10f, 0.50f, 0.90f, 1.30f, 0.05f, 0.07f),
        Orb(0.72f, 0.04f, 0.46f, 0x3CF8FAFF.toInt(), 4.20f, 1.60f, 1.10f, 0.85f, 0.10f, 0.04f),
    )

    private val bgPaint  = Paint()
    private val orbPaint = Paint()
    private var orbCache: List<Pair<Bitmap, Orb>> = emptyList()

    // ══════════════════════════════════════════════════════════════════════════
    //  PARTICLES  –  small dots & rings floating upward
    // ══════════════════════════════════════════════════════════════════════════

    private data class Particle(
        var y: Float,           // vertical position (0 = top, 1 = bottom screen)
        val x: Float,           // base horizontal position 0..1
        val radius: Float,      // px
        val maxAlpha: Int,      // peak alpha 0..255
        val speedY: Float,      // fraction of screen height consumed per frame
        val driftAmp: Float,    // horizontal oscillation amplitude (fraction of w)
        val driftFreq: Float,
        val driftPhase: Float,
        val hollow: Boolean     // true → ring, false → filled dot
    )

    private val particles = ArrayList<Particle>(20)
    private val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    // ══════════════════════════════════════════════════════════════════════════
    //  ANIMATION TICK
    // ══════════════════════════════════════════════════════════════════════════

    private var tick = 0.0
    private var cW = 0
    private var cH = 0

    private val frame = object : Runnable {
        override fun run() {
            tick += 0.004   // ~0.24 units/s at 60 fps → full sin period ≈ 26 s

            // Move particles upward; reset below-top ones to the bottom
            for (p in particles) {
                p.y -= p.speedY
                if (p.y < -0.06f) p.y = 1.06f
            }

            invalidate()
            if (isAttachedToWindow) postOnAnimation(this)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════════

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frame)
        recycleOrbCache()
    }

    private fun recycleOrbCache() {
        orbCache.forEach { (bmp, _) -> if (!bmp.isRecycled) bmp.recycle() }
        orbCache = emptyList()
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SIZE CHANGE → rebuild all caches
    // ══════════════════════════════════════════════════════════════════════════

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        cW = w; cH = h
        buildOrbCaches(w, h)
        initParticles()
    }

    private fun buildOrbCaches(w: Int, h: Int) {
        recycleOrbCache()

        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#A8B4FF"),
                Color.parseColor("#8C9EFF"),
                Color.parseColor("#7282D8")
            ),
            null,
            Shader.TileMode.CLAMP
        )

        val minDim = min(w, h).toFloat()
        orbCache = orbs.map { orb ->
            val diameter = (orb.radius * minDim * 2f).toInt().coerceAtLeast(4)
            val bmp = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
            val center = diameter * 0.5f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    center, center, center,
                    intArrayOf(orb.color, Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            Canvas(bmp).drawCircle(center, center, center, paint)
            Pair(bmp, orb)
        }
    }

    /**
     * Spawn 20 particles with deterministic randomness (fixed seed)
     * so the layout is always pleasant, never clustered.
     */
    private fun initParticles() {
        particles.clear()
        val rng = java.util.Random(1337L)
        repeat(20) { i ->
            particles += Particle(
                y          = rng.nextFloat(),
                x          = rng.nextFloat(),
                radius     = (rng.nextFloat() * 3.5f + 2.0f) * density,
                maxAlpha   = (rng.nextFloat() * 45f + 25f).toInt(),  // 25–70 of 255
                speedY     = rng.nextFloat() * 0.00055f + 0.00030f,  // ~14-35s to cross screen
                driftAmp   = rng.nextFloat() * 0.022f + 0.006f,
                driftFreq  = rng.nextFloat() * 0.7f + 0.35f,
                driftPhase = rng.nextFloat() * 6.283f,
                hollow     = i % 4 == 0   // 25% rings, 75% filled dots
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAW
    // ══════════════════════════════════════════════════════════════════════════

    override fun onDraw(canvas: Canvas) {
        val w = cW.toFloat()
        val h = cH.toFloat()
        if (w == 0f || h == 0f) return

        // ── 1. Base gradient ─────────────────────────────────────────────────
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // ── 2. Drifting orbs ─────────────────────────────────────────────────
        for ((bmp, orb) in orbCache) {
            val cx = ((orb.baseX + sin(tick * orb.freqX + orb.phaseX) * orb.ampX) * w).toFloat()
            val cy = ((orb.baseY + cos(tick * orb.freqY + orb.phaseY) * orb.ampY) * h).toFloat()
            canvas.drawBitmap(bmp, cx - bmp.width * 0.5f, cy - bmp.height * 0.5f, orbPaint)
        }

        // ── 3. Floating particles ────────────────────────────────────────────
        for (p in particles) {
            val px = ((p.x + sin(tick * p.driftFreq + p.driftPhase) * p.driftAmp) * w).toFloat()
            val py = (p.y * h).toFloat()

            // Smooth alpha fade near top (y≈0) and bottom (y≈1) edges
            val edgeFade = when {
                p.y < 0.10f -> (p.y / 0.10f).coerceIn(0f, 1f)
                p.y > 0.90f -> ((1f - p.y) / 0.10f).coerceIn(0f, 1f)
                else -> 1f
            }
            pPaint.alpha = (p.maxAlpha * edgeFade).toInt()

            if (p.hollow) {
                pPaint.style = Paint.Style.STROKE
                pPaint.strokeWidth = 1.4f * density
                canvas.drawCircle(px, py, p.radius + density, pPaint)
            } else {
                pPaint.style = Paint.Style.FILL
                canvas.drawCircle(px, py, p.radius, pPaint)
            }
        }
    }
}

package com.example.ui.components

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.hypot

@SuppressLint("ViewConstructor")
class FloatingOrbView(context: Context) : FrameLayout(context) {

    var onOrbClick: (() -> Unit)? = null
    var onOrbClose: (() -> Unit)? = null
    var onOpenMainActivity: (() -> Unit)? = null
    var onDragDelta: ((Float, Float) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    private val orbSizePx: Int = (64 * resources.displayMetrics.density).toInt()
    private val orbCanvasView: OrbCanvasView
    private val badgeContainer: LinearLayout
    private val badgeTextView: TextView

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val touchSlop = 16f

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        clipChildren = false
        clipToPadding = false

        val rootHorizontal = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }

        orbCanvasView = OrbCanvasView(context, orbSizePx)
        val orbParams = LinearLayout.LayoutParams(orbSizePx, orbSizePx).apply {
            setMargins(8, 8, 8, 8)
        }
        rootHorizontal.addView(orbCanvasView, orbParams)

        // Status Badge Bubble
        val density = resources.displayMetrics.density
        badgeContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            background = createBadgeBackground()
            visibility = View.GONE
            setOnClickListener {
                onOpenMainActivity?.invoke()
            }
        }

        badgeTextView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            maxLines = 2
            maxWidth = (240 * density).toInt()
            text = ""
        }
        badgeContainer.addView(badgeTextView)

        val badgeParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(4, 0, 12, 0)
        }
        rootHorizontal.addView(badgeContainer, badgeParams)

        addView(rootHorizontal)
        setupTouchEvents()
    }

    private fun createBadgeBackground(): android.graphics.drawable.GradientDrawable {
        val density = resources.displayMetrics.density
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16 * density
            setColor(Color.parseColor("#EE000314"))
            setStroke((1.5f * density).toInt(), Color.parseColor("#00E5FF"))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchEvents() {
        orbCanvasView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        onDragDelta?.invoke(dx, dy)
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        onOrbClick?.invoke()
                    } else {
                        onDragEnd?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun setListeningState(listening: Boolean) {
        orbCanvasView.isListening = listening
    }

    fun updateRms(level: Float) {
        orbCanvasView.rmsLevel = level
    }

    fun showStatusBadge(message: String) {
        post {
            badgeTextView.text = message
            badgeContainer.visibility = View.VISIBLE
        }
    }

    fun hideStatusBadge() {
        post {
            badgeContainer.visibility = View.GONE
        }
    }

    private class OrbCanvasView(context: Context, private val sizePx: Int) : View(context) {

        var isListening: Boolean = false
            set(value) {
                field = value
                invalidate()
            }

        var rmsLevel: Float = 0f
            set(value) {
                field = value
                invalidate()
            }

        private var rotationAngle = 0f
        private val rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                invalidate()
            }
        }

        private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * resources.displayMetrics.density
        }

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#000314")
        }

        private val micPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#00E5FF")
        }

        private val micStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * resources.displayMetrics.density
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#00E5FF")
        }

        init {
            rotationAnimator.start()
        }

        override fun onDetachedFromWindow() {
            rotationAnimator.cancel()
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = (sizePx / 2f) - (8f * resources.displayMetrics.density)

            // Outer Glow Pulse
            val pulseRadius = radius + (rmsLevel * 14f * resources.displayMetrics.density)
            pulsePaint.color = if (isListening) Color.parseColor("#FF007F") else Color.parseColor("#00E5FF")
            pulsePaint.alpha = if (isListening) 180 else 100
            canvas.drawCircle(cx, cy, pulseRadius, pulsePaint)

            // Inner dark circle
            canvas.drawCircle(cx, cy, radius, bgPaint)

            // Rotating Cyber Gradient Ring
            canvas.save()
            canvas.rotate(rotationAngle, cx, cy)
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3.5f * resources.displayMetrics.density
                shader = LinearGradient(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    intArrayOf(
                        Color.parseColor("#00E5FF"),
                        Color.parseColor("#9D00FF"),
                        Color.parseColor("#FF007F"),
                        Color.parseColor("#00E5FF")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, ringPaint)
            canvas.restore()

            // Centered Microphone Icon
            drawMic(canvas, cx, cy)
        }

        private fun drawMic(canvas: Canvas, cx: Float, cy: Float) {
            val d = resources.displayMetrics.density
            val micWidth = 10f * d
            val micHeight = 18f * d

            // Mic capsule body
            val capsuleRect = RectF(
                cx - micWidth / 2f,
                cy - micHeight / 2f - (2f * d),
                cx + micWidth / 2f,
                cy + micHeight / 2f - (2f * d)
            )
            micPaint.color = if (isListening) Color.parseColor("#FF007F") else Color.parseColor("#00E5FF")
            canvas.drawRoundRect(capsuleRect, micWidth / 2f, micWidth / 2f, micPaint)

            // Mic arc holder
            val arcRect = RectF(
                cx - (micWidth / 2f + 4f * d),
                cy - (micHeight / 2f) + (2f * d),
                cx + (micWidth / 2f + 4f * d),
                cy + (micHeight / 2f + 4f * d)
            )
            micStrokePaint.color = if (isListening) Color.parseColor("#FF007F") else Color.parseColor("#00E5FF")
            canvas.drawArc(arcRect, 0f, 180f, false, micStrokePaint)

            // Mic stand vertical stem
            canvas.drawLine(
                cx,
                cy + (micHeight / 2f + 4f * d),
                cx,
                cy + (micHeight / 2f + 9f * d),
                micStrokePaint
            )

            // Mic stand base
            canvas.drawLine(
                cx - 7f * d,
                cy + (micHeight / 2f + 9f * d),
                cx + 7f * d,
                cy + (micHeight / 2f + 9f * d),
                micStrokePaint
            )
        }
    }
}

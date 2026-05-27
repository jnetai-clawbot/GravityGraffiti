package com.jnetai.gravitygraffiti

import android.graphics.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*
import java.util.*
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "GravityGraffiti"
        const val CURRENT_VERSION = "1.0.0"
        const val GITHUB_REPO = "jnetai-clawbot/GravityGraffiti"
    }

    private lateinit var gameView: GameView
    private lateinit var aboutButton: Button
    private lateinit var scoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0A0A1A.toInt()
        window.navigationBarColor = 0xFF0A0A1A.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0A1A.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        scoreText = TextView(this).apply {
            text = "Height: 0"
            setTextColor(0xFFFF3366.toInt())
            textSize = 18f
            setPadding(32, 32, 32, 8)
            typeface = Typeface.MONOSPACE
        }

        gameView = GameView(this, ::updateScore)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 8, 16, 48)
        }

        val restartBtn = Button(this).apply {
            text = "Restart"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { gameView.restart() }
        }

        aboutButton = Button(this).apply {
            text = "About"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFF00FF88.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { showAbout() }
        }

        buttonBar.addView(restartBtn)
        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(32, 0) }
        buttonBar.addView(spacer)
        buttonBar.addView(aboutButton)

        root.addView(scoreText)
        root.addView(gameView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buttonBar)
        setContentView(root)
    }

    private fun updateScore(height: Int) {
        runOnUiThread {
            scoreText.text = "Height: $height"
        }
    }

    private fun showAbout() {
        val builder = AlertDialog.Builder(this, R.style.AboutDialogTheme)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 32)
            setBackgroundColor(0xFF151528.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "Gravity Graffiti"
            setTextColor(0xFF00FF88.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Made by jnetai.com"
            setTextColor(0xFF888899.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        layout.addView(TextView(this).apply {
            text = "Version $CURRENT_VERSION"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })

        val checkBtn = Button(this).apply {
            text = "Check for Update"
            setBackgroundColor(0xFF006644.toInt())
            setTextColor(0xFF00FF88.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            val btn = this
            setOnClickListener {
                btn.isEnabled = false
                btn.text = "Checking..."
                checkForUpdate { result ->
                    runOnUiThread {
                        btn.text = result
                        btn.isEnabled = true
                    }
                }
            }
        }
        layout.addView(checkBtn)

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 24)
        })

        val shareBtn = Button(this).apply {
            text = "Share App"
            setBackgroundColor(0xFF234A6A.toInt())
            setTextColor(0xFF00CCFF.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Gravity Graffiti")
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                }
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        }
        layout.addView(shareBtn)

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        builder.setView(scrollView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkForUpdate(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name").removePrefix("v")

                if (latestTag != CURRENT_VERSION) {
                    callback("New version $latestTag available!")
                } else {
                    callback("You're up to date!")
                }
            } catch (e: Exception) {
                callback("Could not check updates: ${e.message}")
            }
        }
    }
}

class GameView(context: Context, private val scoreCallback: (Int) -> Unit) : View(context) {

    data class LineSegment(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val creationTime: Long
    )

    data class GravityWell(
        val x: Float, val y: Float,
        val creationTime: Long
    )

    companion object {
        const val FADE_MS = 3000L
        const val GRAVITY = 0.45f
        const val BOUNCE = 0.55f
        const val WELL_FORCE = 80f
        const val LONG_PRESS_MS = 500L
        const val SEGMENT_MIN_LEN = 6f
        const val CHAR_RADIUS = 16f
        const val WELL_RADIUS = 40f
        const val VELOCITY_DAMP = 0.995f
    }

    private val lines = mutableListOf<LineSegment>()
    private val wells = mutableListOf<GravityWell>()

    private var charX = 0f
    private var charY = 0f
    private var charVX = 0f
    private var charVY = 0f

    private var drawStartX = 0f
    private var drawStartY = 0f
    private var isDrawing = false
    private var longPressTriggered = false
    private var touchStartTime = 0L

    private var gameOver = false
    private var maxHeight = 0f
    private var initialized = false

    private val bgPaint = Paint().apply {
        color = 0xFF0A0A1A.toInt()
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = 0xFFFF3366.toInt()
        strokeWidth = 7f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val wellRingPaint = Paint().apply {
        color = 0xFFFF3366.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val wellFillPaint = Paint().apply {
        color = 0x22FF3366.toInt()
        style = Paint.Style.FILL
    }

    private val charPaint = Paint().apply {
        color = 0xFF00CCFF.toInt()
        style = Paint.Style.FILL
    }

    private val charGlowPaint = Paint().apply {
        color = 0x2200CCFF.toInt()
        style = Paint.Style.FILL
    }

    private val dividerPaint = Paint().apply {
        color = 0x15FFFFFF.toInt()
        strokeWidth = 3f
    }

    private val zoneLabelPaint = Paint().apply {
        color = 0x22FFFFFF.toInt()
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!initialized) {
            charX = w * 0.78f
            charY = h * 0.25f
            initialized = true
            post(gameLoop)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) restart()
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawing = true
                longPressTriggered = false
                drawStartX = event.x
                drawStartY = event.y
                touchStartTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDrawing) return true

                val elapsed = System.currentTimeMillis() - touchStartTime
                val distFromStart = sqrt(
                    (event.x - drawStartX).pow(2) + (event.y - drawStartY).pow(2)
                )

                if (elapsed > LONG_PRESS_MS && distFromStart < 30f && !longPressTriggered) {
                    longPressTriggered = true
                    wells.add(GravityWell(event.x, event.y, System.currentTimeMillis()))
                    drawStartX = event.x
                    drawStartY = event.y
                    return true
                }

                if (!longPressTriggered || elapsed > LONG_PRESS_MS) {
                    val segDx = event.x - drawStartX
                    val segDy = event.y - drawStartY
                    val segLen = sqrt(segDx * segDx + segDy * segDy)
                    if (segLen > SEGMENT_MIN_LEN) {
                        lines.add(
                            LineSegment(
                                drawStartX, drawStartY,
                                event.x, event.y,
                                System.currentTimeMillis()
                            )
                        )
                        drawStartX = event.x
                        drawStartY = event.y
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDrawing = false
                longPressTriggered = false
            }
        }
        return true
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (gameOver) return
            update()
            invalidate()
            postDelayed(this, 16)
        }
    }

    private fun update() {
        val now = System.currentTimeMillis()

        lines.removeAll { now - it.creationTime > FADE_MS }
        wells.removeAll { now - it.creationTime > FADE_MS }

        charVY += GRAVITY

        for (well in wells) {
            val age = now - well.creationTime
            val strength = (1f - age.toFloat() / FADE_MS).coerceIn(0f, 1f)
            val dx = well.x - charX
            val dy = well.y - charY
            val distSq = dx * dx + dy * dy
            val dist = sqrt(distSq).coerceAtLeast(1f)
            val force = (WELL_FORCE * strength) / dist
            charVX += (dx / dist) * force
            charVY += (dy / dist) * force
        }

        charVX *= VELOCITY_DAMP
        charVY *= VELOCITY_DAMP

        charX += charVX
        charY += charVY

        if (charX - CHAR_RADIUS < 0f) {
            charX = CHAR_RADIUS
            charVX = abs(charVX) * BOUNCE
        }
        if (charX + CHAR_RADIUS > width) {
            charX = width - CHAR_RADIUS
            charVX = -abs(charVX) * BOUNCE
        }
        if (charY - CHAR_RADIUS < 0f) {
            charY = CHAR_RADIUS
            charVY = abs(charVY) * BOUNCE
        }
        if (charY > height + CHAR_RADIUS * 2) {
            gameOver = true
            return
        }

        for (seg in lines) {
            resolveCollision(seg)
        }

        val currentHeight = height - charY
        if (currentHeight > maxHeight) {
            maxHeight = currentHeight
            scoreCallback(maxHeight.toInt())
        }
    }

    private fun resolveCollision(seg: LineSegment) {
        val ldx = seg.x2 - seg.x1
        val ldy = seg.y2 - seg.y1
        val lenSq = ldx * ldx + ldy * ldy
        if (lenSq < 0.01f) {
            val d = sqrt((charX - seg.x1).pow(2) + (charY - seg.y1).pow(2))
            if (d < CHAR_RADIUS) {
                val nx = (charX - seg.x1) / d
                val ny = (charY - seg.y1) / d
                charX += (CHAR_RADIUS - d) * nx
                charY += (CHAR_RADIUS - d) * ny
                val dot = charVX * nx + charVY * ny
                if (dot < 0f) {
                    charVX -= 2f * dot * nx * BOUNCE
                    charVY -= 2f * dot * ny * BOUNCE
                }
            }
            return
        }

        var t = ((charX - seg.x1) * ldx + (charY - seg.y1) * ldy) / lenSq
        t = t.coerceIn(0f, 1f)

        val closestX = seg.x1 + t * ldx
        val closestY = seg.y1 + t * ldy

        val dx = charX - closestX
        val dy = charY - closestY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < CHAR_RADIUS && dist > 0.001f) {
            val nx = dx / dist
            val ny = dy / dist
            charX += (CHAR_RADIUS - dist) * nx
            charY += (CHAR_RADIUS - dist) * ny
            val dot = charVX * nx + charVY * ny
            if (dot < 0f) {
                charVX -= 2f * dot * nx * BOUNCE
                charVY -= 2f * dot * ny * BOUNCE
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val dividerX = viewWidth / 2f

        canvas.drawRect(0f, 0f, viewWidth, viewHeight, bgPaint)

        canvas.drawLine(dividerX, 0f, dividerX, viewHeight, dividerPaint)

        val now = System.currentTimeMillis()

        for (seg in lines) {
            val age = now - seg.creationTime
            val alpha = ((1f - age.toFloat() / FADE_MS) * 255).toInt().coerceIn(0, 255)
            linePaint.alpha = alpha
            canvas.drawLine(seg.x1, seg.y1, seg.x2, seg.y2, linePaint)
        }
        linePaint.alpha = 255

        for (well in wells) {
            val age = now - well.creationTime
            val alpha = ((1f - age.toFloat() / FADE_MS) * 255).toInt().coerceIn(0, 255)
            val scale = 1f + (1f - age.toFloat() / FADE_MS) * 0.6f

            wellRingPaint.alpha = alpha
            canvas.drawCircle(well.x, well.y, WELL_RADIUS * scale, wellRingPaint)
            canvas.drawCircle(well.x, well.y, WELL_RADIUS * scale * 0.55f, wellRingPaint)
            canvas.drawCircle(well.x, well.y, WELL_RADIUS * scale * 0.2f, wellRingPaint)

            wellFillPaint.alpha = (alpha * 0.3f).toInt().coerceIn(0, 255)
            canvas.drawCircle(well.x, well.y, WELL_RADIUS * scale, wellFillPaint)
        }
        wellRingPaint.alpha = 255
        wellFillPaint.alpha = 0x22

        canvas.drawCircle(charX, charY, CHAR_RADIUS * 2.2f, charGlowPaint)
        canvas.drawCircle(charX, charY, CHAR_RADIUS, charPaint)

        if (gameOver) {
            val overlayPaint = Paint().apply {
                color = 0xBB000000.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, viewWidth, viewHeight, overlayPaint)

            val titlePaint = Paint().apply {
                color = 0xFFFF3366.toInt()
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("GAME OVER", viewWidth / 2f, viewHeight / 2f - 24, titlePaint)

            val scorePaint = Paint().apply {
                color = 0xFFCCCCCC.toInt()
                textSize = 24f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
            }
            canvas.drawText("Height: ${maxHeight.toInt()}", viewWidth / 2f, viewHeight / 2f + 24, scorePaint)

            val restartPaint = Paint().apply {
                color = 0xFF888888.toInt()
                textSize = 22f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Tap to Restart", viewWidth / 2f, viewHeight / 2f + 64, restartPaint)
        }
    }

    fun restart() {
        val wasGameOver = gameOver
        lines.clear()
        wells.clear()
        charX = width * 0.78f
        charY = height * 0.25f
        charVX = 0f
        charVY = 0f
        maxHeight = 0f
        gameOver = false
        isDrawing = false
        longPressTriggered = false
        scoreCallback(0)
        if (wasGameOver) {
            post(gameLoop)
        }
        invalidate()
    }
}

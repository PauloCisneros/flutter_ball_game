package com.epn.gravitygame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class GameView(context: Context) : View(context) {

    private enum class GameState {
        MENU,
        PLAYING,
        GAME_OVER
    }

    enum class ShapeType {
        RECT,
        TRIANGLE
    }

    private var state = GameState.MENU

    private val ball = Ball()
    private val target = Target()
    private val obstacles = mutableListOf<Obstacle>()

    private val blinkingObstacles = mutableListOf<BlinkingObstacle>()

    private var sensorX = 0f
    private var sensorY = 0f
    private var score = 0
    private var lives = 3
    private var started = false
    private var gameOver = false
    private var scoreFlash = 0

    private val effects = mutableListOf<ImpactEffect>()

    private val safeZoneRadius = 200f

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        isFakeBoldText = true
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
    }

    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
    }

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 15, 23, 42) // oscuro translúcido
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 232, 240)
        strokeWidth = 3f
    }

    fun updateSensorValues(x: Float, y: Float) {
        if (state != GameState.PLAYING) return
        sensorX = x
        sensorY = y
        updateGame()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetGame()
    }

    private fun resetGame() {
        score = 0
        lives = 3
        gameOver = false
        started = false

        ball.position.set(width / 2f, height / 2f)
        target.relocate(width, height)

        createObstacles()

        state = GameState.MENU
        invalidate()
    }

    private fun isInSafeZone(rect: RectF): Boolean {
        val centerX = width / 2f
        val centerY = height / 2f

        val obsCenterX = rect.centerX()
        val obsCenterY = rect.centerY()

        val dx = obsCenterX - centerX
        val dy = obsCenterY - centerY

        return (dx * dx + dy * dy) < (safeZoneRadius * safeZoneRadius)
    }

    private fun createObstacles() {
        obstacles.clear()
        blinkingObstacles.clear()

        if (width == 0 || height == 0) return

        val o1 = Obstacle(RectF(width * 0.18f, height * 0.34f, width * 0.48f, height * 0.39f))
        val o2 = Obstacle(RectF(width * 0.55f, height * 0.55f, width * 0.86f, height * 0.60f))
        val o3 = Obstacle(RectF(width * 0.25f, height * 0.73f, width * 0.62f, height * 0.78f))

        val blink = RectF(
            width * 0.40f,
            height * 0.45f,
            width * 0.70f,
            height * 0.50f
        )

        // normales
        listOf(o1, o2, o3).forEach {
            if (!isInSafeZone(it.rect)) {
                obstacles.add(it)
            }
        }

        // blinking
        if (!isInSafeZone(blink)) {
            blinkingObstacles.add(BlinkingObstacle(blink))
        }
    }

    private fun updateGame() {
        ball.update(sensorX, sensorY, width, height)

        if (Collision.circleWithCircle(ball.position, ball.radius(), target.position, target.radius())) {
            score += 10
            scoreFlash = 20
            target.relocate(width, height)
            vibrate(35)
        }

        obstacles.forEach { obstacle ->
            if (Collision.circleWithRect(ball.position, ball.radius(), obstacle.rect)) {

                // Efecto de impacto
                effects.add(
                    ImpactEffect(ball.position.x, ball.position.y)
                )

                lives--
                ball.position.set(width / 2f, height / 2f)
                vibrate(120)

                if (lives <= 0) {
                    state = GameState.GAME_OVER
                }
                return@forEach
            }
        }

        blinkingObstacles.forEach { obstacle ->
            obstacle.update()

            if (obstacle.isVisible() &&
                Collision.circleWithRect(ball.position, ball.radius(), obstacle.rect)
            ) {
                lives--
                ball.position.set(width / 2f, height / 2f)
                vibrate(120)

                if (lives <= 0) {
                    gameOver = true
                }
            }
        }
        effects.forEach { it.update() }
        effects.removeAll { !it.alive }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        drawHeader(canvas)

        when (state) {

            GameState.MENU -> {
                drawMenu(canvas)
            }

            GameState.PLAYING -> {
                target.draw(canvas)
                obstacles.forEach { it.draw(canvas) }
                blinkingObstacles.forEach { it.draw(canvas) }
                effects.forEach { it.draw(canvas) }
                ball.draw(canvas)
            }

            GameState.GAME_OVER -> {
                drawGameOver(canvas)
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(Color.rgb(248, 250, 252))
        val step = 80
        var x = 0
        while (x < width) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), linePaint)
            x += step
        }
        var y = 0
        while (y < height) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
            y += step
        }
    }

    private fun drawHeader(canvas: Canvas) {

        canvas.drawRoundRect(
            24f, 24f,
            width - 24f, 160f,
            28f, 28f,
            panelPaint
        )

        canvas.drawText(
            "Gravity Ball",
            48f,
            75f,
            titlePaint
        )

        // ⭐ efecto score flash AQUÍ (antes de dibujar)
        if (scoreFlash > 0) {
            textPaint.color = Color.YELLOW
            scoreFlash--
        } else {
            textPaint.color = Color.WHITE
        }

        canvas.drawText(
            "⭐ $score",
            48f,
            125f,
            textPaint
        )

        canvas.drawText(
            "❤️ $lives",
            width - 160f,
            125f,
            textPaint
        )

        canvas.drawText(
            "X:${sensorX.roundToInt()}  Y:${sensorY.roundToInt()}",
            width - 280f,
            155f,
            smallPaint
        )
    }

    private fun drawStartOverlay(canvas: Canvas) {
        val box = RectF(60f, height * 0.28f, width - 60f, height * 0.62f)
        canvas.drawRoundRect(box, 36f, 36f, panelPaint)

        canvas.drawText("Toca para iniciar", box.left + 50f, box.top + 90f, titlePaint)

        canvas.drawText(
            "Incline el dispositivo en la dirección",
            box.left + 50f,
            box.top + 150f,
            textPaint
        )

        canvas.drawText(
            "en la que desea mover la bola.",
            box.left + 50f,
            box.top + 190f,
            textPaint
        )

        canvas.drawText(
            "Recoja los objetivos verdes y evite",
            box.left + 50f,
            box.top + 240f,
            textPaint
        )

        canvas.drawText(
            "los obstáculos rojos.",
            box.left + 50f,
            box.top + 280f,
            textPaint
        )
    }

    private fun drawGameOver(canvas: Canvas) {
        val box = RectF(60f, height * 0.30f, width - 60f, height * 0.58f)
        canvas.drawRoundRect(box, 36f, 36f, panelPaint)
        canvas.drawText("Juego terminado", box.left + 50f, box.top + 90f, titlePaint)
        canvas.drawText("Puntaje final: $score", box.left + 50f, box.top + 150f, textPaint)
        canvas.drawText("Toca para reiniciar", box.left + 50f, box.top + 205f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            when (state) {

                GameState.MENU -> {
                    state = GameState.PLAYING
                    started = true
                }

                GameState.GAME_OVER -> {
                    resetGame()
                    state = GameState.PLAYING
                    started = true
                }

                GameState.PLAYING -> {
                    // nada
                }
            }

            invalidate()
            return true
        }
        return true
    }

    private fun vibrate(milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    private fun drawMenu(canvas: Canvas) {
        val box = RectF(60f, height * 0.25f, width - 60f, height * 0.65f)
        canvas.drawRoundRect(box, 36f, 36f, panelPaint)

        canvas.drawText(
            "Gravity Ball",
            box.left + 50f,
            box.top + 100f,
            titlePaint
        )

        canvas.drawText(
            "Inclina el dispositivo para mover la bola",
            box.left + 50f,
            box.top + 180f,
            textPaint
        )

        canvas.drawText(
            "Evita obstáculos y recoge objetivos",
            box.left + 50f,
            box.top + 230f,
            textPaint
        )

        canvas.drawText(
            "Toca para comenzar",
            box.left + 50f,
            box.top + 320f,
            titlePaint
        )
    }
}

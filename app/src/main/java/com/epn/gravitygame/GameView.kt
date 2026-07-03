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

    private val ball = Ball()
    private val target = Target()
    private val obstacles = mutableListOf<Obstacle>()

    private var sensorX = 0f
    private var sensorY = 0f
    private var score = 0
    private var lives = 3
    private var started = false
    private var gameOver = false

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(15, 23, 42)
        textSize = 48f
        isFakeBoldText = true
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(51, 65, 85)
        textSize = 32f
    }

    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 116, 139)
        textSize = 24f
    }

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 255, 255, 255)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 232, 240)
        strokeWidth = 3f
    }

    fun updateSensorValues(x: Float, y: Float) {
        if (!started || gameOver) return
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
        invalidate()
    }

    private fun createObstacles() {
        obstacles.clear()
        if (width == 0 || height == 0) return
        obstacles.add(Obstacle(RectF(width * 0.18f, height * 0.34f, width * 0.48f, height * 0.39f)))
        obstacles.add(Obstacle(RectF(width * 0.55f, height * 0.55f, width * 0.86f, height * 0.60f)))
        obstacles.add(Obstacle(RectF(width * 0.25f, height * 0.73f, width * 0.62f, height * 0.78f)))
    }

    private fun updateGame() {
        ball.update(sensorX, sensorY, width, height)

        if (Collision.circleWithCircle(ball.position, ball.radius(), target.position, target.radius())) {
            score += 10
            target.relocate(width, height)
            vibrate(35)
        }

        obstacles.forEach { obstacle ->
            if (Collision.circleWithRect(ball.position, ball.radius(), obstacle.rect)) {
                lives--
                ball.position.set(width / 2f, height / 2f)
                vibrate(120)
                if (lives <= 0) {
                    gameOver = true
                }
                return@forEach
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawHeader(canvas)
        target.draw(canvas)
        obstacles.forEach { it.draw(canvas) }
        ball.draw(canvas)

        if (!started) drawStartOverlay(canvas)
        if (gameOver) drawGameOver(canvas)
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
        canvas.drawRoundRect(24f, 24f, width - 24f, 138f, 28f, 28f, panelPaint)
        canvas.drawText("Gravity Ball Kotlin", 48f, 72f, titlePaint)
        canvas.drawText("Puntaje: $score    Vidas: $lives", 48f, 116f, textPaint)
        canvas.drawText("X: ${sensorX.roundToInt()}  Y: ${sensorY.roundToInt()}", width - 230f, 116f, smallPaint)
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
            if (gameOver) {
                resetGame()
            } else {
                started = true
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
}

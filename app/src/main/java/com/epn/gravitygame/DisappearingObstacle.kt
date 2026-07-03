package com.epn.gravitygame

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color

class BlinkingObstacle(
    val rect: RectF
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(239, 68, 68)
    }

    private var visible = true
    private var timer = 0

    fun update() {
        timer++

        if (timer >= 40) { // velocidad de cambio
            visible = !visible
            timer = 0
        }
    }

    fun draw(canvas: Canvas) {
        if (!visible) return

        val path = android.graphics.Path()

        val topX = rect.centerX()
        val topY = rect.top

        val leftX = rect.left
        val bottomY = rect.bottom
        val rightX = rect.right

        path.moveTo(topX, topY)
        path.lineTo(leftX, bottomY)
        path.lineTo(rightX, bottomY)
        path.close()

        canvas.drawPath(path, paint)
    }

    fun isVisible(): Boolean = visible
}
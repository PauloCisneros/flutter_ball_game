package com.epn.gravitygame

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class ImpactEffect(
    var x: Float,
    var y: Float,
    var radius: Float = 0f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.FILL
    }

    var alive = true

    fun update() {
        radius += 10f
        if (radius > 80f) {
            alive = false
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
    }
}
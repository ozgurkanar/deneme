package com.lumastudio.lumaleap

import android.graphics.RectF

/**
 * A simple "Murk Crawler" enemy that patrols back and forth between minX and maxX.
 */
class Enemy(
    var x: Float,
    var y: Float,
    val minX: Float,
    val maxX: Float,
    val speed: Float = 220f
) {
    companion object {
        const val WIDTH = 64f
        const val HEIGHT = 64f
    }

    var direction = 1f // 1 = right, -1 = left
    var alive = true

    fun bounds(): RectF = RectF(x, y, x + WIDTH, y + HEIGHT)

    fun update(dt: Float) {
        if (!alive) return
        x += speed * direction * dt
        if (x < minX) {
            x = minX
            direction = 1f
        } else if (x + WIDTH > maxX) {
            x = maxX - WIDTH
            direction = -1f
        }
    }
}

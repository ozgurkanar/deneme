package com.lumastudio.lumaleap

import android.graphics.RectF

/**
 * The player character: Luma, a brave firefly-spirit girl explorer.
 * All positions/sizes are in "world units" (independent of screen pixel density).
 */
class Player(startX: Float, startY: Float) {

    companion object {
        const val WIDTH = 70f
        const val HEIGHT = 100f
        const val MOVE_SPEED = 620f          // world units per second
        const val JUMP_VELOCITY = -1500f     // world units per second
        const val GRAVITY = 3400f            // world units per second^2
        const val MAX_FALL_SPEED = 2200f
        const val INVULNERABLE_DURATION = 1.2f // seconds after being hit
    }

    var x = startX
    var y = startY
    var velX = 0f
    var velY = 0f
    var onGround = false
    var facingRight = true
    var invulnerableTime = 0f

    val startPosX = startX
    val startPosY = startY

    fun bounds(): RectF = RectF(x, y, x + WIDTH, y + HEIGHT)

    fun moveLeft() {
        velX = -MOVE_SPEED
        facingRight = false
    }

    fun moveRight() {
        velX = MOVE_SPEED
        facingRight = true
    }

    fun stopHorizontal() {
        velX = 0f
    }

    fun jump() {
        if (onGround) {
            velY = JUMP_VELOCITY
            onGround = false
        }
    }

    fun bounceOffEnemy() {
        velY = JUMP_VELOCITY * 0.6f
    }

    fun respawn() {
        x = startPosX
        y = startPosY
        velX = 0f
        velY = 0f
        onGround = false
        invulnerableTime = INVULNERABLE_DURATION
    }

    fun isInvulnerable(): Boolean = invulnerableTime > 0f

    fun update(dt: Float) {
        // Gravity
        velY += GRAVITY * dt
        if (velY > MAX_FALL_SPEED) velY = MAX_FALL_SPEED

        x += velX * dt
        y += velY * dt

        if (invulnerableTime > 0f) {
            invulnerableTime -= dt
            if (invulnerableTime < 0f) invulnerableTime = 0f
        }
    }
}

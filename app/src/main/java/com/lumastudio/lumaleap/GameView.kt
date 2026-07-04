package com.lumastudio.lumaleap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Custom Canvas/SurfaceView based game engine. No external engine, no external
 * image assets - everything is drawn with primitive shapes.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private enum class State { MENU, PLAYING, LEVEL_TRANSITION, GAME_OVER, VICTORY }

    // ---- Reference world / scaling ----
    private val refHeight = Levels.WORLD_HEIGHT
    private var scale = 1f
    private var screenW = 0
    private var screenH = 0

    // ---- Thread control ----
    private var gameThread: Thread? = null
    @Volatile private var running = false
    private val holderLock = Object()

    // ---- State ----
    private var state = State.MENU
    private var currentLevelNumber = 1
    private var level: Level = Levels.forLevelNumber(1)
    private var player = Player(level.startX, level.startY)
    private var enemies = level.freshEnemies()
    private var cameraX = 0f
    private var score = 0
    private var lives = 3
    private var transitionTimer = 0f

    // ---- Input flags ----
    @Volatile private var leftPressed = false
    @Volatile private var rightPressed = false
    @Volatile private var jumpRequested = false
    private var leftPointerId = -1
    private var rightPointerId = -1
    private var jumpPointerId = -1

    // ---- Button hit boxes (screen pixels, set in onSizeChanged) ----
    private var btnLeft = RectF()
    private var btnRight = RectF()
    private var btnJump = RectF()
    private var btnStart = RectF()
    private var btnRestart = RectF()

    // ---- Paints (created once) ----
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        isFakeBoldText = true
    }
    private val bigTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 110f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    // ---------------- Lifecycle ----------------

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenW = width
        screenH = height
        scale = height / refHeight
        layoutButtons()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    fun resume() {
        if (running) return
        running = true
        gameThread = Thread(this, "LumaLeap-GameThread")
        gameThread?.start()
    }

    fun pause() {
        running = false
        try {
            gameThread?.join(500)
        } catch (e: InterruptedException) {
            // ignore
        }
        gameThread = null
    }

    private fun layoutButtons() {
        val margin = 40f
        val btnSize = screenH * 0.22f
        btnLeft = RectF(margin, screenH - btnSize - margin, margin + btnSize, screenH - margin)
        btnRight = RectF(
            margin * 2 + btnSize, screenH - btnSize - margin,
            margin * 2 + btnSize * 2, screenH - margin
        )
        btnJump = RectF(
            screenW - btnSize - margin, screenH - btnSize - margin,
            screenW - margin, screenH - margin
        )
        val bw = 420f
        val bh = 140f
        btnStart = RectF(screenW / 2f - bw / 2f, screenH / 2f + 40f, screenW / 2f + bw / 2f, screenH / 2f + 40f + bh)
        btnRestart = btnStart
    }

    // ---------------- Main loop ----------------

    override fun run() {
        var lastTime = System.nanoTime()
        val targetFrameTime = 1_000_000_000L / 60L

        while (running) {
            val now = System.nanoTime()
            var dt = (now - lastTime) / 1_000_000_000f
            lastTime = now
            if (dt > 0.05f) dt = 0.05f // clamp to avoid huge jumps (e.g. after backgrounding)

            update(dt)
            drawFrame()

            val frameTime = System.nanoTime() - now
            val sleepTime = (targetFrameTime - frameTime) / 1_000_000L
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime)
                } catch (e: InterruptedException) {
                    // ignore
                }
            }
        }
    }

    private fun drawFrame() {
        val surfaceHolder = holder
        if (!surfaceHolder.surface.isValid) return
        synchronized(holderLock) {
            val canvas = try {
                surfaceHolder.lockCanvas()
            } catch (e: Exception) {
                null
            } ?: return
            try {
                drawGame(canvas)
            } finally {
                try {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    // ---------------- Update ----------------

    private fun update(dt: Float) {
        when (state) {
            State.PLAYING -> updatePlaying(dt)
            State.LEVEL_TRANSITION -> {
                transitionTimer -= dt
                if (transitionTimer <= 0f) {
                    startLevel(currentLevelNumber)
                }
            }
            else -> { /* no physics in menu / game over / victory */ }
        }
    }

    private fun updatePlaying(dt: Float) {
        // Horizontal input
        if (leftPressed && !rightPressed) player.moveLeft()
        else if (rightPressed && !leftPressed) player.moveRight()
        else player.stopHorizontal()

        if (jumpRequested) {
            player.jump()
            jumpRequested = false
        }

        player.update(dt)

        // Platform collision (simple AABB, resolve vertical landing + side blocking)
        player.onGround = false
        for (p in level.platforms) {
            val pBounds = player.bounds()
            if (RectF.intersects(pBounds, p.rect)) {
                resolvePlatformCollision(pBounds, p.rect)
            }
        }

        // Clamp to world bounds horizontally
        if (player.x < 0f) player.x = 0f
        if (player.x + Player.WIDTH > level.worldWidth) player.x = level.worldWidth - Player.WIDTH

        // Fell into a pit
        if (player.y > level.pitY) {
            loseLife()
            return
        }

        // Enemies
        for (enemy in enemies) {
            if (!enemy.alive) continue
            enemy.update(dt)
            val eBounds = enemy.bounds()
            val playerBoundsNow = player.bounds()
            if (RectF.intersects(playerBoundsNow, eBounds)) {
                val playerBottomPrev = playerBoundsNow.bottom - player.velY * dt
                val stompedFromAbove = player.velY > 0f && playerBottomPrev <= eBounds.top + 24f
                if (stompedFromAbove) {
                    enemy.alive = false
                    player.bounceOffEnemy()
                    score += 50
                } else if (!player.isInvulnerable()) {
                    loseLife()
                    return
                }
            }
        }

        // Collectibles
        val pb = player.bounds()
        for (c in level.collectibles) {
            if (!c.collected && RectF.intersects(pb, c.bounds())) {
                c.collected = true
                score += 10
            }
        }

        // Goal reached
        if (RectF.intersects(pb, level.goal)) {
            onLevelComplete()
            return
        }

        // Camera follows the player, clamped to world bounds
        val halfScreenWorld = (screenW / scale) / 2f
        cameraX = player.x + Player.WIDTH / 2f - halfScreenWorld
        val maxCam = level.worldWidth - (screenW / scale)
        if (cameraX < 0f) cameraX = 0f
        if (cameraX > maxCam && maxCam > 0f) cameraX = maxCam
    }

    /** Resolves collision between the player and a single platform rectangle. */
    private fun resolvePlatformCollision(pBounds: RectF, platform: RectF) {
        val overlapLeft = pBounds.right - platform.left
        val overlapRight = platform.right - pBounds.left
        val overlapTop = pBounds.bottom - platform.top
        val overlapBottom = platform.bottom - pBounds.top

        val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)

        when (minOverlap) {
            overlapTop -> {
                // Landing on top of the platform
                if (player.velY >= 0f) {
                    player.y = platform.top - Player.HEIGHT
                    player.velY = 0f
                    player.onGround = true
                }
            }
            overlapBottom -> {
                // Hitting head on the underside
                if (player.velY < 0f) {
                    player.y = platform.bottom
                    player.velY = 0f
                }
            }
            overlapLeft -> {
                player.x = platform.left - Player.WIDTH
            }
            overlapRight -> {
                player.x = platform.right
            }
        }
    }

    private fun loseLife() {
        lives -= 1
        if (lives <= 0) {
            state = State.GAME_OVER
        } else {
            player.respawn()
        }
    }

    private fun onLevelComplete() {
        if (currentLevelNumber >= 3) {
            state = State.VICTORY
        } else {
            currentLevelNumber += 1
            state = State.LEVEL_TRANSITION
            transitionTimer = 1.2f
        }
    }

    private fun startLevel(levelNumber: Int) {
        level = Levels.forLevelNumber(levelNumber)
        player = Player(level.startX, level.startY)
        enemies = level.freshEnemies()
        cameraX = 0f
        state = State.PLAYING
    }

    private fun startNewGame() {
        currentLevelNumber = 1
        score = 0
        lives = 3
        startLevel(1)
    }

    // ---------------- Drawing ----------------

    private fun drawGame(canvas: Canvas) {
        drawBackground(canvas)

        when (state) {
            State.MENU -> drawMenu(canvas)
            State.PLAYING -> drawWorld(canvas)
            State.LEVEL_TRANSITION -> {
                drawWorld(canvas)
                drawCenteredBanner(canvas, "Level $currentLevelNumber")
            }
            State.GAME_OVER -> drawGameOver(canvas)
            State.VICTORY -> drawVictory(canvas)
        }
    }

    private fun palette(themeSeed: Int): IntArray {
        // [sky top, sky bottom, ground, platform, accent]
        return when (themeSeed) {
            1 -> intArrayOf(Color.rgb(50, 40, 110), Color.rgb(120, 90, 200), Color.rgb(60, 40, 90), Color.rgb(110, 80, 170), Color.rgb(255, 224, 102))
            2 -> intArrayOf(Color.rgb(20, 40, 70), Color.rgb(60, 110, 160), Color.rgb(35, 55, 75), Color.rgb(80, 140, 190), Color.rgb(150, 230, 255))
            else -> intArrayOf(Color.rgb(15, 15, 40), Color.rgb(60, 30, 90), Color.rgb(30, 20, 50), Color.rgb(90, 60, 130), Color.rgb(255, 180, 220))
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val colors = palette(level.themeSeed)
        paint.shader = null
        paint.color = colors[0]
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH * 0.6f, paint)
        paint.color = colors[1]
        canvas.drawRect(0f, screenH * 0.6f, screenW.toFloat(), screenH.toFloat(), paint)
    }

    private fun worldToScreenX(worldX: Float): Float = (worldX - cameraX) * scale
    private fun worldToScreenY(worldY: Float): Float = worldY * scale

    private fun drawWorld(canvas: Canvas) {
        val colors = palette(level.themeSeed)

        // Platforms
        paint.color = colors[3]
        for (p in level.platforms) {
            val left = worldToScreenX(p.rect.left)
            val right = worldToScreenX(p.rect.right)
            if (right < 0f || left > screenW) continue
            canvas.drawRect(left, worldToScreenY(p.rect.top), right, worldToScreenY(p.rect.bottom), paint)
        }

        // Collectibles (glowing orbs)
        paint.color = colors[4]
        for (c in level.collectibles) {
            if (c.collected) continue
            val cx = worldToScreenX(c.x + Collectible.SIZE / 2f)
            val cy = worldToScreenY(c.y + Collectible.SIZE / 2f)
            if (cx < -50 || cx > screenW + 50) continue
            canvas.drawCircle(cx, cy, (Collectible.SIZE / 2f) * scale, paint)
        }

        // Goal marker (a glowing beacon)
        val goalLeft = worldToScreenX(level.goal.left)
        val goalTop = worldToScreenY(level.goal.top)
        val goalBottom = worldToScreenY(level.goal.bottom)
        paint.color = Color.rgb(255, 240, 180)
        canvas.drawRect(goalLeft, goalTop, goalLeft + 10f * scale, goalBottom, paint)
        val flagPath = Path()
        flagPath.moveTo(goalLeft + 10f * scale, goalTop)
        flagPath.lineTo(goalLeft + 70f * scale, goalTop + 30f * scale)
        flagPath.lineTo(goalLeft + 10f * scale, goalTop + 60f * scale)
        flagPath.close()
        paint.color = Color.rgb(255, 120, 150)
        canvas.drawPath(flagPath, paint)

        // Enemies
        for (enemy in enemies) {
            if (!enemy.alive) continue
            drawEnemy(canvas, enemy)
        }

        // Player
        drawPlayer(canvas)

        drawHud(canvas)
        drawControls(canvas)
    }

    private fun drawPlayer(canvas: Canvas) {
        if (player.isInvulnerable() && (System.currentTimeMillis() / 100) % 2 == 0L) {
            return // simple blink effect while invulnerable
        }
        val left = worldToScreenX(player.x)
        val top = worldToScreenY(player.y)
        val w = Player.WIDTH * scale
        val h = Player.HEIGHT * scale

        // Body
        paint.color = Color.rgb(255, 140, 200)
        canvas.drawRoundRect(left, top + h * 0.25f, left + w, top + h, 16f * scale, 16f * scale, paint)

        // Head
        paint.color = Color.rgb(255, 224, 200)
        val headCx = left + w / 2f
        val headCy = top + h * 0.22f
        canvas.drawCircle(headCx, headCy, w * 0.42f, paint)

        // Hair
        paint.color = Color.rgb(120, 70, 200)
        canvas.drawArc(
            headCx - w * 0.42f, headCy - w * 0.5f, headCx + w * 0.42f, headCy + w * 0.3f,
            180f, 180f, true, paint
        )

        // Glow accessory (firefly companion light on her back)
        paint.color = Color.rgb(255, 255, 150)
        val glowX = if (player.facingRight) left - 6f * scale else left + w + 6f * scale
        canvas.drawCircle(glowX, top + h * 0.35f, 10f * scale, paint)
    }

    private fun drawEnemy(canvas: Canvas, enemy: Enemy) {
        val left = worldToScreenX(enemy.x)
        val top = worldToScreenY(enemy.y)
        val w = Enemy.WIDTH * scale
        val h = Enemy.HEIGHT * scale
        paint.color = Color.rgb(80, 30, 60)
        canvas.drawRoundRect(left, top, left + w, top + h, 12f * scale, 12f * scale, paint)
        paint.color = Color.rgb(255, 60, 60)
        val eyeY = top + h * 0.4f
        canvas.drawCircle(left + w * 0.3f, eyeY, 6f * scale, paint)
        canvas.drawCircle(left + w * 0.7f, eyeY, 6f * scale, paint)
    }

    private fun drawHud(canvas: Canvas) {
        val hudY = 70f
        // Lives (hearts)
        paint.color = Color.rgb(255, 90, 130)
        for (i in 0 until lives) {
            canvas.drawCircle(60f + i * 55f, hudY, 20f, paint)
        }
        // Score
        canvas.drawText("Score: $score", screenW / 2f - 100f, hudY + 15f, textPaint)
        // Level
        canvas.drawText("Level $currentLevelNumber/3", screenW - 320f, hudY + 15f, textPaint)
    }

    private fun drawControls(canvas: Canvas) {
        paint.alpha = 110
        paint.color = Color.WHITE
        canvas.drawOval(btnLeft, paint)
        canvas.drawOval(btnRight, paint)
        canvas.drawOval(btnJump, paint)
        paint.alpha = 255

        paint.color = Color.rgb(40, 30, 60)
        drawTriangle(canvas, btnLeft, pointingLeft = true)
        drawTriangle(canvas, btnRight, pointingLeft = false)

        paint.color = Color.rgb(40, 30, 60)
        canvas.drawText("JUMP", btnJump.centerX() - 60f, btnJump.centerY() + 15f, textPaint)
    }

    private fun drawTriangle(canvas: Canvas, rect: RectF, pointingLeft: Boolean) {
        val path = Path()
        val cx = rect.centerX()
        val cy = rect.centerY()
        val size = rect.width() * 0.18f
        if (pointingLeft) {
            path.moveTo(cx + size, cy - size)
            path.lineTo(cx - size, cy)
            path.lineTo(cx + size, cy + size)
        } else {
            path.moveTo(cx - size, cy - size)
            path.lineTo(cx + size, cy)
            path.lineTo(cx - size, cy + size)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawMenu(canvas: Canvas) {
        bigTextPaint.textSize = 130f
        canvas.drawText("Luma's Leap", screenW / 2f, screenH * 0.35f, bigTextPaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("A glowing adventure through 3 hand-crafted worlds", screenW / 2f, screenH * 0.35f + 70f, textPaint)

        paint.color = Color.rgb(255, 200, 90)
        canvas.drawRoundRect(btnStart, 30f, 30f, paint)
        bigTextPaint.textSize = 64f
        paint.color = Color.BLACK
        canvas.drawText("START", btnStart.centerX(), btnStart.centerY() + 20f, bigTextPaint.apply { color = Color.rgb(40, 30, 20) })
        bigTextPaint.color = Color.WHITE

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawGameOver(canvas: Canvas) {
        bigTextPaint.textSize = 120f
        canvas.drawText("Game Over", screenW / 2f, screenH * 0.35f, bigTextPaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Final Score: $score", screenW / 2f, screenH * 0.35f + 70f, textPaint)

        paint.color = Color.rgb(255, 200, 90)
        canvas.drawRoundRect(btnRestart, 30f, 30f, paint)
        bigTextPaint.textSize = 64f
        canvas.drawText("RESTART", btnRestart.centerX(), btnRestart.centerY() + 20f, bigTextPaint.apply { color = Color.rgb(40, 30, 20) })
        bigTextPaint.color = Color.WHITE

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawVictory(canvas: Canvas) {
        bigTextPaint.textSize = 120f
        canvas.drawText("You Win!", screenW / 2f, screenH * 0.35f, bigTextPaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Luma saved the Glowing Meadow! Score: $score", screenW / 2f, screenH * 0.35f + 70f, textPaint)

        paint.color = Color.rgb(255, 200, 90)
        canvas.drawRoundRect(btnRestart, 30f, 30f, paint)
        bigTextPaint.textSize = 64f
        canvas.drawText("PLAY AGAIN", btnRestart.centerX(), btnRestart.centerY() + 20f, bigTextPaint.apply { color = Color.rgb(40, 30, 20) })
        bigTextPaint.color = Color.WHITE

        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawCenteredBanner(canvas: Canvas, text: String) {
        paint.color = Color.argb(160, 0, 0, 0)
        canvas.drawRect(0f, screenH / 2f - 100f, screenW.toFloat(), screenH / 2f + 100f, paint)
        bigTextPaint.textSize = 100f
        canvas.drawText(text, screenW / 2f, screenH / 2f + 35f, bigTextPaint)
    }

    // ---------------- Input: touch ----------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val px = event.getX(idx)
                val py = event.getY(idx)
                handlePointerDown(pid, px, py)
            }
            MotionEvent.ACTION_MOVE -> {
                // No drag-based controls needed; buttons are checked only on down.
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                handlePointerUp(pid)
            }
            MotionEvent.ACTION_CANCEL -> {
                leftPressed = false
                rightPressed = false
                leftPointerId = -1
                rightPointerId = -1
                jumpPointerId = -1
            }
        }
        return true
    }

    private fun handlePointerDown(pid: Int, px: Float, py: Float) {
        when (state) {
            State.MENU -> {
                if (btnStart.contains(px, py)) startNewGame()
            }
            State.GAME_OVER, State.VICTORY -> {
                if (btnRestart.contains(px, py)) {
                    state = State.MENU
                }
            }
            State.PLAYING -> {
                if (btnLeft.contains(px, py)) {
                    leftPressed = true
                    leftPointerId = pid
                } else if (btnRight.contains(px, py)) {
                    rightPressed = true
                    rightPointerId = pid
                } else if (btnJump.contains(px, py)) {
                    jumpRequested = true
                    jumpPointerId = pid
                }
            }
            else -> {}
        }
    }

    private fun handlePointerUp(pid: Int) {
        if (pid == leftPointerId) {
            leftPressed = false
            leftPointerId = -1
        }
        if (pid == rightPointerId) {
            rightPressed = false
            rightPointerId = -1
        }
        if (pid == jumpPointerId) {
            jumpPointerId = -1
        }
    }

    // ---------------- Input: keyboard (for testing with a hardware keyboard) ----------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> leftPressed = true
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> rightPressed = true
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                if (state == State.PLAYING) jumpRequested = true
            }
            KeyEvent.KEYCODE_ENTER -> {
                when (state) {
                    State.MENU -> startNewGame()
                    State.GAME_OVER, State.VICTORY -> state = State.MENU
                    else -> {}
                }
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> leftPressed = false
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> rightPressed = false
            else -> return super.onKeyUp(keyCode, event)
        }
        return true
    }
}

package com.ozgur.burakslongroad;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {
    private static final float WORLD_H = 540f;
    private static final int MAX_LEVELS = 5;
    private static final String PREFS = "buraks_long_road_save";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random(9);
    private final SoundEngine soundEngine = new SoundEngine();
    private final SharedPreferences prefs;

    private Screen screen = Screen.MENU;
    private Level level;
    private Player player;
    private int levelIndex = 0;
    private int unlockedLevel = 0;
    private int totalScore = 0;
    private float cameraX = 0;
    private long lastFrameNanos = 0;
    private float storyFade = 0f;
    private float runTime = 0f;
    private float transitionTimer = 0f;
    private boolean audioEnabled = true;

    private boolean leftDown;
    private boolean rightDown;
    private boolean jumpDown;
    private boolean actionDown;
    private boolean keyLeft;
    private boolean keyRight;
    private boolean keyJump;
    private boolean keyAction;
    private boolean jumpLatch;

    private RectF btnLeft = new RectF();
    private RectF btnRight = new RectF();
    private RectF btnJump = new RectF();
    private RectF btnAction = new RectF();
    private RectF btnPause = new RectF();
    private RectF menuStart = new RectF();
    private RectF menuContinue = new RectF();
    private RectF menuAudio = new RectF();
    private RectF menuCredits = new RectF();
    private RectF wideButton = new RectF();

    private List<StoryCard> activeStory = new ArrayList<>();
    private int storyIndex = 0;
    private StoryExit storyExit = StoryExit.START_LEVEL;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        unlockedLevel = prefs.getInt("unlocked", 0);
        audioEnabled = prefs.getBoolean("audio", true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setSubpixelText(true);
        soundEngine.setEnabled(audioEnabled);
        showMenu();
    }

    public void resumeGame() {
        requestFocus();
        if (audioEnabled && screen == Screen.PLAYING) soundEngine.startMusic(levelIndex);
        invalidate();
    }

    public void pauseGameAudioOnly() {
        soundEngine.stopMusic();
    }

    public boolean handleBackPressed() {
        if (screen == Screen.PLAYING) {
            screen = Screen.PAUSED;
            soundEngine.stopMusic();
            invalidate();
            return true;
        }
        if (screen == Screen.STORY || screen == Screen.CREDITS || screen == Screen.PAUSED || screen == Screen.GAME_OVER || screen == Screen.VICTORY) {
            showMenu();
            return true;
        }
        return false;
    }

    private void showMenu() {
        screen = Screen.MENU;
        soundEngine.startMenuMusic();
        invalidate();
    }

    private void newGame() {
        totalScore = 0;
        levelIndex = 0;
        showStory(storyFor(0, true), StoryExit.START_LEVEL);
    }

    private void continueGame() {
        totalScore = 0;
        levelIndex = Math.max(0, Math.min(unlockedLevel, MAX_LEVELS - 1));
        showStory(storyFor(levelIndex, true), StoryExit.START_LEVEL);
    }

    private void showStory(List<StoryCard> cards, StoryExit exit) {
        activeStory.clear();
        activeStory.addAll(cards);
        storyExit = exit;
        storyIndex = 0;
        storyFade = 0f;
        screen = Screen.STORY;
        soundEngine.startStoryMusic();
        invalidate();
    }

    private void startCurrentLevel() {
        level = LevelFactory.create(levelIndex);
        player = new Player(level.startX, level.startY);
        player.lives = 3;
        cameraX = 0;
        jumpLatch = false;
        screen = Screen.PLAYING;
        soundEngine.startMusic(levelIndex);
        invalidate();
    }

    private void nextLevelOrVictory() {
        if (levelIndex >= MAX_LEVELS - 1) {
            int finalScore = totalScore + level.scoreEarned;
            prefs.edit().putInt("unlocked", MAX_LEVELS - 1).putInt("bestScore", Math.max(finalScore, prefs.getInt("bestScore", 0))).apply();
            totalScore = finalScore;
            showStory(endingStory(), StoryExit.VICTORY);
        } else {
            totalScore += level.scoreEarned;
            levelIndex++;
            if (levelIndex > unlockedLevel) {
                unlockedLevel = levelIndex;
                prefs.edit().putInt("unlocked", unlockedLevel).apply();
            }
            showStory(storyFor(levelIndex, false), StoryExit.START_LEVEL);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        if (lastFrameNanos == 0) lastFrameNanos = now;
        float dt = Math.min(0.033f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        runTime += dt;

        computeButtons();

        if (screen == Screen.PLAYING) updateGame(dt);
        if (screen == Screen.STORY) storyFade = Math.min(1f, storyFade + dt * 2.2f);
        if (screen == Screen.GAME_OVER || screen == Screen.VICTORY) transitionTimer += dt;

        switch (screen) {
            case MENU:
                drawMenu(canvas);
                break;
            case STORY:
                drawStory(canvas);
                break;
            case PLAYING:
                drawGame(canvas);
                drawHud(canvas);
                drawControls(canvas);
                break;
            case PAUSED:
                drawGame(canvas);
                drawHud(canvas);
                drawPause(canvas);
                break;
            case GAME_OVER:
                drawGame(canvas);
                drawEndPanel(canvas, false);
                break;
            case VICTORY:
                drawVictory(canvas);
                break;
            case CREDITS:
                drawCredits(canvas);
                break;
        }
        postInvalidateOnAnimation();
    }

    private float scale() {
        return getHeight() <= 0 ? 1f : getHeight() / WORLD_H;
    }

    private float viewWorldW() {
        return getWidth() / scale();
    }

    private float sx(float worldX) {
        return (worldX - cameraX) * scale();
    }

    private float sy(float worldY) {
        return worldY * scale();
    }

    private float sw(float worldW) {
        return worldW * scale();
    }

    private void computeButtons() {
        float w = getWidth();
        float h = getHeight();
        float pad = Math.max(14f, h * 0.025f);
        float s = Math.max(62f, h * 0.15f);
        btnLeft.set(pad, h - s - pad, pad + s, h - pad);
        btnRight.set(pad + s + pad * 0.7f, h - s - pad, pad + s * 2f + pad * 0.7f, h - pad);
        btnJump.set(w - s - pad, h - s - pad, w - pad, h - pad);
        btnAction.set(w - s * 2.15f - pad, h - s - pad, w - s * 1.15f - pad, h - pad);
        float ps = Math.max(42f, h * 0.075f);
        btnPause.set(w - ps - pad, pad, w - pad, pad + ps);
        menuStart.set(w * 0.5f - 170f, h * 0.55f, w * 0.5f + 170f, h * 0.55f + 58f);
        menuContinue.set(w * 0.5f - 170f, h * 0.55f + 72f, w * 0.5f + 170f, h * 0.55f + 130f);
        menuAudio.set(w * 0.5f - 170f, h * 0.55f + 144f, w * 0.5f + 170f, h * 0.55f + 202f);
        menuCredits.set(w * 0.5f - 170f, h * 0.55f + 216f, w * 0.5f + 170f, h * 0.55f + 274f);
        wideButton.set(w * 0.5f - 190f, h * 0.70f, w * 0.5f + 190f, h * 0.70f + 62f);
    }

    private void updateGame(float dt) {
        if (level == null || player == null) return;
        level.time += dt;
        for (MovingPlatform mp : level.movingPlatforms) mp.update(dt);
        for (Enemy e : level.enemies) e.update(dt, level);
        for (Particle prt : level.particles) prt.update(dt);
        for (int i = level.particles.size() - 1; i >= 0; i--) if (level.particles.get(i).life <= 0) level.particles.remove(i);

        boolean l = leftDown || keyLeft;
        boolean r = rightDown || keyRight;
        boolean j = jumpDown || keyJump;
        boolean a = actionDown || keyAction;

        float targetVx = 0;
        if (l) targetVx -= player.speed;
        if (r) targetVx += player.speed;
        player.vx += (targetVx - player.vx) * Math.min(1f, dt * 12f);
        if (Math.abs(targetVx) > 0.1f) player.facing = targetVx > 0 ? 1 : -1;

        if (j && !jumpLatch && player.onGround) {
            player.vy = -player.jumpPower;
            player.onGround = false;
            soundEngine.jump();
            spawnDust(player.x + player.w / 2, player.y + player.h, 8, 0x99FFFFFF);
        }
        jumpLatch = j;

        if (a) tryInteract();

        player.vy += level.gravity * dt;
        player.vy = Math.min(player.vy, 720f);
        player.invuln = Math.max(0, player.invuln - dt);
        player.anim += dt;

        movePlayerX(dt);
        movePlayerY(dt);

        handleCollectibles();
        handleEnemies();
        handleHazards();
        handleCheckpoints();
        handleSwitches();
        handleDoor();

        if (player.y > WORLD_H + 180) hurtPlayer(true);

        float targetCam = player.x - viewWorldW() * 0.38f;
        targetCam = Math.max(0, Math.min(targetCam, level.width - viewWorldW()));
        cameraX += (targetCam - cameraX) * Math.min(1f, dt * 5f);
    }

    private void tryInteract() {
        for (Switch swt : level.switches) {
            if (!swt.on && distance(player.centerX(), player.centerY(), swt.x, swt.y) < 90f) {
                swt.on = true;
                for (Bridge b : level.bridges) b.active = true;
                soundEngine.checkpoint();
                level.message = "Burak found the courage to cross.";
                level.messageTimer = 2.3f;
                spawnDust(swt.x, swt.y, 22, 0xAAFFE09A);
            }
        }
    }

    private void movePlayerX(float dt) {
        player.x += player.vx * dt;
        for (Solid s : level.allSolids()) {
            if (s.active && RectF.intersects(player.bounds(), s.rect())) {
                if (player.vx > 0) player.x = s.x - player.w;
                else if (player.vx < 0) player.x = s.x + s.w;
                player.vx = 0;
            }
        }
        player.x = Math.max(0, Math.min(player.x, level.width - player.w));
    }

    private void movePlayerY(float dt) {
        player.y += player.vy * dt;
        player.onGround = false;
        RectF pb = player.bounds();
        for (Solid s : level.allSolids()) {
            if (!s.active) continue;
            RectF sr = s.rect();
            if (RectF.intersects(pb, sr)) {
                if (player.vy > 0) {
                    player.y = s.y - player.h;
                    player.vy = 0;
                    player.onGround = true;
                    if (s instanceof MovingPlatform) player.x += ((MovingPlatform) s).dxLast;
                } else if (player.vy < 0) {
                    player.y = s.y + s.h;
                    player.vy = 0;
                }
                pb = player.bounds();
            }
        }
    }

    private void handleCollectibles() {
        RectF pb = player.bounds();
        for (Collectible c : level.collectibles) {
            if (!c.taken && RectF.intersects(pb, c.rect())) {
                c.taken = true;
                level.scoreEarned += 10;
                soundEngine.collect();
                spawnDust(c.x, c.y, 12, 0xCCFFD66B);
                level.message = c.message;
                level.messageTimer = 1.8f;
            }
        }
    }

    private void handleEnemies() {
        RectF pb = player.bounds();
        for (Enemy e : level.enemies) {
            if (e.defeated) continue;
            RectF er = e.rect();
            if (RectF.intersects(pb, er)) {
                boolean stomp = player.vy > 60 && player.y + player.h - e.y < 20f;
                if (stomp) {
                    e.defeated = true;
                    player.vy = -player.jumpPower * 0.62f;
                    level.scoreEarned += 25;
                    soundEngine.collect();
                    spawnDust(e.x + e.w / 2, e.y + e.h / 2, 18, 0xAA8DD7FF);
                } else {
                    hurtPlayer(false);
                }
            }
        }
    }

    private void handleHazards() {
        RectF pb = player.bounds();
        for (Hazard h : level.hazards) if (RectF.intersects(pb, h.rect())) hurtPlayer(false);
    }

    private void handleCheckpoints() {
        for (Checkpoint cp : level.checkpoints) {
            if (!cp.used && Math.abs(player.centerX() - cp.x) < 32f && Math.abs(player.centerY() - cp.y) < 90f) {
                cp.used = true;
                player.respawnX = cp.x;
                player.respawnY = cp.y - player.h;
                soundEngine.checkpoint();
                level.message = "Checkpoint: a paper plane points forward.";
                level.messageTimer = 2.0f;
                spawnDust(cp.x, cp.y, 18, 0xAAFFFFFF);
            }
        }
    }

    private void handleSwitches() {
        for (Switch swt : level.switches) {
            if (!swt.on && distance(player.centerX(), player.centerY(), swt.x, swt.y) < 46f) {
                level.message = "Tap the hand button to unfold the bridge.";
                level.messageTimer = 0.25f;
            }
        }
    }

    private void handleDoor() {
        if (RectF.intersects(player.bounds(), level.door.rect())) {
            soundEngine.victory();
            nextLevelOrVictory();
        }
    }

    private void hurtPlayer(boolean fell) {
        if (!fell && player.invuln > 0) return;
        player.lives--;
        soundEngine.hurt();
        spawnDust(player.centerX(), player.centerY(), 20, 0xCCFF7B7B);
        if (player.lives <= 0) {
            screen = Screen.GAME_OVER;
            transitionTimer = 0;
            soundEngine.stopMusic();
            return;
        }
        player.x = player.respawnX;
        player.y = player.respawnY;
        player.vx = 0;
        player.vy = 0;
        player.invuln = 1.2f;
        cameraX = Math.max(0, player.x - viewWorldW() * 0.30f);
        level.message = "Burak breathes in and tries again.";
        level.messageTimer = 2.0f;
    }

    private void spawnDust(float x, float y, int count, int color) {
        for (int i = 0; i < count; i++) {
            float ang = random.nextFloat() * 6.28318f;
            float sp = 35f + random.nextFloat() * 95f;
            level.particles.add(new Particle(x, y, (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp, 0.45f + random.nextFloat() * 0.55f, color));
        }
    }

    private void drawMenu(Canvas c) {
        drawFullBackground(c, 4, 0);
        drawSoftOverlay(c, 0x44000000);
        drawTitleLogo(c, getWidth() / 2f, getHeight() * 0.20f);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(19));
        textPaint.setColor(0xFFEAF5FF);
        drawMultiline(c, "A heartfelt 2D platform journey across four hours of distance.", getWidth() / 2f, getHeight() * 0.38f, getWidth() * 0.74f, sp(24));

        drawButton(c, menuStart, "New Journey", 0xFFEAA64B);
        drawButton(c, menuContinue, unlockedLevel > 0 ? "Continue: Chapter " + (unlockedLevel + 1) : "Continue: Not yet", unlockedLevel > 0 ? 0xFF6EB5FF : 0xFF55606E);
        drawButton(c, menuAudio, audioEnabled ? "Sound: On" : "Sound: Off", 0xFF7FD9A7);
        drawButton(c, menuCredits, "Credits", 0xFFB894FF);

        textPaint.setTextSize(sp(13));
        textPaint.setColor(0xBFFFFFFF);
        c.drawText("No Unity • No external assets • Programmatic art and generated music", getWidth() / 2f, getHeight() - sp(20), textPaint);
    }

    private void drawTitleLogo(Canvas c, float x, float y) {
        paint.setShader(new LinearGradient(0, y - 80, 0, y + 80, 0xFFFFD17A, 0xFFFF8C5D, Shader.TileMode.CLAMP));
        textPaint.setShader(paint.getShader());
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(42));
        c.drawText("BURAK'S", x, y, textPaint);
        textPaint.setTextSize(sp(48));
        c.drawText("LONG ROAD", x, y + sp(52), textPaint);
        textPaint.setShader(null);
        textPaint.setFakeBoldText(false);
        drawBurakPortrait(c, x - getWidth() * 0.27f, y + sp(28), 1.15f);
        drawPaperPlane(c, x + getWidth() * 0.26f, y + sp(8), 1.3f, 0xEEFFFFFF);
    }

    private void drawStory(Canvas c) {
        int chapter = Math.min(levelIndex, MAX_LEVELS - 1);
        drawFullBackground(c, chapter, runTime * 8f);
        drawSoftOverlay(c, 0x660A1024);
        StoryCard card = activeStory.isEmpty() ? new StoryCard("Burak's Long Road", "Tap to continue.", 0) : activeStory.get(storyIndex);

        float alpha = storyFade;
        int a = (int) (alpha * 255);
        float panelW = getWidth() * 0.72f;
        float panelH = getHeight() * 0.48f;
        rect.set(getWidth() * 0.14f, getHeight() * 0.26f, getWidth() * 0.14f + panelW, getHeight() * 0.26f + panelH);
        paint.setColor((a << 24) | 0x001D2741);
        c.drawRoundRect(rect, 28, 28, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor((a << 24) | 0x00FFE3A5);
        c.drawRoundRect(rect, 28, 28, paint);
        paint.setStyle(Paint.Style.FILL);

        if (card.illustration == 0) drawHomeIllustration(c, rect.left + panelW * 0.18f, rect.top + panelH * 0.50f, alpha);
        else if (card.illustration == 1) drawRoadIllustration(c, rect.left + panelW * 0.18f, rect.top + panelH * 0.50f, alpha);
        else if (card.illustration == 2) drawRainIllustration(c, rect.left + panelW * 0.18f, rect.top + panelH * 0.50f, alpha);
        else if (card.illustration == 3) drawCityIllustration(c, rect.left + panelW * 0.18f, rect.top + panelH * 0.50f, alpha);
        else drawReunionIllustration(c, rect.left + panelW * 0.18f, rect.top + panelH * 0.50f, alpha);

        textPaint.setAlpha(a);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);
        textPaint.setColor(0xFFFFE0A6);
        textPaint.setTextSize(sp(28));
        c.drawText(card.title, rect.left + panelW * 0.36f, rect.top + sp(58), textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(sp(18));
        drawMultilineLeft(c, card.body, rect.left + panelW * 0.36f, rect.top + sp(98), panelW * 0.55f, sp(25));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(15));
        textPaint.setColor(0xCCFFFFFF);
        c.drawText("Tap anywhere to continue", getWidth() / 2f, rect.bottom - sp(22), textPaint);
        textPaint.setAlpha(255);
    }

    private void drawGame(Canvas c) {
        if (level == null || player == null) return;
        drawFullBackground(c, levelIndex, cameraX * 0.06f);
        drawParallax(c);
        drawWorld(c);
        drawPlayer(c);
        drawForeground(c);
    }

    private void drawFullBackground(Canvas c, int idx, float offset) {
        int top;
        int bottom;
        switch (idx) {
            case 0:
                top = 0xFF6DBCEB;
                bottom = 0xFFFFD59A;
                break;
            case 1:
                top = 0xFF80C7F2;
                bottom = 0xFFD4F2B5;
                break;
            case 2:
                top = 0xFF566B94;
                bottom = 0xFF99A9BA;
                break;
            case 3:
                top = 0xFF1F2F5C;
                bottom = 0xFF4F7DBD;
                break;
            default:
                top = 0xFF231A47;
                bottom = 0xFFFFA86B;
                break;
        }
        paint.setShader(new LinearGradient(0, 0, 0, getHeight(), top, bottom, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);

        paint.setColor(idx == 3 || idx == 4 ? 0x55FFFFFF : 0x44FFF5CF);
        for (int i = 0; i < 24; i++) {
            float x = ((i * 173 + offset * (i % 4 + 1)) % (getWidth() + 180)) - 90;
            float y = (float) ((Math.sin(i * 2.1 + runTime * 0.2) + 1) * getHeight() * 0.18 + 25);
            c.drawCircle(x, y, 1.5f + (i % 3), paint);
        }
    }

    private void drawParallax(Canvas c) {
        float sc = scale();
        if (levelIndex == 0) {
            drawClouds(c, 0.18f, 0xAAFFFFFF);
            drawHills(c, 0.08f, 0x883F9E75, 385);
            drawHouses(c, 0.22f);
        } else if (levelIndex == 1) {
            drawClouds(c, 0.12f, 0xCCFFFFFF);
            drawHills(c, 0.10f, 0x994EAD64, 380);
            drawFence(c, 0.34f);
        } else if (levelIndex == 2) {
            drawRain(c);
            drawHills(c, 0.08f, 0x8842586E, 370);
            drawBridgeFar(c);
        } else if (levelIndex == 3) {
            drawCitySkyline(c, 0.13f);
            drawTrainLights(c);
        } else {
            drawCitySkyline(c, 0.09f);
            paint.setShader(new RadialGradient(getWidth() * 0.5f, getHeight() * 0.35f, getHeight() * 0.75f, 0x55FFE1A0, 0x00161A36, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
        }
        paint.setColor(0x33FFFFFF);
        c.drawRect(0, 0, getWidth(), 2 * sc, paint);
    }

    private void drawWorld(Canvas c) {
        for (Bridge b : level.bridges) drawPlatform(c, b, b.active ? 0xFFB77F4A : 0x555A4633, true);
        for (Platform p : level.platforms) drawPlatform(c, p, p.color, false);
        for (MovingPlatform mp : level.movingPlatforms) drawMovingPlatform(c, mp);
        for (Hazard h : level.hazards) drawHazard(c, h);
        for (Switch swt : level.switches) drawSwitch(c, swt);
        for (Checkpoint cp : level.checkpoints) drawCheckpoint(c, cp);
        for (Collectible col : level.collectibles) if (!col.taken) drawCollectible(c, col);
        for (Enemy e : level.enemies) if (!e.defeated) drawEnemy(c, e);
        drawDoor(c, level.door);
        for (Particle prt : level.particles) drawParticle(c, prt);
    }

    private void drawPlatform(Canvas c, Solid p, int color, boolean bridge) {
        float x = sx(p.x), y = sy(p.y), w = sw(p.w), h = sw(p.h);
        if (x + w < -40 || x > getWidth() + 40) return;
        rect.set(x, y, x + w, y + h);
        paint.setShader(new LinearGradient(0, y, 0, y + h, lighten(color, 1.10f), darken(color, 0.70f), Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, sw(6), sw(6), paint);
        paint.setShader(null);
        paint.setColor(0x33000000);
        c.drawRect(x, y + h * 0.70f, x + w, y + h, paint);
        if (!bridge) {
            paint.setColor(0x44FFFFFF);
            for (float tx = x + sw(14); tx < x + w; tx += sw(46)) c.drawCircle(tx, y + sw(8), sw(2.1f), paint);
        } else {
            paint.setColor(0x663B2A1D);
            for (float tx = x + sw(18); tx < x + w; tx += sw(32)) c.drawLine(tx, y, tx - sw(10), y + h, paint);
        }
    }

    private void drawMovingPlatform(Canvas c, MovingPlatform mp) {
        drawPlatform(c, mp, 0xFF5DA6BE, false);
        paint.setColor(0xAAFFFFFF);
        float x = sx(mp.x), y = sy(mp.y), w = sw(mp.w);
        c.drawCircle(x + w * 0.22f, y + sw(8), sw(3), paint);
        c.drawCircle(x + w * 0.78f, y + sw(8), sw(3), paint);
    }

    private void drawHazard(Canvas c, Hazard h) {
        float x = sx(h.x), y = sy(h.y), w = sw(h.w), hh = sw(h.h);
        if (x + w < -40 || x > getWidth() + 40) return;
        paint.setColor(h.kind == 0 ? 0xCC516B7D : 0xDD9E4466);
        Path path = new Path();
        for (int i = 0; i <= 8; i++) {
            float px = x + w * i / 8f;
            float py = y + (i % 2 == 0 ? hh : 0);
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.lineTo(x + w, y + hh);
        path.lineTo(x, y + hh);
        path.close();
        c.drawPath(path, paint);
    }

    private void drawSwitch(Canvas c, Switch s) {
        float x = sx(s.x), y = sy(s.y);
        paint.setColor(s.on ? 0xFFFFD36E : 0xFF5C6688);
        c.drawCircle(x, y, sw(18), paint);
        paint.setColor(0xFF1E2941);
        c.drawCircle(x, y, sw(9), paint);
        paint.setColor(s.on ? 0xFFFFFFFF : 0xFFBFD0FF);
        c.drawLine(x - sw(10), y - sw(10), x + sw(10), y + sw(10), paint);
        c.drawLine(x + sw(10), y - sw(10), x - sw(10), y + sw(10), paint);
    }

    private void drawCheckpoint(Canvas c, Checkpoint cp) {
        float x = sx(cp.x), y = sy(cp.y);
        paint.setStrokeWidth(sw(3));
        paint.setColor(cp.used ? 0xFFFFD36E : 0xFFEAF5FF);
        c.drawLine(x, y + sw(38), x, y - sw(35), paint);
        drawPaperPlane(c, x + sw(18), y - sw(25), scale() * 0.7f, cp.used ? 0xFFFFD36E : 0xFFFFFFFF);
        paint.setStrokeWidth(1f);
    }

    private void drawCollectible(Canvas c, Collectible col) {
        float bob = (float) Math.sin(runTime * 4 + col.x * 0.02f) * sw(4);
        float x = sx(col.x), y = sy(col.y) + bob;
        paint.setShader(new RadialGradient(x, y, sw(18), 0xFFFFFFFF, 0xFFFFBA40, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, sw(14), paint);
        paint.setShader(null);
        drawPaperPlane(c, x, y, scale() * 0.38f, 0xFFFFFFFF);
    }

    private void drawEnemy(Canvas c, Enemy e) {
        float x = sx(e.x), y = sy(e.y), w = sw(e.w), h = sw(e.h);
        paint.setShader(new LinearGradient(0, y, 0, y + h, 0xFF9AB3D6, 0xFF4D5E87, Shader.TileMode.CLAMP));
        rect.set(x, y + h * 0.18f, x + w, y + h);
        c.drawRoundRect(rect, w * 0.35f, w * 0.35f, paint);
        paint.setShader(null);
        paint.setColor(0xEEFFFFFF);
        c.drawCircle(x + w * 0.34f, y + h * 0.45f, sw(4), paint);
        c.drawCircle(x + w * 0.66f, y + h * 0.45f, sw(4), paint);
        paint.setColor(0xFF20314A);
        c.drawCircle(x + w * 0.35f, y + h * 0.45f, sw(1.8f), paint);
        c.drawCircle(x + w * 0.67f, y + h * 0.45f, sw(1.8f), paint);
        paint.setColor(0xAAFFFFFF);
        c.drawOval(x + w * 0.15f, y, x + w * 0.85f, y + h * 0.36f, paint);
    }

    private void drawDoor(Canvas c, Door d) {
        float x = sx(d.x), y = sy(d.y), w = sw(d.w), h = sw(d.h);
        paint.setShader(new LinearGradient(x, y, x, y + h, 0xFFFFD98B, 0xFFFF795D, Shader.TileMode.CLAMP));
        rect.set(x, y, x + w, y + h);
        c.drawRoundRect(rect, sw(12), sw(12), paint);
        paint.setShader(null);
        paint.setColor(0xAAFFFFFF);
        c.drawCircle(x + w * 0.74f, y + h * 0.52f, sw(4.5f), paint);
        drawPaperPlane(c, x + w * 0.50f, y + h * 0.28f, scale() * 0.55f, 0xEEFFFFFF);
    }

    private void drawParticle(Canvas c, Particle p) {
        int alpha = Math.max(0, Math.min(255, (int) (255 * p.life / p.maxLife)));
        paint.setColor((p.color & 0x00FFFFFF) | (alpha << 24));
        c.drawCircle(sx(p.x), sy(p.y), sw(2.5f + p.life * 2f), paint);
    }

    private void drawPlayer(Canvas c) {
        float x = sx(player.x), y = sy(player.y), w = sw(player.w), h = sw(player.h);
        if (player.invuln > 0 && ((int) (runTime * 14) % 2 == 0)) return;
        float step = (float) Math.sin(player.anim * 12) * (Math.abs(player.vx) > 5 && player.onGround ? sw(3) : 0);

        paint.setColor(0x44000000);
        c.drawOval(x + w * 0.05f, y + h + sw(2), x + w * 0.95f, y + h + sw(10), paint);

        // legs
        paint.setColor(0xFF263D68);
        rect.set(x + w * 0.25f, y + h * 0.66f, x + w * 0.43f, y + h * 0.98f + step);
        c.drawRoundRect(rect, sw(5), sw(5), paint);
        rect.set(x + w * 0.57f, y + h * 0.66f, x + w * 0.75f, y + h * 0.98f - step);
        c.drawRoundRect(rect, sw(5), sw(5), paint);

        // coat
        paint.setShader(new LinearGradient(x, y + h * 0.34f, x, y + h * 0.74f, 0xFFFFB44A, 0xFFE05C43, Shader.TileMode.CLAMP));
        rect.set(x + w * 0.18f, y + h * 0.34f, x + w * 0.82f, y + h * 0.75f);
        c.drawRoundRect(rect, sw(10), sw(10), paint);
        paint.setShader(null);

        // scarf
        paint.setColor(0xFF60B6FF);
        rect.set(x + w * 0.20f, y + h * 0.38f, x + w * 0.86f, y + h * 0.47f);
        c.drawRoundRect(rect, sw(6), sw(6), paint);
        rect.set(x + (player.facing > 0 ? w * 0.73f : w * 0.07f), y + h * 0.43f, x + (player.facing > 0 ? w * 1.05f : w * 0.35f), y + h * 0.55f);
        c.drawRoundRect(rect, sw(6), sw(6), paint);

        // face
        paint.setColor(0xFFFFD2A6);
        c.drawOval(x + w * 0.21f, y + h * 0.08f, x + w * 0.79f, y + h * 0.50f, paint);

        // red hair
        paint.setColor(0xFFD34424);
        Path hair = new Path();
        hair.moveTo(x + w * 0.18f, y + h * 0.21f);
        hair.cubicTo(x + w * 0.22f, y - h * 0.04f, x + w * 0.72f, y - h * 0.02f, x + w * 0.82f, y + h * 0.22f);
        hair.cubicTo(x + w * 0.70f, y + h * 0.08f, x + w * 0.51f, y + h * 0.16f, x + w * 0.38f, y + h * 0.09f);
        hair.cubicTo(x + w * 0.31f, y + h * 0.18f, x + w * 0.25f, y + h * 0.24f, x + w * 0.18f, y + h * 0.21f);
        c.drawPath(hair, paint);
        paint.setColor(0xFFB92F1B);
        c.drawCircle(x + w * 0.23f, y + h * 0.27f, sw(7), paint);
        c.drawCircle(x + w * 0.76f, y + h * 0.27f, sw(7), paint);

        // eyes and smile
        paint.setColor(0xFF192A45);
        float eyeOffset = player.facing > 0 ? sw(2) : -sw(2);
        c.drawCircle(x + w * 0.40f + eyeOffset, y + h * 0.30f, sw(2.7f), paint);
        c.drawCircle(x + w * 0.60f + eyeOffset, y + h * 0.30f, sw(2.7f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sw(1.8f));
        RectF smile = new RectF(x + w * 0.42f, y + h * 0.33f, x + w * 0.60f, y + h * 0.45f);
        c.drawArc(smile, 15, 150, false, paint);
        paint.setStyle(Paint.Style.FILL);

        // hands
        paint.setColor(0xFFFFD2A6);
        c.drawCircle(x + w * 0.12f, y + h * 0.55f - step * 0.6f, sw(5), paint);
        c.drawCircle(x + w * 0.88f, y + h * 0.55f + step * 0.6f, sw(5), paint);
    }

    private void drawForeground(Canvas c) {
        if (level.messageTimer > 0) {
            level.messageTimer -= 0.016f;
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(0xEEFFFFFF);
            textPaint.setTextSize(sp(15));
            float y = getHeight() * 0.18f;
            rect.set(getWidth() * 0.22f, y - sp(28), getWidth() * 0.78f, y + sp(12));
            paint.setColor(0x77131B31);
            c.drawRoundRect(rect, 16, 16, paint);
            c.drawText(level.message, getWidth() / 2f, y, textPaint);
        }
    }

    private void drawHud(Canvas c) {
        float pad = sp(12);
        rect.set(pad, pad, pad + sp(315), pad + sp(46));
        paint.setColor(0x66131B31);
        c.drawRoundRect(rect, sp(16), sp(16), paint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(16));
        textPaint.setColor(Color.WHITE);
        c.drawText("Chapter " + (levelIndex + 1) + ": " + level.name, pad + sp(14), pad + sp(22), textPaint);
        textPaint.setTextSize(sp(14));
        textPaint.setFakeBoldText(false);
        c.drawText("Score " + (totalScore + level.scoreEarned) + "   Lives " + player.lives, pad + sp(14), pad + sp(40), textPaint);
        drawButtonIcon(c, btnPause, "Ⅱ");
    }

    private void drawControls(Canvas c) {
        drawControlCircle(c, btnLeft, "◀", leftDown || keyLeft);
        drawControlCircle(c, btnRight, "▶", rightDown || keyRight);
        drawControlCircle(c, btnAction, "✦", actionDown || keyAction);
        drawControlCircle(c, btnJump, "↑", jumpDown || keyJump);
    }

    private void drawControlCircle(Canvas c, RectF r, String label, boolean active) {
        paint.setColor(active ? 0xAAFFE1A0 : 0x66131B31);
        c.drawOval(r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(0xAAFFFFFF);
        c.drawOval(r, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(r.height() * 0.42f);
        textPaint.setColor(Color.WHITE);
        c.drawText(label, r.centerX(), r.centerY() + r.height() * 0.14f, textPaint);
        textPaint.setFakeBoldText(false);
    }

    private void drawButtonIcon(Canvas c, RectF r, String label) {
        paint.setColor(0x66131B31);
        c.drawRoundRect(r, 12, 12, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(r.height() * 0.50f);
        textPaint.setColor(0xFFFFFFFF);
        c.drawText(label, r.centerX(), r.centerY() + r.height() * 0.18f, textPaint);
        textPaint.setFakeBoldText(false);
    }

    private void drawPause(Canvas c) {
        drawSoftOverlay(c, 0x99000000);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(38));
        textPaint.setColor(0xFFFFE0A6);
        c.drawText("Paused", getWidth() / 2f, getHeight() * 0.34f, textPaint);
        drawButton(c, wideButton, "Resume", 0xFFEAA64B);
        RectF b2 = new RectF(wideButton.left, wideButton.bottom + sp(18), wideButton.right, wideButton.bottom + sp(80));
        drawButton(c, b2, "Main Menu", 0xFF6EB5FF);
    }

    private void drawEndPanel(Canvas c, boolean victory) {
        drawSoftOverlay(c, 0xAA000000);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(34));
        textPaint.setColor(victory ? 0xFFFFE0A6 : 0xFFFF9E9E);
        c.drawText(victory ? "Journey Complete" : "Try Again", getWidth() / 2f, getHeight() * 0.31f, textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(17));
        textPaint.setColor(0xFFFFFFFF);
        drawMultiline(c, victory ? "Burak keeps the road in his heart, but this time he reaches the arms waiting at the end." : "The road feels long, but Burak has not lost hope.", getWidth() / 2f, getHeight() * 0.40f, getWidth() * 0.70f, sp(24));
        drawButton(c, wideButton, victory ? "Back to Menu" : "Restart Chapter", 0xFFEAA64B);
    }

    private void drawVictory(Canvas c) {
        drawFullBackground(c, 4, runTime * 10f);
        drawCitySkyline(c, 0.06f);
        drawSoftOverlay(c, 0x33101024);
        drawReunionIllustration(c, getWidth() * 0.5f, getHeight() * 0.42f, 1f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(34));
        textPaint.setColor(0xFFFFE0A6);
        c.drawText("They Finally Meet", getWidth() / 2f, getHeight() * 0.17f, textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(17));
        textPaint.setColor(0xFFFFFFFF);
        drawMultiline(c, "Four hours was never just distance. It was a road made of courage, memories, and love.", getWidth() / 2f, getHeight() * 0.72f, getWidth() * 0.72f, sp(24));
        drawButton(c, wideButton, "Main Menu", 0xFFEAA64B);
    }

    private void drawCredits(Canvas c) {
        drawFullBackground(c, 3, runTime * 8f);
        drawSoftOverlay(c, 0x77000000);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(34));
        textPaint.setColor(0xFFFFE0A6);
        c.drawText("Credits", getWidth() / 2f, getHeight() * 0.20f, textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(18));
        textPaint.setColor(Color.WHITE);
        drawMultiline(c, "Game, art, levels, UI, and music are generated in code.\nA small story about a child, a long road, and a love that stays kind from both sides.", getWidth() / 2f, getHeight() * 0.34f, getWidth() * 0.74f, sp(27));
        drawButton(c, wideButton, "Back", 0xFFEAA64B);
    }

    private void drawButton(Canvas c, RectF r, String text, int color) {
        paint.setShader(new LinearGradient(0, r.top, 0, r.bottom, lighten(color, 1.15f), darken(color, 0.78f), Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 22, 22, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(0x99FFFFFF);
        c.drawRoundRect(r, 22, 22, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(18));
        textPaint.setColor(Color.WHITE);
        c.drawText(text, r.centerX(), r.centerY() + sp(6), textPaint);
        textPaint.setFakeBoldText(false);
    }

    private void drawSoftOverlay(Canvas c, int color) {
        paint.setColor(color);
        c.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    private void drawClouds(Canvas c, float parallax, int color) {
        paint.setColor(color);
        for (int i = 0; i < 8; i++) {
            float x = ((i * 210 - cameraX * parallax) % (getWidth() + 260)) - 130;
            float y = 45 + (i % 4) * 28;
            c.drawOval(x, y, x + 110, y + 30, paint);
            c.drawOval(x + 24, y - 18, x + 78, y + 32, paint);
        }
    }

    private void drawHills(Canvas c, float parallax, int color, float baseY) {
        paint.setColor(color);
        Path path = new Path();
        float h = getHeight();
        float offset = -cameraX * parallax * scale();
        path.moveTo(-100 + offset % 260, sy(baseY));
        for (float x = -100 + offset % 260; x < getWidth() + 300; x += 260) {
            path.cubicTo(x + 80, sy(baseY - 100), x + 170, sy(baseY - 70), x + 260, sy(baseY));
        }
        path.lineTo(getWidth() + 300, h);
        path.lineTo(-200, h);
        path.close();
        c.drawPath(path, paint);
    }

    private void drawHouses(Canvas c, float parallax) {
        float off = -cameraX * parallax * scale();
        for (int i = 0; i < 12; i++) {
            float x = (i * 145 + off) % (getWidth() + 200) - 90;
            float y = sy(355 + (i % 2) * 12);
            paint.setColor(i % 2 == 0 ? 0x99FFE0B6 : 0x99C4E6FF);
            c.drawRect(x, y, x + 75, y + 58, paint);
            paint.setColor(0xAA9A5D48);
            Path roof = new Path();
            roof.moveTo(x - 8, y);
            roof.lineTo(x + 37, y - 32);
            roof.lineTo(x + 83, y);
            roof.close();
            c.drawPath(roof, paint);
            paint.setColor(0xAAFFFFFF);
            c.drawRect(x + 15, y + 18, x + 30, y + 34, paint);
            c.drawRect(x + 47, y + 18, x + 62, y + 34, paint);
        }
    }

    private void drawFence(Canvas c, float parallax) {
        float off = -cameraX * parallax * scale();
        paint.setColor(0x88FFFFFF);
        for (float x = off % 55 - 55; x < getWidth() + 55; x += 55) c.drawRect(x, sy(405), x + 8, sy(455), paint);
        c.drawRect(0, sy(425), getWidth(), sy(433), paint);
    }

    private void drawRain(Canvas c) {
        paint.setColor(0x66D6EAFF);
        paint.setStrokeWidth(2f);
        for (int i = 0; i < 60; i++) {
            float x = (i * 73 + runTime * 220) % (getWidth() + 80) - 40;
            float y = (i * 41 + runTime * 400) % (getHeight() + 80) - 40;
            c.drawLine(x, y, x - 12, y + 32, paint);
        }
        paint.setStrokeWidth(1f);
    }

    private void drawBridgeFar(Canvas c) {
        paint.setColor(0x66405263);
        for (float x = -cameraX * 0.18f * scale() % 140 - 100; x < getWidth() + 120; x += 140) {
            c.drawRect(x, sy(385), x + 80, sy(394), paint);
            c.drawLine(x, sy(385), x + 40, sy(335), paint);
            c.drawLine(x + 80, sy(385), x + 40, sy(335), paint);
        }
    }

    private void drawCitySkyline(Canvas c, float parallax) {
        float off = -cameraX * parallax * scale();
        for (int i = 0; i < 15; i++) {
            float x = (i * 90 + off) % (getWidth() + 180) - 90;
            float h = sy(80 + (i % 5) * 28);
            paint.setColor(i % 2 == 0 ? 0x88394768 : 0x88546289);
            c.drawRect(x, sy(390) - h, x + 70, sy(420), paint);
            paint.setColor(0x99FFD36E);
            for (int yy = 0; yy < 4; yy++) for (int xx = 0; xx < 2; xx++) if ((i + yy + xx) % 2 == 0) c.drawRect(x + 14 + xx * 24, sy(390) - h + 18 + yy * 28, x + 24 + xx * 24, sy(390) - h + 30 + yy * 28, paint);
        }
    }

    private void drawTrainLights(Canvas c) {
        paint.setColor(0xAAFFD36E);
        for (float x = (-cameraX * 0.5f + runTime * 180) % (getWidth() + 220) - 110; x < getWidth() + 200; x += 220) {
            c.drawCircle(x, sy(430), sw(5), paint);
            c.drawCircle(x + 32, sy(430), sw(5), paint);
        }
    }

    private void drawHomeIllustration(Canvas c, float x, float y, float alpha) {
        paint.setAlpha((int) (255 * alpha));
        paint.setColor(0xFFFFD7A0);
        c.drawRect(x - 72, y - 18, x + 70, y + 62, paint);
        paint.setColor(0xFFD36B5A);
        Path roof = new Path();
        roof.moveTo(x - 90, y - 18);
        roof.lineTo(x, y - 82);
        roof.lineTo(x + 90, y - 18);
        roof.close();
        c.drawPath(roof, paint);
        drawBurakPortrait(c, x - 38, y + 18, 0.80f);
        paint.setColor(0xFFFFCFA6);
        c.drawOval(x + 28, y - 4, x + 88, y + 68, paint);
        paint.setColor(0xFF5A453D);
        c.drawCircle(x + 58, y - 12, 24, paint);
        paint.setAlpha(255);
    }

    private void drawRoadIllustration(Canvas c, float x, float y, float alpha) {
        paint.setAlpha((int) (255 * alpha));
        paint.setColor(0xFF3C4D66);
        Path road = new Path();
        road.moveTo(x - 140, y + 70);
        road.lineTo(x - 30, y - 70);
        road.lineTo(x + 30, y - 70);
        road.lineTo(x + 140, y + 70);
        road.close();
        c.drawPath(road, paint);
        drawPaperPlane(c, x + 18, y - 48, 1.5f, 0xFFFFFFFF);
        drawBurakPortrait(c, x - 75, y + 20, 0.7f);
        paint.setAlpha(255);
    }

    private void drawRainIllustration(Canvas c, float x, float y, float alpha) {
        paint.setAlpha((int) (255 * alpha));
        paint.setColor(0xFF516B7D);
        c.drawRoundRect(x - 120, y - 50, x + 130, y + 58, 20, 20, paint);
        paint.setColor(0xFFBFD9FF);
        for (int i = 0; i < 10; i++) c.drawLine(x - 100 + i * 24, y - 36, x - 114 + i * 24, y + 8, paint);
        drawBurakPortrait(c, x - 16, y + 32, 0.75f);
        drawPaperPlane(c, x + 72, y - 4, 1.1f, 0xFFFFFFFF);
        paint.setAlpha(255);
    }

    private void drawCityIllustration(Canvas c, float x, float y, float alpha) {
        paint.setAlpha((int) (255 * alpha));
        paint.setColor(0xFF2C355A);
        for (int i = 0; i < 5; i++) c.drawRect(x - 115 + i * 50, y - 80 - i % 2 * 30, x - 80 + i * 50, y + 70, paint);
        paint.setColor(0xFFFFD36E);
        for (int i = 0; i < 8; i++) c.drawCircle(x - 100 + i * 32, y - 25 + (i % 2) * 25, 5, paint);
        drawBurakPortrait(c, x - 65, y + 36, 0.75f);
        paint.setAlpha(255);
    }

    private void drawReunionIllustration(Canvas c, float x, float y, float alpha) {
        paint.setAlpha((int) (255 * alpha));
        paint.setColor(0x55FFFFFF);
        c.drawCircle(x, y - 20, Math.min(getWidth(), getHeight()) * 0.20f, paint);
        drawBurakPortrait(c, x - 55, y + 20, 0.92f);
        // father
        paint.setColor(0xFFFFC89E);
        c.drawOval(x + 22, y - 80, x + 92, y - 10, paint);
        paint.setColor(0xFF604536);
        c.drawArc(new RectF(x + 20, y - 86, x + 94, y - 25), 185, 170, true, paint);
        paint.setColor(0xFF4671B8);
        c.drawRoundRect(x + 8, y - 8, x + 106, y + 112, 20, 20, paint);
        paint.setColor(0xFFFFC89E);
        c.drawCircle(x + 17, y + 36, 10, paint);
        c.drawCircle(x + 97, y + 36, 10, paint);
        paint.setColor(0xFFFFE0A6);
        drawPaperPlane(c, x + 6, y - 112, 1.1f, 0xFFFFFFFF);
        paint.setAlpha(255);
    }

    private void drawBurakPortrait(Canvas c, float x, float y, float scaleLocal) {
        float s = 48f * scaleLocal;
        paint.setColor(0xFFFFB44A);
        c.drawRoundRect(x - s * 0.42f, y - s * 0.02f, x + s * 0.42f, y + s * 0.84f, s * 0.16f, s * 0.16f, paint);
        paint.setColor(0xFFFFD2A6);
        c.drawOval(x - s * 0.38f, y - s * 0.72f, x + s * 0.38f, y + s * 0.05f, paint);
        paint.setColor(0xFFD34424);
        c.drawArc(new RectF(x - s * 0.44f, y - s * 0.82f, x + s * 0.44f, y - s * 0.22f), 185, 170, true, paint);
        c.drawCircle(x - s * 0.28f, y - s * 0.39f, s * 0.12f, paint);
        c.drawCircle(x + s * 0.28f, y - s * 0.39f, s * 0.12f, paint);
        paint.setColor(0xFF192A45);
        c.drawCircle(x - s * 0.13f, y - s * 0.36f, s * 0.035f, paint);
        c.drawCircle(x + s * 0.13f, y - s * 0.36f, s * 0.035f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.3f, s * 0.025f));
        c.drawArc(new RectF(x - s * 0.13f, y - s * 0.25f, x + s * 0.13f, y - s * 0.08f), 15, 150, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPaperPlane(Canvas c, float x, float y, float s, int color) {
        paint.setColor(color);
        Path p = new Path();
        p.moveTo(x - 22 * s, y - 9 * s);
        p.lineTo(x + 27 * s, y);
        p.lineTo(x - 22 * s, y + 9 * s);
        p.lineTo(x - 10 * s, y);
        p.close();
        c.drawPath(p, paint);
        paint.setColor(0x55263D68);
        c.drawLine(x - 10 * s, y, x + 27 * s, y, paint);
    }

    private void drawMultiline(Canvas c, String text, float cx, float y, float maxWidth, float lineHeight) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (String line : wrap(text, maxWidth, textPaint)) {
            c.drawText(line, cx, y, textPaint);
            y += lineHeight;
        }
    }

    private void drawMultilineLeft(Canvas c, String text, float x, float y, float maxWidth, float lineHeight) {
        textPaint.setTextAlign(Paint.Align.LEFT);
        for (String line : wrap(text, maxWidth, textPaint)) {
            c.drawText(line, x, y, textPaint);
            y += lineHeight;
        }
    }

    private List<String> wrap(String text, float maxWidth, Paint p) {
        List<String> lines = new ArrayList<>();
        String[] rawLines = text.split("\\n");
        for (String raw : rawLines) {
            String[] words = raw.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (p.measureText(test) > maxWidth && line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(test);
                }
            }
            if (line.length() > 0) lines.add(line.toString());
        }
        return lines;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (screen == Screen.MENU) {
                if (menuStart.contains(x, y)) newGame();
                else if (menuContinue.contains(x, y) && unlockedLevel > 0) continueGame();
                else if (menuAudio.contains(x, y)) {
                    audioEnabled = !audioEnabled;
                    prefs.edit().putBoolean("audio", audioEnabled).apply();
                    soundEngine.setEnabled(audioEnabled);
                    if (audioEnabled) soundEngine.startMenuMusic(); else soundEngine.stopMusic();
                    invalidate();
                } else if (menuCredits.contains(x, y)) {
                    screen = Screen.CREDITS;
                    invalidate();
                }
                return true;
            }
            if (screen == Screen.STORY) {
                storyIndex++;
                storyFade = 0f;
                if (storyIndex >= activeStory.size()) {
                    if (storyExit == StoryExit.START_LEVEL) startCurrentLevel();
                    else if (storyExit == StoryExit.VICTORY) {
                        screen = Screen.VICTORY;
                        soundEngine.startVictoryMusic();
                    } else showMenu();
                }
                return true;
            }
            if (screen == Screen.PAUSED) {
                RectF b2 = new RectF(wideButton.left, wideButton.bottom + sp(18), wideButton.right, wideButton.bottom + sp(80));
                if (wideButton.contains(x, y)) {
                    screen = Screen.PLAYING;
                    soundEngine.startMusic(levelIndex);
                } else if (b2.contains(x, y)) showMenu();
                return true;
            }
            if (screen == Screen.GAME_OVER) {
                if (wideButton.contains(x, y)) startCurrentLevel();
                return true;
            }
            if (screen == Screen.VICTORY || screen == Screen.CREDITS) {
                if (wideButton.contains(x, y)) showMenu();
                return true;
            }
        }

        if (screen == Screen.PLAYING) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                int idx = event.getActionIndex();
                float px = event.getX(idx), py = event.getY(idx);
                if (btnPause.contains(px, py)) {
                    screen = Screen.PAUSED;
                    soundEngine.stopMusic();
                    return true;
                }
            }
            leftDown = rightDown = jumpDown = actionDown = false;
            for (int i = 0; i < event.getPointerCount(); i++) {
                if (action == MotionEvent.ACTION_POINTER_UP && i == event.getActionIndex()) continue;
                float px = event.getX(i), py = event.getY(i);
                if (btnLeft.contains(px, py)) leftDown = true;
                if (btnRight.contains(px, py)) rightDown = true;
                if (btnJump.contains(px, py)) jumpDown = true;
                if (btnAction.contains(px, py)) actionDown = true;
            }
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                leftDown = rightDown = jumpDown = actionDown = false;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) keyLeft = true;
        if (keyCode == KeyEvent.KEYCODE_D || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) keyRight = true;
        if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_W || keyCode == KeyEvent.KEYCODE_DPAD_UP) keyJump = true;
        if (keyCode == KeyEvent.KEYCODE_E || keyCode == KeyEvent.KEYCODE_ENTER) keyAction = true;
        if (keyCode == KeyEvent.KEYCODE_P && screen == Screen.PLAYING) screen = Screen.PAUSED;
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) keyLeft = false;
        if (keyCode == KeyEvent.KEYCODE_D || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) keyRight = false;
        if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_W || keyCode == KeyEvent.KEYCODE_DPAD_UP) keyJump = false;
        if (keyCode == KeyEvent.KEYCODE_E || keyCode == KeyEvent.KEYCODE_ENTER) keyAction = false;
        return true;
    }

    private List<StoryCard> storyFor(int chapter, boolean opening) {
        List<StoryCard> cards = new ArrayList<>();
        if (chapter == 0) {
            cards.add(new StoryCard("The Window Seat", "Burak is nine. His hair is the color of autumn leaves, and every Friday evening he sits by the window with a paper plane in his hand.", 0));
            cards.add(new StoryCard("Four Hours Away", "His mother makes tea and smiles softly. His father calls from another city. They both love him, but the road between them is four hours long.", 0));
            cards.add(new StoryCard("A Little Plan", "When a storm delays the weekend visit, Burak decides to follow the paper planes he and his father once folded together. Each one points toward the city lights.", 1));
        } else if (chapter == 1) {
            cards.add(new StoryCard("Beyond the Neighborhood", "Burak leaves a note for his mother and steps beyond the familiar streets. The morning smells like bread, rain, and courage.", 1));
        } else if (chapter == 2) {
            cards.add(new StoryCard("Rain on the Long Bridge", "Clouds gather over the fields. Burak remembers his father saying, 'A brave heart is not a heart without fear. It is a heart that keeps walking.'", 2));
        } else if (chapter == 3) {
            cards.add(new StoryCard("The City Approach", "Night arrives. The other city glows ahead like a box of stars. Burak is tired, but every light feels like it might be his father's window.", 3));
        } else {
            cards.add(new StoryCard("The Last Station", "The road grows quiet. Burak sees the station clock and the final paper plane. His hands shake, but his smile returns.", 4));
        }
        return cards;
    }

    private List<StoryCard> endingStory() {
        List<StoryCard> cards = new ArrayList<>();
        cards.add(new StoryCard("The Open Door", "Burak knocks once. Then twice. The door opens before the third knock.", 4));
        cards.add(new StoryCard("No One Was Forgotten", "His father kneels and hugs him so tightly that the whole four-hour road seems to fold into one small room.", 4));
        cards.add(new StoryCard("A Road With Two Homes", "Burak learns that love can live in two cities, two homes, and still be whole. The distance is real, but so is the way back.", 4));
        return cards;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private int lighten(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }

    private int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.max(0, (int) (Color.red(color) * factor));
        int g = Math.max(0, (int) (Color.green(color) * factor));
        int b = Math.max(0, (int) (Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }

    private enum Screen { MENU, STORY, PLAYING, PAUSED, GAME_OVER, VICTORY, CREDITS }
    private enum StoryExit { START_LEVEL, VICTORY, MENU }

    private static final class StoryCard {
        final String title;
        final String body;
        final int illustration;
        StoryCard(String title, String body, int illustration) {
            this.title = title;
            this.body = body;
            this.illustration = illustration;
        }
    }

    private static class Player {
        float x, y, w = 34, h = 58, vx, vy;
        float speed = 215, jumpPower = 445;
        boolean onGround;
        int facing = 1;
        int lives = 3;
        float invuln = 0;
        float anim = 0;
        float respawnX, respawnY;
        Player(float x, float y) { this.x = x; this.y = y; respawnX = x; respawnY = y; }
        RectF bounds() { return new RectF(x + 5, y + 4, x + w - 5, y + h); }
        float centerX() { return x + w / 2f; }
        float centerY() { return y + h / 2f; }
    }

    private static class Solid {
        float x, y, w, h;
        int color = 0xFF6B8F5A;
        boolean active = true;
        Solid(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; }
        RectF rect() { return new RectF(x, y, x + w, y + h); }
    }

    private static final class Platform extends Solid {
        Platform(float x, float y, float w, float h, int color) { super(x, y, w, h); this.color = color; }
    }

    private static final class MovingPlatform extends Solid {
        float minX, maxX, speed, dir = 1, dxLast = 0;
        MovingPlatform(float x, float y, float w, float h, float minX, float maxX, float speed) {
            super(x, y, w, h); this.minX = minX; this.maxX = maxX; this.speed = speed;
        }
        void update(float dt) {
            float old = x;
            x += speed * dir * dt;
            if (x < minX) { x = minX; dir = 1; }
            if (x > maxX) { x = maxX; dir = -1; }
            dxLast = x - old;
        }
    }

    private static final class Bridge extends Solid {
        Bridge(float x, float y, float w, float h) { super(x, y, w, h); active = false; }
    }

    private static final class Collectible {
        float x, y;
        boolean taken;
        String message;
        Collectible(float x, float y, String message) { this.x = x; this.y = y; this.message = message; }
        RectF rect() { return new RectF(x - 16, y - 16, x + 16, y + 16); }
    }

    private static final class Enemy {
        float x, y, w = 42, h = 34, minX, maxX, speed, dir = 1;
        boolean defeated;
        Enemy(float x, float y, float minX, float maxX, float speed) { this.x = x; this.y = y; this.minX = minX; this.maxX = maxX; this.speed = speed; }
        RectF rect() { return new RectF(x, y, x + w, y + h); }
        void update(float dt, Level level) {
            x += speed * dir * dt;
            if (x < minX) { x = minX; dir = 1; }
            if (x > maxX) { x = maxX; dir = -1; }
        }
    }

    private static final class Hazard {
        float x, y, w, h; int kind;
        Hazard(float x, float y, float w, float h, int kind) { this.x = x; this.y = y; this.w = w; this.h = h; this.kind = kind; }
        RectF rect() { return new RectF(x, y, x + w, y + h); }
    }

    private static final class Checkpoint {
        float x, y; boolean used;
        Checkpoint(float x, float y) { this.x = x; this.y = y; }
    }

    private static final class Door {
        float x, y, w = 56, h = 82;
        Door(float x, float y) { this.x = x; this.y = y; }
        RectF rect() { return new RectF(x, y, x + w, y + h); }
    }

    private static final class Switch {
        float x, y; boolean on;
        Switch(float x, float y) { this.x = x; this.y = y; }
    }

    private static final class Particle {
        float x, y, vx, vy, life, maxLife; int color;
        Particle(float x, float y, float vx, float vy, float life, int color) { this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.maxLife = life; this.color = color; }
        void update(float dt) { x += vx * dt; y += vy * dt; vy += 170f * dt; life -= dt; }
    }

    private static final class Level {
        String name;
        float width, startX, startY, gravity = 980f, time = 0;
        int scoreEarned = 0;
        String message = "";
        float messageTimer = 0;
        Door door;
        List<Platform> platforms = new ArrayList<>();
        List<MovingPlatform> movingPlatforms = new ArrayList<>();
        List<Bridge> bridges = new ArrayList<>();
        List<Collectible> collectibles = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>();
        List<Hazard> hazards = new ArrayList<>();
        List<Checkpoint> checkpoints = new ArrayList<>();
        List<Switch> switches = new ArrayList<>();
        List<Particle> particles = new ArrayList<>();
        List<Solid> allSolids() {
            List<Solid> all = new ArrayList<>();
            all.addAll(platforms);
            all.addAll(movingPlatforms);
            all.addAll(bridges);
            return all;
        }
    }

    private static final class LevelFactory {
        static Level create(int idx) {
            Level l = new Level();
            l.width = 2850 + idx * 260;
            l.startX = 80;
            l.startY = 360;
            int baseColor;
            if (idx == 0) { l.name = "Home Streets"; baseColor = 0xFF79A65B; }
            else if (idx == 1) { l.name = "Outskirts"; baseColor = 0xFF6FA75B; }
            else if (idx == 2) { l.name = "Rain Bridge"; baseColor = 0xFF5F7389; }
            else if (idx == 3) { l.name = "Night City"; baseColor = 0xFF536E9B; }
            else { l.name = "Father's Station"; baseColor = 0xFF8B6EB9; }

            add(l.platforms, 0, 455, 420, 85, baseColor);
            add(l.platforms, 500, 455, 310, 85, baseColor);
            add(l.platforms, 900, 455, 270, 85, baseColor);
            add(l.platforms, 1280, 455, 360, 85, baseColor);
            add(l.platforms, 1760, 455, 300, 85, baseColor);
            add(l.platforms, 2190, 455, 310, 85, baseColor);
            add(l.platforms, 2600, 455, 420, 85, baseColor);

            add(l.platforms, 340, 365, 120, 28, tint(baseColor, 1.08f));
            add(l.platforms, 720, 340, 120, 28, tint(baseColor, 1.08f));
            add(l.platforms, 1040, 310, 130, 28, tint(baseColor, 1.08f));
            add(l.platforms, 1480, 360, 130, 28, tint(baseColor, 1.08f));
            add(l.platforms, 1950, 335, 130, 28, tint(baseColor, 1.08f));
            add(l.platforms, 2350, 300, 120, 28, tint(baseColor, 1.08f));

            l.collectibles.add(new Collectible(355, 330, "A memory plane: 'Dad taught me the first fold.'"));
            l.collectibles.add(new Collectible(750, 305, "A memory plane: 'Mom said courage can be quiet.'"));
            l.collectibles.add(new Collectible(1075, 275, "A memory plane: 'Four hours can still be crossed.'"));
            l.collectibles.add(new Collectible(1510, 325, "A memory plane: 'Keep walking.'"));
            l.collectibles.add(new Collectible(1990, 300, "A memory plane: 'Almost there.'"));
            l.collectibles.add(new Collectible(2380, 265, "A memory plane: 'One more chapter.'"));

            l.enemies.add(new Enemy(610, 421, 545, 760, 52 + idx * 8));
            l.enemies.add(new Enemy(1380, 421, 1290, 1580, 62 + idx * 8));
            l.enemies.add(new Enemy(2240, 421, 2195, 2460, 70 + idx * 7));

            l.hazards.add(new Hazard(420, 498, 80, 42, idx == 2 ? 1 : 0));
            l.hazards.add(new Hazard(810, 498, 90, 42, idx == 3 ? 1 : 0));
            l.hazards.add(new Hazard(1640, 498, 120, 42, idx == 2 ? 1 : 0));
            l.hazards.add(new Hazard(2060, 498, 130, 42, idx == 3 ? 1 : 0));

            l.checkpoints.add(new Checkpoint(1220, 420));
            l.checkpoints.add(new Checkpoint(2140, 420));

            if (idx >= 1) {
                l.movingPlatforms.add(new MovingPlatform(1160, 375, 130, 26, 1120, 1290, 65 + idx * 10));
                l.movingPlatforms.add(new MovingPlatform(2480, 355, 120, 26, 2450, 2600, 75 + idx * 8));
            }
            if (idx >= 2) {
                Bridge b = new Bridge(1660, 390, 210, 26);
                l.bridges.add(b);
                l.switches.add(new Switch(1535, 330));
                l.collectibles.add(new Collectible(1805, 350, "A memory plane: 'Bridges open when hope is touched.'"));
            }
            if (idx >= 3) {
                l.movingPlatforms.add(new MovingPlatform(1840, 270, 120, 24, 1810, 2020, 90));
                l.hazards.add(new Hazard(2500, 498, 110, 42, 1));
                l.enemies.add(new Enemy(2690, 421, 2635, 2870, 86));
            }
            if (idx == 4) {
                l.switches.add(new Switch(2290, 265));
                l.bridges.add(new Bridge(2385, 330, 230, 24));
                l.collectibles.add(new Collectible(2500, 295, "The final plane: 'I was waiting too.'"));
                add(l.platforms, 2860, 420, 260, 120, baseColor);
                l.width = 3250;
            }
            l.door = new Door(l.width - 150, 373);
            return l;
        }

        private static void add(List<Platform> list, float x, float y, float w, float h, int color) {
            list.add(new Platform(x, y, w, h, color));
        }

        private static int tint(int color, float factor) {
            int a = Color.alpha(color);
            int r = Math.min(255, (int) (Color.red(color) * factor));
            int g = Math.min(255, (int) (Color.green(color) * factor));
            int b = Math.min(255, (int) (Color.blue(color) * factor));
            return Color.argb(a, r, g, b);
        }
    }

    private static final class SoundEngine {
        private boolean enabled = true;
        private ToneGenerator tone;
        private Thread musicThread;
        private volatile boolean musicRunning;
        private int currentMode = -99;

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (!enabled) stopMusic();
        }

        void startMenuMusic() { startLoop(-1); }
        void startStoryMusic() { startLoop(-2); }
        void startVictoryMusic() { startLoop(9); }
        void startMusic(int level) { startLoop(level); }

        private synchronized void startLoop(final int mode) {
            if (!enabled) return;
            if (musicRunning && currentMode == mode) return;
            stopMusic();
            currentMode = mode;
            musicRunning = true;
            musicThread = new Thread(() -> runMusic(mode), "GeneratedMusic");
            musicThread.setDaemon(true);
            musicThread.start();
        }

        synchronized void stopMusic() {
            musicRunning = false;
            currentMode = -99;
            if (musicThread != null) {
                try { musicThread.join(80); } catch (InterruptedException ignored) { }
                musicThread = null;
            }
        }

        void jump() { beep(ToneGenerator.TONE_PROP_BEEP, 55); }
        void collect() { beep(ToneGenerator.TONE_PROP_ACK, 70); }
        void hurt() { beep(ToneGenerator.TONE_PROP_NACK, 120); }
        void checkpoint() { beep(ToneGenerator.TONE_PROP_PROMPT, 90); }
        void victory() { beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 130); }

        private void beep(int toneType, int ms) {
            if (!enabled) return;
            try {
                if (tone == null) tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
                tone.startTone(toneType, ms);
            } catch (RuntimeException ignored) { }
        }

        private void runMusic(int mode) {
            final int sampleRate = 22050;
            final int min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            final int bufferSize = Math.max(min, sampleRate / 2);
            AudioTrack track = null;
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    track = new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                            .setAudioFormat(new AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                            .setBufferSizeInBytes(bufferSize * 2)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build();
                } else {
                    track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2, AudioTrack.MODE_STREAM);
                }
                track.play();
                short[] buffer = new short[1024];
                double phase = 0;
                double[] notes = melody(mode);
                int sampleCounter = 0;
                while (musicRunning) {
                    for (int i = 0; i < buffer.length; i++) {
                        double sec = sampleCounter / (double) sampleRate;
                        int noteIndex = (int) (sec / 0.42) % notes.length;
                        double freq = notes[noteIndex];
                        double amp = 0.12;
                        double beat = (sec % 0.42) / 0.42;
                        if (beat < 0.12) amp *= beat / 0.12;
                        if (beat > 0.78) amp *= (1.0 - beat) / 0.22;
                        phase += 2.0 * Math.PI * freq / sampleRate;
                        double wave = Math.sin(phase) * 0.75 + Math.sin(phase * 0.5) * 0.20;
                        buffer[i] = (short) (wave * amp * Short.MAX_VALUE);
                        sampleCounter++;
                    }
                    track.write(buffer, 0, buffer.length);
                }
            } catch (RuntimeException ignored) {
            } finally {
                if (track != null) {
                    try { track.stop(); } catch (RuntimeException ignored) { }
                    try { track.release(); } catch (RuntimeException ignored) { }
                }
            }
        }

        private double[] melody(int mode) {
            if (mode == -1) return hz(new int[]{60, 64, 67, 72, 67, 64, 62, 60});
            if (mode == -2) return hz(new int[]{57, 60, 64, 65, 64, 60, 57, 55});
            if (mode == 9) return hz(new int[]{60, 64, 67, 72, 76, 72, 67, 64, 60, 72});
            if (mode == 2) return hz(new int[]{57, 60, 62, 65, 62, 60, 55, 57});
            if (mode == 3) return hz(new int[]{55, 59, 62, 67, 71, 67, 62, 59});
            return hz(new int[]{60, 62, 64, 67, 69, 67, 64, 62});
        }

        private double[] hz(int[] midi) {
            double[] out = new double[midi.length];
            for (int i = 0; i < midi.length; i++) out[i] = 440.0 * Math.pow(2.0, (midi[i] - 69) / 12.0);
            return out;
        }
    }
}

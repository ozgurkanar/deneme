package com.ozgur.burakroad;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Burak'in Uzun Yolu - LibGDX prototype.
 *
 * Bu surum gercek PNG/WAV assetleri kullanir. Assetler tools/generate_assets.py tarafindan uretilir.
 * Ana yol kurali: zorunlu platformlar pixel-perfect ziplama istemez.
 */
public class BurakinUzunYoluGame extends ApplicationAdapter implements InputProcessor {
    private static final float W = 960f;
    private static final float H = 540f;
    private static final float GRAVITY = -1180f;
    private static final float MAX_FALL = -760f;
    private static final int LEVEL_COUNT = 5;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private FitViewport viewport;
    private FitViewport hudViewport;
    private BitmapFont font;

    private Texture burakTex, enemyTex, tileTex, objectTex, uiTex;
    private Texture[] backgrounds = new Texture[LEVEL_COUNT];
    private TextureRegion[] burakFrames;
    private TextureRegion[] enemyFrames;
    private TextureRegion[] tiles;
    private TextureRegion[] objects;
    private TextureRegion[] uiRegions;

    private Sound sndJump, sndCollect, sndHurt, sndCheckpoint, sndWin;
    private Music musicStory, musicLevel, musicFinal;
    private boolean audio = true;

    private Screen screen = Screen.MENU;
    private Level level;
    private Player player;
    private int levelIndex = 0;
    private int score = 0;
    private float runtime = 0;
    private float storyTimer = 0;
    private int storyIndex = 0;
    private Array<String> storyCards = new Array<>();

    private final Rectangle bStart = new Rectangle(330, 282, 300, 58);
    private final Rectangle bContinue = new Rectangle(330, 212, 300, 58);
    private final Rectangle bAudio = new Rectangle(330, 142, 300, 58);
    private final Rectangle bBack = new Rectangle(330, 64, 300, 58);
    private final Rectangle bLeft = new Rectangle(28, 26, 82, 82);
    private final Rectangle bRight = new Rectangle(124, 26, 82, 82);
    private final Rectangle bJump = new Rectangle(850, 26, 82, 82);
    private final Rectangle bAction = new Rectangle(748, 26, 82, 82);
    private final Rectangle bPause = new Rectangle(884, 468, 54, 48);

    private boolean leftTouch, rightTouch, jumpTouch, actionTouch;
    private boolean leftKey, rightKey, jumpKey, actionKey;
    private boolean jumpLatch;
    private float messageTimer = 0;
    private String message = "";

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        hudCamera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        hudViewport = new FitViewport(W, H, hudCamera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        hudViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        font = new BitmapFont();
        font.getData().setScale(1.15f);
        loadAssets();
        Gdx.input.setInputProcessor(this);
        startMenuMusic();
    }

    private void loadAssets() {
        burakTex = tex("images/burak_sheet.png", true);
        enemyTex = tex("images/enemy_doubt_sheet.png", true);
        tileTex = tex("images/tileset.png", false);
        objectTex = tex("images/objects.png", false);
        uiTex = tex("images/ui_atlas.png", false);
        backgrounds[0] = tex("images/bg_home.png", false);
        backgrounds[1] = tex("images/bg_busroad.png", false);
        backgrounds[2] = tex("images/bg_rainvalley.png", false);
        backgrounds[3] = tex("images/bg_city.png", false);
        backgrounds[4] = tex("images/bg_final.png", false);
        burakFrames = TextureRegion.split(burakTex, 64, 64)[0];
        enemyFrames = TextureRegion.split(enemyTex, 48, 48)[0];
        tiles = TextureRegion.split(tileTex, 64, 64)[0];
        objects = TextureRegion.split(objectTex, 64, 64)[0];
        uiRegions = TextureRegion.split(uiTex, 256, 128)[0];
        try { sndJump = Gdx.audio.newSound(Gdx.files.internal("audio/jump.wav")); } catch (Exception ignored) {}
        try { sndCollect = Gdx.audio.newSound(Gdx.files.internal("audio/collect.wav")); } catch (Exception ignored) {}
        try { sndHurt = Gdx.audio.newSound(Gdx.files.internal("audio/hurt.wav")); } catch (Exception ignored) {}
        try { sndCheckpoint = Gdx.audio.newSound(Gdx.files.internal("audio/checkpoint.wav")); } catch (Exception ignored) {}
        try { sndWin = Gdx.audio.newSound(Gdx.files.internal("audio/win.wav")); } catch (Exception ignored) {}
        try { musicStory = Gdx.audio.newMusic(Gdx.files.internal("audio/music_story.wav")); musicStory.setLooping(true); } catch (Exception ignored) {}
        try { musicLevel = Gdx.audio.newMusic(Gdx.files.internal("audio/music_level.wav")); musicLevel.setLooping(true); } catch (Exception ignored) {}
        try { musicFinal = Gdx.audio.newMusic(Gdx.files.internal("audio/music_final.wav")); musicFinal.setLooping(true); } catch (Exception ignored) {}
    }

    private Texture tex(String path, boolean sprite) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(sprite ? Texture.TextureFilter.Nearest : Texture.TextureFilter.Linear, sprite ? Texture.TextureFilter.Nearest : Texture.TextureFilter.Linear);
        return t;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hudViewport.update(width, height, true);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        runtime += dt;
        updateTouchControls();
        if (screen == Screen.PLAYING) updateGame(dt);
        if (messageTimer > 0) messageTimer -= dt;

        Gdx.gl.glClearColor(0.04f, 0.06f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (screen == Screen.MENU) drawMenu();
        else if (screen == Screen.STORY) drawStory();
        else if (screen == Screen.PLAYING) drawPlaying(false);
        else if (screen == Screen.PAUSED) { drawPlaying(true); drawPause(); }
        else if (screen == Screen.GAME_OVER) { drawPlaying(true); drawEnd(false); }
        else if (screen == Screen.VICTORY) drawVictory();
    }

    private void updateGame(float dt) {
        float move = 0;
        if (leftTouch || leftKey) move -= 1;
        if (rightTouch || rightKey) move += 1;
        player.vx = move * player.speed;
        if (move != 0) player.face = move > 0 ? 1 : -1;
        boolean wantsJump = jumpTouch || jumpKey;
        if (wantsJump && !jumpLatch && player.onGround) {
            player.vy = player.jumpPower;
            player.onGround = false;
            play(sndJump, .45f);
        }
        jumpLatch = wantsJump;
        if ((actionTouch || actionKey) && level != null) activateNearSwitch();

        player.vy = Math.max(MAX_FALL, player.vy + GRAVITY * dt);
        player.x += player.vx * dt;
        resolveHorizontal();
        player.y += player.vy * dt;
        resolveVertical();
        player.x = MathUtils.clamp(player.x, 0, level.width - player.w);
        player.anim += dt;

        for (MovingPlatform mp : level.movers) mp.update(dt);
        for (Enemy e : level.enemies) e.update(dt);
        handleCollectibles();
        handleEnemies();
        handleHazardsAndFalls();
        handleCheckpoints();
        if (player.rect().overlaps(level.door.rect())) advance();
        updateCamera();
    }

    private void updateCamera() {
        float cx = MathUtils.clamp(player.x + 160, W / 2f, Math.max(W / 2f, level.width - W / 2f));
        camera.position.set(cx, H / 2f, 0);
        camera.update();
    }

    private List<Solid> solids() {
        ArrayList<Solid> all = new ArrayList<>();
        all.addAll(level.platforms);
        all.addAll(level.movers);
        for (Bridge b : level.bridges) if (b.active) all.add(b);
        return all;
    }

    private void resolveHorizontal() {
        Rectangle r = player.rect();
        for (Solid s : solids()) {
            if (r.overlaps(s.rect())) {
                if (player.vx > 0) player.x = s.x - player.w;
                else if (player.vx < 0) player.x = s.x + s.w;
                player.vx = 0;
                r = player.rect();
            }
        }
    }

    private void resolveVertical() {
        player.onGround = false;
        Rectangle r = player.rect();
        for (Solid s : solids()) {
            Rectangle sr = s.rect();
            if (r.overlaps(sr)) {
                if (player.vy > 0) {
                    player.y = s.y - player.h;
                    player.vy = 0;
                } else if (player.vy <= 0) {
                    player.y = s.y + s.h;
                    player.vy = 0;
                    player.onGround = true;
                    if (s instanceof MovingPlatform) player.x += ((MovingPlatform) s).dxLast;
                }
                r = player.rect();
            }
        }
    }

    private void handleCollectibles() {
        Rectangle pr = player.rect();
        for (Collectible c : level.collectibles) {
            if (!c.taken && pr.overlaps(c.rect())) {
                c.taken = true;
                score += 10;
                message = c.text;
                messageTimer = 2.2f;
                play(sndCollect, .55f);
            }
        }
    }

    private void handleEnemies() {
        Rectangle pr = player.rect();
        for (Enemy e : level.enemies) {
            if (e.dead) continue;
            if (pr.overlaps(e.rect())) {
                boolean stomp = player.vy < -20 && player.y > e.y + e.h * .35f;
                if (stomp) {
                    e.dead = true;
                    player.vy = player.jumpPower * .58f;
                    score += 25;
                    play(sndCollect, .45f);
                } else hurt();
            }
        }
    }

    private void handleHazardsAndFalls() {
        if (player.y < -80) { hurt(); return; }
        Rectangle pr = player.rect();
        for (Hazard h : level.hazards) if (pr.overlaps(h.rect())) { hurt(); return; }
    }

    private void handleCheckpoints() {
        Rectangle pr = player.rect();
        for (Checkpoint c : level.checkpoints) {
            if (!c.used && pr.overlaps(c.rect())) {
                c.used = true;
                player.respawnX = c.x;
                player.respawnY = c.y + 50;
                message = "Kontrol noktasi acildi.";
                messageTimer = 2f;
                play(sndCheckpoint, .55f);
            }
        }
    }

    private void activateNearSwitch() {
        for (Switch sw : level.switches) {
            if (!sw.on && Math.abs(player.x + player.w / 2 - sw.x) < 58 && Math.abs(player.y - sw.y) < 80) {
                sw.on = true;
                for (Bridge b : level.bridges) b.active = true;
                message = "Kopru acildi.";
                messageTimer = 1.6f;
                play(sndCheckpoint, .45f);
            }
        }
    }

    private void hurt() {
        if (player.invincible > 0) return;
        player.lives--;
        play(sndHurt, .55f);
        if (player.lives <= 0) {
            stopMusic();
            screen = Screen.GAME_OVER;
            return;
        }
        player.x = player.respawnX;
        player.y = player.respawnY;
        player.vx = 0;
        player.vy = 0;
        player.invincible = 1.4f;
        message = "Burak yeniden deniyor.";
        messageTimer = 1.6f;
    }

    private void advance() {
        play(sndWin, .6f);
        if (levelIndex >= LEVEL_COUNT - 1) {
            screen = Screen.VICTORY;
            startFinalMusic();
        } else {
            levelIndex++;
            showStory(levelIndex);
        }
    }

    private void startNewGame() {
        score = 0;
        levelIndex = 0;
        showStory(0);
    }

    private void showStory(int chapter) {
        storyIndex = 0;
        storyTimer = 0;
        storyCards.clear();
        if (chapter == 0) {
            storyCards.add("Pencere Kenari\nBurak dokuz yasinda. Kizil saclari aksam isiginda parliyor. Her cuma babasinin yolunu dusunur.");
            storyCards.add("Dort Saat\nAnnesi onu severek buyutur. Babasi baska sehirde yasar; aralarinda dort saatlik yol vardir. Sevgi eksilmez, ozlem buyur.");
            storyCards.add("Kagit Ucaklar\nBurak babasina ulasmak icin anilarla dolu kagit ucaklarin izini takip eder.");
        } else if (chapter == 1) storyCards.add("Otogar Yolu\nMahalle geride kalir. Burak korkar ama annesinin sesi icindedir: dogru adimi atabilirsin.");
        else if (chapter == 2) storyCards.add("Yagmurlu Vadi\nYagmur baslar. Burak babasinin sozunu hatirlar: cesaret korkusuz olmak degil, yurumeye devam etmektir.");
        else if (chapter == 3) storyCards.add("Sehir Isiklari\nUzaktaki sehir parlar. Burak her isikta babasinin penceresini arar.");
        else storyCards.add("Son Donemec\nKapiya cok az kaldi. Dort saatlik yol artik bir nefes kadar yakin.");
        screen = Screen.STORY;
        startStoryMusic();
    }

    private void startLevel() {
        level = LevelFactory.create(levelIndex);
        player = new Player(level.startX, level.startY);
        jumpLatch = false;
        screen = Screen.PLAYING;
        message = "Ana yol rahat ziplama payiyla tasarlandi.";
        messageTimer = 2.2f;
        updateCamera();
        startLevelMusic();
    }

    private void drawMenu() {
        drawHudBackground(4);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        drawCentered("BURAK'IN", 270, 2.9f, Color.valueOf("FFE2A3"));
        drawCentered("UZUN YOLU", 218, 3.35f, Color.valueOf("FFFFFF"));
        drawCentered("LibGDX • gercek PNG/WAV asset pipeline • Turkce hikaye", 174, 1.05f, Color.valueOf("DDEBFF"));
        drawButton(bStart, "Yeni Oyuna Basla", 0);
        drawButton(bContinue, "Devam Et", 1);
        drawButton(bAudio, audio ? "Ses: Acik" : "Ses: Kapali", 2);
        drawButton(bBack, "Cikis / Geri", 3);
        batch.end();
    }

    private void drawStory() {
        drawHudBackground(levelIndex);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        drawPanel(120, 92, 720, 350);
        String[] parts = storyCards.get(storyIndex).split("\\n", 2);
        drawCentered(parts[0], 390, 2.05f, Color.valueOf("FFE2A3"));
        drawWrapped(parts.length > 1 ? parts[1] : "", 170, 322, 620, 1.15f, Color.WHITE);
        drawCentered("Devam etmek icin dokun", 128, 1f, Color.valueOf("DDEBFF"));
        batch.end();
    }

    private void drawPlaying(boolean dim) {
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawWorldBackground();
        drawLevel();
        drawActors();
        batch.end();
        hudViewport.apply();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        if (dim) drawTint(0, 0, W, H, new Color(0, 0, 0, .45f));
        drawHud();
        drawControls();
        batch.end();
    }

    private void drawHudBackground(int idx) {
        hudViewport.apply();
        hudCamera.position.set(W / 2f, H / 2f, 0);
        hudCamera.update();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        batch.draw(backgrounds[MathUtils.clamp(idx, 0, 4)], 0, 0, W, H);
        drawTint(0, 0, W, H, new Color(.02f, .04f, .12f, .42f));
        batch.end();
    }

    private void drawWorldBackground() {
        Texture bg = backgrounds[levelIndex];
        float x = camera.position.x - W / 2f;
        batch.draw(bg, x, 0, W, H);
        // parallax light strips
        Color old = batch.getColor();
        batch.setColor(1, 1, 1, .18f);
        for (int i = 0; i < 6; i++) {
            float px = x + ((i * 230 - camera.position.x * .18f) % 1160f);
            batch.draw(objects[6], px, 390 + (i % 3) * 22, 22, 22);
        }
        batch.setColor(old);
    }

    private void drawLevel() {
        for (Platform p : level.platforms) drawTiled(p, p.tile);
        for (Bridge b : level.bridges) if (b.active) drawTiled(b, 4); else drawGhostBridge(b);
        for (MovingPlatform m : level.movers) drawTiled(m, 3);
        for (Hazard h : level.hazards) batch.draw(objects[4], h.x, h.y, h.w, h.h);
        for (Switch s : level.switches) batch.draw(objects[6], s.x - 22, s.y - 22, 44, 44);
        for (Checkpoint c : level.checkpoints) batch.draw(objects[c.used ? 1 : 0], c.x - 24, c.y - 12, 48, 64);
        for (Collectible c : level.collectibles) if (!c.taken) batch.draw(objects[0], c.x - 20, c.y - 20 + MathUtils.sin(runtime * 4 + c.x) * 4, 40, 40);
        batch.draw(objects[2], level.door.x, level.door.y, level.door.w, level.door.h);
    }

    private void drawTiled(Solid p, int tile) {
        for (float xx = p.x; xx < p.x + p.w - 1; xx += 64) {
            for (float yy = p.y; yy < p.y + p.h - 1; yy += 64) {
                float ww = Math.min(64, p.x + p.w - xx);
                float hh = Math.min(64, p.y + p.h - yy);
                batch.draw(tiles[MathUtils.clamp(tile, 0, tiles.length - 1)], xx, yy, ww, hh);
            }
        }
    }

    private void drawGhostBridge(Bridge b) {
        Color old = batch.getColor();
        batch.setColor(1, 1, 1, .38f);
        drawTiled(b, 4);
        batch.setColor(old);
    }

    private void drawActors() {
        for (Enemy e : level.enemies) if (!e.dead) {
            int frame = ((int)(runtime * 8)) % enemyFrames.length;
            batch.draw(enemyFrames[frame], e.x, e.y, e.w, e.h);
        }
        int frame = 0;
        if (!player.onGround) frame = player.vy > 0 ? 3 : 4;
        else if (Math.abs(player.vx) > 1) frame = 1 + (((int)(runtime * 10)) % 2);
        if (player.invincible > 0) player.invincible -= Gdx.graphics.getDeltaTime();
        if (player.invincible <= 0 || ((int)(runtime * 16) % 2 == 0)) {
            TextureRegion r = burakFrames[frame];
            if (player.face < 0) batch.draw(r, player.x + player.w, player.y, -player.w, player.h);
            else batch.draw(r, player.x, player.y, player.w, player.h);
        }
    }

    private void drawHud() {
        drawPanel(14, 480, 370, 46);
        drawText("Bolum " + (levelIndex + 1) + ": " + level.name, 30, 514, 1f, Color.WHITE);
        drawText("Puan " + score + "   Can " + player.lives, 30, 492, .9f, Color.valueOf("FFE2A3"));
        if (messageTimer > 0) {
            drawPanel(210, 430, 540, 42);
            drawCentered(message, 456, .9f, Color.WHITE);
        }
        drawSmallButton(bPause, "II");
    }

    private void drawControls() {
        drawSmallButton(bLeft, "◀");
        drawSmallButton(bRight, "▶");
        drawSmallButton(bAction, "✦");
        drawSmallButton(bJump, "▲");
    }

    private void drawPause() {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        drawCentered("Duraklatildi", 360, 2.5f, Color.valueOf("FFE2A3"));
        drawButton(bStart, "Devam Et", 0);
        drawButton(bContinue, "Ana Menu", 1);
        batch.end();
    }

    private void drawEnd(boolean win) {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        drawCentered(win ? "Kazandin" : "Tekrar Dene", 360, 2.4f, win ? Color.valueOf("FFE2A3") : Color.valueOf("FF9E9E"));
        drawWrapped(win ? "Burak yolu tamamladi." : "Bu yol uzun ama imkansiz degil. Bolum artik daha adil ziplama payiyla tasarlandi.", 210, 302, 540, 1.1f, Color.WHITE);
        drawButton(bStart, "Bolumu Yeniden Baslat", 0);
        drawButton(bContinue, "Ana Menu", 1);
        batch.end();
    }

    private void drawVictory() {
        drawHudBackground(4);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        drawPanel(130, 90, 700, 360);
        drawCentered("Nihayet Bulusuyorlar", 390, 2.1f, Color.valueOf("FFE2A3"));
        drawWrapped("Burak kapiyi calar. Babasi daha ucuncu vurusu beklemeden kapiyi acar. Dort saatlik mesafe, simsiki bir sarilmanin icinde kuculur.", 180, 320, 600, 1.15f, Color.WHITE);
        drawCentered("Puan: " + score, 205, 1.25f, Color.valueOf("FFE2A3"));
        drawButton(bStart, "Ana Menu", 0);
        batch.end();
    }

    private void drawButton(Rectangle r, String text, int uiIndex) {
        batch.draw(uiRegions[MathUtils.clamp(uiIndex, 0, uiRegions.length - 1)], r.x, r.y, r.width, r.height);
        drawCenteredIn(text, r, 1.05f, Color.WHITE);
    }

    private void drawSmallButton(Rectangle r, String text) {
        batch.draw(objects[7], r.x, r.y, r.width, r.height);
        drawCenteredIn(text, r, 1.2f, Color.WHITE);
    }

    private void drawPanel(float x, float y, float w, float h) {
        batch.draw(uiRegions[2], x, y, w, h);
    }

    private void drawTint(float x, float y, float w, float h, Color color) {
        Color old = batch.getColor();
        batch.setColor(color);
        batch.draw(uiRegions[2], x, y, w, h);
        batch.setColor(old);
    }

    private void drawCentered(String s, float y, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, s, W / 2f - s.length() * 4.2f * scale, y);
    }

    private void drawText(String s, float x, float y, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, s, x, y);
    }

    private void drawCenteredIn(String s, Rectangle r, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, s, r.x + r.width / 2f - s.length() * 3.8f * scale, r.y + r.height / 2f + 8 * scale);
    }

    private void drawWrapped(String s, float x, float y, float width, float scale, Color color) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, s, x, y, width, -1, true);
    }

    private void updateTouchControls() {
        leftTouch = rightTouch = jumpTouch = actionTouch = false;
        for (int i = 0; i < 5; i++) {
            if (!Gdx.input.isTouched(i)) continue;
            Vector3 v = hudViewport.unproject(new Vector3(Gdx.input.getX(i), Gdx.input.getY(i), 0));
            if (bLeft.contains(v.x, v.y)) leftTouch = true;
            if (bRight.contains(v.x, v.y)) rightTouch = true;
            if (bJump.contains(v.x, v.y)) jumpTouch = true;
            if (bAction.contains(v.x, v.y)) actionTouch = true;
        }
    }

    @Override public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) leftKey = true;
        if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) rightKey = true;
        if (keycode == Input.Keys.W || keycode == Input.Keys.UP || keycode == Input.Keys.SPACE) jumpKey = true;
        if (keycode == Input.Keys.E || keycode == Input.Keys.ENTER) actionKey = true;
        if (keycode == Input.Keys.P && screen == Screen.PLAYING) screen = Screen.PAUSED;
        return true;
    }
    @Override public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) leftKey = false;
        if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) rightKey = false;
        if (keycode == Input.Keys.W || keycode == Input.Keys.UP || keycode == Input.Keys.SPACE) jumpKey = false;
        if (keycode == Input.Keys.E || keycode == Input.Keys.ENTER) actionKey = false;
        return true;
    }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Vector3 v = hudViewport.unproject(new Vector3(screenX, screenY, 0));
        if (screen == Screen.MENU) {
            if (bStart.contains(v.x, v.y)) startNewGame();
            else if (bContinue.contains(v.x, v.y)) startNewGame();
            else if (bAudio.contains(v.x, v.y)) { audio = !audio; if (!audio) stopMusic(); else startMenuMusic(); }
            return true;
        }
        if (screen == Screen.STORY) {
            storyIndex++;
            if (storyIndex >= storyCards.size) startLevel();
            return true;
        }
        if (screen == Screen.PLAYING && bPause.contains(v.x, v.y)) { screen = Screen.PAUSED; return true; }
        if (screen == Screen.PAUSED) {
            if (bStart.contains(v.x, v.y)) screen = Screen.PLAYING;
            else if (bContinue.contains(v.x, v.y)) { screen = Screen.MENU; startMenuMusic(); }
            return true;
        }
        if (screen == Screen.GAME_OVER) {
            if (bStart.contains(v.x, v.y)) startLevel();
            else if (bContinue.contains(v.x, v.y)) { screen = Screen.MENU; startMenuMusic(); }
            return true;
        }
        if (screen == Screen.VICTORY) {
            if (bStart.contains(v.x, v.y)) { screen = Screen.MENU; startMenuMusic(); }
            return true;
        }
        return true;
    }

    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return true; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return true; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

    private void play(Sound sound, float volume) { if (audio && sound != null) sound.play(volume); }
    private void startMenuMusic() { startMusic(musicStory); }
    private void startStoryMusic() { startMusic(musicStory); }
    private void startLevelMusic() { startMusic(musicLevel); }
    private void startFinalMusic() { startMusic(musicFinal); }
    private void startMusic(Music m) {
        if (!audio) return;
        stopMusic();
        if (m != null) { m.setVolume(.28f); m.play(); }
    }
    private void stopMusic() {
        if (musicStory != null) musicStory.stop();
        if (musicLevel != null) musicLevel.stop();
        if (musicFinal != null) musicFinal.stop();
    }

    @Override public void dispose() {
        stopMusic();
        batch.dispose(); font.dispose();
        burakTex.dispose(); enemyTex.dispose(); tileTex.dispose(); objectTex.dispose(); uiTex.dispose();
        for (Texture t : backgrounds) if (t != null) t.dispose();
        if (sndJump != null) sndJump.dispose(); if (sndCollect != null) sndCollect.dispose(); if (sndHurt != null) sndHurt.dispose();
        if (sndCheckpoint != null) sndCheckpoint.dispose(); if (sndWin != null) sndWin.dispose();
        if (musicStory != null) musicStory.dispose(); if (musicLevel != null) musicLevel.dispose(); if (musicFinal != null) musicFinal.dispose();
    }

    private enum Screen { MENU, STORY, PLAYING, PAUSED, GAME_OVER, VICTORY }

    private static final class Player {
        float x, y, w = 52, h = 70, vx, vy;
        float speed = 250, jumpPower = 575;
        int face = 1, lives = 3;
        float respawnX, respawnY, anim, invincible;
        boolean onGround;
        Player(float x, float y) { this.x = x; this.y = y; respawnX = x; respawnY = y; }
        Rectangle rect() { return new Rectangle(x + 8, y + 2, w - 16, h - 3); }
    }
    private static class Solid {
        float x, y, w, h; int tile;
        Solid(float x, float y, float w, float h, int tile) { this.x = x; this.y = y; this.w = w; this.h = h; this.tile = tile; }
        Rectangle rect() { return new Rectangle(x, y, w, h); }
    }
    private static final class Platform extends Solid { Platform(float x, float y, float w, float h, int tile) { super(x, y, w, h, tile); } }
    private static final class MovingPlatform extends Solid {
        float minX, maxX, speed, dir = 1, dxLast;
        MovingPlatform(float x, float y, float w, float h, float minX, float maxX, float speed) { super(x, y, w, h, 3); this.minX = minX; this.maxX = maxX; this.speed = speed; }
        void update(float dt) { float old = x; x += speed * dir * dt; if (x < minX) { x = minX; dir = 1; } if (x > maxX) { x = maxX; dir = -1; } dxLast = x - old; }
    }
    private static final class Bridge extends Solid { boolean active; Bridge(float x, float y, float w, float h) { super(x, y, w, h, 4); } }
    private static final class Hazard { float x,y,w,h; Hazard(float x,float y,float w,float h){this.x=x;this.y=y;this.w=w;this.h=h;} Rectangle rect(){return new Rectangle(x,y,w,h);} }
    private static final class Door { float x,y,w=66,h=94; Door(float x,float y){this.x=x;this.y=y;} Rectangle rect(){return new Rectangle(x,y,w,h);} }
    private static final class Switch { float x,y; boolean on; Switch(float x,float y){this.x=x;this.y=y;} }
    private static final class Checkpoint { float x,y; boolean used; Checkpoint(float x,float y){this.x=x;this.y=y;} Rectangle rect(){return new Rectangle(x-24,y-12,48,64);} }
    private static final class Collectible { float x,y; boolean taken; String text; Collectible(float x,float y,String text){this.x=x;this.y=y;this.text=text;} Rectangle rect(){return new Rectangle(x-22,y-22,44,44);} }
    private static final class Enemy { float x,y,w=48,h=42,minX,maxX,speed,dir=1; boolean dead; Enemy(float x,float y,float minX,float maxX,float speed){this.x=x;this.y=y;this.minX=minX;this.maxX=maxX;this.speed=speed;} void update(float dt){x += speed*dir*dt; if(x<minX){x=minX;dir=1;} if(x>maxX){x=maxX;dir=-1;}} Rectangle rect(){return new Rectangle(x+5,y+3,w-10,h-4);} }
    private static final class Level {
        String name; float width,startX,startY; Door door;
        List<Platform> platforms = new ArrayList<>();
        List<MovingPlatform> movers = new ArrayList<>();
        List<Bridge> bridges = new ArrayList<>();
        List<Hazard> hazards = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>();
        List<Collectible> collectibles = new ArrayList<>();
        List<Checkpoint> checkpoints = new ArrayList<>();
        List<Switch> switches = new ArrayList<>();
    }

    private static final class LevelFactory {
        static Level create(int idx) {
            Level l = new Level();
            l.startX = 80; l.startY = 136; l.width = 3100 + idx * 350;
            l.name = idx == 0 ? "Mahalle" : idx == 1 ? "Otogar Yolu" : idx == 2 ? "Yagmurlu Vadi" : idx == 3 ? "Sehir Isiklari" : "Bulusma Sokagi";
            int tile = Math.min(idx, 4);
            // Main path: safe gaps and reachable ledges. Ground top is 104, raised-platform tops stay around 220-250 on mandatory route.
            add(l,0,40,520,64,tile); add(l,610,40,360,64,tile); add(l,1080,40,420,64,tile); add(l,1600,40,400,64,tile); add(l,2110,40,370,64,tile); add(l,2580,40,420,64,tile);
            add(l,300,170,150,34,tile); add(l,780,194,150,34,tile); add(l,1230,218,160,34,tile); add(l,1760,188,170,34,tile); add(l,2280,210,160,34,tile); add(l,2800,202,170,34,tile);
            if (idx >= 1) { l.movers.add(new MovingPlatform(980,155,145,30,940,1130,78)); l.movers.add(new MovingPlatform(2460,170,145,30,2410,2610,86)); }
            if (idx >= 2) { Bridge b = new Bridge(1510,154,230,30); l.bridges.add(b); l.switches.add(new Switch(1375,205)); l.collectibles.add(new Collectible(1605,212,"Kopru acildi; umut bazen bir dugmeye dokunmaktir.")); }
            if (idx >= 3) { l.movers.add(new MovingPlatform(1960,250,135,30,1930,2140,92)); add(l,2140,300,150,34,tile); }
            if (idx == 4) { Bridge b2 = new Bridge(3000,180,220,30); l.bridges.add(b2); l.switches.add(new Switch(2860,245)); add(l,3220,40,420,64,tile); l.width = 3650; }
            l.hazards.add(new Hazard(520,40,90,42)); l.hazards.add(new Hazard(1500,40,100,42)); l.hazards.add(new Hazard(2480,40,100,42));
            if (idx >= 2) l.hazards.add(new Hazard(2000,40,110,42));
            l.enemies.add(new Enemy(700,104,640,930,65 + idx*5)); l.enemies.add(new Enemy(1700,104,1630,1950,72 + idx*5)); l.enemies.add(new Enemy(2660,104,2600,2960,78 + idx*5));
            l.collectibles.add(new Collectible(330,226,"Kagit ucak: Babam ilk katlamayi bana ogretmisti."));
            l.collectibles.add(new Collectible(810,250,"Kagit ucak: Annem, cesaretin sessiz de olabilecegini soyledi."));
            l.collectibles.add(new Collectible(1260,274,"Kagit ucak: Dort saatlik yol da asilir."));
            l.collectibles.add(new Collectible(1795,244,"Kagit ucak: Yurumeye devam et."));
            l.collectibles.add(new Collectible(2310,266,"Kagit ucak: Sehir isiklari yaklasiyor."));
            l.collectibles.add(new Collectible(2830,258,"Kagit ucak: Cok az kaldi."));
            l.checkpoints.add(new Checkpoint(1540,104)); l.checkpoints.add(new Checkpoint(2520,104));
            l.door = new Door(l.width - 150, 104);
            return l;
        }
        private static void add(Level l,float x,float y,float w,float h,int tile){ l.platforms.add(new Platform(x,y,w,h,tile)); }
    }
}

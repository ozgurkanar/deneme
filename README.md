# Luma's Leap

## 1. Game Concept
**Luma's Leap** is an original 2D side-scrolling platformer starring **Luma**, a brave
firefly-spirit girl who explores three glowing worlds — the Glowing Meadow, the Crystal
Hollow, and the Starlit Peak — collecting light-orbs and defeating shadowy "Murk Crawler"
creatures by jumping on them. Everything (character, enemies, levels, backgrounds, icon)
is drawn procedurally with Android's `Canvas` API — no Unity, no external engines, and no
external image/sound assets required.

## 2. Full Project File Tree
```
LumaLeap/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── .github/
│   └── workflows/
│       └── build-apk.yml
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/lumastudio/lumaleap/
        │   ├── MainActivity.kt
        │   ├── GameView.kt
        │   ├── Player.kt
        │   ├── Enemy.kt
        │   └── Level.kt
        └── res/
            ├── values/
            │   ├── strings.xml
            │   ├── colors.xml
            │   └── themes.xml
            └── drawable/
                └── ic_launcher.xml
```

## 3. Complete Source Code
All source files are included in this project folder (see the file tree above). Every
file is complete and ready to compile — no TODOs, no placeholders.

Key gameplay logic lives in:
- **`Player.kt`** — physics constants, movement, jumping, gravity, invulnerability.
- **`Enemy.kt`** — patrol behavior for the Murk Crawlers.
- **`Level.kt`** — data for all 3 levels (platforms, collectibles, enemies, goal).
- **`GameView.kt`** — the game loop (`SurfaceView` + `Thread`), rendering, touch and
  keyboard input, camera, collision, and the menu/game-over/victory state machine.
- **`MainActivity.kt`** — hosts `GameView`, handles fullscreen + lifecycle.

## 4. Build Instructions

### Option A — Build in the cloud with GitHub Actions (recommended, no installs needed)
1. Create a new empty repository on GitHub.
2. Upload **all files in this folder**, preserving the folder structure (including the
   hidden `.github` folder).
3. GitHub will automatically run the **"Build Luma's Leap APK"** workflow on push.
   You can also trigger it manually from the **Actions** tab → select the workflow →
   **Run workflow**.
4. When the run finishes (green checkmark), open the run, scroll to **Artifacts**, and
   download **`LumaLeap-debug-apk`**. Unzip it to get `app-debug.apk`.

### Option B — Build locally (if you have Android Studio / SDK + Gradle installed)
```bash
cd LumaLeap
gradle wrapper --gradle-version 8.4    # only needed once, generates gradlew
./gradlew assembleDebug
```
The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

You can also simply open the `LumaLeap` folder in Android Studio and click **Run** —
Android Studio will handle the Gradle wrapper and SDK automatically.

## 5. GitHub Actions APK Build Workflow
Included at `.github/workflows/build-apk.yml`. It:
1. Checks out the repo.
2. Installs JDK 17 and the Android SDK.
3. Generates the Gradle wrapper (so you don't need to commit large binary wrapper files).
4. Runs `./gradlew assembleDebug`.
5. Uploads the resulting APK as a downloadable build artifact.

## 6. How to Install the APK on Android
1. Copy `app-debug.apk` to your phone (email, cloud drive, USB, etc.).
2. On your phone, tap the APK file. If prompted, allow **"Install unknown apps"** for
   the app you used to open the file (Settings → Apps → Special access → Install
   unknown apps).
3. Tap **Install**, then **Open**.
4. The game launches directly in fullscreen landscape mode — no setup screens.

## 7. Testing Checklist
- [ ] App installs and launches without crashing.
- [ ] Main menu appears with a **START** button.
- [ ] Tapping **START** begins Level 1.
- [ ] On-screen left/right buttons move Luma; jump button makes her jump.
- [ ] Hardware keyboard (if connected): A/D or arrow keys move, Space/W/Up jumps.
- [ ] Luma cannot jump again while airborne (no infinite jumping).
- [ ] Luma lands correctly on platforms of different heights.
- [ ] Camera scrolls smoothly as Luma moves right.
- [ ] Falling into a gap costs a life and respawns Luma at the level start.
- [ ] Touching a Murk Crawler from the side costs a life.
- [ ] Jumping on top of a Murk Crawler defeats it and bounces Luma upward.
- [ ] Collecting orbs increases the score shown in the HUD.
- [ ] Reaching the goal beacon advances to the next level (Level 2, then Level 3).
- [ ] Each level feels progressively harder (more gaps/enemies).
- [ ] Losing all 3 lives shows the **Game Over** screen with final score.
- [ ] Completing Level 3 shows the **Victory** screen.
- [ ] **RESTART** / **PLAY AGAIN** returns to the main menu correctly.
- [ ] Game runs on different screen sizes without layout/scaling issues.

## 8. Known Limitations
- No sound/music (kept dependency-free and asset-free per the requirements).
- Collision detection uses straightforward AABB checks, not pixel-perfect physics —
  fine for this style of game but not a full physics engine.
- Visuals are procedural shapes (circles/rects/paths) rather than hand-drawn sprite art.
- Only 3 levels are included, as requested; the `Level.kt` structure makes it
  straightforward to add more.
- The GitHub Actions workflow builds a **debug** APK (unsigned, fine for sideloading/
  testing). For a signed release build you'd add a signing config and keystore secret.

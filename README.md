# Burak's Long Road

A native Android 2D story-platformer. No Unity, no external art packs, no manual sprites.
Everything is drawn in Java Canvas at runtime. Music and sound effects are generated programmatically.

## Story

Burak is a 9-year-old red-haired child. He lives with his mother. His parents are separated, but both love him deeply.
His father lives in another city, about four hours away. When a weekend visit is delayed, Burak follows paper planes and memories across neighborhoods, fields, rain, city lights, and the final station to reach his father.

The story is warm, family-friendly, and hopeful. The mother is caring; the father is loving; the distance is the obstacle.

## Features

- Native Android Java project
- 2D side-scrolling platform gameplay
- 5 story chapters
- Touch controls: left, right, jump, action
- Keyboard controls for emulator/testing: A/D, arrows, Space, E
- Main menu, pause menu, credits, game-over, victory screen
- Checkpoints, lives, respawn, enemies, hazards, collectibles
- Light puzzle mechanics with switches and bridges
- Programmatic character, backgrounds, parallax, particles, UI and app icon
- Generated music and sound effects; no bundled external audio files
- GitHub Actions workflow for APK/AAB build

## File tree

```text
BuraksLongRoad/
  settings.gradle
  build.gradle
  gradle.properties
  README.md
  .github/workflows/build-android.yml
  app/
    build.gradle
    src/main/
      AndroidManifest.xml
      java/com/ozgur/burakslongroad/
        MainActivity.java
        GameView.java
      res/values/
        strings.xml
        styles.xml
        colors.xml
      res/drawable/
        app_icon.xml
```

## Easiest build method: GitHub Actions

You do not need Unity.
You do not need to create sprites.
You do not need to create sounds.

1. Create a new empty GitHub repository.
2. Upload all files from this folder into the repository.
3. Go to the repository's **Actions** tab.
4. Open **Build Android APK and AAB**.
5. Press **Run workflow**.
6. Wait until the build finishes.
7. Open the finished workflow run.
8. Download the artifact named **BuraksLongRoad-builds**.
9. Extract the downloaded ZIP.
10. Install the APK from:

```text
app-debug.apk
```

The workflow also creates a release AAB here:

```text
app-release.aab
```

Important: The included release build is signed with the debug signing config so CI can produce a file without your private key.
For Google Play production, replace the signing config with your real upload key before uploading.

## Local build method

Requirements:

- Android Studio or Android SDK
- JDK 17
- Gradle 9.3.1 or newer compatible Gradle installation
- Android SDK platform 37
- Android Build Tools 36.0.0

Build APK:

```bash
gradle :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build AAB:

```bash
gradle :app:bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Install APK on Android phone

### Method 1: Direct install on phone

1. Copy `app-debug.apk` to your phone.
2. Open it from Files / My Files.
3. Android may ask to allow installs from this source.
4. Enable permission for that file manager/browser.
5. Install the APK.
6. Open **Burak's Long Road**.

### Method 2: ADB install

```bash
adb install -r app-debug.apk
```

## Controls

### Phone

- Left button: move left
- Right button: move right
- Up button: jump
- Star/hand button: interact with switches
- Pause button: top-right

### Keyboard / emulator

- A or Left Arrow: move left
- D or Right Arrow: move right
- Space / W / Up Arrow: jump
- E / Enter: interact
- P: pause

## Publishing notes for Google Play

Before uploading to Google Play production:

1. Replace the temporary debug signing config with your own secure upload key.
2. Change `applicationId` if you want your own package name.
3. Update `versionCode` and `versionName` for each release.
4. Create proper store screenshots from the game.
5. Test on multiple Android devices.
6. Consider adding Play policy pages: privacy policy, data safety, and content rating.

## Known limitations

This is a complete lightweight mini-adventure, not a commercial-scale engine project.
The visuals and sounds are generated in code, so they are intentionally stylized rather than bitmap-heavy.
It is designed as a solid base that can be extended with more chapters, animations, localization, achievements, analytics, and production signing.

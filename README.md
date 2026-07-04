# Buraks Long Road

A native Android 2D story-platformer. No Unity, no external art packs, no manual sprites.
Everything is drawn in Java Canvas at runtime. Music and sound effects are generated programmatically.

## Build stability update

This version also fixes an Android resource compilation issue caused by an apostrophe in the `app_name` string resource. The launcher label is now `Buraks Long Road` to avoid AAPT string escape failures.

This version is pinned to a safer Android build chain:

- Android Gradle Plugin: `8.13.2`
- Gradle used in GitHub Actions: `8.13`
- JDK: `17`
- compileSdk: `36`
- targetSdk: `36`
- SDK package installed by CI: `platforms;android-36`
- Build Tools installed by CI: `35.0.0`

The previous `platforms;android-37` install step was removed because some GitHub SDK manager environments do not expose that package yet.

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
- Main menu, continue, pause menu, credits, game-over, victory screen
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
  KURULUM_TR.md
  .gitignore
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
2. Upload all files from this folder into the repository. Do not upload only the ZIP file.
3. Go to the repository's **Actions** tab.
4. If GitHub asks to enable workflows, enable them.
5. Open **Build Android APK and AAB**.
6. Press **Run workflow**.
7. Wait until the build finishes.
8. Open the finished workflow run.
9. Download the artifact named **BuraksLongRoad-builds**.
10. Extract the downloaded ZIP.
11. Install the APK from:

```text
app-debug.apk
```

The workflow also creates a release AAB here:

```text
app-release.aab
```

Important: The included release build is signed with the debug signing config so CI can produce a file without your private key.
For Google Play production, replace the signing config with your real upload key before uploading.

## GitHub Desktop upload checklist

The GitHub repository root must show these files directly:

```text
app/
.github/
build.gradle
settings.gradle
gradle.properties
README.md
KURULUM_TR.md
```

If the repository shows only `BuraksLongRoad.zip`, the upload is wrong. Extract the ZIP and upload the extracted project files.

## Local build method

Requirements:

- Android Studio or Android SDK
- JDK 17
- Gradle 8.13 compatible installation, or Android Studio's bundled Gradle support
- Android SDK platform 36
- Android SDK Build Tools 35.0.0 or newer compatible build tools

Build APK:

```bash
gradle --no-daemon :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build AAB:

```bash
gradle --no-daemon :app:bundleRelease
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
6. Open **Buraks Long Road**.

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
6. Prepare Play Console requirements: privacy policy, Data Safety form, content rating, store listing, screenshots, and production signing.

## Troubleshooting

### `Failed to find package 'platforms;android-37'`

Use this updated version. It no longer requests `android-37`; the workflow installs `platforms;android-36`.

### `Invalid unicode escape sequence in string` or `string/app_name does not contain a valid string resource`

Use this updated version. The launcher label in `app/src/main/res/values/strings.xml` no longer contains an apostrophe, and the workflow includes a resource sanity check before building.

### `No workflows are shown in GitHub Actions`

Make sure the repository contains this file:

```text
.github/workflows/build-android.yml
```

### `gradle: command not found` locally

Use GitHub Actions or open the project in Android Studio. The GitHub workflow installs Gradle automatically.

### APK installs but phone blocks it

Enable installation from unknown sources for the browser or file manager you used to open the APK.

## Known limitations

This is a complete lightweight mini-adventure, not a commercial-scale engine project.
The visuals and sounds are generated in code, so they are intentionally stylized rather than bitmap-heavy.
It is designed as a solid base that can be extended with more chapters, animations, localization, achievements, analytics, and production signing.

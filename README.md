# ShooterPro

A **2D top-down shooter** for Android built with **Kotlin and libGDX**.

The project explores building a game with minimal external assets by generating most elements directly from code.

---

## Overview

In ShooterPro you control a circular player character and fight waves of enemies in an arena.

The goal is to survive through **five waves of enemies** and defeat the final boss.

### Key idea

The project intentionally avoids traditional assets:

**Graphics**  
All visuals are drawn using `ShapeRenderer` (circles, rectangles, and lines).  
No image files are used.

**Audio**  
Sound effects are generated mathematically using PCM wave equations.  
No `.mp3` or `.wav` files are included.

---


## Requirements

- Android Studio Hedgehog or newer
- Android SDK 24+
- Kotlin 1.9+

---

## Running the Project
File → Open → Select ShooterPro folder

2. Wait for **Gradle Sync**

3. Run on device or emulator
Run → Run 'app'
> On first open Android Studio may ask you to **Trust the Project**.

---

## Project Structure
app/src/main/
├── java/com/rafeeqi/kids/shooter/
│ ├── MainActivity.kt
│ ├── ShooterActivity.kt
│ ├── ShooterGame.kt
│ ├── IntroScreen.kt
│ ├── ArenaScreen.kt
│ ├── SoundManager.kt
│ └── ArabicText.kt
└── res/


### Main components

**MainActivity.kt**  
Entry screen with play button.

**ShooterActivity.kt**  
Launches the libGDX game.

**ShooterGame.kt**  
Handles screens and save data.

**ArenaScreen.kt**  
Main gameplay loop and enemy logic.

**SoundManager.kt**  
Procedural sound generator.

**ArabicText.kt**  
Workaround for rendering Arabic text.

---

## Controls

| Action | Input |
|------|------|
Move | Dynamic joystick (left side)
Shoot | Tap right side of the screen
Dash | Bottom-left button
Switch weapon | Top-right button
Bomb | Bottom-right button

---

## Game Elements

**Weapons**

- Pistol (infinite)
- Shotgun
- Minigun
- Blade

**Enemies**

- Grunt
- Heavy
- Sniper
- Flanker
- Boss

**Pickups**

- Health
- Shield
- Ammo
- Bombs

**Progression**

- 5 waves with increasing difficulty

---

## Arabic Text in libGDX

libGDX does not properly support Arabic rendering (RTL + glyph shaping).  
To solve this, text is rendered using **Android Canvas** and then converted to a libGDX texture.

```kotlin
val paint = Paint().apply { textSize = size }
Canvas(bitmap).drawText(text, x, y, paint)

val texture = Texture(pixmapFromBitmap(bitmap))
```
---
## Dependencies


val gdxVersion = "1.12.1"
 

implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    // ✅ natives — مش runtimeOnly عشان extractNatives تشوفهم
natives("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-armeabi-v7a")
natives("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-arm64-v8a")
natives("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86")
natives("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86_64")

Native libraries are included for all Android architectures.

License

Native Libraries (libGDX)

ShooterPro uses libGDX native libraries (.so) for different Android architectures.

Instead of manually copying them, the project extracts the required natives automatically during the build.


Gradle Configuration
// LibGDX natives configuration
val natives: Configuration by configurations.creating
Extracting the Native Libraries

During build, Gradle extracts the .so files from the libGDX artifacts and places them inside the project.

tasks.register("extractNatives") {
    doLast {
        natives.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val classifier = artifact.classifier ?: return@forEach

            // convert "natives-arm64-v8a" → "arm64-v8a"
            val abiName = classifier.removePrefix("natives-")

            val dest = file("$projectDir/libs/$abiName")
            dest.mkdirs()

            copy {
                from(zipTree(artifact.file))
                into(dest)
                include("**/*.so")

                eachFile {
                    relativePath = RelativePath(true, name)
                }

                includeEmptyDirs = false
            }
        }
    }
}```
## Build Hook

The extraction task runs automatically before the Android build process.
```
afterEvaluate {
    tasks.matching { it.name.startsWith("merge") && it.name.contains("JniLibFolders") }
        .configureEach {
            dependsOn("extractNatives")
        }

    tasks.matching { it.name == "preBuild" }
        .configureEach {
            dependsOn("extractNatives")
        }
}
```
This ensures the correct native libraries are available for all supported ABIs during compilation.


## MIT License
Free to use and modify with attribution.

1. Open the project in **Android Studio**

File → Open → Select ShooterPro folder

Technical Highlights
- Procedural audio generation
- Code-only graphics using ShapeRenderer
- Custom Arabic text rendering workaround
- Dynamic joystick controls
- Native library extraction with Gradle

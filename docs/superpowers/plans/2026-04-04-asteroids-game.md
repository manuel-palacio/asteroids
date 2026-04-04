# Asteroids Game Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete neon-arcade Asteroids clone in libGDX/Kotlin for Android and desktop with full VFX polish.

**Architecture:** Screen-as-orchestrator. `World` owns all entities and systems; `GameScreen` coordinates update/render. Scene2D for HUD only. VFX layered over geometry via FBO bloom pass.

**Tech Stack:** Kotlin 1.9.22, libGDX 1.12.1, LWJGL3 (desktop), Android SDK 24+, JUnit 5, Gradle 8 Kotlin DSL.

---

## File Map

```
settings.gradle.kts
build.gradle.kts
gradle.properties
core/build.gradle.kts
desktop/build.gradle.kts
android/build.gradle.kts
android/AndroidManifest.xml
android/src/main/kotlin/com/asteroids/android/AndroidLauncher.kt
desktop/src/main/kotlin/com/asteroids/desktop/DesktopLauncher.kt
assets/shaders/bloom.vert
assets/shaders/bloomH.frag
assets/shaders/bloomV.frag

core/src/main/kotlin/com/asteroids/
  AsteroidsGame.kt
  util/Settings.kt
  util/WorldMath.kt
  game/entity/Ship.kt
  game/entity/Asteroid.kt          ← includes AsteroidFactory companion
  game/entity/Bullet.kt
  game/entity/Saucer.kt
  game/World.kt
  game/system/BulletPool.kt
  game/system/CollisionSystem.kt
  game/system/WaveSystem.kt
  input/GameInput.kt
  input/InputHandler.kt
  input/TouchControls.kt
  render/Starfield.kt
  render/ShipRenderer.kt
  render/AsteroidRenderer.kt
  render/SaucerRenderer.kt
  render/GameRenderer.kt
  render/HudRenderer.kt
  vfx/ScreenShake.kt
  vfx/ParticlePool.kt
  vfx/ThrustTrail.kt
  vfx/Explosion.kt
  vfx/VfxManager.kt
  vfx/BloomPass.kt
  screen/GameScreen.kt
  screen/MenuScreen.kt
  screen/GameOverScreen.kt

core/src/test/kotlin/com/asteroids/
  util/WorldMathTest.kt
  game/entity/AsteroidFactoryTest.kt
  game/system/BulletPoolTest.kt
  game/system/CollisionSystemTest.kt
  game/system/WaveSystemTest.kt
  vfx/ScreenShakeTest.kt
```

---

### Task 1: Gradle project scaffold

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `core/build.gradle.kts`
- Create: `desktop/build.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/AndroidManifest.xml`
- Create: `android/src/main/kotlin/com/asteroids/android/AndroidLauncher.kt`
- Create: `desktop/src/main/kotlin/com/asteroids/desktop/DesktopLauncher.kt`

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
rootProject.name = "asteroids"
include("core", "desktop", "android")
```

- [ ] **Step 2: Create `build.gradle.kts` (root)**

```kotlin
plugins {
    id("com.android.application") version "8.2.2" apply false
    kotlin("android") version "1.9.22" apply false
    kotlin("jvm") version "1.9.22" apply false
}
```

- [ ] **Step 3: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2g
android.useAndroidX=true
```

- [ ] **Step 4: Create `core/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")
}

val gdxVersion = "1.12.1"

dependencies {
    implementation("com.badlogic.gdx:gdx:$gdxVersion")
    implementation("com.badlogic.gdx:gdx-freetype:$gdxVersion")

    testImplementation("com.badlogic.gdx:gdx-backend-headless:$gdxVersion")
    testImplementation("com.badlogic.gdx:gdx-platform:$gdxVersion:natives-desktop")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 5: Create `desktop/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm")
    application
}

val gdxVersion = "1.12.1"

dependencies {
    implementation(project(":core"))
    implementation("com.badlogic.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogic.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogic.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.palacesoft.asteroids.desktop.DesktopLauncher")
}

tasks.jar {
    manifest { attributes["Main-Class"] = "com.palacesoft.asteroids.desktop.DesktopLauncher" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

- [ ] **Step 6: Create `android/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion = "1.12.1"
val natives: Configuration by configurations.creating

android {
    compileSdk = 34
    namespace = "com.palacesoft.asteroids"
    defaultConfig {
        applicationId = "com.palacesoft.asteroids"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    sourceSets["main"].apply {
        assets.srcDirs(rootProject.files("assets"))
        jniLibs.srcDir("libs")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogic.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogic.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogic.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogic.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogic.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogic.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogic.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        natives.files.forEach { jar ->
            val outputDir = file("libs/${jar.nameWithoutExtension.substringAfterLast('-')}")
            outputDir.mkdirs()
            copy { from(zipTree(jar)); into(outputDir); include("*.so") }
        }
    }
}
tasks.named("preBuild") { dependsOn("copyAndroidNatives") }
```

- [ ] **Step 7: Create `android/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-feature android:glEsVersion="0x00020000" android:required="true"/>
    <application
        android:allowBackup="false"
        android:hardwareAccelerated="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Asteroids">
        <activity
            android:name=".android.AndroidLauncher"
            android:exported="true"
            android:screenOrientation="landscape"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: Create `android/src/main/kotlin/com/asteroids/android/AndroidLauncher.kt`**

```kotlin
package com.palacesoft.asteroids.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.palacesoft.asteroids.AsteroidsGame

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        initialize(AsteroidsGame(), config)
    }
}
```

- [ ] **Step 9: Create `desktop/src/main/kotlin/com/asteroids/desktop/DesktopLauncher.kt`**

```kotlin
package com.palacesoft.asteroids.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.palacesoft.asteroids.AsteroidsGame

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Asteroids")
            setWindowedMode(1600, 900)
            setForegroundFPS(60)
            useVsync(true)
        }
        Lwjgl3Application(AsteroidsGame(), config)
    }
}
```

- [ ] **Step 10: Create stub `AsteroidsGame.kt` so it compiles**

```kotlin
package com.palacesoft.asteroids

import com.badlogic.gdx.Game

class AsteroidsGame : Game() {
    override fun create() {}
}
```

- [ ] **Step 11: Sync and verify the project builds**

```bash
./gradlew :core:compileKotlin :desktop:compileKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 12: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties core/ desktop/ android/
git commit -m "feat: add Gradle scaffold for core/desktop/android modules"
```

---

### Task 2: Utilities — Settings and WorldMath

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/util/Settings.kt`
- Create: `core/src/main/kotlin/com/asteroids/util/WorldMath.kt`
- Create: `core/src/test/kotlin/com/asteroids/util/WorldMathTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `core/src/test/kotlin/com/asteroids/util/WorldMathTest.kt`:

```kotlin
package com.palacesoft.asteroids.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorldMathTest {
    @Test fun `wrapCoord wraps below min`() =
        assertEquals(1990f, wrapCoord(-10f, 0f, 2000f), 0.01f)

    @Test fun `wrapCoord wraps above max`() =
        assertEquals(10f, wrapCoord(2010f, 0f, 2000f), 0.01f)

    @Test fun `wrapCoord unchanged within bounds`() =
        assertEquals(500f, wrapCoord(500f, 0f, 2000f), 0.01f)

    @Test fun `circlesOverlap true when overlapping`() =
        assertTrue(circlesOverlap(0f, 0f, 10f, 5f, 0f, 10f))

    @Test fun `circlesOverlap false when touching edge`() =
        assertFalse(circlesOverlap(0f, 0f, 5f, 10f, 0f, 5f))

    @Test fun `circlesOverlap false when apart`() =
        assertFalse(circlesOverlap(0f, 0f, 5f, 20f, 0f, 5f))
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.util.WorldMathTest"
```

Expected: FAIL — `wrapCoord` not found.

- [ ] **Step 3: Create `core/src/main/kotlin/com/asteroids/util/Settings.kt`**

```kotlin
package com.palacesoft.asteroids.util

object Settings {
    const val WORLD_WIDTH  = 1600f
    const val WORLD_HEIGHT = 900f
    var bloomEnabled = true
    var sfxEnabled   = true
}
```

- [ ] **Step 4: Create `core/src/main/kotlin/com/asteroids/util/WorldMath.kt`**

```kotlin
package com.palacesoft.asteroids.util

fun wrapCoord(value: Float, min: Float, max: Float): Float {
    val range = max - min
    return when {
        value < min -> value + range
        value > max -> value - range
        else -> value
    }
}

fun circlesOverlap(
    x1: Float, y1: Float, r1: Float,
    x2: Float, y2: Float, r2: Float
): Boolean {
    val dx = x2 - x1
    val dy = y2 - y1
    val radSum = r1 + r2
    return dx * dx + dy * dy < radSum * radSum
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.util.WorldMathTest"
```

Expected: PASS — 6 tests.

- [ ] **Step 6: Commit**

```bash
git add core/src/
git commit -m "feat: add Settings constants and WorldMath utilities"
```

---

### Task 3: Game entities

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/game/entity/Ship.kt`
- Create: `core/src/main/kotlin/com/asteroids/game/entity/Asteroid.kt`
- Create: `core/src/main/kotlin/com/asteroids/game/entity/Bullet.kt`
- Create: `core/src/main/kotlin/com/asteroids/game/entity/Saucer.kt`
- Create: `core/src/test/kotlin/com/asteroids/game/entity/AsteroidFactoryTest.kt`

- [ ] **Step 1: Write failing AsteroidFactory tests**

```kotlin
package com.palacesoft.asteroids.game.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsteroidFactoryTest {
    @Test fun `split LARGE yields 2 MEDIUM children`() {
        val a = AsteroidFactory.createRandom(100f, 100f, AsteroidSize.LARGE)
        val kids = AsteroidFactory.split(a)
        assertEquals(2, kids.size)
        assertTrue(kids.all { it.size == AsteroidSize.MEDIUM })
    }

    @Test fun `split MEDIUM yields 2 SMALL children`() {
        val a = AsteroidFactory.createRandom(100f, 100f, AsteroidSize.MEDIUM)
        val kids = AsteroidFactory.split(a)
        assertEquals(2, kids.size)
        assertTrue(kids.all { it.size == AsteroidSize.SMALL })
    }

    @Test fun `split SMALL yields no children`() {
        val a = AsteroidFactory.createRandom(100f, 100f, AsteroidSize.SMALL)
        assertTrue(AsteroidFactory.split(a).isEmpty())
    }

    @Test fun `createRandom velocity magnitude within expected range`() {
        val a = AsteroidFactory.createRandom(0f, 0f, AsteroidSize.LARGE)
        val speed = Math.sqrt((a.velX * a.velX + a.velY * a.velY).toDouble()).toFloat()
        assertTrue(speed in AsteroidSize.LARGE.minSpeed..AsteroidSize.LARGE.maxSpeed)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.entity.AsteroidFactoryTest"
```

Expected: FAIL — `AsteroidSize` not found.

- [ ] **Step 3: Create `Ship.kt`**

```kotlin
package com.palacesoft.asteroids.game.entity

import com.palacesoft.asteroids.util.Settings

class Ship {
    var x = Settings.WORLD_WIDTH / 2f
    var y = Settings.WORLD_HEIGHT / 2f
    var velX = 0f
    var velY = 0f
    var rotation = 90f          // degrees; 0=right, 90=up
    val radius = 12f
    var alive = true
    var invulnerableTimer = 0f
    var thrusting = false
    var visible = true
    var flickerAccum = 0f

    fun reset() {
        x = Settings.WORLD_WIDTH / 2f
        y = Settings.WORLD_HEIGHT / 2f
        velX = 0f; velY = 0f
        rotation = 90f
        alive = true
        invulnerableTimer = 3f
        visible = true
        flickerAccum = 0f
    }
}
```

- [ ] **Step 4: Create `Asteroid.kt`**

```kotlin
package com.palacesoft.asteroids.game.entity

import kotlin.math.*
import kotlin.random.Random

enum class AsteroidSize(
    val radius: Float,
    val minSpeed: Float,
    val maxSpeed: Float,
    val score: Int
) {
    LARGE(48f,  30f,  70f, 20),
    MEDIUM(24f, 60f, 110f, 50),
    SMALL(12f, 100f, 150f, 100)
}

class Asteroid(
    var x: Float,
    var y: Float,
    var velX: Float,
    var velY: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val size: AsteroidSize,
    val shape: FloatArray   // interleaved x,y offsets for polygon vertices
) {
    val radius get() = size.radius
    var alive = true
}

object AsteroidFactory {
    fun createRandom(x: Float, y: Float, size: AsteroidSize, rng: Random = Random): Asteroid {
        val angle   = rng.nextFloat() * 2f * PI.toFloat()
        val speed   = size.minSpeed + rng.nextFloat() * (size.maxSpeed - size.minSpeed)
        val velX    = cos(angle) * speed
        val velY    = sin(angle) * speed
        val rotSpd  = (rng.nextFloat() - 0.5f) * 120f
        val verts   = 8 + rng.nextInt(5)
        val shape   = generateShape(size.radius, verts, rng)
        return Asteroid(x, y, velX, velY, rng.nextFloat() * 360f, rotSpd, size, shape)
    }

    private fun generateShape(radius: Float, verts: Int, rng: Random): FloatArray {
        val pts = FloatArray(verts * 2)
        for (i in 0 until verts) {
            val a = (i.toFloat() / verts) * 2f * PI.toFloat()
            val r = radius * (0.75f + rng.nextFloat() * 0.25f)
            pts[i * 2]     = cos(a) * r
            pts[i * 2 + 1] = sin(a) * r
        }
        return pts
    }

    fun split(asteroid: Asteroid, rng: Random = Random): List<Asteroid> = when (asteroid.size) {
        AsteroidSize.LARGE  -> List(2) { createRandom(asteroid.x, asteroid.y, AsteroidSize.MEDIUM, rng) }
        AsteroidSize.MEDIUM -> List(2) { createRandom(asteroid.x, asteroid.y, AsteroidSize.SMALL, rng) }
        AsteroidSize.SMALL  -> emptyList()
    }
}
```

- [ ] **Step 5: Create `Bullet.kt`**

```kotlin
package com.palacesoft.asteroids.game.entity

import com.palacesoft.asteroids.util.Settings

class Bullet {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var alive = false
    var distanceTravelled = 0f
    var fromPlayer = true
    val radius = 3f

    companion object {
        const val SPEED        = 650f
        val MAX_DISTANCE       = Settings.WORLD_WIDTH * 0.8f
    }
}
```

- [ ] **Step 6: Create `Saucer.kt`**

```kotlin
package com.palacesoft.asteroids.game.entity

enum class SaucerSize { LARGE, SMALL }

class Saucer(val size: SaucerSize) {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var alive = false
    var shootTimer = 0f
    val radius get() = if (size == SaucerSize.LARGE) 22f else 11f

    companion object {
        const val SHOOT_INTERVAL = 2f
        const val SPEED          = 130f
    }
}
```

- [ ] **Step 7: Run tests**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.entity.AsteroidFactoryTest"
```

Expected: PASS — 4 tests.

- [ ] **Step 8: Commit**

```bash
git add core/src/
git commit -m "feat: add game entity classes and AsteroidFactory"
```

---

### Task 4: BulletPool

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/game/system/BulletPool.kt`
- Create: `core/src/test/kotlin/com/asteroids/game/system/BulletPoolTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.entity.Bullet
import com.palacesoft.asteroids.game.entity.Ship
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BulletPoolTest {
    private lateinit var pool: BulletPool
    private val bullets = mutableListOf<Bullet>()
    private val ship = Ship()

    @BeforeEach fun setup() { pool = BulletPool(); bullets.clear() }

    @Test fun `acquire adds a live bullet`() {
        pool.acquire(ship, bullets)
        assertEquals(1, bullets.count { it.alive })
    }

    @Test fun `cannot exceed 4 active bullets`() {
        repeat(10) { pool.acquire(ship, bullets) }
        assertEquals(4, bullets.count { it.alive })
    }

    @Test fun `dead bullet is recycled on next acquire`() {
        pool.acquire(ship, bullets)
        bullets.first { it.alive }.alive = false
        val sizeBefore = bullets.size
        pool.acquire(ship, bullets)
        assertEquals(sizeBefore, bullets.size) // no new object added, recycled slot used
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.BulletPoolTest"
```

Expected: FAIL — `BulletPool` not found.

- [ ] **Step 3: Create `BulletPool.kt`**

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.entity.Bullet
import com.palacesoft.asteroids.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin

class BulletPool {
    private val pool = Array(20) { Bullet() }
    private val maxActive = 4

    fun acquire(ship: Ship, bullets: MutableList<Bullet>) {
        val active = bullets.count { it.alive }
        if (active >= maxActive) return

        val bullet = pool.firstOrNull { !it.alive } ?: return
        val rad = Math.toRadians(ship.rotation.toDouble())
        bullet.x = ship.x + cos(rad).toFloat() * ship.radius
        bullet.y = ship.y + sin(rad).toFloat() * ship.radius
        bullet.velX = ship.velX + cos(rad).toFloat() * Bullet.SPEED
        bullet.velY = ship.velY + sin(rad).toFloat() * Bullet.SPEED
        bullet.alive = true
        bullet.distanceTravelled = 0f
        bullet.fromPlayer = true
        if (!bullets.contains(bullet)) bullets.add(bullet)
    }

    fun acquireForSaucer(saucer: com.palacesoft.asteroids.game.entity.Saucer,
                         targetX: Float, targetY: Float,
                         bullets: MutableList<Bullet>,
                         spreadDeg: Float = 0f) {
        val bullet = pool.firstOrNull { !it.alive } ?: return
        val angle = Math.atan2((targetY - saucer.y).toDouble(),
                               (targetX - saucer.x).toDouble()).toFloat()
        val spread = Math.toRadians(((Math.random() - 0.5) * spreadDeg * 2).toDouble()).toFloat()
        val finalAngle = angle + spread
        bullet.x = saucer.x; bullet.y = saucer.y
        bullet.velX = cos(finalAngle) * Bullet.SPEED
        bullet.velY = sin(finalAngle) * Bullet.SPEED
        bullet.alive = true
        bullet.distanceTravelled = 0f
        bullet.fromPlayer = false
        if (!bullets.contains(bullet)) bullets.add(bullet)
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.BulletPoolTest"
```

Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add core/src/
git commit -m "feat: add BulletPool with 4-bullet cap and recycling"
```

---

### Task 5: CollisionSystem

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/game/system/CollisionSystem.kt`
- Create: `core/src/test/kotlin/com/asteroids/game/system/CollisionSystemTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.entity.*
import com.palacesoft.asteroids.game.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CollisionSystemTest {
    private fun makeWorld() = World()

    @Test fun `player bullet hitting asteroid marks both dead and scores`() {
        val world = makeWorld()
        val ast = AsteroidFactory.createRandom(400f, 400f, AsteroidSize.LARGE)
        world.asteroids.add(ast)
        val bullet = Bullet().apply {
            x = ast.x; y = ast.y
            alive = true; fromPlayer = true
        }
        world.bullets.add(bullet)
        world.collisionSystem.update()
        assertFalse(bullet.alive)
        assertFalse(ast.alive)
        assertTrue(world.score > 0)
    }

    @Test fun `invulnerable ship not hit by asteroid`() {
        val world = makeWorld()
        world.ship.invulnerableTimer = 1f
        val ast = AsteroidFactory.createRandom(world.ship.x, world.ship.y, AsteroidSize.LARGE)
        world.asteroids.add(ast)
        world.collisionSystem.update()
        assertTrue(world.ship.alive)
    }

    @Test fun `vulnerable ship hit by asteroid dies`() {
        val world = makeWorld()
        world.ship.invulnerableTimer = 0f
        val ast = AsteroidFactory.createRandom(world.ship.x, world.ship.y, AsteroidSize.LARGE)
        world.asteroids.add(ast)
        world.collisionSystem.update()
        assertFalse(world.ship.alive)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.CollisionSystemTest"
```

Expected: FAIL — `World` not found.

- [ ] **Step 3: Create a minimal `World.kt` stub (will be expanded in Task 7)**

```kotlin
package com.palacesoft.asteroids.game

import com.palacesoft.asteroids.game.entity.*
import com.palacesoft.asteroids.game.system.BulletPool
import com.palacesoft.asteroids.game.system.CollisionSystem
import com.palacesoft.asteroids.game.system.WaveSystem
import com.palacesoft.asteroids.input.GameInput

class World {
    val ship = Ship()
    val asteroids = mutableListOf<Asteroid>()
    val bullets   = mutableListOf<Bullet>()
    val saucers   = mutableListOf<Saucer>()
    var score = 0
    var lives = 3
    var wave  = 0

    val input            = GameInput()
    val bulletPool       = BulletPool()
    val collisionSystem  = CollisionSystem(this)
    val waveSystem       = WaveSystem(this)
    var vfx: Any? = null  // replaced with VfxManager in Task 13
}
```

- [ ] **Step 4: Create `GameInput.kt` stub (needed by World)**

```kotlin
package com.palacesoft.asteroids.input

data class GameInput(
    var rotateLeft: Boolean  = false,
    var rotateRight: Boolean = false,
    var thrust: Boolean      = false,
    var fire: Boolean        = false,
    var hyperspace: Boolean  = false
)
```

- [ ] **Step 5: Create a minimal `WaveSystem.kt` stub**

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.World

class WaveSystem(private val world: World) {
    fun update(delta: Float) {}
    fun start() {}
}
```

- [ ] **Step 6: Create `CollisionSystem.kt`**

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.game.entity.AsteroidFactory
import com.palacesoft.asteroids.util.circlesOverlap

class CollisionSystem(private val world: World) {

    fun update() {
        checkBulletsVsAsteroids()
        checkBulletsVsSaucers()
        checkShipVsAsteroids()
        checkShipVsSaucers()
        checkShipVsSaucerBullets()
    }

    private fun checkBulletsVsAsteroids() {
        for (bullet in world.bullets) {
            if (!bullet.alive || !bullet.fromPlayer) continue
            for (ast in world.asteroids) {
                if (!ast.alive) continue
                if (circlesOverlap(bullet.x, bullet.y, bullet.radius,
                                   ast.x, ast.y, ast.radius)) {
                    bullet.alive = false
                    ast.alive = false
                    world.score += ast.size.score
                    val children = AsteroidFactory.split(ast)
                    world.asteroids.addAll(children)
                    // TODO Task 13: world.vfx?.spawnExplosion(ast.x, ast.y, ast.size)
                    break
                }
            }
        }
    }

    private fun checkBulletsVsSaucers() {
        for (bullet in world.bullets) {
            if (!bullet.alive || !bullet.fromPlayer) continue
            for (saucer in world.saucers) {
                if (!saucer.alive) continue
                if (circlesOverlap(bullet.x, bullet.y, bullet.radius,
                                   saucer.x, saucer.y, saucer.radius)) {
                    bullet.alive = false
                    saucer.alive = false
                    world.score += 1000
                    break
                }
            }
        }
    }

    private fun checkShipVsAsteroids() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (ast in world.asteroids) {
            if (!ast.alive) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius,
                               ast.x, ast.y, ast.radius)) {
                world.ship.alive = false
                return
            }
        }
    }

    private fun checkShipVsSaucers() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (saucer in world.saucers) {
            if (!saucer.alive) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius,
                               saucer.x, saucer.y, saucer.radius)) {
                world.ship.alive = false
                return
            }
        }
    }

    private fun checkShipVsSaucerBullets() {
        if (!world.ship.alive || world.ship.invulnerableTimer > 0f) return
        for (bullet in world.bullets) {
            if (!bullet.alive || bullet.fromPlayer) continue
            if (circlesOverlap(world.ship.x, world.ship.y, world.ship.radius,
                               bullet.x, bullet.y, bullet.radius)) {
                bullet.alive = false
                world.ship.alive = false
                return
            }
        }
    }
}
```

- [ ] **Step 7: Run collision tests**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.CollisionSystemTest"
```

Expected: PASS — 3 tests.

- [ ] **Step 8: Commit**

```bash
git add core/src/
git commit -m "feat: add CollisionSystem with bullet/asteroid/ship/saucer checks"
```

---

### Task 6: WaveSystem (full implementation)

**Files:**
- Modify: `core/src/main/kotlin/com/asteroids/game/system/WaveSystem.kt`
- Create: `core/src/test/kotlin/com/asteroids/game/system/WaveSystemTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.World
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WaveSystemTest {
    @Test fun `wave 1 spawns 4 asteroids`() {
        val world = World().apply { wave = 1 }
        world.waveSystem.spawnWave()
        assertEquals(4, world.asteroids.size)
    }

    @Test fun `wave 3 spawns 8 asteroids`() {
        val world = World().apply { wave = 3 }
        world.waveSystem.spawnWave()
        assertEquals(8, world.asteroids.size)
    }

    @Test fun `asteroid count is wave times 2 plus 2`() {
        for (w in 1..5) {
            val world = World().apply { wave = w }
            world.waveSystem.spawnWave()
            assertEquals(w * 2 + 2, world.asteroids.size, "Failed for wave $w")
        }
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.WaveSystemTest"
```

Expected: FAIL — `spawnWave` not found.

- [ ] **Step 3: Replace WaveSystem with full implementation**

```kotlin
package com.palacesoft.asteroids.game.system

import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.game.entity.AsteroidFactory
import com.palacesoft.asteroids.game.entity.AsteroidSize
import com.palacesoft.asteroids.util.Settings
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class WaveSystem(private val world: World) {
    private var betweenWaveTimer = 0f
    private var saucerTimer      = 0f
    private val SAUCER_INTERVAL  = 25f
    private val BETWEEN_WAVE_GAP = 3f

    fun update(delta: Float) {
        if (world.ship.alive.not() && world.lives > 0) {
            handleRespawn(delta)
            return
        }
        val liveAsteroids = world.asteroids.count { it.alive }
        val liveSaucers   = world.saucers.count { it.alive }

        if (liveAsteroids == 0 && liveSaucers == 0) {
            betweenWaveTimer += delta
            if (betweenWaveTimer >= BETWEEN_WAVE_GAP) {
                betweenWaveTimer = 0f
                world.wave++
                spawnWave()
            }
        }

        saucerTimer += delta
        if (saucerTimer >= SAUCER_INTERVAL) {
            saucerTimer = 0f
            spawnSaucer()
        }

        updateSaucers(delta)
    }

    fun spawnWave() {
        val count = world.wave * 2 + 2
        repeat(count) {
            val (x, y) = randomEdgePosition()
            world.asteroids.add(AsteroidFactory.createRandom(x, y, AsteroidSize.LARGE))
        }
    }

    private fun randomEdgePosition(): Pair<Float, Float> {
        val edge = Random.nextInt(4)
        return when (edge) {
            0 -> Pair(Random.nextFloat() * Settings.WORLD_WIDTH, 0f)
            1 -> Pair(Random.nextFloat() * Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT)
            2 -> Pair(0f, Random.nextFloat() * Settings.WORLD_HEIGHT)
            else -> Pair(Settings.WORLD_WIDTH, Random.nextFloat() * Settings.WORLD_HEIGHT)
        }
    }

    private fun spawnSaucer() {
        val saucer = world.saucers.firstOrNull { !it.alive } ?: return
        val fromLeft = Random.nextBoolean()
        saucer.x = if (fromLeft) 0f else Settings.WORLD_WIDTH
        saucer.y = Random.nextFloat() * Settings.WORLD_HEIGHT
        saucer.velX = (if (fromLeft) 1f else -1f) * com.palacesoft.asteroids.game.entity.Saucer.SPEED
        saucer.velY = 0f
        saucer.alive = true
        saucer.shootTimer = 0f
    }

    private fun updateSaucers(delta: Float) {
        for (saucer in world.saucers) {
            if (!saucer.alive) continue
            saucer.x += saucer.velX * delta
            saucer.y += saucer.velY * delta
            // Drift slightly vertically
            saucer.velY = sin(saucer.x * 0.005f) * 60f

            // Shoot
            saucer.shootTimer += delta
            if (saucer.shootTimer >= com.palacesoft.asteroids.game.entity.Saucer.SHOOT_INTERVAL) {
                saucer.shootTimer = 0f
                val spread = if (saucer.size == com.palacesoft.asteroids.game.entity.SaucerSize.LARGE) 360f else 15f
                world.bulletPool.acquireForSaucer(saucer, world.ship.x, world.ship.y,
                                                  world.bullets, spread)
            }
            // Remove if off-screen
            if (saucer.x < -60f || saucer.x > Settings.WORLD_WIDTH + 60f) saucer.alive = false
        }
    }

    private fun handleRespawn(delta: Float) {
        betweenWaveTimer += delta
        if (betweenWaveTimer >= 2f) {
            betweenWaveTimer = 0f
            world.lives--
            world.ship.reset()
        }
    }

    fun start() {
        world.wave = 1
        spawnWave()
        world.ship.reset()
        // Pre-allocate 2 saucers
        repeat(2) { world.saucers.add(com.palacesoft.asteroids.game.entity.Saucer(
            com.palacesoft.asteroids.game.entity.SaucerSize.LARGE)) }
        saucerTimer = 0f
        betweenWaveTimer = 0f
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.game.system.WaveSystemTest"
```

Expected: PASS — 3 tests.

- [ ] **Step 5: Run all tests**

```bash
./gradlew :core:test
```

Expected: PASS — all tests.

- [ ] **Step 6: Commit**

```bash
git add core/src/
git commit -m "feat: add WaveSystem with wave progression and saucer spawning"
```

---

### Task 7: World — full update loop

**Files:**
- Modify: `core/src/main/kotlin/com/asteroids/game/World.kt`

- [ ] **Step 1: Replace World stub with full implementation**

```kotlin
package com.palacesoft.asteroids.game

import com.palacesoft.asteroids.game.entity.*
import com.palacesoft.asteroids.game.system.BulletPool
import com.palacesoft.asteroids.game.system.CollisionSystem
import com.palacesoft.asteroids.game.system.WaveSystem
import com.palacesoft.asteroids.input.GameInput
import com.palacesoft.asteroids.util.Settings
import com.palacesoft.asteroids.util.wrapCoord
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class World {
    val ship        = Ship()
    val asteroids   = mutableListOf<Asteroid>()
    val bullets     = mutableListOf<Bullet>()
    val saucers     = mutableListOf<Saucer>()
    var score       = 0
    var lives       = 3
    var wave        = 0
    var gameOver    = false

    val input            = GameInput()
    val bulletPool       = BulletPool()
    val collisionSystem  = CollisionSystem(this)
    val waveSystem       = WaveSystem(this)
    var vfx: Any? = null  // replaced with VfxManager in Task 13

    private val ROTATE_SPEED = 200f  // deg/s
    private val THRUST_FORCE = 400f  // units/s²
    private val DRAG         = 0.985f
    private val MAX_SPEED    = 500f
    private val FIRE_RATE    = 0.22f // seconds between shots
    private var fireCooldown = 0f

    fun start() {
        waveSystem.start()
    }

    fun update(delta: Float) {
        if (gameOver) return
        updateShip(delta)
        updateBullets(delta)
        asteroids.forEach { if (it.alive) { it.x += it.velX * delta; it.y += it.velY * delta
            it.rotation += it.rotSpeed * delta
            it.x = wrapCoord(it.x, 0f, Settings.WORLD_WIDTH)
            it.y = wrapCoord(it.y, 0f, Settings.WORLD_HEIGHT) } }
        asteroids.removeAll { !it.alive }
        bullets.removeAll   { !it.alive }
        collisionSystem.update()
        waveSystem.update(delta)
        if (lives <= 0) gameOver = true
    }

    private fun updateShip(delta: Float) {
        if (!ship.alive) return
        // Invulnerability flicker
        if (ship.invulnerableTimer > 0f) {
            ship.invulnerableTimer -= delta
            ship.flickerAccum += delta
            ship.visible = (ship.flickerAccum % 0.2f) < 0.1f
        } else {
            ship.visible = true
            ship.flickerAccum = 0f
        }
        // Rotation
        if (input.rotateLeft)  ship.rotation += ROTATE_SPEED * delta
        if (input.rotateRight) ship.rotation -= ROTATE_SPEED * delta
        // Thrust
        ship.thrusting = input.thrust
        if (input.thrust) {
            val rad = Math.toRadians(ship.rotation.toDouble())
            ship.velX += cos(rad).toFloat() * THRUST_FORCE * delta
            ship.velY += sin(rad).toFloat() * THRUST_FORCE * delta
        }
        // Drag + clamp
        ship.velX *= DRAG; ship.velY *= DRAG
        val spd = sqrt(ship.velX * ship.velX + ship.velY * ship.velY)
        if (spd > MAX_SPEED) { ship.velX = ship.velX / spd * MAX_SPEED; ship.velY = ship.velY / spd * MAX_SPEED }
        // Move + wrap
        ship.x += ship.velX * delta; ship.y += ship.velY * delta
        ship.x = wrapCoord(ship.x, 0f, Settings.WORLD_WIDTH)
        ship.y = wrapCoord(ship.y, 0f, Settings.WORLD_HEIGHT)
        // Fire
        fireCooldown -= delta
        if (input.fire && fireCooldown <= 0f) {
            bulletPool.acquire(ship, bullets)
            fireCooldown = FIRE_RATE
        }
        // Hyperspace
        if (input.hyperspace) {
            ship.x = (Math.random() * Settings.WORLD_WIDTH).toFloat()
            ship.y = (Math.random() * Settings.WORLD_HEIGHT).toFloat()
            ship.invulnerableTimer = 1.5f
            input.hyperspace = false
        }
    }

    private fun updateBullets(delta: Float) {
        for (b in bullets) {
            if (!b.alive) continue
            b.x += b.velX * delta; b.y += b.velY * delta
            b.x = wrapCoord(b.x, 0f, Settings.WORLD_WIDTH)
            b.y = wrapCoord(b.y, 0f, Settings.WORLD_HEIGHT)
            val moved = sqrt(b.velX * b.velX + b.velY * b.velY) * delta
            b.distanceTravelled += moved
            if (b.distanceTravelled > Bullet.MAX_DISTANCE) b.alive = false
        }
    }
}
```

- [ ] **Step 2: Run all tests to confirm nothing broken**

```bash
./gradlew :core:test
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/com/asteroids/game/World.kt
git commit -m "feat: add full World update loop with ship physics and bullet movement"
```

---

### Task 8: Input system

**Files:**
- Modify: `core/src/main/kotlin/com/asteroids/input/GameInput.kt` (already exists)
- Create: `core/src/main/kotlin/com/asteroids/input/InputHandler.kt`
- Create: `core/src/main/kotlin/com/asteroids/input/TouchControls.kt`

- [ ] **Step 1: Create `InputHandler.kt`**

```kotlin
package com.palacesoft.asteroids.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys

class InputHandler(private val gameInput: GameInput) {
    private val touch = TouchControls(gameInput)

    fun poll() {
        // Keyboard — always active (desktop dev)
        gameInput.rotateLeft  = Gdx.input.isKeyPressed(Keys.LEFT)  || Gdx.input.isKeyPressed(Keys.A)
        gameInput.rotateRight = Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)
        gameInput.thrust      = Gdx.input.isKeyPressed(Keys.UP)    || Gdx.input.isKeyPressed(Keys.W)
        gameInput.fire        = Gdx.input.isKeyPressed(Keys.SPACE)

        // Touch overrides (OR'd so both can work simultaneously)
        touch.poll(gameInput)
    }
}
```

- [ ] **Step 2: Create `TouchControls.kt`**

```kotlin
package com.palacesoft.asteroids.input

import com.badlogic.gdx.Gdx
import com.palacesoft.asteroids.util.Settings

class TouchControls(private val gameInput: GameInput) {
    // Left half = rotate; right lower = fire; right upper = hyperspace
    private val midX get() = Settings.WORLD_WIDTH / 2f
    private val midY get() = Settings.WORLD_HEIGHT / 2f

    // Joystick drag state
    var joystickAnchorX = 0f
    var joystickAnchorY = 0f
    var joystickActive  = false
    var joystickCurrX   = 0f
    var joystickCurrY   = 0f
    private var hyperspacePointer = -1

    fun poll(input: GameInput) {
        if (!Gdx.input.isTouched()) {
            joystickActive = false
            input.thrust      = input.thrust      || false
            input.rotateLeft  = input.rotateLeft  || false
            input.rotateRight = input.rotateRight || false
            input.fire        = input.fire        || false
            return
        }

        // Scale touch coords to world coords
        val scaleX = Settings.WORLD_WIDTH  / Gdx.graphics.width
        val scaleY = Settings.WORLD_HEIGHT / Gdx.graphics.height

        for (i in 0..4) {
            if (!Gdx.input.isTouched(i)) continue
            val tx = Gdx.input.getX(i) * scaleX
            val ty = (Gdx.graphics.height - Gdx.input.getY(i)) * scaleY  // flip Y

            if (tx < midX) {
                // Left zone — virtual joystick
                if (!joystickActive) {
                    joystickAnchorX = tx; joystickAnchorY = ty
                    joystickActive = true
                }
                joystickCurrX = tx; joystickCurrY = ty
                val dx = joystickCurrX - joystickAnchorX
                val deadzone = 20f
                if (dx < -deadzone) input.rotateLeft  = true
                if (dx >  deadzone) input.rotateRight = true
                // vertical drag → thrust
                val dy = joystickCurrY - joystickAnchorY
                if (dy > deadzone) input.thrust = true
            } else {
                // Right zone — fire (lower) or hyperspace (upper)
                if (ty < midY) {
                    input.fire = true
                } else {
                    if (hyperspacePointer == -1) {
                        hyperspacePointer = i
                        input.hyperspace = true
                    }
                }
            }
        }
        if (!Gdx.input.isTouched()) hyperspacePointer = -1
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/src/
git commit -m "feat: add InputHandler and TouchControls for keyboard and virtual joystick"
```

---

### Task 9: Rendering — Starfield, geometry, GameRenderer

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/render/Starfield.kt`
- Create: `core/src/main/kotlin/com/asteroids/render/ShipRenderer.kt`
- Create: `core/src/main/kotlin/com/asteroids/render/AsteroidRenderer.kt`
- Create: `core/src/main/kotlin/com/asteroids/render/SaucerRenderer.kt`
- Create: `core/src/main/kotlin/com/asteroids/render/GameRenderer.kt`
- Modify: `core/src/main/kotlin/com/asteroids/AsteroidsGame.kt`

- [ ] **Step 1: Create `Starfield.kt`**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.Color
import com.palacesoft.asteroids.util.Settings
import kotlin.random.Random

class Starfield {
    data class Star(var x: Float, var y: Float, val layer: Int, var brightness: Float, val twinkleSpeed: Float)

    private val stars = List(160) {
        Star(
            x = Random.nextFloat() * Settings.WORLD_WIDTH,
            y = Random.nextFloat() * Settings.WORLD_HEIGHT,
            layer = Random.nextInt(2),
            brightness = 0.3f + Random.nextFloat() * 0.7f,
            twinkleSpeed = 0.5f + Random.nextFloat() * 2f
        )
    }
    private var time = 0f

    fun update(delta: Float, shipVelX: Float, shipVelY: Float) {
        time += delta
        val drift0 = 0.02f; val drift1 = 0.05f
        stars.forEach { s ->
            val drift = if (s.layer == 0) drift0 else drift1
            s.x -= shipVelX * drift * delta
            s.y -= shipVelY * drift * delta
            if (s.x < 0) s.x += Settings.WORLD_WIDTH
            if (s.x > Settings.WORLD_WIDTH) s.x -= Settings.WORLD_WIDTH
            if (s.y < 0) s.y += Settings.WORLD_HEIGHT
            if (s.y > Settings.WORLD_HEIGHT) s.y -= Settings.WORLD_HEIGHT
        }
    }

    fun render(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Point)
        stars.forEach { s ->
            val twinkle = 0.5f + 0.5f * Math.sin((time * s.twinkleSpeed).toDouble()).toFloat()
            val b = s.brightness * (0.6f + 0.4f * twinkle)
            val size = if (s.layer == 1) 1f else 0.7f
            sr.color = Color(b * size, b * size, b, 1f)
            sr.point(s.x, s.y, 0f)
        }
        sr.end()
    }
}
```

- [ ] **Step 2: Create `ShipRenderer.kt`**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin

class ShipRenderer {
    private val SHIP_COLOR   = Color(0f, 1f, 0.9f, 1f)  // cyan
    private val THRUST_COLOR = Color(0.4f, 0.7f, 1f, 1f) // blue-white
    private var thrustFlicker = 0f

    fun update(delta: Float) { thrustFlicker += delta }

    fun render(sr: ShapeRenderer, ship: Ship) {
        if (!ship.visible) return
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val cos = cos(rad); val sin = sin(rad)
        val r = ship.radius

        // Triangle vertices in local space, rotated
        val nx = cos * r;          val ny = sin * r
        val lx = cos(-rad + 2.5f) * r * 0.75f; val ly = sin(-rad + 2.5f) * r * 0.75f
        val rx = cos(-rad - 2.5f) * r * 0.75f; val ry = sin(-rad - 2.5f) * r * 0.75f

        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = SHIP_COLOR
        sr.line(ship.x + nx, ship.y + ny, ship.x + lx, ship.y + ly)
        sr.line(ship.x + lx, ship.y + ly, ship.x - cos * r * 0.5f, ship.y - sin * r * 0.5f)
        sr.line(ship.x - cos * r * 0.5f, ship.y - sin * r * 0.5f, ship.x + rx, ship.y + ry)
        sr.line(ship.x + rx, ship.y + ry, ship.x + nx, ship.y + ny)

        // Engine flame
        if (ship.thrusting && (thrustFlicker % 0.1f) < 0.05f) {
            sr.color = THRUST_COLOR
            val flameLen = r * (1.2f + (Math.random() * 0.5f).toFloat())
            sr.line(ship.x + lx, ship.y + ly,
                    ship.x - cos * flameLen, ship.y - sin * flameLen)
            sr.line(ship.x - cos * flameLen, ship.y - sin * flameLen,
                    ship.x + rx, ship.y + ry)
        }
        sr.end()
    }
}
```

- [ ] **Step 3: Create `AsteroidRenderer.kt`**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.entity.Asteroid
import kotlin.math.cos
import kotlin.math.sin

class AsteroidRenderer {
    private val COLOR = Color(0.9f, 0.9f, 0.9f, 1f)

    fun render(sr: ShapeRenderer, asteroids: List<Asteroid>) {
        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = COLOR
        for (ast in asteroids) {
            if (!ast.alive) continue
            val rad = Math.toRadians(ast.rotation.toDouble()).toFloat()
            val cosR = cos(rad); val sinR = sin(rad)
            val verts = ast.shape.size / 2
            for (i in 0 until verts) {
                val x0 = ast.shape[i * 2];     val y0 = ast.shape[i * 2 + 1]
                val x1 = ast.shape[((i + 1) % verts) * 2]
                val y1 = ast.shape[((i + 1) % verts) * 2 + 1]
                val rx0 = cosR * x0 - sinR * y0 + ast.x
                val ry0 = sinR * x0 + cosR * y0 + ast.y
                val rx1 = cosR * x1 - sinR * y1 + ast.x
                val ry1 = sinR * x1 + cosR * y1 + ast.y
                sr.line(rx0, ry0, rx1, ry1)
            }
        }
        sr.end()
    }
}
```

- [ ] **Step 4: Create `SaucerRenderer.kt`**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.entity.Saucer

class SaucerRenderer {
    private val COLOR = Color(1f, 0.2f, 1f, 1f)  // magenta

    fun render(sr: ShapeRenderer, saucers: List<Saucer>) {
        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = COLOR
        for (s in saucers) {
            if (!s.alive) continue
            val r = s.radius
            // Top dome arc
            sr.arc(s.x, s.y + r * 0.2f, r * 0.5f, 0f, 180f, 12)
            // Bottom body
            sr.line(s.x - r, s.y, s.x + r, s.y)
            sr.arc(s.x, s.y, r, 180f, 180f, 12)
            // Dome outline
            sr.line(s.x - r * 0.5f, s.y + r * 0.2f, s.x + r * 0.5f, s.y + r * 0.2f)
        }
        sr.end()
    }
}
```

- [ ] **Step 5: Create `GameRenderer.kt`**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.vfx.BloomPass
import com.palacesoft.asteroids.vfx.VfxManager

class GameRenderer(
    private val camera: OrthographicCamera,
    private val batch: SpriteBatch,
    private val sr: ShapeRenderer
) {
    private val starfield      = Starfield()
    private val shipRenderer   = ShipRenderer()
    private val asteroidRend   = AsteroidRenderer()
    private val saucerRend     = SaucerRenderer()
    private var bloomPass: BloomPass? = null   // set in Task 14
    var hudRenderer: HudRenderer? = null       // set in Task 15

    fun init(bloomPass: BloomPass) { this.bloomPass = bloomPass }

    fun update(delta: Float, world: World) {
        starfield.update(delta, world.ship.velX, world.ship.velY)
        shipRenderer.update(delta)
    }

    fun render(world: World, shakeOffX: Float, shakeOffY: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Camera with shake
        camera.position.set(
            com.palacesoft.asteroids.util.Settings.WORLD_WIDTH / 2f + shakeOffX,
            com.palacesoft.asteroids.util.Settings.WORLD_HEIGHT / 2f + shakeOffY, 0f)
        camera.update()
        sr.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        // Pass 1: Background
        starfield.render(sr)

        // Pass 2: Geometry (non-emissive)
        asteroidRend.render(sr, world.asteroids)
        if (world.ship.visible) shipRenderer.render(sr, world.ship)

        // Pass 3: Emissive geometry captured for bloom (Task 14)
        bloomPass?.let { bp ->
            bp.beginCapture()
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            sr.projectionMatrix = camera.combined
            // Render emissive objects (bullets, saucer, engine)
            sr.begin(ShapeRenderer.ShapeType.Filled)
            sr.color = com.badlogic.gdx.graphics.Color(1f, 1f, 0.2f, 1f)
            for (b in world.bullets) {
                if (b.alive) sr.circle(b.x, b.y, b.radius)
            }
            sr.end()
            if (world.ship.visible && world.ship.thrusting) shipRenderer.render(sr, world.ship)
            saucerRend.render(sr, world.saucers)
            bp.endCapture()
            bp.render(batch)
        } ?: run {
            // No bloom — render bullets normally
            sr.begin(ShapeRenderer.ShapeType.Filled)
            sr.color = com.badlogic.gdx.graphics.Color(1f, 1f, 0.2f, 1f)
            for (b in world.bullets) { if (b.alive) sr.circle(b.x, b.y, b.radius) }
            sr.end()
            saucerRend.render(sr, world.saucers)
        }

        // Pass 4: HUD (no shake)
        camera.position.set(
            com.palacesoft.asteroids.util.Settings.WORLD_WIDTH / 2f,
            com.palacesoft.asteroids.util.Settings.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        hudRenderer?.render(world)
    }
}
```

- [ ] **Step 6: Replace `AsteroidsGame.kt` with full version**

```kotlin
package com.palacesoft.asteroids

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.screen.MenuScreen
import com.palacesoft.asteroids.util.Settings

class AsteroidsGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var sr: ShapeRenderer
    lateinit var camera: OrthographicCamera

    override fun create() {
        batch  = SpriteBatch()
        sr     = ShapeRenderer()
        camera = OrthographicCamera(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT)
        camera.position.set(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        sr.dispose()
        screen?.dispose()
    }
}
```

- [ ] **Step 7: Verify compilation**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add core/src/
git commit -m "feat: add Starfield, geometry renderers, and GameRenderer pipeline"
```

---

### Task 10: GameScreen — first playable build

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/screen/GameScreen.kt`
- Create stub: `core/src/main/kotlin/com/asteroids/screen/MenuScreen.kt`
- Create stub: `core/src/main/kotlin/com/asteroids/screen/GameOverScreen.kt`
- Create stub: `core/src/main/kotlin/com/asteroids/render/HudRenderer.kt`

- [ ] **Step 1: Create `HudRenderer.kt` stub (full version in Task 15)**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.palacesoft.asteroids.game.World

class HudRenderer(batch: SpriteBatch, camera: OrthographicCamera) {
    private val font = BitmapFont()  // placeholder — replace with arcade font in Task 15

    fun render(world: World) {
        // TODO Task 15: full Scene2D HUD
    }

    fun dispose() { font.dispose() }
}
```

- [ ] **Step 2: Create `GameScreen.kt`**

```kotlin
package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.input.InputHandler
import com.palacesoft.asteroids.render.GameRenderer
import com.palacesoft.asteroids.render.HudRenderer
import com.palacesoft.asteroids.vfx.ScreenShake

class GameScreen(private val game: AsteroidsGame) : Screen {
    private val world        = World()
    private val inputHandler = InputHandler(world.input)
    private val shake        = ScreenShake()
    private val renderer     = GameRenderer(game.camera, game.batch, game.sr)

    init {
        renderer.hudRenderer = HudRenderer(game.batch, game.camera)
        world.start()
        Gdx.input.isCatchBackKey = true
    }

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)  // cap at 50ms to prevent spiral of death
        inputHandler.poll()
        world.update(dt)
        shake.update(dt)
        renderer.update(dt, world)
        renderer.render(world, shake.offsetX, shake.offsetY)

        if (world.gameOver) game.setScreen(GameOverScreen(game, world.score))
    }

    override fun resize(width: Int, height: Int) {}
    override fun show()   { Gdx.input.isCatchBackKey = true }
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() { renderer.hudRenderer?.dispose() }
}
```

- [ ] **Step 3: Create `MenuScreen.kt` stub**

```kotlin
package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.palacesoft.asteroids.AsteroidsGame

class MenuScreen(private val game: AsteroidsGame) : Screen {
    private val font = BitmapFont()

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.batch.begin()
        font.draw(game.batch, "ASTEROIDS", 760f, 500f)
        font.draw(game.batch, "Press SPACE or tap to start", 680f, 440f)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) ||
            Gdx.input.justTouched()) {
            game.setScreen(GameScreen(game))
        }
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() { font.dispose() }
}
```

- [ ] **Step 4: Create `GameOverScreen.kt` stub**

```kotlin
package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.palacesoft.asteroids.AsteroidsGame

class GameOverScreen(private val game: AsteroidsGame, private val score: Int) : Screen {
    private val font = BitmapFont()

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.batch.begin()
        font.draw(game.batch, "GAME OVER", 760f, 500f)
        font.draw(game.batch, "Score: $score", 780f, 450f)
        font.draw(game.batch, "Tap to retry", 770f, 400f)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) ||
            Gdx.input.justTouched()) {
            game.setScreen(GameScreen(game))
        }
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() { font.dispose() }
}
```

- [ ] **Step 5: Create `ScreenShake.kt` stub (full version in Task 11)**

```kotlin
package com.palacesoft.asteroids.vfx

class ScreenShake {
    var offsetX = 0f; var offsetY = 0f
    private var trauma = 0f
    fun update(delta: Float) { trauma = (trauma - delta * 1.5f).coerceAtLeast(0f)
        val shake = trauma * trauma * 15f
        offsetX = (Math.random().toFloat() * 2f - 1f) * shake
        offsetY = (Math.random().toFloat() * 2f - 1f) * shake }
    fun trigger(intensity: Float) { trauma = (trauma + intensity).coerceAtMost(1f) }
}
```

- [ ] **Step 6: Run the desktop build**

```bash
./gradlew :desktop:run
```

Expected: Window opens, starfield visible, ship controllable, asteroids spawn. Game is playable — no VFX polish yet.

- [ ] **Step 7: Commit**

```bash
git add core/src/
git commit -m "feat: first playable build — GameScreen wiring up world, input, and renderer"
```

---

### Task 11: ScreenShake — full implementation with tests

**Files:**
- Modify: `core/src/main/kotlin/com/asteroids/vfx/ScreenShake.kt`
- Create: `core/src/test/kotlin/com/asteroids/vfx/ScreenShakeTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.palacesoft.asteroids.vfx

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScreenShakeTest {
    @Test fun `trauma decays to zero over time`() {
        val shake = ScreenShake()
        shake.trigger(1f)
        repeat(100) { shake.update(0.1f) }
        assertEquals(0f, shake.trauma, 0.01f)
    }

    @Test fun `trauma capped at 1`() {
        val shake = ScreenShake()
        shake.trigger(5f)
        assertEquals(1f, shake.trauma, 0.001f)
    }

    @Test fun `no shake when trauma is zero`() {
        val shake = ScreenShake()
        shake.update(0.016f)
        assertEquals(0f, shake.offsetX, 0.001f)
        assertEquals(0f, shake.offsetY, 0.001f)
    }
}
```

- [ ] **Step 2: Run to confirm first two pass, third fails**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.vfx.ScreenShakeTest"
```

Expected: "no shake when trauma is zero" FAILS — current impl always produces noise.

- [ ] **Step 3: Replace `ScreenShake.kt` with full implementation**

```kotlin
package com.palacesoft.asteroids.vfx

import kotlin.math.sin

class ScreenShake {
    var offsetX  = 0f
    var offsetY  = 0f
    var trauma   = 0f
        private set
    private var time = 0f
    private val DECAY     = 1.8f
    private val MAX_SHIFT = 18f
    private val FREQ      = 40f

    fun update(delta: Float) {
        if (trauma <= 0f) { offsetX = 0f; offsetY = 0f; return }
        trauma = (trauma - DECAY * delta).coerceAtLeast(0f)
        time  += delta
        val power = trauma * trauma
        offsetX = sin(time * FREQ)          * power * MAX_SHIFT
        offsetY = sin(time * FREQ + 1.234f) * power * MAX_SHIFT
    }

    fun trigger(intensity: Float) {
        trauma = (trauma + intensity).coerceAtMost(1f)
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.vfx.ScreenShakeTest"
```

Expected: PASS — 3 tests.

- [ ] **Step 5: Commit**

```bash
git add core/src/
git commit -m "feat: full ScreenShake implementation with trauma-squared decay"
```

---

### Task 12: ParticlePool + ThrustTrail

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/vfx/ParticlePool.kt`
- Create: `core/src/main/kotlin/com/asteroids/vfx/ThrustTrail.kt`

- [ ] **Step 1: Create `ParticlePool.kt`**

```kotlin
package com.palacesoft.asteroids.vfx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class Particle {
    var x = 0f; var y = 0f
    var velX = 0f; var velY = 0f
    var life = 0f; var maxLife = 1f
    var r = 1f; var g = 1f; var b = 1f
    var size = 2f
    var alive = false
    var isLine = false      // if true, drawn as a short line (debris)
    var angle  = 0f         // rotation for line debris
    var rotSpeed = 0f
}

class ParticlePool(capacity: Int = 400) {
    val particles = Array(capacity) { Particle() }
    private var head = 0

    fun acquire(): Particle? {
        var checked = 0
        while (checked < particles.size) {
            val p = particles[head]
            head = (head + 1) % particles.size
            checked++
            if (!p.alive) return p
        }
        return null  // pool exhausted — silently drop
    }

    fun update(delta: Float) {
        particles.forEach { p ->
            if (!p.alive) return@forEach
            p.x    += p.velX * delta
            p.y    += p.velY * delta
            p.life -= delta
            p.angle += p.rotSpeed * delta
            if (p.life <= 0f) p.alive = false
        }
    }

    fun render(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Filled)
        particles.forEach { p ->
            if (!p.alive) return@forEach
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            sr.color = Color(p.r, p.g, p.b, alpha)
            if (!p.isLine) {
                sr.circle(p.x, p.y, p.size * alpha)
            }
        }
        sr.end()

        sr.begin(ShapeRenderer.ShapeType.Line)
        particles.forEach { p ->
            if (!p.alive || !p.isLine) return@forEach
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            sr.color = Color(p.r, p.g, p.b, alpha)
            val cos = kotlin.math.cos(Math.toRadians(p.angle.toDouble())).toFloat()
            val sin = kotlin.math.sin(Math.toRadians(p.angle.toDouble())).toFloat()
            val len = p.size * 4f
            sr.line(p.x - cos * len, p.y - sin * len, p.x + cos * len, p.y + sin * len)
        }
        sr.end()
    }
}
```

- [ ] **Step 2: Create `ThrustTrail.kt`**

```kotlin
package com.palacesoft.asteroids.vfx

import com.palacesoft.asteroids.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ThrustTrail(private val pool: ParticlePool) {
    private var accumulator = 0f
    private val EMIT_INTERVAL = 0.03f

    fun update(delta: Float, ship: Ship) {
        if (!ship.thrusting || !ship.alive) { accumulator = 0f; return }
        accumulator += delta
        while (accumulator >= EMIT_INTERVAL) {
            accumulator -= EMIT_INTERVAL
            emit(ship)
        }
    }

    private fun emit(ship: Ship) {
        val p = pool.acquire() ?: return
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val spread = (Random.nextFloat() - 0.5f) * 0.6f
        p.x = ship.x - cos(rad) * ship.radius * 0.7f
        p.y = ship.y - sin(rad) * ship.radius * 0.7f
        p.velX = ship.velX - cos(rad + spread) * 120f
        p.velY = ship.velY - sin(rad + spread) * 120f
        p.life = 0.25f; p.maxLife = 0.25f
        p.r = 0.4f; p.g = 0.7f; p.b = 1f
        p.size = 3f
        p.alive = true; p.isLine = false
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :core:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/src/
git commit -m "feat: add ParticlePool (400 pre-allocated) and ThrustTrail emitter"
```

---

### Task 13: Explosion + VfxManager — wire VFX into game loop

**Files:**
- Create: `core/src/main/kotlin/com/asteroids/vfx/Explosion.kt`
- Create: `core/src/main/kotlin/com/asteroids/vfx/VfxManager.kt`
- Modify: `core/src/main/kotlin/com/asteroids/game/system/CollisionSystem.kt`
- Modify: `core/src/main/kotlin/com/asteroids/game/World.kt`
- Modify: `core/src/main/kotlin/com/asteroids/screen/GameScreen.kt`

- [ ] **Step 1: Create `Explosion.kt`**

```kotlin
package com.palacesoft.asteroids.vfx

import com.palacesoft.asteroids.game.entity.AsteroidSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Explosion(private val pool: ParticlePool) {
    fun spawn(x: Float, y: Float, size: AsteroidSize) {
        val count   = when (size) { AsteroidSize.LARGE -> 16; AsteroidSize.MEDIUM -> 10; AsteroidSize.SMALL -> 6 }
        val speed   = when (size) { AsteroidSize.LARGE -> 200f; AsteroidSize.MEDIUM -> 150f; AsteroidSize.SMALL -> 100f }
        val life    = when (size) { AsteroidSize.LARGE -> 0.9f; AsteroidSize.MEDIUM -> 0.6f; AsteroidSize.SMALL -> 0.4f }
        val debris  = when (size) { AsteroidSize.LARGE -> 4; AsteroidSize.MEDIUM -> 2; AsteroidSize.SMALL -> 0 }

        repeat(count) {
            val p = pool.acquire() ?: return@repeat
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd   = speed * (0.3f + Random.nextFloat() * 0.7f)
            p.x = x; p.y = y
            p.velX = cos(angle) * spd; p.velY = sin(angle) * spd
            p.life = life * (0.5f + Random.nextFloat() * 0.5f); p.maxLife = p.life
            // Colour: white → orange → fade (approximated by initial colour)
            p.r = 1f; p.g = 0.5f + Random.nextFloat() * 0.5f; p.b = 0.1f
            p.size = 3f; p.alive = true; p.isLine = false
        }

        // Tumbling debris line segments
        repeat(debris) {
            val p = pool.acquire() ?: return@repeat
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            p.x = x; p.y = y
            p.velX = cos(angle) * speed * 0.3f; p.velY = sin(angle) * speed * 0.3f
            p.life = 1.5f; p.maxLife = p.life
            p.r = 0.9f; p.g = 0.6f; p.b = 0.2f
            p.size = size.radius * 0.3f
            p.angle = Random.nextFloat() * 360f
            p.rotSpeed = (Random.nextFloat() - 0.5f) * 300f
            p.alive = true; p.isLine = true
        }
    }

    fun spawnShipExplosion(x: Float, y: Float) {
        repeat(20) {
            val p = pool.acquire() ?: return@repeat
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd = 80f + Random.nextFloat() * 160f
            p.x = x; p.y = y
            p.velX = cos(angle) * spd; p.velY = sin(angle) * spd
            p.life = 1.2f; p.maxLife = p.life
            p.r = 0f; p.g = 0.9f; p.b = 1f  // cyan for ship
            p.size = 2f; p.alive = true; p.isLine = false
        }
    }
}
```

- [ ] **Step 2: Create `VfxManager.kt`**

```kotlin
package com.palacesoft.asteroids.vfx

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.entity.AsteroidSize

class VfxManager(private val sr: ShapeRenderer, camera: OrthographicCamera) {
    val pool        = ParticlePool(400)
    val shake       = ScreenShake()
    private val explosion = Explosion(pool)
    private val thrust    = ThrustTrail(pool)

    fun spawnExplosion(x: Float, y: Float, size: AsteroidSize) {
        explosion.spawn(x, y, size)
        val intensity = when (size) { AsteroidSize.LARGE -> 0.6f; AsteroidSize.MEDIUM -> 0.35f; AsteroidSize.SMALL -> 0.15f }
        shake.trigger(intensity)
    }

    fun spawnShipExplosion(x: Float, y: Float) {
        explosion.spawnShipExplosion(x, y)
        shake.trigger(0.8f)
    }

    fun updateThrust(delta: Float, ship: com.palacesoft.asteroids.game.entity.Ship) {
        thrust.update(delta, ship)
    }

    fun update(delta: Float) {
        pool.update(delta)
        shake.update(delta)
    }

    fun renderParticles() {
        pool.render(sr)
    }

    val offsetX get() = shake.offsetX
    val offsetY get() = shake.offsetY
}
```

- [ ] **Step 3: Update `CollisionSystem.kt` — replace `// TODO Task 13` comment**

Find in `checkBulletsVsAsteroids()`:

```kotlin
// TODO Task 13: world.vfx?.spawnExplosion(ast.x, ast.y, ast.size)
```

Replace with:

```kotlin
(world.vfx as? com.palacesoft.asteroids.vfx.VfxManager)?.spawnExplosion(ast.x, ast.y, ast.size)
```

Also add to `checkShipVsAsteroids()` after `world.ship.alive = false`:

```kotlin
(world.vfx as? com.palacesoft.asteroids.vfx.VfxManager)?.spawnShipExplosion(world.ship.x, world.ship.y)
```

- [ ] **Step 4: Update `GameScreen.kt` — wire VfxManager**

Add field and init block changes:

```kotlin
// After: private val shake = ScreenShake()
private val vfx = VfxManager(game.sr, game.camera)

// In init {}, after renderer.hudRenderer = ...:
world.vfx = vfx
```

Update `render()` — replace `shake.update(dt)` and `shake.offsetX/Y` usages:

```kotlin
override fun render(delta: Float) {
    val dt = delta.coerceAtMost(0.05f)
    inputHandler.poll()
    world.update(dt)
    vfx.update(dt)
    vfx.updateThrust(dt, world.ship)
    renderer.update(dt, world)
    renderer.render(world, vfx.offsetX, vfx.offsetY)
    if (world.gameOver) game.setScreen(GameOverScreen(game, world.score))
}
```

- [ ] **Step 5: Update `GameRenderer.kt` — render particles after geometry**

In `render()`, after the bloom pass block add:

```kotlin
// Render particles (after bloom so they appear above glow)
sr.projectionMatrix = camera.combined
// particles are rendered by caller via vfxManager.renderParticles()
// GameScreen calls this:
```

Actually simpler: add `var vfx: VfxManager? = null` field to `GameRenderer`, and in `render()` after the bloom block:

```kotlin
sr.projectionMatrix = camera.combined
vfx?.renderParticles()
```

And in `GameScreen.init {}` add: `renderer.vfx = vfx`

- [ ] **Step 6: Run and verify visually**

```bash
./gradlew :desktop:run
```

Expected: Explosions appear when asteroids die. Screen shakes. Thrust trail visible.

- [ ] **Step 7: Commit**

```bash
git add core/src/
git commit -m "feat: add Explosion, ThrustTrail, VfxManager — particle effects and screen shake live"
```

---

### Task 14: BloomPass — FBO glow effect

**Files:**
- Create: `assets/shaders/bloom.vert`
- Create: `assets/shaders/bloomH.frag`
- Create: `assets/shaders/bloomV.frag`
- Create: `core/src/main/kotlin/com/asteroids/vfx/BloomPass.kt`
- Modify: `core/src/main/kotlin/com/asteroids/screen/GameScreen.kt`

- [ ] **Step 1: Create `assets/shaders/bloom.vert`**

```glsl
attribute vec4 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;
uniform mat4 u_projTrans;
varying vec2 v_texCoords;
varying vec4 v_color;

void main() {
    v_texCoords = a_texCoord0;
    v_color = a_color;
    gl_Position = u_projTrans * a_position;
}
```

- [ ] **Step 2: Create `assets/shaders/bloomH.frag` (horizontal Gaussian blur)**

```glsl
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_width;

void main() {
    float x = 1.0 / u_width;
    vec4 sum = vec4(0.0);
    sum += texture2D(u_texture, vec2(v_texCoords.x - 4.0*x, v_texCoords.y)) * 0.0162;
    sum += texture2D(u_texture, vec2(v_texCoords.x - 3.0*x, v_texCoords.y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x - 2.0*x, v_texCoords.y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x - 1.0*x, v_texCoords.y)) * 0.1945;
    sum += texture2D(u_texture, v_texCoords)                                 * 0.2275;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 1.0*x, v_texCoords.y)) * 0.1945;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 2.0*x, v_texCoords.y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 3.0*x, v_texCoords.y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 4.0*x, v_texCoords.y)) * 0.0162;
    gl_FragColor = sum;
}
```

- [ ] **Step 3: Create `assets/shaders/bloomV.frag` (vertical blur)**

```glsl
#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_height;

void main() {
    float y = 1.0 / u_height;
    vec4 sum = vec4(0.0);
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 4.0*y)) * 0.0162;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 3.0*y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 2.0*y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 1.0*y)) * 0.1945;
    sum += texture2D(u_texture, v_texCoords)                                  * 0.2275;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 1.0*y)) * 0.1945;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 2.0*y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 3.0*y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 4.0*y)) * 0.0162;
    gl_FragColor = sum;
}
```

- [ ] **Step 4: Create `BloomPass.kt`**

```kotlin
package com.palacesoft.asteroids.vfx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.palacesoft.asteroids.util.Settings

class BloomPass(private val batch: SpriteBatch) : Disposable {
    private val w = (Gdx.graphics.width  / 2).coerceAtLeast(1)
    private val h = (Gdx.graphics.height / 2).coerceAtLeast(1)

    private val fbo1 = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)
    private val fbo2 = FrameBuffer(Pixmap.Format.RGBA8888, w, h, false)

    private val shaderH = ShaderProgram(
        Gdx.files.internal("shaders/bloom.vert").readString(),
        Gdx.files.internal("shaders/bloomH.frag").readString()
    )
    private val shaderV = ShaderProgram(
        Gdx.files.internal("shaders/bloom.vert").readString(),
        Gdx.files.internal("shaders/bloomV.frag").readString()
    )

    init {
        require(shaderH.isCompiled) { "BloomH shader error: ${shaderH.log}" }
        require(shaderV.isCompiled) { "BloomV shader error: ${shaderV.log}" }
        ShaderProgram.pedantic = false
    }

    fun beginCapture() {
        fbo1.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }

    fun endCapture() = fbo1.end()

    fun render(batch: SpriteBatch) {
        if (!Settings.bloomEnabled) return
        val tex1 = fbo1.colorBufferTexture
        val tex2 = fbo2.colorBufferTexture
        tex1.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                       com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)

        // Horizontal blur: fbo1 → fbo2
        fbo2.begin()
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.shader = shaderH
        batch.begin()
        shaderH.setUniformf("u_width", w.toFloat())
        batch.draw(tex1, 0f, 0f, w.toFloat(), h.toFloat(),
                   0, 0, tex1.width, tex1.height, false, true)
        batch.end()
        fbo2.end()

        // Vertical blur + additive blend to screen
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE)  // additive
        batch.shader = shaderV
        batch.begin()
        shaderV.setUniformf("u_height", h.toFloat())
        batch.draw(tex2, 0f, 0f, Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT,
                   0, 0, tex2.width, tex2.height, false, true)
        batch.end()
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)  // restore
        batch.shader = null
    }

    override fun dispose() {
        fbo1.dispose(); fbo2.dispose()
        shaderH.dispose(); shaderV.dispose()
    }
}
```

- [ ] **Step 5: Wire BloomPass into GameScreen**

Add field:

```kotlin
private val bloom = BloomPass(game.batch)
```

In `init {}` after `renderer.vfx = vfx`:

```kotlin
renderer.init(bloom)
```

In `dispose()`:

```kotlin
bloom.dispose()
```

- [ ] **Step 6: Run desktop build and verify glow**

```bash
./gradlew :desktop:run
```

Expected: Bullets and saucer have a soft neon glow halo. If shader fails to compile, check `ShaderProgram.log` output in console.

- [ ] **Step 7: Commit**

```bash
git add assets/shaders/ core/src/
git commit -m "feat: add FBO bloom pass with 9-tap separable Gaussian blur"
```

---

### Task 15: HudRenderer — Scene2D score, lives, wave banner

**Files:**
- Modify: `core/src/main/kotlin/com/asteroids/render/HudRenderer.kt`

- [ ] **Step 1: Replace HudRenderer stub with Scene2D implementation**

```kotlin
package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.palacesoft.asteroids.game.World
import com.palacesoft.asteroids.util.Settings

class HudRenderer(batch: SpriteBatch, camera: OrthographicCamera) {
    // Use FitViewport so HUD scales to screen
    private val viewport = FitViewport(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT, camera)
    val stage = Stage(viewport, batch)

    private val font = BitmapFont().apply { data.setScale(2f); color = Color.CYAN }
    private val scoreStyle  = Label.LabelStyle(font, Color.CYAN)
    private val waveStyle   = Label.LabelStyle(BitmapFont().apply { data.setScale(1.8f) }, Color.WHITE)
    private val popupStyle  = Label.LabelStyle(BitmapFont().apply { data.setScale(1.5f) }, Color.YELLOW)

    private val scoreLabel = Label("0", scoreStyle).apply {
        setPosition(20f, Settings.WORLD_HEIGHT - 40f)
    }
    private val livesLabel = Label("", scoreStyle).apply {
        setPosition(Settings.WORLD_WIDTH - 160f, Settings.WORLD_HEIGHT - 40f)
    }
    private val waveLabel = Label("", waveStyle).apply {
        setWidth(Settings.WORLD_WIDTH)
        setPosition(0f, Settings.WORLD_HEIGHT / 2f + 60f)
        setAlignment(Align.center)
        color.a = 0f
    }

    private var lastScore = -1
    private var lastWave  = -1
    private var lastLives = -1

    init {
        stage.addActor(scoreLabel)
        stage.addActor(livesLabel)
        stage.addActor(waveLabel)
    }

    fun render(world: World) {
        // Score
        if (world.score != lastScore) {
            lastScore = world.score
            scoreLabel.setText(world.score.toString())
            if (world.score > 0) spawnScorePopup(world.score)
        }
        // Lives — show as triangles via text (replace with ShapeRenderer in a polish pass)
        if (world.lives != lastLives) {
            lastLives = world.lives
            livesLabel.setText("△ ".repeat(world.lives.coerceAtLeast(0)))
        }
        // Wave banner
        if (world.wave != lastWave && world.wave > 0) {
            lastWave = world.wave
            waveLabel.setText("WAVE ${world.wave}")
            waveLabel.clearActions()
            waveLabel.color.a = 0f
            waveLabel.addAction(Actions.sequence(
                Actions.fadeIn(0.4f),
                Actions.delay(2f),
                Actions.fadeOut(0.6f)
            ))
        }

        stage.act()
        stage.draw()
    }

    private fun spawnScorePopup(score: Int) {
        // Show a small "+N" label that floats up and fades
        val popup = Label("+${score - (lastScore - (score - lastScore))}", popupStyle)
        popup.setPosition(scoreLabel.x + 100f, scoreLabel.y)
        popup.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0f, 60f, 1f),
                Actions.fadeOut(1f)
            ),
            Actions.removeActor()
        ))
        stage.addActor(popup)
    }

    fun resize(width: Int, height: Int) = viewport.update(width, height, true)

    override fun dispose() {
        stage.dispose()
        font.dispose()
    }
}
```

- [ ] **Step 2: Update `GameScreen.resize()`**

```kotlin
override fun resize(width: Int, height: Int) {
    renderer.hudRenderer?.resize(width, height)
}
```

- [ ] **Step 3: Run desktop build**

```bash
./gradlew :desktop:run
```

Expected: Score top-left in cyan, lives shown as triangles top-right, "WAVE N" banner fades in/out at wave start.

- [ ] **Step 4: Commit**

```bash
git add core/src/
git commit -m "feat: add Scene2D HUD with score, lives, wave banner, and score popups"
```

---

### Task 16: Polish MenuScreen and GameOverScreen

**Files:**
- Modify: `core/src/main/kotlin/com/asteroids/screen/MenuScreen.kt`
- Modify: `core/src/main/kotlin/com/asteroids/screen/GameOverScreen.kt`

- [ ] **Step 1: Replace `MenuScreen.kt` with polished version**

```kotlin
package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.utils.Align
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.render.Starfield
import com.palacesoft.asteroids.util.Settings

class MenuScreen(private val game: AsteroidsGame) : Screen {
    private val starfield = Starfield()
    private val font = BitmapFont().apply { data.setScale(3f); color = com.badlogic.gdx.graphics.Color.CYAN }
    private val subFont = BitmapFont().apply { data.setScale(1.5f); color = com.badlogic.gdx.graphics.Color.WHITE }
    private var time = 0f

    override fun render(delta: Float) {
        time += delta
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)
        // Pulsing title
        val scale = 3f + 0.15f * kotlin.math.sin(time * 2f).toFloat()
        font.data.setScale(scale)
        game.batch.begin()
        font.draw(game.batch, "ASTEROIDS",
                  Settings.WORLD_WIDTH / 2f - 180f, Settings.WORLD_HEIGHT / 2f + 60f)
        subFont.draw(game.batch, "PRESS SPACE OR TAP TO START",
                     Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f - 20f)
        subFont.draw(game.batch, "LEFT / RIGHT  –  ROTATE     UP / DRAG  –  THRUST",
                     Settings.WORLD_WIDTH / 2f - 260f, Settings.WORLD_HEIGHT / 2f - 80f)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) || Gdx.input.justTouched())
            game.setScreen(GameScreen(game))
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() { font.dispose(); subFont.dispose() }
}
```

- [ ] **Step 2: Replace `GameOverScreen.kt` with polished version**

```kotlin
package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.render.Starfield
import com.palacesoft.asteroids.util.Settings

class GameOverScreen(private val game: AsteroidsGame, private val finalScore: Int) : Screen {
    private val starfield = Starfield()
    private val titleFont = BitmapFont().apply { data.setScale(3.5f); color = com.badlogic.gdx.graphics.Color.RED }
    private val scoreFont = BitmapFont().apply { data.setScale(2f); color = com.badlogic.gdx.graphics.Color.WHITE }
    private val subFont   = BitmapFont().apply { data.setScale(1.5f); color = com.badlogic.gdx.graphics.Color.GRAY }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)
        game.batch.begin()
        titleFont.draw(game.batch, "GAME OVER",
                       Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f + 80f)
        scoreFont.draw(game.batch, "SCORE  $finalScore",
                       Settings.WORLD_WIDTH / 2f - 100f, Settings.WORLD_HEIGHT / 2f)
        subFont.draw(game.batch, "PRESS SPACE OR TAP TO RETRY",
                     Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f - 80f)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) || Gdx.input.justTouched())
            game.setScreen(GameScreen(game))
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() { titleFont.dispose(); scoreFont.dispose(); subFont.dispose() }
}
```

- [ ] **Step 3: Run full desktop build**

```bash
./gradlew :desktop:run
```

Expected: Polished menu with pulsing title and starfield. Game over screen with score.

- [ ] **Step 4: Run all tests**

```bash
./gradlew :core:test
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/
git commit -m "feat: polish menu and game over screens with starfield and styled text"
```

---

### Task 17: Android debug build verification + packaging checklist

**Files:** No new files.

- [ ] **Step 1: Build Android debug APK**

```bash
./gradlew :android:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. APK at `android/build/outputs/apk/debug/android-debug.apk`.

- [ ] **Step 2: Install on connected device or emulator**

```bash
./gradlew :android:installDebug
```

Expected: App installs and launches in landscape orientation.

- [ ] **Step 3: Verify ProGuard rules don't strip libGDX**

Create `android/proguard-rules.pro` if not present:

```
-keep class com.badlogic.** { *; }
-keep interface com.badlogic.** { *; }
-keepclassmembers class com.badlogic.** { *; }
-keep class com.palacesoft.asteroids.** { *; }
```

- [ ] **Step 4: Final test run**

```bash
./gradlew :core:test
```

Expected: All tests PASS.

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "feat: complete Asteroids game — all systems, VFX, HUD, and Android build"
```

---

## Spec Coverage Self-Check

| Spec Requirement | Covered in Task |
|---|---|
| Rotating ship, thrust, screen wrap | Task 7 (World) |
| Bullets, bullet pool | Task 4 (BulletPool) |
| Asteroid splitting | Task 5 (CollisionSystem) |
| Saucers + AI | Task 6 (WaveSystem) |
| Score, lives, respawn, invulnerability | Task 7 (World) |
| Game over | Task 10 (GameScreen) |
| Touch controls | Task 8 (TouchControls) |
| Keyboard controls | Task 8 (InputHandler) |
| Neon geometry rendering | Task 9 |
| Animated starfield | Task 9 |
| Particle explosions + debris | Task 13 (Explosion) |
| Thrust trail | Task 12 (ThrustTrail) |
| Screen shake | Task 11 (ScreenShake) |
| Ship flicker on invulnerability | Task 7 (World) |
| Bloom/glow post-processing | Task 14 (BloomPass) |
| HUD — score, lives, wave banner | Task 15 (HudRenderer) |
| Score popups | Task 15 (HudRenderer) |
| Menu screen | Task 16 |
| Game over screen | Task 16 |
| Wave progression | Task 6 (WaveSystem) |
| Android build | Task 17 |
| Performance: pool pre-allocation | Tasks 4, 12 |
| Performance: bloom disable flag | Task 14 (Settings.bloomEnabled) |

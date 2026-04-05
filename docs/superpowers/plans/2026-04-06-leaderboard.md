# Leaderboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Google Play Games Services global leaderboard — submit score at game over, show leaderboard on demand via a button on the Game Over screen.

**Architecture:** A `GameServices` interface lives in `core` (zero Android imports). `AndroidLauncher` creates a `GooglePlayServices` implementation and injects it into `AsteroidsGame.gameServices`. `GameScreen` submits the score; `GameOverScreen` shows the LEADERBOARD button.

**Tech Stack:** Kotlin, libGDX 1.12.1, Google Play Games Services v2 (`play-services-games-v2:19.0.0`), JUnit 5

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `core/src/main/kotlin/com/palacesoft/asteroids/GameServices.kt` | Platform-agnostic interface |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/AsteroidsGame.kt` | Hold nullable `gameServices` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt` | Submit score before game-over transition |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt` | Add LEADERBOARD button + split tap zones |
| Create | `android/src/main/kotlin/com/palacesoft/asteroids/android/GooglePlayServices.kt` | GPGS implementation |
| Modify | `android/src/main/kotlin/com/palacesoft/asteroids/android/AndroidLauncher.kt` | Inject `GooglePlayServices` |
| Modify | `android/build.gradle.kts` | Add `play-services-games-v2` dependency |
| Modify | `android/AndroidManifest.xml` | Add `APP_ID` meta-data |
| Create | `android/src/main/res/values/games_ids.xml` | Leaderboard ID placeholder |
| Create | `core/src/test/kotlin/com/palacesoft/asteroids/GameServicesTest.kt` | Test score submission timing |

---

### Task 1: Add GPGS dependency and placeholder config

**Files:**
- Modify: `android/build.gradle.kts`
- Modify: `android/AndroidManifest.xml`
- Create: `android/src/main/res/values/games_ids.xml`

- [ ] **Step 1: Add dependency to `android/build.gradle.kts`**

In the `dependencies` block, after the existing `implementation` lines, add:

```kotlin
implementation("com.google.android.gms:play-services-games-v2:19.0.0")
```

- [ ] **Step 2: Create `games_ids.xml` with placeholder values**

Create `android/src/main/res/values/games_ids.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Replace these placeholder values with IDs from Google Play Console
  after registering the game under Play Games Services.
  See: https://developers.google.com/games/services/android/quickstart
-->
<resources>
    <string name="app_id" translatable="false">YOUR_APP_ID</string>
    <string name="leaderboard_high_score" translatable="false">YOUR_LEADERBOARD_ID</string>
</resources>
```

- [ ] **Step 3: Add APP_ID meta-data to `android/AndroidManifest.xml`**

Inside `<application>`, after the existing `<activity>` block, add:

```xml
<meta-data
    android:name="com.google.android.gms.games.APP_ID"
    android:value="@string/app_id" />
```

- [ ] **Step 4: Sync and verify the project builds**

```bash
./gradlew :android:assembleDebug 2>&1 | tail -5
```

Expected output ends with: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add android/build.gradle.kts android/AndroidManifest.xml android/src/main/res/values/games_ids.xml
git commit -m "chore: add GPGS v2 dependency and placeholder config"
```

---

### Task 2: `GameServices` interface and `AsteroidsGame` wiring

**Files:**
- Create: `core/src/main/kotlin/com/palacesoft/asteroids/GameServices.kt`
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/AsteroidsGame.kt`
- Create: `core/src/test/kotlin/com/palacesoft/asteroids/GameServicesTest.kt`

- [ ] **Step 1: Write the failing test**

Create `core/src/test/kotlin/com/palacesoft/asteroids/GameServicesTest.kt`:

```kotlin
package com.palacesoft.asteroids

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameServicesTest {

    private class RecordingGameServices : GameServices {
        val submittedScores = mutableListOf<Int>()
        var leaderboardShown = false
        override fun submitScore(score: Int) { submittedScores.add(score) }
        override fun showLeaderboard() { leaderboardShown = true }
    }

    @Test fun `submitScore records the score`() {
        val svc = RecordingGameServices()
        svc.submitScore(1234)
        assertEquals(listOf(1234), svc.submittedScores)
    }

    @Test fun `showLeaderboard sets flag`() {
        val svc = RecordingGameServices()
        svc.showLeaderboard()
        assertTrue(svc.leaderboardShown)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails (interface missing)**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.GameServicesTest" 2>&1 | tail -10
```

Expected: `FAILED` — `GameServices` not found.

- [ ] **Step 3: Create the `GameServices` interface**

Create `core/src/main/kotlin/com/palacesoft/asteroids/GameServices.kt`:

```kotlin
package com.palacesoft.asteroids

interface GameServices {
    fun submitScore(score: Int)
    fun showLeaderboard()
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :core:test --tests "com.palacesoft.asteroids.GameServicesTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 5: Add `gameServices` to `AsteroidsGame`**

In `core/src/main/kotlin/com/palacesoft/asteroids/AsteroidsGame.kt`, add one property after `lateinit var sounds: SoundManager`:

```kotlin
var gameServices: GameServices? = null
```

Full `AsteroidsGame.kt` after change:

```kotlin
package com.palacesoft.asteroids

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.audio.SoundManager
import com.palacesoft.asteroids.screen.MenuScreen
import com.palacesoft.asteroids.util.Settings

class AsteroidsGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var sr: ShapeRenderer
    lateinit var camera: OrthographicCamera
    lateinit var sounds: SoundManager
    var gameServices: GameServices? = null

    override fun create() {
        batch  = SpriteBatch()
        sr     = ShapeRenderer()
        camera = OrthographicCamera(Settings.WORLD_WIDTH, Settings.WORLD_HEIGHT)
        camera.position.set(Settings.WORLD_WIDTH / 2f, Settings.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        sounds = SoundManager()
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        sr.dispose()
        sounds.dispose()
    }
}
```

- [ ] **Step 6: Verify full build still passes**

```bash
./gradlew :core:test 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/GameServices.kt \
        core/src/main/kotlin/com/palacesoft/asteroids/AsteroidsGame.kt \
        core/src/test/kotlin/com/palacesoft/asteroids/GameServicesTest.kt
git commit -m "feat: add GameServices interface and wire into AsteroidsGame"
```

---

### Task 3: Submit score in `GameScreen` at game over

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt`

Current game-over line in `GameScreen.render()` (line 47):
```kotlin
if (world.gameOver) game.setScreen(GameOverScreen(game, world.score))
```

- [ ] **Step 1: Replace the game-over transition line**

Change the single line to submit the score before transitioning:

```kotlin
if (world.gameOver) {
    game.gameServices?.submitScore(world.score)
    game.setScreen(GameOverScreen(game, world.score))
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :core:compileKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt
git commit -m "feat: submit score to GameServices at game over"
```

---

### Task 4: Add LEADERBOARD button to `GameOverScreen`

**Files:**
- Modify: `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt`

The current screen renders three text lines and handles a single tap-anywhere-to-retry. We split the tap zone: upper 2/3 = retry, lower 1/3 = leaderboard (only when `gameServices != null`).

- [ ] **Step 1: Replace `GameOverScreen.kt` with the updated version**

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
    private val starfield  = Starfield()
    private val titleFont  = BitmapFont().apply { data.setScale(3.5f); color = com.badlogic.gdx.graphics.Color.RED }
    private val scoreFont  = BitmapFont().apply { data.setScale(2f);   color = com.badlogic.gdx.graphics.Color.WHITE }
    private val subFont    = BitmapFont().apply { data.setScale(1.5f); color = com.badlogic.gdx.graphics.Color.GRAY }
    private val lbFont     = BitmapFont().apply { data.setScale(1.5f); color = com.badlogic.gdx.graphics.Color.CYAN }

    // Normalised screen-y threshold: touch below this fraction triggers leaderboard
    private val LB_ZONE_THRESHOLD = 0.33f

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.sr.projectionMatrix = game.camera.combined
        starfield.update(delta, 0f, 0f)
        starfield.render(game.sr)
        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        titleFont.draw(game.batch, "GAME OVER",
                       Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f + 80f)
        scoreFont.draw(game.batch, "SCORE  $finalScore",
                       Settings.WORLD_WIDTH / 2f - 100f, Settings.WORLD_HEIGHT / 2f)
        subFont.draw(game.batch, "PRESS SPACE OR TAP TO RETRY",
                     Settings.WORLD_WIDTH / 2f - 200f, Settings.WORLD_HEIGHT / 2f - 80f)
        if (game.gameServices != null) {
            lbFont.draw(game.batch, "LEADERBOARD",
                        Settings.WORLD_WIDTH / 2f - 100f, Settings.WORLD_HEIGHT / 2f - 140f)
        }
        game.batch.end()

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            game.setScreen(GameScreen(game))
            return
        }

        if (Gdx.input.justTouched()) {
            val normY = Gdx.input.y.toFloat() / Gdx.graphics.height.toFloat()
            // normY = 0 at top of screen, 1 at bottom
            if (game.gameServices != null && normY > (1f - LB_ZONE_THRESHOLD)) {
                game.gameServices?.showLeaderboard()
            } else {
                game.setScreen(GameScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) {}
    override fun show()   {}
    override fun hide()   {}
    override fun pause()  {}
    override fun resume() {}
    override fun dispose() {
        titleFont.dispose(); scoreFont.dispose(); subFont.dispose(); lbFont.dispose()
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :core:compileKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt
git commit -m "feat: add LEADERBOARD button to GameOverScreen"
```

---

### Task 5: `GooglePlayServices` implementation

**Files:**
- Create: `android/src/main/kotlin/com/palacesoft/asteroids/android/GooglePlayServices.kt`

- [ ] **Step 1: Create `GooglePlayServices.kt`**

```kotlin
package com.palacesoft.asteroids.android

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.games.PlayGames
import com.palacesoft.asteroids.GameServices

class GooglePlayServices(private val activity: Activity) : GameServices {

    private val leaderboardId: String by lazy {
        activity.getString(R.string.leaderboard_high_score)
    }

    override fun submitScore(score: Int) {
        val client = PlayGames.getLeaderboardsClient(activity)
        client.submitScore(leaderboardId, score.toLong())
    }

    override fun showLeaderboard() {
        val signInClient = PlayGames.getGamesSignInClient(activity)
        signInClient.isAuthenticated.addOnCompleteListener { authTask ->
            val isAuthenticated = authTask.isSuccessful && authTask.result.isAuthenticated
            if (isAuthenticated) {
                openLeaderboard()
            } else {
                signInClient.signIn().addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        openLeaderboard()
                    } else {
                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                "Sign in to Google Play to view leaderboard",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun openLeaderboard() {
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener { intent ->
                activity.startActivityForResult(intent, RC_LEADERBOARD)
            }
    }

    companion object {
        private const val RC_LEADERBOARD = 9004
    }
}
```

- [ ] **Step 2: Verify Android module compiles**

```bash
./gradlew :android:compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add android/src/main/kotlin/com/palacesoft/asteroids/android/GooglePlayServices.kt
git commit -m "feat: implement GooglePlayServices with GPGS v2 sign-in and leaderboard"
```

---

### Task 6: Wire `GooglePlayServices` into `AndroidLauncher`

**Files:**
- Modify: `android/src/main/kotlin/com/palacesoft/asteroids/android/AndroidLauncher.kt`

- [ ] **Step 1: Update `AndroidLauncher.kt`**

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
        val game = AsteroidsGame().also {
            it.gameServices = GooglePlayServices(this)
        }
        initialize(game, config)
    }
}
```

- [ ] **Step 2: Build the full Android debug APK**

```bash
./gradlew :android:assembleDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` and an APK at `android/build/outputs/apk/debug/android-debug.apk`

- [ ] **Step 3: Commit**

```bash
git add android/src/main/kotlin/com/palacesoft/asteroids/android/AndroidLauncher.kt
git commit -m "feat: inject GooglePlayServices into AsteroidsGame on Android"
```

---

### Task 7: Run all tests and final verification

- [ ] **Step 1: Run full core test suite**

```bash
./gradlew :core:test 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`, all tests pass (including the new `GameServicesTest`).

- [ ] **Step 2: Build the release APK to confirm no ProGuard issues**

```bash
./gradlew :android:assembleRelease 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Push**

```bash
git push
```

---

## Play Console Setup (post-code, manual steps)

These steps are done in the Google Play Console UI — not in code. Do them before submitting to the Play Store:

1. Create a new app in [Play Console](https://play.google.com/console)
2. Navigate to **Play Games Services → Setup and management → Configuration**
3. Add a leaderboard named "High Score" — copy the generated leaderboard ID
4. Copy the App ID from the GPGS configuration page
5. Replace `YOUR_APP_ID` and `YOUR_LEADERBOARD_ID` in `android/src/main/res/values/games_ids.xml`
6. Add your SHA-1 fingerprint to the Play Console credential (from `./gradlew signingReport`)
7. Publish a test build to an internal track to verify end-to-end sign-in and score submission

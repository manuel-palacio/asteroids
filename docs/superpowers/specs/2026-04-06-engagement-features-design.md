# Engagement Features Design

**Date:** 2026-04-06
**Features:** Personal Best · Wave/Asteroid HUD · Streak Multiplier

---

## Goals

1. **Personal Best** — persist and display the player's all-time high score on the HUD and game-over screen.
2. **Wave/Asteroid HUD** — show a persistent "WAVE N · N LEFT" indicator so the player always knows their progress through the current wave.
3. **Streak Multiplier** — reward rapid kills with a score multiplier (up to 4×), reset on death.

---

## Feature 1: Personal Best

### Storage

Use libGDX's `Preferences` API — backed by `SharedPreferences` on Android, a file on desktop. No platform interface needed; `core` stays clean.

`Settings` gains a `highScore` property:

```kotlin
var highScore: Int
    get() = Gdx.app.getPreferences("asteroids").getInteger("highScore", 0)
    set(value) {
        val prefs = Gdx.app.getPreferences("asteroids")
        prefs.putInteger("highScore", value)
        prefs.flush()
    }
```

### Score save point

In `GameScreen.render()`, inside the `gameOverHandled` guard (after `submitScore`, before `setScreen`):

```kotlin
if (world.score > Settings.highScore) Settings.highScore = world.score
```

### HUD display

`HudRenderer` gets a dim grey label top-centre showing `"BEST  $highScore"`. Updated only when `Settings.highScore` changes (cached locally). Positioned between `scoreLabel` (top-left) and `livesLabel` (top-right), centred on `WORLD_WIDTH / 2f`.

### Game-over screen

`GameOverScreen` shows `"BEST  $best"` below the score line. If the player just beat their personal best (`finalScore > previousBest`), the best line is rendered in gold and prefixed with `"NEW BEST! "`.

---

## Feature 2: Wave/Asteroid Count HUD

### New label

`HudRenderer` adds a `waveCountLabel` (scale 1.4f, white, semi-transparent alpha 0.7) positioned top-centre:

```
WAVE 3  ·  5 LEFT
```

- Updated every frame when either `world.wave` or live asteroid count changes (dirty-check: cache last wave + last count).
- Live count = `world.asteroids.count { it.alive }`.
- Saucers are not counted — they spawn on a timer independent of wave completion.
- The existing `waveLabel` flash banner (centred, mid-screen) is unchanged — it handles the dramatic wave announcement; `waveCountLabel` handles ongoing tactical info.

---

## Feature 3: Streak Multiplier

### New class: `StreakSystem`

`core/src/main/kotlin/com/palacesoft/asteroids/game/system/StreakSystem.kt`

Owns all streak logic. Instantiated in `World`, subscribes to `GameEventBus` during `World` construction.

```kotlin
class StreakSystem(private val world: World) {
    var multiplier: Int = 1
        private set

    private var streakCount  = 0
    private var streakTimer  = 0f
    private val STREAK_WINDOW = 1.5f
    private val MAX_MULTIPLIER = 4

    fun subscribe() {
        GameEventBus.subscribe { event ->
            when (event) {
                is GameEvent.AsteroidDestroyed -> onKill()
                is GameEvent.SaucerDestroyed   -> onKill()
                is GameEvent.PlayerHit         -> reset()
                else -> {}
            }
        }
    }

    fun update(delta: Float) {
        if (streakCount > 0) {
            streakTimer -= delta
            if (streakTimer <= 0f) reset()
        }
    }

    private fun onKill() {
        streakCount++
        streakTimer = STREAK_WINDOW
        multiplier = minOf(streakCount, MAX_MULTIPLIER)
        world.scoreMultiplier = multiplier
    }

    fun reset() {
        streakCount = 0
        streakTimer = 0f
        multiplier  = 1
        world.scoreMultiplier = 1
    }
}
```

### `World` changes

- Add `var scoreMultiplier: Int = 1`
- Add `val streakSystem = StreakSystem(this)`
- Call `streakSystem.subscribe()` in `World.init` (after systems are set up)
- Call `streakSystem.update(delta)` in `World.update()`

### Score multiplication

In `CollisionSystem.checkBulletsVsAsteroids()`:
```kotlin
world.score += ast.size.score * world.scoreMultiplier
```

In `CollisionSystem.checkBulletsVsSaucers()`:
```kotlin
world.score += saucerScore * world.scoreMultiplier
```

`ScoreAwarded` event fires with the **multiplied** score so the floating popup shows what the player actually earned (e.g. `+80` at 4× instead of `+20`).

### HUD display

`HudRenderer` adds a `multiplierLabel` (scale 2f, yellow/cyan) positioned just right of the score label. Only visible when `world.streakSystem.multiplier > 1`. Shows `"2×"`, `"3×"`, `"4×"`. Fades in immediately on activation; fades out over 0.5s when multiplier resets to 1.

---

## File Map

| Action | Path |
|--------|------|
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/util/Settings.kt` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/game/World.kt` |
| Create | `core/src/main/kotlin/com/palacesoft/asteroids/game/system/StreakSystem.kt` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/game/system/CollisionSystem.kt` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/render/HudRenderer.kt` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameScreen.kt` |
| Modify | `core/src/main/kotlin/com/palacesoft/asteroids/screen/GameOverScreen.kt` |
| Create | `core/src/test/kotlin/com/palacesoft/asteroids/game/system/StreakSystemTest.kt` |

---

## Out of Scope

- Streak persistence across sessions (resets on every game start)
- Multiplier affecting the `ScoreAwarded` popup value (base score shown for clarity)
- Wave count for saucers (saucers are wave-independent)

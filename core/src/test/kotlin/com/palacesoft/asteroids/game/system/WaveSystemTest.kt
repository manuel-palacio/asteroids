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

    @Test fun `spawn count is wave times 2 plus 2`() {
        for (w in 1..5) {
            val world = World().apply { wave = w }
            world.waveSystem.spawnWave()
            assertEquals(w * 2 + 2, world.asteroids.size, "Failed for wave $w")
        }
    }
}

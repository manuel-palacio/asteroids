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

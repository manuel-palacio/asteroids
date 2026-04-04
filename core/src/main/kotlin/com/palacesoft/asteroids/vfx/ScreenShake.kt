package com.palacesoft.asteroids.vfx

import kotlin.math.sin

class ScreenShake {
    var offsetX  = 0f
    var offsetY  = 0f
    var trauma   = 0f
        private set
    private var time = 0f

    fun update(delta: Float) {
        if (trauma <= 0f) { offsetX = 0f; offsetY = 0f; return }
        trauma = (trauma - 1.8f * delta).coerceAtLeast(0f)
        time  += delta
        val power = trauma * trauma
        offsetX = sin(time * 40f)          * power * 18f
        offsetY = sin(time * 40f + 1.234f) * power * 18f
    }

    fun trigger(intensity: Float) { trauma = (trauma + intensity).coerceAtMost(1f) }
}

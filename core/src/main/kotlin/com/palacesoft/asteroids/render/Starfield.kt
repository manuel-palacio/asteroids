package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.util.Settings
import kotlin.math.sin
import kotlin.random.Random

class Starfield {
    private data class Star(
        var x: Float, var y: Float,
        val layer: Int,
        val brightness: Float,
        val twinkleSpeed: Float
    )

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

    fun update(delta: Float, shipVelX: Float = 0f, shipVelY: Float = 0f) {
        time += delta
        stars.forEach { s ->
            val drift = if (s.layer == 0) 0.02f else 0.05f
            s.x = ((s.x - shipVelX * drift * delta) + Settings.WORLD_WIDTH)  % Settings.WORLD_WIDTH
            s.y = ((s.y - shipVelY * drift * delta) + Settings.WORLD_HEIGHT) % Settings.WORLD_HEIGHT
        }
    }

    fun render(sr: ShapeRenderer) {
        sr.begin(ShapeRenderer.ShapeType.Point)
        stars.forEach { s ->
            val twinkle = 0.5f + 0.5f * sin((time * s.twinkleSpeed).toDouble()).toFloat()
            val b = s.brightness * (0.12f + 0.08f * twinkle)
            sr.color = Color(b, b, b, 1f)
            sr.point(s.x, s.y, 0f)
        }
        sr.end()
    }
}

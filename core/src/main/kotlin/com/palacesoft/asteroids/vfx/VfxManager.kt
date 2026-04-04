package com.palacesoft.asteroids.vfx

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.entity.AsteroidSize
import com.palacesoft.asteroids.game.entity.Ship

class VfxManager(private val sr: ShapeRenderer) {
    val pool             = ParticlePool(400)
    val shake            = ScreenShake()
    private val explosion = Explosion(pool)
    private val thrust    = ThrustTrail(pool)

    fun spawnExplosion(x: Float, y: Float, size: AsteroidSize) {
        explosion.spawn(x, y, size)
        val intensity = when (size) {
            AsteroidSize.LARGE  -> 0.6f
            AsteroidSize.MEDIUM -> 0.35f
            AsteroidSize.SMALL  -> 0.15f
        }
        shake.trigger(intensity)
    }

    fun spawnShipExplosion(x: Float, y: Float) {
        explosion.spawnShipExplosion(x, y)
        shake.trigger(0.8f)
    }

    fun update(delta: Float) {
        pool.update(delta)
        shake.update(delta)
    }

    fun updateThrust(delta: Float, ship: Ship) {
        thrust.update(delta, ship)
    }

    fun renderParticles() {
        pool.render(sr)
    }

    val offsetX: Float get() = shake.offsetX
    val offsetY: Float get() = shake.offsetY
}

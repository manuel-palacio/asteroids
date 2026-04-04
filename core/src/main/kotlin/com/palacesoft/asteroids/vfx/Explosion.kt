package com.palacesoft.asteroids.vfx

import com.palacesoft.asteroids.game.entity.AsteroidSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Explosion(private val pool: ParticlePool) {
    fun spawn(x: Float, y: Float, size: AsteroidSize) {
        val count  = when (size) { AsteroidSize.LARGE -> 16; AsteroidSize.MEDIUM -> 10; AsteroidSize.SMALL -> 6 }
        val speed  = when (size) { AsteroidSize.LARGE -> 200f; AsteroidSize.MEDIUM -> 150f; AsteroidSize.SMALL -> 100f }
        val life   = when (size) { AsteroidSize.LARGE -> 0.9f; AsteroidSize.MEDIUM -> 0.6f; AsteroidSize.SMALL -> 0.4f }
        val debris = when (size) { AsteroidSize.LARGE -> 4; AsteroidSize.MEDIUM -> 2; AsteroidSize.SMALL -> 0 }

        repeat(count) {
            val p = pool.acquire() ?: return@repeat
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd   = speed * (0.3f + Random.nextFloat() * 0.7f)
            p.x = x; p.y = y
            p.velX = cos(angle) * spd; p.velY = sin(angle) * spd
            p.life = life * (0.5f + Random.nextFloat() * 0.5f); p.maxLife = p.life
            p.r = 1f; p.g = 0.5f + Random.nextFloat() * 0.5f; p.b = 0.1f
            p.size = 3f; p.alive = true; p.isLine = false
        }

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
            p.r = 0f; p.g = 0.9f; p.b = 1f
            p.size = 2f; p.alive = true; p.isLine = false
        }
    }
}

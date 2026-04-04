// core/src/main/kotlin/com/palacesoft/asteroids/vfx/VfxManager.kt (STUB)
package com.palacesoft.asteroids.vfx

import com.palacesoft.asteroids.game.entity.AsteroidSize
import com.palacesoft.asteroids.game.entity.Ship

class VfxManager {
    fun spawnExplosion(x: Float, y: Float, size: AsteroidSize) {}
    fun spawnShipExplosion(x: Float, y: Float) {}
    fun spawnDebris(x: Float, y: Float, velX: Float, velY: Float) {}
    fun update(delta: Float) {}
    fun updateThrust(delta: Float, ship: Ship) {}
    fun renderParticles() {}
    val offsetX: Float get() = 0f
    val offsetY: Float get() = 0f
}

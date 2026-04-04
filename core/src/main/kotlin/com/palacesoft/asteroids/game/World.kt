package com.palacesoft.asteroids.game

import com.palacesoft.asteroids.game.entity.*
import com.palacesoft.asteroids.game.system.BulletPool
import com.palacesoft.asteroids.game.system.CollisionSystem
import com.palacesoft.asteroids.game.system.WaveSystem
import com.palacesoft.asteroids.input.GameInput

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
    var vfx: Any?        = null  // replaced with VfxManager later
}

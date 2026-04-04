package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.palacesoft.asteroids.game.entity.Ship
import kotlin.math.cos
import kotlin.math.sin

class ShipRenderer {
    private val SHIP_COLOR   = Color(0f, 1f, 0.9f, 1f)
    private val THRUST_COLOR = Color(0.4f, 0.7f, 1f, 1f)
    private var thrustFlicker = 0f

    fun update(delta: Float) { thrustFlicker += delta }

    fun render(sr: ShapeRenderer, ship: Ship) {
        if (!ship.visible) return
        val rad = Math.toRadians(ship.rotation.toDouble()).toFloat()
        val cosR = cos(rad); val sinR = sin(rad)
        val r = ship.radius

        val nx = cosR * r;  val ny = sinR * r
        val lx = cos(rad + 2.5f) * r * 0.75f; val ly = sin(rad + 2.5f) * r * 0.75f
        val rx = cos(rad - 2.5f) * r * 0.75f; val ry = sin(rad - 2.5f) * r * 0.75f
        val bx = -cosR * r * 0.5f;            val by = -sinR * r * 0.5f

        sr.begin(ShapeRenderer.ShapeType.Line)
        sr.color = SHIP_COLOR
        sr.line(ship.x + nx, ship.y + ny, ship.x + lx, ship.y + ly)
        sr.line(ship.x + lx, ship.y + ly, ship.x + bx, ship.y + by)
        sr.line(ship.x + bx, ship.y + by, ship.x + rx, ship.y + ry)
        sr.line(ship.x + rx, ship.y + ry, ship.x + nx, ship.y + ny)

        if (ship.thrusting && (thrustFlicker % 0.1f) < 0.05f) {
            sr.color = THRUST_COLOR
            val flameLen = r * (1.2f + (Math.random() * 0.5f).toFloat())
            sr.line(ship.x + lx, ship.y + ly, ship.x - cosR * flameLen, ship.y - sinR * flameLen)
            sr.line(ship.x - cosR * flameLen, ship.y - sinR * flameLen, ship.x + rx, ship.y + ry)
        }
        sr.end()
    }
}

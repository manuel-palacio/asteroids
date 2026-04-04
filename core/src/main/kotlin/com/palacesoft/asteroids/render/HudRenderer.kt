package com.palacesoft.asteroids.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.palacesoft.asteroids.game.World

class HudRenderer(batch: SpriteBatch, camera: OrthographicCamera) {
    private val font = BitmapFont()

    fun render(world: World) {
        // TODO Task 15: full Scene2D HUD
    }

    fun resize(width: Int, height: Int) {}

    fun dispose() { font.dispose() }
}

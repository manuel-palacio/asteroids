package com.palacesoft.asteroids.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.palacesoft.asteroids.AsteroidsGame

class MenuScreen(private val game: AsteroidsGame) : Screen {
    private val font = BitmapFont()
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.03f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        game.batch.begin()
        font.draw(game.batch, "ASTEROIDS - Press SPACE to start", 600f, 450f)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) || Gdx.input.justTouched())
            game.setScreen(GameScreen(game))
    }
    override fun resize(w: Int, h: Int) {}
    override fun show() {}
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() { font.dispose() }
}

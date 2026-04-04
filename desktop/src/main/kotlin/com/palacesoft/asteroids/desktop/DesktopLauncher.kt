package com.palacesoft.asteroids.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.palacesoft.asteroids.AsteroidsGame
import com.palacesoft.asteroids.util.Settings

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        Settings.fxQuality = com.palacesoft.asteroids.effects.EffectQuality.HIGH
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Asteroids")
            setWindowedMode(1600, 900)
            setForegroundFPS(60)
            useVsync(true)
        }
        Lwjgl3Application(AsteroidsGame(), config)
    }
}

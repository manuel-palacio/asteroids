package com.palacesoft.asteroids.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.palacesoft.asteroids.AsteroidsGame

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Asteroids")
            setWindowedMode(1600, 900)
            setForegroundFPS(60)
            useVsync(true)
        }
        Lwjgl3Application(AsteroidsGame(), config)
    }
}

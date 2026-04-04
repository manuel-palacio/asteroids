package com.asteroids.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.asteroids.AsteroidsGame

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        initialize(AsteroidsGame(), config)
    }
}

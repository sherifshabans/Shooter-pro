package com.rafeeqi.kids.shooter

import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class ShooterActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val cfg = AndroidApplicationConfiguration().apply {
            useGL30 = false; numSamples = 0; depth = 0
            useAccelerometer = false; useCompass = false
            r = 8; g = 8; b = 8; a = 0
        }
        initialize(ShooterGame(), cfg)
    }
    @Deprecated("Deprecated")
    override fun onBackPressed() { finish() }
}

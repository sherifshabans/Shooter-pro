package com.rafeeqi.kids.shooter

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity — الواجهة الرئيسية للتطبيق
 * تعرض معلومات اللعبة وزر "العب الآن" الذي يفتح ShooterActivity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvHighScore: TextView
    private lateinit var tvGamesPlayed: TextView
    private lateinit var tvPlayerName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("shooter_profile_v3", MODE_PRIVATE)

        tvHighScore    = findViewById(R.id.tvHighScore)
        tvGamesPlayed  = findViewById(R.id.tvGamesPlayed)
        tvPlayerName   = findViewById(R.id.tvPlayerName)

        val btnPlay    = findViewById<View>(R.id.btnPlay)
        val btnHowTo   = findViewById<View>(R.id.btnHowTo)
        val cardHowTo  = findViewById<View>(R.id.cardHowTo)

        btnPlay.setOnClickListener {
            startActivity(Intent(this, ShooterActivity::class.java))
        }

        // زر "كيف تلعب" — يفتح/يخفي البطاقة
        var howToVisible = false
        btnHowTo.setOnClickListener {
            howToVisible = !howToVisible
            cardHowTo.visibility = if (howToVisible) View.VISIBLE else View.GONE
        }

        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        // تحديث الإحصائيات في كل مرة يرجع المستخدم من اللعبة
        loadStats()
    }

    private fun loadStats() {
        val highScore   = prefs.getInt("highScore", 0)
        val gamesCount  = prefs.getInt("historyCount", 0)
        val playerName  = prefs.getString("name", "مقاتل") ?: "مقاتل"

        tvHighScore.text   = "🏆  $highScore"
        tvGamesPlayed.text = "🎮  $gamesCount مباراة"
        tvPlayerName.text  = playerName
    }

    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        }
    }
}

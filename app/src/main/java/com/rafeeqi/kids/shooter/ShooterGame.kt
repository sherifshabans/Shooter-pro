package com.rafeeqi.kids.shooter

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.g2d.SpriteBatch

// ─────────────────────────────────────────────────────────────────────
//  GameRecord + PlayerProfile
// ─────────────────────────────────────────────────────────────────────
data class GameRecord(val score: Int, val wave: Int, val kills: Int, val level: Int)

object PlayerProfile {
    private lateinit var prefs: Preferences
    var name:  String = "مقاتل"
    var color: Int    = 0       // 0=Blue 1=Green 2=Red 3=Gold 4=Purple

    val history = mutableListOf<GameRecord>()
    var highScore = 0

    fun load() {
        prefs     = Gdx.app.getPreferences("shooter_profile_v3")
        name      = prefs.getString("name", "مقاتل")
        color     = prefs.getInteger("color", 0)
        highScore = prefs.getInteger("highScore", 0)
        history.clear()
        val count = prefs.getInteger("historyCount", 0)
        repeat(count) { i ->
            history.add(GameRecord(
                score  = prefs.getInteger("h${i}_score", 0),
                wave   = prefs.getInteger("h${i}_wave",  0),
                kills  = prefs.getInteger("h${i}_kills", 0),
                level  = prefs.getInteger("h${i}_level", 0)
            ))
        }
    }

    fun save() {
        prefs.putString("name", name)
        prefs.putInteger("color", color)
        prefs.putInteger("highScore", highScore)
        val keep = history.takeLast(10)
        prefs.putInteger("historyCount", keep.size)
        keep.forEachIndexed { i, r ->
            prefs.putInteger("h${i}_score", r.score)
            prefs.putInteger("h${i}_wave",  r.wave)
            prefs.putInteger("h${i}_kills", r.kills)
            prefs.putInteger("h${i}_level", r.level)
        }
        prefs.flush()
    }

    fun addRecord(score: Int, wave: Int, kills: Int, level: Int) {
        history.add(GameRecord(score, wave, kills, level))
        if (score > highScore) highScore = score
        save()
    }
}

// ─────────────────────────────────────────────────────────────────────
//  ShooterGame
// ─────────────────────────────────────────────────────────────────────
class ShooterGame : Game() {
    lateinit var batch: SpriteBatch

    override fun create() {
        batch = SpriteBatch()
        // تهيئة نظام النصوص العربية أولاً (Android Canvas)
        ArabicText.init()
        PlayerProfile.load()
        setScreen(IntroScreen(this))
    }

    override fun dispose() {
        super.dispose()
        batch.dispose()
        ArabicText.dispose()
    }
}

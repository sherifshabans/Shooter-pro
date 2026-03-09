package com.rafeeqi.kids.shooter

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.*


object SoundManager {

    private val sounds    = mutableMapOf<String, Sound?>()
    var sfxVolume         = 0.85f
    private var ready     = false

    fun init() {
        if (ready) return
        try {
            // ── أسلحة ──────────────────────────────────────────
            reg("pistol",     shot(230f,  0.13f, 20f, 0.52f))
            reg("shotgun",    shot(100f,  0.24f,  9f, 0.82f))
            reg("minigun",    shot(300f,  0.07f, 30f, 0.42f))
            reg("melee",      meleeHit())

            // ── إصابات ─────────────────────────────────────────
            reg("hit_enemy",  hit(200f, 0.10f))
            reg("hit_player", hit( 85f, 0.20f, punch = true))
            reg("shield_hit", shieldHit())

            // ── انفجارات ───────────────────────────────────────
            reg("explosion",     explosion(0.50f, 55f))
            reg("big_explosion", explosion(0.80f, 38f))  // للبوص ولقنبلة
            reg("grenade_throw", grenadeThrow())

            // ── بيك أبات ───────────────────────────────────────
            reg("pickup",  pickup(sweepUp = true))
            reg("levelup", levelUpSound())
            reg("click",   click())
            reg("reload",  reload())

            // ── نبض القلب (HP منخفض) ──────────────────────────
            reg("heartbeat", heartbeat())

            // ── موجات / حالة اللعبة ───────────────────────────
            reg("wave_start",  waveStart())
            reg("wave_clear",  waveClear())
            reg("game_over",   gameOver())
            reg("victory",     victory())
            reg("death_player",deathPlayer())

            ready = true
        } catch (e: Exception) {
            Gdx.app.log("SoundManager", "Init failed: ${e.message}")
        }
    }

    // ── Public API ────────────────────────────────────────────────
    fun play(name: String, pitch: Float = 1f, vol: Float = sfxVolume) {
        if (!ready) return
        try {
            val p = (pitch + MathUtils.random(-0.04f, 0.04f)).coerceIn(0.4f, 2.5f)
            sounds[name]?.play(vol.coerceIn(0f, 1f), p, 0f)
        } catch (_: Exception) {}
    }

    fun playShot(weapon: String) = when (weapon) {
        "SHOTGUN" -> play("shotgun",  MathUtils.random(0.88f, 1.05f))
        "MINIGUN" -> play("minigun",  MathUtils.random(0.95f, 1.10f))
        "MELEE"   -> play("melee",    MathUtils.random(0.90f, 1.10f))
        else      -> play("pistol",   MathUtils.random(0.92f, 1.08f))
    }

    fun playExplosion(size: Float = 1f) {
        val name  = if (size >= 0.9f) "big_explosion" else "explosion"
        val pitch = (0.55f + size * 0.45f).coerceIn(0.4f, 2f)
        val vol   = (sfxVolume * (0.6f + size * 0.4f)).coerceIn(0f, 1f)
        play(name, pitch, vol)
    }

    fun playHeartbeat()  = play("heartbeat",  vol = sfxVolume * 0.75f)
    fun playWaveStart()  = play("wave_start")
    fun playWaveClear()  = play("wave_clear")
    fun playGameOver()   = play("game_over",   vol = sfxVolume * 0.95f)
    fun playVictory()    = play("victory",     vol = sfxVolume * 0.90f)
    fun playDeathPlayer()= play("death_player",vol = sfxVolume * 0.95f)
    fun playReload()     = play("reload",      MathUtils.random(0.92f, 1.08f))
    fun playGrenadeThrow()= play("grenade_throw")

    fun dispose() {
        sounds.values.forEach { it?.dispose() }
        sounds.clear()
        ready = false
    }

    // ── PCM builders ──────────────────────────────────────────────
    private val SR = 22050

    private fun reg(name: String, pcm: ShortArray) {
        sounds[name] = loadFromPcm(pcm)
    }

    // طلق رصاص
    private fun shot(freq: Float, dur: Float, decay: Float, noise: Float) =
        ShortArray((SR * dur).toInt()) { i ->
            val t = i.toFloat() / SR
            val env = exp(-decay * t).toFloat()
            clamp(env * (sin(TWO_PI * freq * t).toFloat() * (1 - noise)
                    + MathUtils.random(-1f, 1f) * noise) * 0.90f)
        }

    // إصابة
    private fun hit(freq: Float, dur: Float, punch: Boolean = false) =
        ShortArray((SR * dur).toInt()) { i ->
            val t = i.toFloat() / SR
            val env = exp(-22.0 * t).toFloat()
            val f = if (punch) freq * exp(-15.0 * t).toFloat() else freq
            clamp(env * (sin(TWO_PI * f * t).toFloat() * 0.75f
                    + MathUtils.random(-1f, 1f) * 0.30f) * 0.85f)
        }

    // انفجار
    private fun explosion(dur: Float, basFreq: Float = 55f) =
        ShortArray((SR * dur).toInt()) { i ->
            val t = i.toFloat() / SR
            val env = exp(-5.5 * t).toFloat()
            val bass = sin(TWO_PI * basFreq * t).toFloat()
            val sub  = sin(TWO_PI * basFreq * 0.5 * t).toFloat()
            clamp(env * (MathUtils.random(-1f, 1f) * 0.55f
                    + bass * 0.25f + sub * 0.20f) * 0.98f)
        }

    // رمي قنبلة (ثقيلة تطير)
    private fun grenadeThrow() = ShortArray((SR * 0.20f).toInt()) { i ->
        val t   = i.toFloat() / SR
        val env = exp(-8.0 * t).toFloat()
        val f   = 320f * exp(-4.0 * t).toFloat()
        clamp(env * (sin(TWO_PI * f * t).toFloat() * 0.55f
                + MathUtils.random(-1f, 1f) * 0.45f) * 0.82f)
    }

    // ضربة بالسلاح الأبيض
    private fun meleeHit() = ShortArray((SR * 0.15f).toInt()) { i ->
        val t   = i.toFloat() / SR
        val env = exp(-30.0 * t).toFloat()
        clamp(env * (sin(TWO_PI * 520.0 * t).toFloat() * 0.50f
                + MathUtils.random(-1f, 1f) * 0.55f) * 0.90f)
    }

    // درع
    private fun shieldHit() = ShortArray((SR * 0.18f).toInt()) { i ->
        val t = i.toFloat() / SR
        val env = exp(-13.0 * t).toFloat()
        clamp(env * (sin(TWO_PI * 1100.0 * t).toFloat() * 0.80f
                + MathUtils.random(-1f, 1f) * 0.20f) * 0.72f)
    }

    // بيك آب
    private fun pickup(sweepUp: Boolean, mult: Float = 1f): ShortArray {
        val dur = 0.25f
        return ShortArray((SR * dur).toInt()) { i ->
            val t   = i.toFloat() / SR
            val env = exp(-7.0 * t).toFloat()
            val f   = if (sweepUp) 350f + 700f * (t / dur) * mult else 800f - 400f * (t / dur)
            clamp(env * sin(TWO_PI * f * t).toFloat() * 0.78f)
        }
    }

    // ارتفاع مستوى - 3 نغمات صاعدة
    private fun levelUpSound(): ShortArray {
        val notes = floatArrayOf(523f, 659f, 784f, 1047f)
        val noteDur = (SR * 0.18f).toInt()
        val out = ShortArray(noteDur * notes.size)
        notes.forEachIndexed { ni, freq ->
            val base = ni * noteDur
            for (i in 0 until noteDur) {
                val t   = i.toFloat() / SR
                val env = exp(-6.0 * t).toFloat()
                out[base + i] = clamp(env * sin(TWO_PI * freq * t).toFloat() * 0.82f)
            }
        }
        return out
    }

    // نبض قلب
    private fun heartbeat(): ShortArray {
        val dur = (SR * 0.55f).toInt()
        return ShortArray(dur) { i ->
            val t = i.toFloat() / SR
            // نبضتان سريعتان 0.0 و 0.18
            val beat1 = if (t < 0.14f) exp(-18.0 * t).toFloat() else 0f
            val beat2 = run {
                val t2 = t - 0.22f
                if (t2 in 0f..0.14f) exp(-20.0 * t2).toFloat() else 0f
            }
            val bass = sin(TWO_PI * 65.0 * t).toFloat()
            clamp((beat1 + beat2) * bass * 0.95f)
        }
    }

    // بداية موجة - إنذار ثلاثي
    private fun waveStart(): ShortArray {
        val freqs = floatArrayOf(600f, 720f, 900f)
        val bpDur = (SR * 0.12f).toInt()
        val gap   = (SR * 0.06f).toInt()
        val total = (bpDur + gap) * freqs.size
        val out = ShortArray(total)
        freqs.forEachIndexed { fi, freq ->
            val base = fi * (bpDur + gap)
            for (i in 0 until bpDur) {
                val t   = i.toFloat() / SR
                val env = exp(-8.0 * t).toFloat()
                out[base + i] = clamp(env * sin(TWO_PI * freq * t).toFloat() * 0.80f)
            }
        }
        return out
    }

    // إنهاء موجة - نغمة صاعدة ناعمة
    private fun waveClear(): ShortArray {
        val dur = (SR * 0.70f).toInt()
        return ShortArray(dur) { i ->
            val t   = i.toFloat() / SR
            val env = exp(-4.5 * t).toFloat()
            val f   = 400f + 900f * (t / (dur.toFloat() / SR))
            clamp(env * sin(TWO_PI * f * t).toFloat() * 0.78f)
        }
    }

    // Game Over - دبل مخيف
    private fun gameOver(): ShortArray {
        val dur = (SR * 1.8f).toInt()
        return ShortArray(dur) { i ->
            val t   = i.toFloat() / SR
            val env = exp(-1.8 * t).toFloat()
            val f   = 120f * exp(-0.8 * t).toFloat()
            val lfo = (1f + 0.3f * sin(TWO_PI * 3.0 * t).toFloat())
            clamp(env * lfo * (sin(TWO_PI * f * t).toFloat() * 0.55f
                    + MathUtils.random(-1f, 1f) * 0.45f) * 0.90f)
        }
    }

    // انتصار - فانفار صاعدة
    private fun victory(): ShortArray {
        val notes  = floatArrayOf(392f, 523f, 659f, 784f, 1047f)
        val durs   = floatArrayOf(0.18f, 0.18f, 0.18f, 0.18f, 0.55f)
        val samples = durs.map { (SR * it).toInt() }
        val total   = samples.sum()
        val out     = ShortArray(total)
        var pos = 0
        notes.forEachIndexed { ni, freq ->
            val n = samples[ni]
            for (i in 0 until n) {
                val t   = i.toFloat() / SR
                val env = if (ni == notes.size - 1) exp(-2.5 * t).toFloat()
                          else exp(-5.0 * t).toFloat()
                val harm2 = sin(TWO_PI * freq * 2.0 * t).toFloat() * 0.25f
                out[pos + i] = clamp(env * (sin(TWO_PI * freq * t).toFloat() * 0.70f + harm2) * 0.85f)
            }
            pos += n
        }
        return out
    }

    // موت اللاعب - هبوط دراماتيكي
    private fun deathPlayer(): ShortArray {
        val dur = (SR * 1.4f).toInt()
        return ShortArray(dur) { i ->
            val t   = i.toFloat() / SR
            val env = exp(-2.2 * t).toFloat()
            val f   = 350f * exp(-3.5 * t).toFloat() + 30f
            clamp(env * (sin(TWO_PI * f * t).toFloat() * 0.60f
                    + MathUtils.random(-1f, 1f) * 0.45f) * 0.92f)
        }
    }

    // صوت إعادة التحميل
    private fun reload(): ShortArray {
        val dur = (SR * 0.28f).toInt()
        return ShortArray(dur) { i ->
            val t   = i.toFloat() / SR
            val click1 = if (t < 0.12f) exp(-45.0 * t).toFloat() * sin(TWO_PI * 800.0 * t).toFloat() else 0f
            val click2 = run {
                val t2 = t - 0.16f
                if (t2 >= 0f) exp(-45.0 * t2).toFloat() * sin(TWO_PI * 600.0 * t2).toFloat() else 0f
            }
            clamp((click1 + click2) * 0.80f)
        }
    }

    // كليك
    private fun click() = ShortArray((SR * 0.05f).toInt()) { i ->
        val t = i.toFloat() / SR
        clamp(exp(-55.0 * t).toFloat() * MathUtils.random(-1f, 1f) * 0.55f)
    }

    // ── Utilities ─────────────────────────────────────────────────
    private val TWO_PI = 2.0 * Math.PI

    private fun clamp(v: Float): Short =
        (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    private fun exp(x: Double) = Math.exp(x)
    private fun sin(x: Double) = Math.sin(x)

    private fun loadFromPcm(pcm: ShortArray): Sound? = try {
        val wav = buildWav(pcm, SR)
        val dir = try { Gdx.files.local("").file().parentFile } catch (_: Exception) { null }
        val tmp = if (dir?.exists() == true) File.createTempFile("snd_", ".wav", dir)
                  else File.createTempFile("snd_", ".wav")
        tmp.deleteOnExit()
        tmp.writeBytes(wav)
        val snd = Gdx.audio.newSound(Gdx.files.absolute(tmp.absolutePath))
        tmp.delete()
        snd
    } catch (e: Exception) {
        Gdx.app.log("SoundManager", "Load failed: ${e.message}")
        null
    }

    private fun buildWav(pcm: ShortArray, sr: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val data = pcm.size * 2
        fun w2(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
        fun w4(v: Int) { w2(v); w2(v shr 16) }
        out.write("RIFF".toByteArray()); w4(36 + data)
        out.write("WAVE".toByteArray()); out.write("fmt ".toByteArray())
        w4(16); w2(1); w2(1); w4(sr); w4(sr * 2); w2(2); w2(16)
        out.write("data".toByteArray()); w4(data)
        pcm.forEach { s -> w2(s.toInt()) }
        return out.toByteArray()
    }
}

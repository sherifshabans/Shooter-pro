package com.rafeeqi.kids.shooter

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.math.*

class IntroScreen(private val game: ShooterGame) : Screen {

    private val W = 900f; private val H = 560f
    private val cam      = OrthographicCamera()
    private val viewport = FitViewport(W, H, cam)
    private val shape    = ShapeRenderer()
    private val fontNum  = BitmapFont().apply { data.setScale(1.4f) }
    private val layout   = GlyphLayout()

    // ── حالة عامة ────────────────────────────────────────────────────
    private var tab        = 0          // 0=العب  1=ملفي  2=السجل
    private var animTime   = 0f
    private var introAlpha = 0f
    private var titlePulse = 0f
    private var caretBlink = 0f

    // ── Sidebar ───────────────────────────────────────────────────────
    private val SIDE_W    = 162f
    private var sideSlide = -SIDE_W     // انزلاق للداخل عند البداية
    private val CONT_X    = SIDE_W + 10f

    // أنيميشن تبديل القسم (fade)
    private var tabAlpha   = 1f
    private var fadingOut  = false
    private var nextTab    = 0

    // ── ألوان المقاتلين ───────────────────────────────────────────────
    private val playerColors = listOf(
        Color(0.18f, 0.48f, 0.98f, 1f) to "أزرق",
        Color(0.15f, 0.78f, 0.12f, 1f) to "أخضر",
        Color(0.88f, 0.10f, 0.10f, 1f) to "أحمر",
        Color(0.95f, 0.80f, 0.05f, 1f) to "ذهبي",
        Color(0.55f, 0.04f, 0.80f, 1f) to "بنفسجي"
    )

    // ── إدخال الاسم ──────────────────────────────────────────────────
    private var typingName    = false
    private var nameBuffer    = PlayerProfile.name
    private var caretFlash    = 0f
    private var savedFlash    = 0f     // وميض "تم الحفظ"

    // ── زر اللعب ─────────────────────────────────────────────────────
    private var playBtnScale = 1f
    private var playBtnDir   = 1f

    // ══════════════════════════════════════════════════════════════════
    //  خلفية السماء — نجوم + سدم + شهب + غبار
    // ══════════════════════════════════════════════════════════════════
    private data class Star(
        var x: Float, var y: Float, var spd: Float, var r: Float,
        var bright: Float, val layer: Int,
        var twinkle: Float = MathUtils.random(0f, 6.28f)
    )
    private val stars = List(180) {
        val lay = MathUtils.random(0, 2)
        Star(
            MathUtils.random(0f, 900f), MathUtils.random(0f, 560f),
            MathUtils.random(0.1f, 0.5f) * (lay + 1),
            MathUtils.random(0.5f, 3f) * (0.55f + lay * 0.35f),
            MathUtils.random(0.4f, 1f), lay
        )
    }

    // سدم (ثابتة في الخلفية)
    private data class Nebula(val x: Float, val y: Float, val r: Float,
                               val cr: Float, val cg: Float, val cb: Float, val a: Float)
    private val nebulae = listOf(
        Nebula(140f,  410f, 220f, 0.22f, 0.04f, 0.60f, 0.13f),
        Nebula(800f,  180f, 260f, 0.04f, 0.18f, 0.65f, 0.11f),
        Nebula(460f,  530f, 170f, 0.58f, 0.04f, 0.22f, 0.09f),
        Nebula(660f,  450f, 145f, 0.04f, 0.48f, 0.38f, 0.08f),
        Nebula(220f,   90f, 185f, 0.42f, 0.10f, 0.58f, 0.07f),
        Nebula(500f,  300f, 130f, 0.08f, 0.30f, 0.55f, 0.06f)
    )

    // شهب متحركة
    private data class ShootingStar(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, val maxLife: Float,
        val cr: Float, val cg: Float, val cb: Float
    )
    private val shootingStars = mutableListOf<ShootingStar>()
    private var shootTimer    = MathUtils.random(0.8f, 2.5f)

    // غبار كوني عائم
    private data class DustParticle(
        var x: Float, var y: Float, var vy: Float,
        var life: Float, val maxLife: Float,
        val cr: Float, val cg: Float, val cb: Float, val r: Float
    )
    private val dustParticles = mutableListOf<DustParticle>()
    private var dustTimer     = 0f

    init {
        cam.setToOrtho(false, W, H)
        SoundManager.init()
        repeat(25) { spawnDust(MathUtils.random(0f, W), MathUtils.random(0f, H)) }
    }

    override fun show() {}

    // ══════════════════════════════════════════════════════════════════
    //  Render
    // ══════════════════════════════════════════════════════════════════
    override fun render(delta: Float) {
        val dt = delta.coerceIn(0f, 0.05f)
        animTime   += dt
        introAlpha  = (introAlpha + dt * 2.5f).coerceAtMost(1f)
        titlePulse  = abs(sin(animTime * 1.7f)).toFloat()
        caretBlink += dt; caretFlash += dt
        playBtnScale = (playBtnScale + playBtnDir * dt * 0.55f).coerceIn(0.93f, 1.07f)
        if (playBtnScale >= 1.07f || playBtnScale <= 0.93f) playBtnDir = -playBtnDir
        if (savedFlash > 0f) savedFlash -= dt

        // ── Sidebar انزلاق ──────────────────────────────────────────
        if (sideSlide < 0f) sideSlide = (sideSlide + dt * SIDE_W * 10f).coerceAtMost(0f)

        // ── fade انتقال بين التبويبات ────────────────────────────────
        if (fadingOut) {
            tabAlpha -= dt * 9f
            if (tabAlpha <= 0f) { tab = nextTab; tabAlpha = 0f; fadingOut = false }
        } else {
            tabAlpha = (tabAlpha + dt * 9f).coerceAtMost(1f)
        }

        // ── تحديث النجوم ────────────────────────────────────────────
        stars.forEach { s ->
            s.x -= s.spd * dt * 20f
            s.twinkle += dt * MathUtils.random(1.2f, 3.8f)
            if (s.x < -4f) { s.x = W + 4f; s.y = MathUtils.random(0f, H) }
        }

        // ── شهب ──────────────────────────────────────────────────────
        shootTimer -= dt
        if (shootTimer <= 0f) {
            shootTimer = MathUtils.random(1.2f, 4.0f)
            val ang = Math.toRadians(MathUtils.random(200f, 248f).toDouble())
            val spd = MathUtils.random(480f, 720f)
            val cols = listOf(Triple(0.88f, 0.92f, 1f), Triple(1f, 0.85f, 0.55f), Triple(0.55f, 0.88f, 1f)).random()
            shootingStars.add(ShootingStar(
                MathUtils.random(80f, W), MathUtils.random(H * 0.35f, H + 30f),
                cos(ang).toFloat() * spd, sin(ang).toFloat() * spd,
                0f, MathUtils.random(0.4f, 0.9f), cols.first, cols.second, cols.third
            ))
        }
        shootingStars.forEach { s -> s.x += s.vx * dt; s.y += s.vy * dt; s.life += dt }
        shootingStars.removeAll { it.life >= it.maxLife }

        // ── غبار ─────────────────────────────────────────────────────
        dustTimer -= dt
        if (dustTimer <= 0f) {
            dustTimer = MathUtils.random(0.3f, 0.9f)
            spawnDust(MathUtils.random(CONT_X, W), MathUtils.random(-8f, H * 0.4f))
        }
        dustParticles.forEach { p -> p.y += p.vy * dt; p.life -= dt }
        dustParticles.removeAll { it.life <= 0f || it.y > H + 15f }

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.07f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        viewport.apply()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        handleInput()
        drawSky()
        drawSidebar()
        when (tab) {
            0 -> drawPlayTab()
            1 -> drawProfileTab()
            2 -> drawHistoryTab()
        }
    }

    private fun spawnDust(x: Float, y: Float) {
        val cols = listOf(Triple(0.38f, 0.68f, 1f), Triple(0.72f, 0.38f, 1f),
                          Triple(0.22f, 0.80f, 0.62f), Triple(1f, 0.62f, 0.28f)).random()
        dustParticles.add(DustParticle(x, y, MathUtils.random(6f, 20f),
            MathUtils.random(3f, 7f), 6f, cols.first, cols.second, cols.third,
            MathUtils.random(1.2f, 3.8f)))
    }

    // ══════════════════════════════════════════════════════════════════
    //  Input
    // ══════════════════════════════════════════════════════════════════
    private fun handleInput() {
        // كيبورد
        if (typingName) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && nameBuffer.isNotEmpty())
                nameBuffer = nameBuffer.dropLast(1)
            for (k in Input.Keys.A..Input.Keys.Z) {
                if (Gdx.input.isKeyJustPressed(k) && nameBuffer.length < 16) {
                    val shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                                Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
                    nameBuffer += if (shift) ('A' + (k - Input.Keys.A)) else ('a' + (k - Input.Keys.A))
                }
            }
            for (k in Input.Keys.NUM_0..Input.Keys.NUM_9)
                if (Gdx.input.isKeyJustPressed(k) && nameBuffer.length < 16)
                    nameBuffer += ('0' + (k - Input.Keys.NUM_0))
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && nameBuffer.length < 16) nameBuffer += ' '
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
                commitName()
        }

        if (!Gdx.input.justTouched()) return
        val wp = viewport.unproject(Vector2(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat()))

        // لمس خارج صندوق الاسم = حفظ تلقائي
        if (typingName) { commitName(); return }

        // لمس Sidebar
        val sx = sideSlide
        if (wp.x in sx .. sx + SIDE_W) {
            val itemH = 64f; val startY = H * 0.60f
            for (i in 0..2) {
                if (wp.y in (startY - i * itemH - itemH * 0.5f)..(startY - i * itemH + itemH * 0.5f)) {
                    if (i != tab && !fadingOut) { nextTab = i; fadingOut = true; SoundManager.play("click", pitch = 1.05f, vol = 0.35f) }
                    return
                }
            }
            return
        }

        // لمس منطقة المحتوى
        when (tab) {
            0 -> {
                val cx = CONT_X + (W - CONT_X) / 2f; val cy = H * 0.36f
                if (Vector2.dst(wp.x, wp.y, cx, cy) < 80f * playBtnScale + 15f) {
                    SoundManager.play("wave_start"); game.setScreen(ArenaScreen(game)); dispose()
                }
            }
            1 -> handleProfileTouch(wp)
        }
    }

    private fun commitName() {
        typingName = false
        PlayerProfile.name = nameBuffer.trim().ifBlank { "مقاتل" }
        nameBuffer = PlayerProfile.name
        PlayerProfile.save()
        savedFlash = 2.2f
        Gdx.input.setOnscreenKeyboardVisible(false)
    }

    private fun handleProfileTouch(wp: Vector2) {
        val cx = CONT_X + 10f; val nbW = (W - cx - 18f) * 0.60f
        val nbY = H * 0.695f
        if (wp.x in cx .. cx + nbW && wp.y in nbY .. nbY + 38f) {
            typingName = true; Gdx.input.setOnscreenKeyboardVisible(true); return
        }
        // زر مسح
        if (wp.x in cx + nbW + 6f .. cx + nbW + 38f && wp.y in nbY .. nbY + 38f) {
            nameBuffer = ""; typingName = true; Gdx.input.setOnscreenKeyboardVisible(true); return
        }
        // ألوان
        val colY = H * 0.42f; val colStartX = cx + 46f; val colGap = 82f
        playerColors.forEachIndexed { i, _ ->
            if (Vector2.dst(wp.x, wp.y, colStartX + i * colGap, colY) < 30f) {
                PlayerProfile.color = i; PlayerProfile.save()
                SoundManager.play("click", pitch = 1.2f)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  drawSky — خلفية السماء الكاملة
    // ══════════════════════════════════════════════════════════════════
    fun drawSky(full: Boolean = true) {
        shape.begin(ShapeRenderer.ShapeType.Filled)

        // تدرج السماء
        shape.color = Color(0.02f, 0.02f, 0.07f, 1f); shape.rect(0f, H * 0.5f, W, H * 0.5f)
        shape.color = Color(0.04f, 0.04f, 0.12f, 1f); shape.rect(0f, 0f, W, H * 0.5f)

        // سدم
        nebulae.forEach { n ->
            val alpha = (n.a * introAlpha).coerceIn(0f, 0.20f)
            shape.color = Color(n.cr, n.cg, n.cb, alpha * 1.3f); shape.circle(n.x, n.y, n.r)
            shape.color = Color(n.cr, n.cg, n.cb, alpha * 0.65f); shape.circle(n.x, n.y, n.r * 1.6f)
        }

        // نجوم
        stars.forEach { s ->
            val ta = (0.50f + 0.50f * sin(s.twinkle).toFloat()) * s.bright * introAlpha
            val c = when (s.layer) {
                0    -> Color(ta * 0.78f, ta * 0.82f, ta, ta)
                1    -> Color(ta * 0.88f, ta * 0.90f, ta, ta)
                else -> Color(ta, ta * 0.97f, ta * 0.90f, ta)
            }
            shape.color = c; shape.circle(s.x, s.y, s.r)
            if (s.r > 1.9f) {
                shape.color = Color(c.r, c.g, c.b, ta * 0.22f); shape.circle(s.x, s.y, s.r * 3f)
            }
        }

        // غبار كوني
        dustParticles.forEach { p ->
            val la = ((p.life / p.maxLife) * 0.40f * introAlpha).coerceIn(0f, 0.40f)
            shape.color = Color(p.cr, p.cg, p.cb, la); shape.circle(p.x, p.y, p.r)
        }
        shape.end()

        // شهب
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2.5f)
        shootingStars.forEach { s ->
            val prog = s.life / s.maxLife
            val tailLen = 55f + prog * 85f
            val speed   = sqrt((s.vx * s.vx + s.vy * s.vy).toDouble()).toFloat()
            val nx = s.vx / speed; val ny = s.vy / speed
            val ha = (1f - prog) * 0.92f * introAlpha
            shape.color = Color(s.cr, s.cg, s.cb, ha)
            shape.line(s.x, s.y, s.x - nx * tailLen, s.y - ny * tailLen)
        }
        Gdx.gl.glLineWidth(1f)
        shape.end()

        shape.begin(ShapeRenderer.ShapeType.Filled)
        shootingStars.forEach { s ->
            val prog = s.life / s.maxLife
            val ha   = (1f - prog) * 0.95f * introAlpha
            shape.color = Color(1f, 1f, 1f, ha);              shape.circle(s.x, s.y, 3f)
            shape.color = Color(s.cr, s.cg, s.cb, ha * 0.5f); shape.circle(s.x, s.y, 6.5f)
        }
        shape.end()
    }

    // ══════════════════════════════════════════════════════════════════
    //  Sidebar
    // ══════════════════════════════════════════════════════════════════
    private fun drawSidebar() {
        val sx = sideSlide
        val a  = introAlpha
        val pCol = playerColors[PlayerProfile.color.coerceIn(0, playerColors.size - 1)].first

        shape.begin(ShapeRenderer.ShapeType.Filled)
        // ظل يمين
        shape.color = Color(0f, 0f, 0f, 0.50f * a); shape.rect(sx + SIDE_W, 0f, 18f, H)
        // خلفية
        shape.color = Color(0.04f, 0.04f, 0.13f, 0.96f * a); shape.rect(sx, 0f, SIDE_W, H)
        shape.color = Color(0.06f, 0.08f, 0.20f, 0.38f * a); shape.rect(sx, H * 0.45f, SIDE_W, H * 0.55f)
        // خط توهج أيمن
        shape.color = Color(pCol.r * 0.55f, pCol.g * 0.55f, pCol.b, 0.60f * a)
        shape.rect(sx + SIDE_W - 2.5f, 0f, 2.5f, H)
        shape.end()

        // ── لوجو ──────────────────────────────────────────────────────
        val logoX = sx + SIDE_W / 2f; val logoY = H - 78f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(pCol.r, pCol.g, pCol.b, (0.08f + titlePulse * 0.07f) * a)
        shape.circle(logoX, logoY + 8f, 68f)
        shape.end()
        game.batch.begin()
        ArabicText.drawCenter(game.batch, "شوتر", logoX, logoY + 22f,
            35f, 0.55f + titlePulse * 0.45f, 0.80f, 1f, a)
        ArabicText.drawCenter(game.batch, "المحترف", logoX, logoY - 8f,
            27f, 1f, 0.72f, 0.15f, a)
        game.batch.end()

        // فاصل أسفل اللوجو
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(pCol.r * 0.4f, pCol.g * 0.4f, pCol.b * 0.8f, 0.40f * a)
        shape.rect(sx + 14f, H - 106f, SIDE_W - 28f, 1.5f)
        shape.end()

        // ── عناصر القائمة الثلاثة ─────────────────────────────────────
        val labels  = listOf("▶  العب", "◉  ملفي", "≡  السجل")
        val itemH   = 64f; val startY = H * 0.60f

        labels.forEachIndexed { i, label ->
            val iy     = startY - i * itemH
            val active = i == tab

            shape.begin(ShapeRenderer.ShapeType.Filled)
            if (active) {
                // خلفية نشطة
                shape.color = Color(pCol.r * 0.22f, pCol.g * 0.18f, pCol.b * 0.50f, 0.88f * a)
                shape.rect(sx, iy - itemH * 0.46f, SIDE_W, itemH * 0.92f)
                // توهج داخلي
                shape.color = Color(pCol.r, pCol.g, pCol.b, 0.07f * a)
                shape.rect(sx, iy - itemH * 0.46f, SIDE_W, itemH * 0.92f)
                // شريط جانبي ساطع
                shape.color = Color(pCol.r, pCol.g, pCol.b, a)
                shape.rect(sx + SIDE_W - 4f, iy - itemH * 0.46f, 4f, itemH * 0.92f)
                // نقطة مؤشر
                shape.color = Color(pCol.r, pCol.g, pCol.b, 0.90f * a)
                shape.circle(sx + 18f, iy, 5.5f)
            } else {
                shape.color = Color(0.08f, 0.10f, 0.20f, 0.30f * a)
                shape.rect(sx + 8f, iy - itemH * 0.43f, SIDE_W - 16f, itemH * 0.86f)
            }
            shape.end()

            // فاصل
            if (i < 2) {
                shape.begin(ShapeRenderer.ShapeType.Filled)
                shape.color = Color(0.14f, 0.17f, 0.30f, 0.42f * a)
                shape.rect(sx + 16f, iy - itemH * 0.5f, SIDE_W - 32f, 1f)
                shape.end()
            }

            game.batch.begin()
            ArabicText.drawCenter(game.batch, label, sx + SIDE_W / 2f, iy + 7f,
                if (active) 24f else 21f,
                if (active) pCol.r else 0.52f,
                if (active) pCol.g else 0.60f,
                if (active) pCol.b else 0.78f,
                (if (active) 1f else 0.62f) * a)
            game.batch.end()
        }

        // ── معلومات اللاعب أسفل Sidebar ───────────────────────────────
        val bY = 56f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.06f, 0.08f, 0.18f, 0.80f * a); shape.rect(sx + 7f, bY - 28f, SIDE_W - 14f, 62f)
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.35f * a); shape.rect(sx + 7f, bY + 30f, SIDE_W - 14f, 2f)
        // أفاتار صغير
        shape.color = Color(pCol.r * 0.5f, pCol.g * 0.5f, pCol.b * 0.8f, a); shape.circle(sx + 26f, bY, 15f)
        shape.color = pCol; shape.circle(sx + 26f, bY, 12f)
        shape.color = Color(1f, 0.9f, 0.2f, a)
        shape.circle(sx + 23.5f, bY + 2.5f, 2.8f); shape.circle(sx + 29.5f, bY + 2.5f, 2.8f)
        shape.end()
        game.batch.begin()
        ArabicText.draw(game.batch, PlayerProfile.name, sx + 45f, bY + 10f, 18f, pCol.r, pCol.g, pCol.b, a)
        ArabicText.draw(game.batch, "HS: ${PlayerProfile.highScore}", sx + 45f, bY - 9f, 15f, 0.52f, 0.68f, 0.88f, a * 0.78f)
        game.batch.end()
    }

    // ══════════════════════════════════════════════════════════════════
    //  تبويب العب
    // ══════════════════════════════════════════════════════════════════
    private fun drawPlayTab() {
        val a   = introAlpha * tabAlpha
        val cx  = CONT_X + (W - CONT_X) / 2f
        val pCol = playerColors[PlayerProfile.color.coerceIn(0, playerColors.size - 1)].first

        game.batch.begin()
        val ts = 70f + titlePulse * 8f
        ArabicText.drawCenter(game.batch, "شوتر المحترف", cx, H * 0.82f,
            ts, 0.52f + titlePulse * 0.48f, 0.72f, 1f, a)
        ArabicText.drawCenter(game.batch, "جاهز للمعركة؟", cx, H * 0.68f,
            24f, 0.45f, 0.65f, 0.90f, a * 0.80f)
        game.batch.end()

        // ── زر اللعب ──────────────────────────────────────────────────
        val cy = H * 0.36f; val r = 78f * playBtnScale
        shape.begin(ShapeRenderer.ShapeType.Filled)
        // هالات خارجية
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.05f * a * titlePulse); shape.circle(cx, cy, r + 52f)
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.10f * a);              shape.circle(cx, cy, r + 30f)
        // جسم الزر
        shape.color = Color(0.04f, 0.09f, 0.26f, 0.93f * a); shape.circle(cx, cy, r)
        shape.color = Color(pCol.r * 0.28f, pCol.g * 0.28f, pCol.b * 0.55f, 0.88f * a); shape.circle(cx, cy, r)
        shape.color = Color(0.16f, 0.42f, 0.92f, 0.96f * a);  shape.circle(cx, cy, r - 10f)
        // لمعة علوية
        shape.color = Color(0.44f, 0.68f, 1f, 0.35f * a); shape.arc(cx, cy, r - 10f, 22f, 136f)
        // مثلث تشغيل
        val ts2 = 27f * playBtnScale
        shape.color = Color(1f, 1f, 1f, 0.97f * a)
        shape.triangle(cx - ts2 * 0.52f, cy - ts2, cx - ts2 * 0.52f, cy + ts2, cx + ts2, cy)
        shape.end()
        // حلقة دوارة
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(3f)
        shape.color = Color(pCol.r * 0.6f + 0.35f, pCol.g * 0.6f + 0.2f, 1f, 0.72f * a)
        shape.arc(cx, cy, r + 6f, animTime * 42f, 290f)
        Gdx.gl.glLineWidth(1.5f)
        shape.color = Color(0.38f, 0.62f, 1f, 0.42f * a); shape.circle(cx, cy, r + 2f)
        Gdx.gl.glLineWidth(1f)
        shape.end()

        game.batch.begin()
        ArabicText.drawCenter(game.batch, "العب الآن!", cx, cy - r - 22f, 32f, 1f, 0.86f, 0.16f, a)
        if (PlayerProfile.history.isNotEmpty()) {
            ArabicText.drawCenter(game.batch, "أعلى نقطة: ${PlayerProfile.highScore}",
                cx, H * 0.13f, 26f, 1f, 0.82f, 0.08f, a * (0.60f + titlePulse * 0.40f))
        }
        ArabicText.drawCenter(game.batch, "اضغط للعب!",
            cx, H * 0.055f, 20f, 0.48f, 0.72f, 1f, a * (0.45f + titlePulse * 0.55f))
        game.batch.end()
    }

    // ══════════════════════════════════════════════════════════════════
    //  تبويب الملف الشخصي
    // ══════════════════════════════════════════════════════════════════
    private fun drawProfileTab() {
        val a    = introAlpha * tabAlpha
        val cx   = CONT_X + 10f
        val cW   = W - cx - 14f
        val pCol = playerColors[PlayerProfile.color.coerceIn(0, playerColors.size - 1)].first
        val pName = playerColors[PlayerProfile.color.coerceIn(0, playerColors.size - 1)].second

        // ── هيدر ──────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(pCol.r * 0.10f, pCol.g * 0.06f, pCol.b * 0.22f, 0.78f * a)
        shape.rect(cx, H * 0.80f, cW, H * 0.17f)
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.55f * a); shape.rect(cx, H * 0.97f, cW, 3.5f)
        shape.end()

        // ── أفاتار ────────────────────────────────────────────────────
        val avX = cx + 68f; val avY = H * 0.86f; val avR = 34f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        val pp = 0.05f + sin(animTime * 2.8f).toFloat() * 0.04f
        shape.color = Color(pCol.r, pCol.g, pCol.b, pp * a);    shape.circle(avX, avY, avR + 18f)
        shape.color = Color(0f, 0f, 0f, 0.38f * a);             shape.circle(avX + 2f, avY - 2f, avR + 4f)
        shape.color = Color(pCol.r * 0.5f, pCol.g * 0.5f, pCol.b * 0.78f, a); shape.circle(avX, avY, avR + 4f)
        shape.color = pCol;                                       shape.circle(avX, avY, avR)
        shape.color = Color(pCol.r * 0.38f, pCol.g * 0.38f, pCol.b * 0.62f, a); shape.arc(avX, avY, avR, 0f, 180f)
        val hR = avR * 0.46f
        shape.color = Color(0.16f, 0.16f, 0.22f, 0.92f * a);   shape.circle(avX, avY + avR * 0.25f, hR + 3f)
        shape.color = Color(pCol.r * 0.72f, pCol.g * 0.72f, pCol.b * 0.95f, a); shape.circle(avX, avY + avR * 0.25f, hR)
        shape.color = Color(1f, 0.92f, 0.22f, a)
        shape.circle(avX - hR * 0.38f, avY + avR * 0.32f, 4.2f); shape.circle(avX + hR * 0.38f, avY + avR * 0.32f, 4.2f)
        shape.color = Color(0.20f, 0.20f, 0.26f, 0.88f * a)
        shape.rectLine(avX + avR * 0.22f, avY, avX + avR + 16f, avY + 3f, 4.5f)
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2.5f)
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.85f * a); shape.circle(avX, avY, avR + 4f)
        Gdx.gl.glLineWidth(1f)
        shape.end()

        // معلومات بجانب الأفاتار
        val iX = avX + avR + 28f; val iY = H * 0.90f
        val hs = PlayerProfile.highScore
        val games = PlayerProfile.history.size
        val bestW = if (PlayerProfile.history.isNotEmpty()) PlayerProfile.history.maxOf { it.wave } else 0
        game.batch.begin()
        ArabicText.draw(game.batch, PlayerProfile.name, iX, iY,   34f, pCol.r, pCol.g, pCol.b, a)
        ArabicText.draw(game.batch, "لون: $pName",     iX, iY - 30f, 19f, 0.62f, 0.72f, 0.90f, a * 0.85f)
        ArabicText.draw(game.batch, "أعلى نقطة:",      iX, iY - 52f, 17f, 0.48f, 0.58f, 0.76f, a * 0.72f)
        ArabicText.draw(game.batch, "$hs",             iX + 105f, iY - 52f, 22f, 1f, 0.82f, 0.10f, a)
        ArabicText.draw(game.batch, "مباريات: $games  •  أعلى موجة: $bestW/5",
            iX, iY - 73f, 16f, 0.48f, 0.58f, 0.76f, a * 0.70f)
        game.batch.end()

        // ════ قسم الاسم ═══════════════════════════════════════════════
        val secNY = H * 0.73f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(pCol.r * 0.07f, pCol.g * 0.04f, pCol.b * 0.18f, 0.88f * a)
        shape.rect(cx, secNY - 58f, cW, 64f)
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.28f * a); shape.rect(cx, secNY + 6f, cW, 1.5f)
        shape.end()

        game.batch.begin()
        ArabicText.draw(game.batch, "الاسم", cx + 12f, secNY, 20f, 0.58f, 0.68f, 0.90f, a)
        if (savedFlash > 0f) {
            val sa = (savedFlash / 2.2f).coerceIn(0f, 1f)
            ArabicText.draw(game.batch, "✓ محفوظ", cx + 88f, secNY, 20f, 0.15f, 1f, 0.42f, sa * a)
        }
        game.batch.end()

        val nbX = cx + 12f; val nbY = secNY - 50f; val nbW = cW * 0.60f; val nbH = 36f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.02f, 0.03f, 0.10f, 0.92f * a); shape.rect(nbX, nbY, nbW, nbH)
        if (typingName) {
            shape.color = Color(pCol.r * 0.20f, pCol.g * 0.10f, pCol.b * 0.42f, 0.45f * a)
            shape.rect(nbX, nbY, nbW, nbH)
        }
        // زر مسح (×)
        shape.color = Color(0.65f, 0.12f, 0.12f, 0.70f * a); shape.circle(nbX + nbW + 20f, nbY + nbH / 2f, 14f)
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(if (typingName) 2.2f else 1.5f)
        shape.color = if (typingName) Color(pCol.r * 0.65f, pCol.g * 0.65f, 1f, a)
                      else            Color(0.22f, 0.28f, 0.45f, 0.72f * a)
        shape.rect(nbX, nbY, nbW, nbH)
        Gdx.gl.glLineWidth(1f)
        shape.end()

        val disp = if (typingName && (caretFlash % 1f) < 0.5f) "$nameBuffer|" else nameBuffer
        game.batch.begin()
        ArabicText.draw(game.batch, disp, nbX + 10f, nbY + nbH, 24f, 1f, 1f, 1f, a)
        ArabicText.draw(game.batch, "${16 - nameBuffer.length}", nbX + nbW - 24f, nbY + nbH,
            16f, 0.35f, 0.48f, 0.68f, a * 0.72f)
        ArabicText.draw(game.batch, "×", nbX + nbW + 14f, nbY + nbH, 22f, 1f, 0.50f, 0.50f, a)
        val hint = if (typingName) "Enter أو Esc = حفظ" else "اضغط لتعديل الاسم"
        ArabicText.draw(game.batch, hint, nbX + 2f, nbY - 5f, 15f, 0.38f, 0.52f, 0.76f, a * 0.65f)
        game.batch.end()

        // ════ اختيار اللون ══════════════════════════════════════════════
        val secCY = H * 0.475f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(pCol.r * 0.07f, pCol.g * 0.04f, pCol.b * 0.18f, 0.88f * a)
        shape.rect(cx, secCY - 70f, cW, 78f)
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.28f * a); shape.rect(cx, secCY + 8f, cW, 1.5f)
        shape.end()
        game.batch.begin()
        ArabicText.draw(game.batch, "لون المقاتل", cx + 12f, secCY, 20f, 0.58f, 0.68f, 0.90f, a)
        game.batch.end()

        val colY = secCY - 38f; val colStartX = cx + 46f; val colGap = 82f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        playerColors.forEachIndexed { i, (col, colName) ->
            val ccx = colStartX + i * colGap; val sel = PlayerProfile.color == i
            val rd = if (sel) 21f else 16f
            if (sel) {
                shape.color = Color(col.r, col.g, col.b, 0.20f * a); shape.circle(ccx, colY, rd + 14f)
                shape.color = Color(col.r * 0.45f, col.g * 0.45f, col.b * 0.78f, 0.55f * a); shape.circle(ccx, colY, rd + 6f)
            }
            shape.color = Color(col.r * 0.40f, col.g * 0.40f, col.b * 0.55f, a); shape.circle(ccx, colY, rd + 4f)
            shape.color = Color(col.r, col.g, col.b, a); shape.circle(ccx, colY, rd)
            shape.color = Color(1f, 1f, 1f, 0.28f * a); shape.circle(ccx - rd * 0.20f, colY + rd * 0.28f, rd * 0.30f)
        }
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(3f)
        playerColors.forEachIndexed { i, (col, _) ->
            if (PlayerProfile.color == i) {
                shape.color = Color(1f, 1f, 1f, 0.90f * a); shape.circle(colStartX + i * colGap, colY, 27f)
            }
        }
        Gdx.gl.glLineWidth(1f)
        shape.end()
        game.batch.begin()
        playerColors.forEachIndexed { i, (col, colName) ->
            ArabicText.drawCenter(game.batch, colName, colStartX + i * colGap, colY - 28f,
                15f, col.r, col.g, col.b, (if (PlayerProfile.color == i) 1f else 0.58f) * a)
        }
        game.batch.end()

        // ════ إحصائيات ══════════════════════════════════════════════════
        val secSY = H * 0.28f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.04f, 0.05f, 0.14f, 0.85f * a); shape.rect(cx, 8f, cW, secSY - 8f)
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.22f * a); shape.rect(cx, secSY, cW, 1.5f)
        shape.end()
        game.batch.begin()
        ArabicText.draw(game.batch, "إحصائياتي", cx + 12f, secSY - 8f, 20f, 0.58f, 0.68f, 0.90f, a)
        val totalKills = PlayerProfile.history.sumOf { it.kills }
        val bestLvl    = if (PlayerProfile.history.isNotEmpty()) PlayerProfile.history.maxOf { it.level } else 1
        val stats = listOf(
            Triple("أعلى نقطة",     "$hs",         Color(1f, 0.84f, 0.12f, 1f)),
            Triple("أعلى موجة",     "$bestW / 5",  Color(0.28f, 1f, 0.48f, 1f)),
            Triple("مجموع القتلات", "$totalKills",  Color(1f, 0.38f, 0.18f, 1f)),
            Triple("عدد المباريات", "$games",        Color(0.48f, 0.76f, 1f, 1f)),
            Triple("أعلى مستوى",   "LV $bestLvl",   Color(0.68f, 0.48f, 1f, 1f))
        )
        stats.forEachIndexed { i, (label, value, col) ->
            val sy = secSY - 36f - i * 29f
            if (sy < 16f) return@forEachIndexed
            ArabicText.draw(game.batch, label,   cx + 12f,       sy, 17f, 0.48f, 0.58f, 0.76f, a * 0.78f)
            ArabicText.draw(game.batch, value,   cx + 178f,      sy, 20f, col.r, col.g, col.b, a)
        }
        game.batch.end()
    }

    // ══════════════════════════════════════════════════════════════════
    //  تبويب السجل
    // ══════════════════════════════════════════════════════════════════
    private fun drawHistoryTab() {
        val a    = introAlpha * tabAlpha
        val cx   = CONT_X + 10f
        val cW   = W - cx - 14f

        game.batch.begin()
        ArabicText.draw(game.batch, "سجل المباريات", cx + 14f, H * 0.94f, 34f, 1f, 0.86f, 0.28f, a)
        game.batch.end()

        if (PlayerProfile.history.isEmpty()) {
            game.batch.begin()
            ArabicText.drawCenter(game.batch, "لم تلعب أي مباراة بعد!", cx + cW / 2f, H / 2f + 20f, 28f, 0.42f, 0.50f, 0.65f, a)
            ArabicText.drawCenter(game.batch, "العب وحقق أعلى نقطة!", cx + cW / 2f, H / 2f - 16f, 20f, 0.30f, 0.38f, 0.52f, a * 0.68f)
            game.batch.end()
            return
        }

        val totalGames = PlayerProfile.history.size
        val totalKills = PlayerProfile.history.sumOf { it.kills }
        val bestWave   = PlayerProfile.history.maxOf { it.wave }

        // شريط إجمالي ذهبي
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.16f, 0.12f, 0.02f, 0.88f * a); shape.rect(cx, H * 0.84f, cW, 50f)
        shape.color = Color(1f, 0.82f, 0.10f, 0.55f * a);    shape.rect(cx, H * 0.84f + 48f, cW, 2f)
        shape.end()
        game.batch.begin()
        ArabicText.draw(game.batch, "الرقم القياسي:", cx + 10f,  H * 0.84f + 36f, 19f, 0.70f, 0.60f, 0.10f, a)
        ArabicText.draw(game.batch, "${PlayerProfile.highScore}", cx + 148f, H * 0.84f + 36f, 25f, 1f, 0.88f, 0.14f, a)
        ArabicText.draw(game.batch, "مباريات: $totalGames",   cx + 280f, H * 0.84f + 36f, 18f, 0.52f, 0.72f, 1f, a)
        ArabicText.draw(game.batch, "موجة: $bestWave/5",      cx + 452f, H * 0.84f + 36f, 18f, 0.32f, 0.90f, 0.52f, a)
        ArabicText.draw(game.batch, "قتلات: $totalKills",     cx + 592f, H * 0.84f + 36f, 18f, 1f, 0.40f, 0.18f, a)
        game.batch.end()

        // رأس الجدول
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.07f, 0.09f, 0.20f, 0.90f * a); shape.rect(cx, H * 0.77f, cW, 27f)
        shape.end()
        game.batch.begin()
        ArabicText.draw(game.batch, "#",       cx + 8f,   H * 0.77f + 19f, 17f, 0.55f, 0.65f, 0.85f, a)
        ArabicText.draw(game.batch, "النتيجة", cx + 50f,  H * 0.77f + 19f, 17f, 0.55f, 0.65f, 0.85f, a)
        ArabicText.draw(game.batch, "الموجة",  cx + 182f, H * 0.77f + 19f, 17f, 0.55f, 0.65f, 0.85f, a)
        ArabicText.draw(game.batch, "القتلى",  cx + 292f, H * 0.77f + 19f, 17f, 0.55f, 0.65f, 0.85f, a)
        ArabicText.draw(game.batch, "المستوى", cx + 398f, H * 0.77f + 19f, 17f, 0.55f, 0.65f, 0.85f, a)
        ArabicText.draw(game.batch, "التقييم", cx + 512f, H * 0.77f + 19f, 17f, 0.55f, 0.65f, 0.85f, a)
        game.batch.end()

        // صفوف البيانات
        val rows = PlayerProfile.history.reversed().take(10)
        rows.forEachIndexed { i, r ->
            val ry = H * 0.740f - i * 35f
            if (ry < 14f) return@forEachIndexed
            val isTop = r.score == PlayerProfile.highScore
            val even  = i % 2 == 0

            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.color = when { isTop -> Color(0.18f, 0.14f, 0.02f, 0.44f * a); even -> Color(0.05f, 0.07f, 0.16f, 0.52f * a); else -> Color(0.03f, 0.05f, 0.12f, 0.38f * a) }
            shape.rect(cx, ry - 27f, cW, 31f)
            val wPct = r.wave.toFloat() / 5f
            shape.color = Color(wPct * 0.16f, wPct * 0.82f, 0.32f + wPct * 0.32f, 0.62f * a)
            shape.rect(cx, ry - 27f, 4f, 31f)
            shape.end()

            val (cr, cg, cb) = when { isTop -> Triple(1f, 0.86f, 0.10f); even -> Triple(0.72f, 0.86f, 1f); else -> Triple(0.52f, 0.58f, 0.70f) }
            game.batch.begin()
            if (isTop) ArabicText.draw(game.batch, "★",         cx + 8f,   ry, 19f, 1f, 0.84f, 0.10f, a)
            else       ArabicText.draw(game.batch, "${i + 1}",   cx + 8f,   ry, 19f, cr, cg, cb, a)
            ArabicText.draw(game.batch, "${r.score}",  cx + 50f,  ry, 19f, cr, cg, cb, a)
            ArabicText.draw(game.batch, "${r.wave}/5", cx + 182f, ry, 19f, cr, cg, cb, a)
            ArabicText.draw(game.batch, "${r.kills}",  cx + 292f, ry, 19f, cr, cg, cb, a)
            ArabicText.draw(game.batch, "LV${r.level}", cx + 398f, ry, 19f, cr, cg, cb, a)
            val (rating, rr, rg, rb) = when {
                r.wave == 5 && r.score > 8000 -> Quad("أسطوري ★", 1f, 0.84f, 0.10f)
                r.wave == 5                   -> Quad("بطل ✦",    0.22f, 1f, 0.48f)
                r.wave >= 4                   -> Quad("ممتاز",     0.42f, 0.84f, 1f)
                r.wave >= 3                   -> Quad("جيد",       0.68f, 0.88f, 0.68f)
                else                          -> Quad("مبتدئ",     0.46f, 0.46f, 0.56f)
            }
            ArabicText.draw(game.batch, rating, cx + 512f, ry, 17f, rr, rg, rb, a)
            game.batch.end()
        }

        game.batch.begin()
        ArabicText.draw(game.batch, "إجمالي المباريات: $totalGames  •  مجموع القتلات: $totalKills",
            cx + 10f, 20f, 16f, 0.32f, 0.38f, 0.52f, a * 0.72f)
        game.batch.end()
    }

    private data class Quad(val a: String, val b: Float, val c: Float, val d: Float)

    override fun resize(w: Int, h: Int) { viewport.update(w, h, true) }
    override fun pause()   {}
    override fun resume()  {}
    override fun hide()    {}
    override fun dispose() { shape.dispose(); fontNum.dispose(); SoundManager.dispose() }
}

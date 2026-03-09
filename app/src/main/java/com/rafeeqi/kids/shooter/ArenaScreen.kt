package com.rafeeqi.kids.shooter

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
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

// ══════════════════════════════════════════════════════════════════════
//  ArenaScreen v3 — Professional 2D Shooter
//
//  جديد في v3:
//   ✅ نصوص عربية صحيحة (ArabicText)
//   ✅ دم على الشاشة عند HP منخفض (vignette أحمر)
//   ✅ نبض قلب عند HP < 30%
//   ✅ سلاح قنبلة (GRENADE pickup)
//   ✅ مؤشرات أعداء خارج الشاشة (أسهم على الحواف)
//   ✅ نظام Kill Streak مع أصوات وأسماء عربية
//   ✅ انيميشن موت سلو-موشن للاعب
//   ✅ شريط صحة بوص بارز
//   ✅ مرحلة ٣ للبوص: هجوم ليزر
//   ✅ صوت إعادة التحميل عند تغيير السلاح
//   ✅ أصوات جديدة في كل حدث
// ══════════════════════════════════════════════════════════════════════
class ArenaScreen(private val game: ShooterGame) : Screen {

    private val WORLD_W = 900f
    private val WORLD_H = 560f

    private val cam      = OrthographicCamera()
    private val viewport = FitViewport(WORLD_W, WORLD_H, cam)
    private val shape    = ShapeRenderer()
    private val fontNum  = BitmapFont().apply { data.setScale(1.4f) }
    private val layout   = GlyphLayout()

    // ── هز الكاميرا ─────────────────────────────────────────────────
    private var shakeTimer     = 0f
    private var shakeIntensity = 0f

    // ── اللاعب ──────────────────────────────────────────────────────
    private val playerPos    = Vector2(WORLD_W / 2f, WORLD_H / 2f)
    private var playerAngle  = 0f
    private var playerHP     = 100
    private var playerShield = 0
    private val MAX_HP       = 100
    private val MAX_SHIELD   = 60
    private val PLAYER_R     = 22f
    private val PLAYER_SPD   = 230f
    private var invTimer     = 0f
    private var shieldRechargeTimer  = 0f
    private val SHIELD_RECHARGE_DELAY = 4.5f
    private var playerAlive  = true
    private var walkAnim     = 0f
    private var isMoving     = false

    // Dash
    private val DASH_SPD   = 820f
    private val DASH_DUR   = 0.14f
    private val DASH_CD    = 1.4f
    private var dashTimer  = 0f
    private var isDashing  = false
    private val dashDir    = Vector2()
    private var dashCooldown = 0f
    private var lastJoyTap = -99f
    private val DOUBLE_TAP = 0.28f

    // ── زر داش مستقل ────────────────────────────────────────────────
    private val DASH_BTN_X = 155f
    private val DASH_BTN_Y = 78f
    private val DASH_BTN_R = 34f
    private var dashBtnFlash = 0f

    // ── أنيميشن المقاتل الواقعي ──────────────────────────────────────
    private var legSwing     = 0f          // زاوية تأرجح الأرجل
    private var legPhase     = 0f          // طور الخطوة (0..2π)
    private var bodyBob      = 0f          // ارتداد الجسم الرأسي
    private var aimRecoil    = 0f          // ارتداد السلاح عند الإطلاق
    private var breathCycle  = 0f          // تنفس الجسم (ثابت)
    private var footstepTimer= 0f          // توقيت بصمات الأقدام
    private val STEP_DIST    = 22f         // مسافة بين خطوتين

    // نبض القلب
    private var heartbeatTimer = 0f

    // تأثير دم الشاشة
    private var bloodOverlay = 0f    // 0..1 — يزيد عند HP منخفض
    private var bloodPulse   = 0f    // نبض للوفيليا

    // سلو-موشن عند الموت
    private var deathSlowTimer  = 0f
    private var deathSlowActive = false

    // ── نظام الأسلحة ─────────────────────────────────────────────────
    enum class WeaponType(
        val label: String, val fireRate: Float, val bulletSpd: Float,
        val damage: Int, val spread: Float, val pellets: Int
    ) {
        PISTOL  ("PISTOL",  0.18f, 660f, 30, 2f,  1),
        SHOTGUN ("SHOTGUN", 0.55f, 480f, 18, 18f, 5),
        MINIGUN ("MINIGUN", 0.07f, 580f, 14, 5f,  1),
        MELEE   ("MELEE",   0.50f, 0f,   55, 0f,  1)  // ضربة قريبة
    }
    private var currentWeapon = WeaponType.PISTOL
    private var shotgunAmmo   = 0
    private var minigunAmmo   = 0
    private var grenadeCount  = 0   // قنابل
    private var fireTimer     = 0f
    private var weaponHeat    = 0f
    private var isMuzzleFlash = false
    private var muzzleTimer   = 0f
    private var switchBtnFlash = 0f

    private val SWITCH_BTN_X = WORLD_W - 80f
    private val SWITCH_BTN_Y = WORLD_H - 92f
    private val SWITCH_BTN_R = 38f

    // زر القنبلة
    private val GREN_BTN_X = WORLD_W - 80f
    private val GREN_BTN_Y = 80f
    private val GREN_BTN_R = 36f

    // ── XP / Level ───────────────────────────────────────────────────
    private var playerXP     = 0
    private var playerLevel  = 1
    private var xpToNext     = 200
    private var levelUpFlash = 0f
    private var levelUpMsg   = ""

    // ── Combo & Kill streak ──────────────────────────────────────────
    private var combo      = 0
    private var comboTimer = 0f
    private val COMBO_WIN  = 3.5f
    private var comboFlash = 0f

    private var streakKills    = 0      // قتلات متتالية في فترة قصيرة
    private var streakTimer    = 0f
    private val STREAK_WIN     = 4.0f
    private var streakMsg      = ""
    private var streakFlash    = 0f

    // ── رصاصات ──────────────────────────────────────────────────────
    private data class Bullet(
        val pos: Vector2, val vel: Vector2,
        var life: Float = 2.0f,
        val isPlayer: Boolean,
        val damage: Int,
        val isCrit: Boolean = false,
        val trail: MutableList<Vector2> = mutableListOf(),
        val cr: Float = 1f, val cg: Float = 1f, val cb: Float = 1f
    )
    private val bullets = mutableListOf<Bullet>()

    // ── قنابل ────────────────────────────────────────────────────────
    private data class Grenade(
        val pos: Vector2, val vel: Vector2,
        var fuse: Float = 2.8f,
        var bounces: Int = 0,
        var radius: Float = 0f,   // لأنيميشن الانفجار
        var exploding: Boolean = false,
        var explodeTimer: Float = 0f
    )
    private val grenades = mutableListOf<Grenade>()
    private val GRENADE_RADIUS = 110f
    private val GRAVITY = 220f

    // ── أعداء ────────────────────────────────────────────────────────
    enum class EnemyType { GRUNT, HEAVY, SNIPER, FLANKER, BOSS_QUEEN }

    private data class Enemy(
        val pos:         Vector2,
        var angle:       Float  = 0f,
        var hp:          Int,
        var maxHp:       Int,
        var armor:       Int    = 0,
        var speed:       Float,
        var fireCD:      Float  = MathUtils.random(0.6f, 2.2f),
        var stunT:       Float  = 0f,
        var hitFlash:    Float  = 0f,
        val radius:      Float  = 20f,
        val type:        EnemyType = EnemyType.GRUNT,
        var phase:       Int    = 1,
        var strafeDir:   Float  = if (MathUtils.randomBoolean()) 1f else -1f,
        var strafeTimer: Float  = MathUtils.random(0.5f, 1.6f),
        var deathTimer:  Float  = -1f,
        var footDustT:   Float  = MathUtils.random(0f, 0.25f),
        var laserTimer:  Float  = 0f   // للبوص مرحلة 3
    )
    private val enemies = mutableListOf<Enemy>()

    // ── جسيمات ──────────────────────────────────────────────────────
    private data class Particle(
        val pos: Vector2, val vel: Vector2,
        var life: Float, val maxLife: Float,
        val cr: Float, val cg: Float, val cb: Float,
        val size: Float,
        val isDecal: Boolean = false
    )
    private val particles = mutableListOf<Particle>()

    // ── بيك آبات ────────────────────────────────────────────────────
    enum class PickupType { HEALTH, SHIELD, AMMO_SHOTGUN, AMMO_MINIGUN, GRENADE }
    private data class Pickup(val pos: Vector2, val type: PickupType, var alive: Boolean = true)
    private val pickups = mutableListOf<Pickup>()

    // ── عوائق ────────────────────────────────────────────────────────
    private data class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float)
    private val obstacles = listOf(
        Obstacle(195f, 185f, 65f, 65f),
        Obstacle(640f, 185f, 65f, 65f),
        Obstacle(195f, 310f, 65f, 65f),
        Obstacle(640f, 310f, 65f, 65f),
        Obstacle(395f, 225f, 85f, 110f)
    )

    // ── أرقام الضرر ──────────────────────────────────────────────────
    private data class DmgNum(val pos: Vector2, val value: Int, var life: Float, val isCrit: Boolean)
    private val dmgNumbers = mutableListOf<DmgNum>()

    // ── حالة اللعبة ──────────────────────────────────────────────────
    private var wave        = 1
    private val MAX_WAVES   = 5
    private var score       = 0
    private var animTime    = 0f
    private var gameOver    = false
    private var playerWon   = false
    private var waveDelay   = 0f
    private var waveMsg     = ""
    private var waveMsgT    = 0f
    private var damageFlash = 0f
    private var killCount   = 0
    private var dustTimer   = 0f

    // ── تحكم باللمس (جويستيك ديناميكي - يظهر في مكان اللمس) ──────────
    private val joyCenter   = Vector2()
    private val joyCurrent  = Vector2()
    private var joyActive   = false
    private var joyPtr      = -1
    private var firePtr     = -1
    private var joyAppearAlpha = 0f   // أنيميشن ظهور الجويستيك

    init {
        cam.setToOrtho(false, WORLD_W, WORLD_H)
        SoundManager.init()
        spawnWave()
    }

    // ══════════════════════════════════════════════════════════════════
    //  Waves
    // ══════════════════════════════════════════════════════════════════
    private fun spawnWave() {
        enemies.clear(); bullets.clear(); pickups.clear()
        particles.clear(); dmgNumbers.clear(); grenades.clear()
        waveDelay = 0f

        when (wave) {
            1 -> {
                se(80f, 80f, EnemyType.GRUNT, 60, 0, 115f)
                se(820f, 80f, EnemyType.GRUNT, 60, 0, 115f)
                se(80f, 480f, EnemyType.GRUNT, 60, 0, 115f)
                se(820f, 480f, EnemyType.GRUNT, 60, 0, 115f)
                se(450f, 50f, EnemyType.GRUNT, 60, 0, 115f)
                showMsg("الموجة ١  —  اقتل الجميع!")
            }
            2 -> {
                se(80f, 80f, EnemyType.GRUNT, 70, 0, 130f)
                se(820f, 80f, EnemyType.GRUNT, 70, 0, 130f)
                se(80f, 480f, EnemyType.HEAVY, 130, 15, 80f)
                se(820f, 480f, EnemyType.HEAVY, 130, 15, 80f)
                se(250f, 50f, EnemyType.FLANKER, 80, 0, 175f)
                se(650f, 50f, EnemyType.FLANKER, 80, 0, 175f)
                showMsg("الموجة ٢  —  مدرعون وسريعون!")
            }
            3 -> {
                se(80f, 80f, EnemyType.SNIPER, 90, 0, 58f)
                se(820f, 80f, EnemyType.SNIPER, 90, 0, 58f)
                se(80f, 480f, EnemyType.FLANKER, 80, 0, 178f)
                se(820f, 480f, EnemyType.FLANKER, 80, 0, 178f)
                se(450f, 50f, EnemyType.HEAVY, 140, 20, 78f)
                se(250f, 480f, EnemyType.GRUNT, 70, 0, 130f)
                se(650f, 480f, EnemyType.GRUNT, 70, 0, 130f)
                showMsg("الموجة ٣  —  قناصة ومتسللون!")
            }
            4 -> {
                se(80f, 100f, EnemyType.HEAVY, 160, 25, 85f)
                se(820f, 100f, EnemyType.HEAVY, 160, 25, 85f)
                se(80f, 460f, EnemyType.SNIPER, 100, 0, 55f)
                se(820f, 460f, EnemyType.SNIPER, 100, 0, 55f)
                se(250f, 100f, EnemyType.FLANKER, 90, 0, 185f)
                se(650f, 100f, EnemyType.FLANKER, 90, 0, 185f)
                se(450f, 50f, EnemyType.HEAVY, 150, 20, 90f)
                showMsg("الموجة ٤  —  الفرقة النخبة!")
            }
            5 -> {
                se(450f, 430f, EnemyType.BOSS_QUEEN, 900, 30, 92f)
                se(200f, 350f, EnemyType.HEAVY, 110, 10, 85f)
                se(700f, 350f, EnemyType.HEAVY, 110, 10, 85f)
                se(200f, 150f, EnemyType.FLANKER, 85, 0, 168f)
                se(700f, 150f, EnemyType.FLANKER, 85, 0, 168f)
                showMsg("الملكة قادمة!  المعركة الأخيرة!")
            }
        }

        pickups.add(Pickup(Vector2(130f, 280f), PickupType.HEALTH))
        pickups.add(Pickup(Vector2(770f, 280f), PickupType.HEALTH))
        if (wave >= 2) pickups.add(Pickup(Vector2(450f, 280f), PickupType.SHIELD))
        if (wave >= 2) pickups.add(Pickup(Vector2(130f, 100f), PickupType.AMMO_SHOTGUN))
        if (wave >= 3) pickups.add(Pickup(Vector2(770f, 100f), PickupType.AMMO_MINIGUN))
        pickups.add(Pickup(Vector2(450f, 100f), PickupType.GRENADE))   // قنبلة في كل موجة

        SoundManager.playWaveStart()
    }

    private fun se(x: Float, y: Float, type: EnemyType, hp: Int, armor: Int, speed: Float) {
        val r = when (type) { EnemyType.HEAVY -> 27f; EnemyType.BOSS_QUEEN -> 44f; else -> 20f }
        enemies.add(Enemy(Vector2(x, y), hp = hp, maxHp = hp, armor = armor, speed = speed, radius = r, type = type))
    }

    private fun showMsg(msg: String) { waveMsg = msg; waveMsgT = 3.2f }
    override fun show() {}

    // ══════════════════════════════════════════════════════════════════
    //  Render
    // ══════════════════════════════════════════════════════════════════
    override fun render(delta: Float) {
        // سلو-موشن عند الموت
        val rawDt = delta.coerceIn(0f, 0.05f)
        val timeScale = if (deathSlowActive) {
            deathSlowTimer -= rawDt
            if (deathSlowTimer <= 0f) deathSlowActive = false
            0.18f
        } else 1f
        val dt = rawDt * timeScale
        animTime += rawDt  // الأنيميشن يسير بطبيعي

        Gdx.gl.glClearColor(0.10f, 0.11f, 0.14f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // هز الكاميرا
        val sx = if (shakeTimer > 0f) MathUtils.random(-shakeIntensity, shakeIntensity) else 0f
        val sy = if (shakeTimer > 0f) MathUtils.random(-shakeIntensity, shakeIntensity) else 0f
        shakeTimer = (shakeTimer - rawDt).coerceAtLeast(0f)
        cam.position.set(WORLD_W / 2f + sx, WORLD_H / 2f + sy, 0f)
        cam.update()

        viewport.apply()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        if (!gameOver && playerAlive) {
            processInput(dt)
            updatePlayer(dt)
            updateEnemies(dt)
            updateBullets(dt)
            updateGrenades(dt)
            updateParticles(dt)
            checkPickups()
            checkWaveEnd(dt)
            updateComboAndNums(dt)
            updateBloodOverlay(dt)
        } else if (gameOver && Gdx.input.justTouched()) {
            game.setScreen(IntroScreen(game))
            dispose(); return
        }

        drawBackground()
        drawObstacles()
        drawWorld()
        drawHUD(rawDt)
        drawBloodOverlay()
    }

    // ══════════════════════════════════════════════════════════════════
    //  Input
    // ══════════════════════════════════════════════════════════════════
    private fun processInput(dt: Float) {
        var foundJoy = false; var foundFire = false
        muzzleTimer -= dt
        isMuzzleFlash = muzzleTimer > 0f

        // زر تغيير السلاح (أعلى يمين) وزر القنبلة (أسفل يمين) وزر داش (يسار سفلي)
        if (Gdx.input.justTouched()) {
            val wp = viewport.unproject(Vector2(Gdx.input.getX(0).toFloat(), Gdx.input.getY(0).toFloat()))
            val dxS = wp.x - SWITCH_BTN_X; val dyS = wp.y - SWITCH_BTN_Y
            if (sqrt(dxS*dxS + dyS*dyS) < SWITCH_BTN_R) { cycleWeapon(); return }
            val dxG = wp.x - GREN_BTN_X; val dyG = wp.y - GREN_BTN_Y
            if (sqrt(dxG*dxG + dyG*dyG) < GREN_BTN_R && grenadeCount > 0) { throwGrenade(); return }
            // زر داش
            val dxD = wp.x - DASH_BTN_X; val dyD = wp.y - DASH_BTN_Y
            if (sqrt(dxD*dxD + dyD*dyD) < DASH_BTN_R && !isDashing && dashCooldown <= 0f && joyActive) {
                startDash(); dashBtnFlash = 0.3f; return
            }
        }

        for (i in 0 until 10) {
            if (!Gdx.input.isTouched(i)) continue
            val tp = viewport.unproject(Vector2(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat()))

            // الجويستيك (نصف يسار) — يظهر في مكان اللمس
            if (tp.x < WORLD_W * 0.48f) {
                if (!foundJoy) {
                    foundJoy = true
                    if (joyPtr != i && Gdx.input.justTouched()) {
                        // نقرة جديدة: ضع مركز الجويستيك هنا
                        joyCenter.set(tp)
                        joyCurrent.set(tp)
                        joyPtr = i
                        joyActive = true
                        joyAppearAlpha = 0f   // ابدأ أنيميشن الظهور
                        // نقرة مزدوجة = داش
                        if (animTime - lastJoyTap < DOUBLE_TAP && !isDashing && dashCooldown <= 0f) {
                            startDash()
                        }
                        lastJoyTap = animTime
                    }
                    if (joyPtr == i || joyActive) {
                        joyCurrent.set(tp)
                        joyActive = true
                    }
                }
            }
            // منطقة النار (نصف يمين)
            else if (tp.x > WORLD_W * 0.52f && tp.y < WORLD_H * 0.7f) {
                if (!foundFire) {
                    foundFire = true
                    if (firePtr < 0) firePtr = i
                    if (firePtr == i) {
                        playerAngle = Math.toDegrees(atan2(
                            (tp.y - playerPos.y).toDouble(),
                            (tp.x - playerPos.x).toDouble()
                        )).toFloat()
                    }
                }
            }
        }
        if (!foundJoy) { joyActive = false; joyPtr = -1; joyAppearAlpha = 0f }
        if (!foundFire) firePtr = -1

        // حركة اللاعب
        isMoving = false
        if (joyActive && !isDashing) {
            val d = Vector2(joyCurrent).sub(joyCenter)
            val len = d.len()
            if (len > 14f) {
                // حساسية تدريجية: حركة أبطأ في المنتصف، أسرع عند الحافة
                val normLen = ((len - 14f) / (58f - 14f)).coerceIn(0f, 1f)
                val speed = PLAYER_SPD * (0.45f + normLen * 0.55f)
                d.nor().scl(speed)
                val dt2 = Gdx.graphics.deltaTime
                val newX = (playerPos.x + d.x * dt2).coerceIn(PLAYER_R, WORLD_W - PLAYER_R)
                val newY = (playerPos.y + d.y * dt2).coerceIn(PLAYER_R, WORLD_H - PLAYER_R)
                if (!collidesObstacle(newX, playerPos.y)) playerPos.x = newX
                if (!collidesObstacle(playerPos.x, newY)) playerPos.y = newY
                playerAngle = Math.toDegrees(atan2(d.y.toDouble(), d.x.toDouble())).toFloat()
                isMoving = true
            }
        }

        // إطلاق نار
        fireTimer -= Gdx.graphics.deltaTime
        val canFire = firePtr >= 0 && !isDashing && weaponHeat < 1f
        if (canFire && fireTimer <= 0f) {
            if (currentWeapon == WeaponType.MELEE) {
                meleAttack()
            } else {
                shootPlayer()
            }
            fireTimer = currentWeapon.fireRate
        }
    }

    private fun collidesObstacle(x: Float, y: Float): Boolean {
        obstacles.forEach { o ->
            if (x + PLAYER_R > o.x && x - PLAYER_R < o.x + o.w &&
                y + PLAYER_R > o.y && y - PLAYER_R < o.y + o.h) return true
        }
        return false
    }

    private fun cycleWeapon() {
        val prev = currentWeapon
        currentWeapon = when (currentWeapon) {
            WeaponType.PISTOL  -> if (shotgunAmmo > 0) WeaponType.SHOTGUN else if (minigunAmmo > 0) WeaponType.MINIGUN else WeaponType.MELEE
            WeaponType.SHOTGUN -> if (minigunAmmo > 0) WeaponType.MINIGUN else WeaponType.PISTOL
            WeaponType.MINIGUN -> WeaponType.PISTOL
            WeaponType.MELEE   -> WeaponType.PISTOL
        }
        if (currentWeapon != prev) {
            SoundManager.playReload()
            switchBtnFlash = 0.35f
        }
    }

    private fun startDash() {
        val dx = joyCurrent.x - joyCenter.x; val dy = joyCurrent.y - joyCenter.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 10f) return
        dashDir.set(dx / len, dy / len)
        isDashing = true; dashTimer = DASH_DUR; dashCooldown = DASH_CD
        spawnDust(Vector2(playerPos).add(-dashDir.x * 20f, -dashDir.y * 20f),
            -dashDir.x * 100f, -dashDir.y * 100f, 0.3f, 0.6f, 1f, 0.4f)
    }

    private fun throwGrenade() {
        if (grenadeCount <= 0) return
        grenadeCount--
        val rad = Math.toRadians(playerAngle.toDouble())
        val spd = 400f
        grenades.add(Grenade(
            pos = Vector2(playerPos).add(
                cos(rad).toFloat() * (PLAYER_R + 10f),
                sin(rad).toFloat() * (PLAYER_R + 10f)
            ),
            vel = Vector2(cos(rad).toFloat() * spd, sin(rad).toFloat() * spd + 80f)
        ))
        SoundManager.playGrenadeThrow()
        triggerShake(1.5f, 0.08f)
    }

    private fun shootPlayer() {
        val isCrit = MathUtils.random() < 0.10f + playerLevel * 0.025f
        val dmg = if (isCrit) currentWeapon.damage * 2 else currentWeapon.damage
        repeat(currentWeapon.pellets) { pi ->
            val spr = if (currentWeapon.pellets > 1)
                (pi - currentWeapon.pellets / 2) * currentWeapon.spread
            else MathUtils.random(-currentWeapon.spread, currentWeapon.spread)
            val rad = Math.toRadians((playerAngle + spr).toDouble())
            val dir = Vector2(cos(rad).toFloat(), sin(rad).toFloat())
            bullets.add(Bullet(
                pos = Vector2(playerPos).add(dir.x * 30f, dir.y * 30f),
                vel = Vector2(dir).scl(currentWeapon.bulletSpd),
                isPlayer = true, damage = dmg, isCrit = isCrit,
                cr = if (isCrit) 0.9f else 1f,
                cg = if (isCrit) 0.2f else 0.95f,
                cb = if (isCrit) 1f else 0.08f
            ))
        }
        SoundManager.playShot(currentWeapon.name)
        isMuzzleFlash = true; muzzleTimer = 0.06f
        aimRecoil = when (currentWeapon) { WeaponType.SHOTGUN -> 9f; WeaponType.MINIGUN -> 3f; else -> 5f }
        triggerShake(1.2f, 0.05f)

        when (currentWeapon) {
            WeaponType.SHOTGUN -> { shotgunAmmo--; if (shotgunAmmo <= 0) { shotgunAmmo = 0; currentWeapon = WeaponType.PISTOL; SoundManager.playReload() } }
            WeaponType.MINIGUN -> { minigunAmmo--; weaponHeat += 0.048f; if (minigunAmmo <= 0) { minigunAmmo = 0; currentWeapon = WeaponType.PISTOL; SoundManager.playReload() } }
            else -> {}
        }
    }

    private fun meleAttack() {
        // يضرب كل الأعداء في نطاق 80 بكسل
        SoundManager.playShot("MELEE")
        triggerShake(2.5f, 0.12f)
        isMuzzleFlash = true; muzzleTimer = 0.10f
        enemies.filter { it.hp > 0 && it.deathTimer < 0f }.forEach { e ->
            val dist = playerPos.dst(e.pos)
            if (dist < 80f + e.radius) {
                val dmg = (WeaponType.MELEE.damage - e.armor).coerceAtLeast(5)
                e.hp -= dmg; e.hitFlash = 0.15f; e.stunT = 0.35f
                score += dmg
                dmgNumbers.add(DmgNum(Vector2(e.pos.x, e.pos.y + e.radius + 12f), dmg, 1f, false))
                SoundManager.play("hit_enemy", pitch = MathUtils.random(0.85f, 1.15f), vol = 0.7f)
                spawnHitFX(e.pos, 1f, 0.5f, 0.1f)
                if (e.hp <= 0) killEnemy(e)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Update
    // ══════════════════════════════════════════════════════════════════

    // ── حركة تلقائية ────────────────────────────────────────────────
    private var autoMoveDir    = Vector2(0f, 0f)   // اتجاه الحركة التلقائية
    private var autoMoveTimer  = 0f                 // مؤقت لتغيير الاتجاه
    private var autoStrafeSide = 1f                 // الجانب الذي يتحرك فيه
    private var idleTimer      = 0f                 // كم من الوقت بدون جويستيك

    private fun updatePlayer(dt: Float) {
        invTimer -= dt
        dashCooldown -= dt; switchBtnFlash -= dt; dashBtnFlash -= dt

        // ── أنيميشن المشي الواقعي ──────────────────────────────────
        breathCycle += dt * 1.8f   // تنفس دائم
        if (isMoving) {
            walkAnim  += dt * 10f
            legPhase  += dt * 14f  // سرعة تأرجح الأرجل
            bodyBob    = sin(legPhase).toFloat() * 3.2f
            footstepTimer -= dt
            if (footstepTimer <= 0f) {
                footstepTimer = 0.32f
                spawnDust(Vector2(playerPos.x + MathUtils.random(-6f,6f), playerPos.y - PLAYER_R * 0.8f),
                    MathUtils.random(-18f,18f), MathUtils.random(-6f,3f), 0.52f, 0.48f, 0.42f, 0.28f)
            }
        } else {
            legPhase  += dt * 1.5f   // تأرجح خفيف عند الوقوف
            bodyBob   *= 0.88f       // تلاشي تدريجي
        }
        legSwing = sin(legPhase).toFloat()

        // ارتداد السلاح يتلاشى
        aimRecoil = (aimRecoil - dt * 18f).coerceAtLeast(0f)
        // أنيميشن ظهور الجويستيك
        if (joyActive) joyAppearAlpha = (joyAppearAlpha + dt * 8f).coerceAtMost(1f)

        // ── حركة تلقائية ذكية عند عدم تحريك الجويستيك ──────────────
        if (!joyActive && !isDashing && playerAlive) {
            idleTimer += dt
            // بعد 0.6 ثانية من التوقف، يبدأ المقاتل يتحرك تلقائياً
            if (idleTimer > 0.6f) {
                val liveEnemies = enemies.filter { it.hp > 0 && it.deathTimer < 0f }
                if (liveEnemies.isNotEmpty()) {
                    // ابحث عن أقرب عدو
                    val nearest = liveEnemies.minByOrNull { it.pos.dst(playerPos) }!!
                    val distToNearest = playerPos.dst(nearest.pos)
                    val toEnemy = Vector2(nearest.pos).sub(playerPos)
                    val dirToEnemy = toEnemy.cpy().nor()

                    // مؤقت تغيير اتجاه الحركة الجانبية
                    autoMoveTimer -= dt
                    if (autoMoveTimer <= 0f) {
                        autoMoveTimer = MathUtils.random(1.2f, 2.5f)
                        autoStrafeSide = if (MathUtils.randomBoolean()) 1f else -1f
                    }

                    // اتجاه الحركة النهائي
                    val moveSpd = PLAYER_SPD * 0.68f
                    val newMove = when {
                        distToNearest > 200f -> {
                            // بعيد → تقدّم نحو العدو مع تحرك جانبي خفيف
                            val perp = Vector2(-dirToEnemy.y, dirToEnemy.x)
                            Vector2(dirToEnemy).scl(0.75f).add(Vector2(perp).scl(autoStrafeSide * 0.40f)).nor().scl(moveSpd)
                        }
                        distToNearest < 95f -> {
                            // قريب جداً → تراجع للخلف + تحرك جانبي
                            val perp = Vector2(-dirToEnemy.y, dirToEnemy.x)
                            Vector2(dirToEnemy).scl(-0.55f).add(Vector2(perp).scl(autoStrafeSide * 0.85f)).nor().scl(moveSpd * 0.8f)
                        }
                        else -> {
                            // مسافة مثالية → تحرك جانبي بحت (strafing)
                            val perp = Vector2(-dirToEnemy.y, dirToEnemy.x)
                            Vector2(perp).scl(autoStrafeSide).scl(moveSpd * 0.9f)
                        }
                    }

                    // ناعم (smooth) على الاتجاه
                    autoMoveDir.lerp(newMove, dt * 6f)

                    val newX = (playerPos.x + autoMoveDir.x * dt).coerceIn(PLAYER_R + 5f, WORLD_W - PLAYER_R - 5f)
                    val newY = (playerPos.y + autoMoveDir.y * dt).coerceIn(PLAYER_R + 5f, WORLD_H - PLAYER_R - 5f)
                    if (!collidesObstacle(newX, playerPos.y)) playerPos.x = newX
                    if (!collidesObstacle(playerPos.x, newY)) playerPos.y = newY

                    // المقاتل يتجه دائماً نحو العدو
                    playerAngle = Math.toDegrees(atan2(
                        (nearest.pos.y - playerPos.y).toDouble(),
                        (nearest.pos.x - playerPos.x).toDouble()
                    )).toFloat()
                    isMoving = autoMoveDir.len() > 30f
                }
            }
        } else {
            idleTimer = 0f
            autoMoveDir.set(0f, 0f)
        }

        if (currentWeapon != WeaponType.MINIGUN || !isMuzzleFlash)
            weaponHeat = (weaponHeat - dt * 0.5f).coerceAtLeast(0f)
        weaponHeat = weaponHeat.coerceIn(0f, 1f)

        if (isDashing) {
            dashTimer -= dt
            playerPos.x = (playerPos.x + dashDir.x * DASH_SPD * dt).coerceIn(PLAYER_R, WORLD_W - PLAYER_R)
            playerPos.y = (playerPos.y + dashDir.y * DASH_SPD * dt).coerceIn(PLAYER_R, WORLD_H - PLAYER_R)
            // جسيمات داش مكثفة
            repeat(if (MathUtils.random() < 0.7f) 2 else 1) {
                spawnDust(Vector2(playerPos).add(MathUtils.random(-12f,12f), MathUtils.random(-8f,8f)),
                    -dashDir.x * 60f + MathUtils.random(-30f,30f),
                    -dashDir.y * 60f + MathUtils.random(-20f,20f),
                    0.3f, 0.6f, 1f, 0.22f)
            }
            if (dashTimer <= 0f) isDashing = false
        }

        shieldRechargeTimer -= dt
        if (playerShield < MAX_SHIELD && shieldRechargeTimer <= 0f)
            playerShield = (playerShield + (10 * dt).toInt()).coerceAtMost(MAX_SHIELD)

        // نبض القلب عند HP منخفض
        val hpPct = playerHP.toFloat() / MAX_HP
        if (hpPct < 0.30f && playerAlive) {
            heartbeatTimer -= dt
            if (heartbeatTimer <= 0f) {
                SoundManager.playHeartbeat()
                heartbeatTimer = 1.5f - (0.30f - hpPct) * 3f  // ازداد سرعة مع انخفاض HP
            }
        } else {
            heartbeatTimer = 0f
        }
    }

    private fun updateBloodOverlay(dt: Float) {
        val hpPct = playerHP.toFloat() / MAX_HP
        val target = when {
            hpPct < 0.20f -> 0.85f
            hpPct < 0.30f -> 0.60f
            hpPct < 0.45f -> 0.35f
            else          -> 0f
        }
        bloodOverlay += (target - bloodOverlay) * dt * 3f
        bloodPulse = abs(sin(animTime * 3.5f)).toFloat()
    }

    private fun spawnDust(pos: Vector2, vx: Float, vy: Float, r: Float, g: Float, b: Float, life: Float) {
        particles.add(Particle(Vector2(pos), Vector2(vx, vy),
            life, life, r, g, b, MathUtils.random(3f, 7f)))
    }

    private fun updateEnemies(dt: Float) {
        val toRemove = mutableListOf<Enemy>()
        enemies.forEach { e ->
            if (e.deathTimer >= 0f) { e.deathTimer -= dt; if (e.deathTimer < 0f) toRemove.add(e); return@forEach }
            if (e.hp <= 0) return@forEach
            e.hitFlash -= dt
            if (e.stunT > 0f) { e.stunT -= dt; return@forEach }

            // مراحل البوص
            val hpPct = e.hp.toFloat() / e.maxHp
            if (e.type == EnemyType.BOSS_QUEEN) {
                if (hpPct < 0.66f && e.phase == 1) {
                    e.phase = 2; e.speed = 120f
                    showMsg("الملكة في المرحلة ٢!")
                    triggerShake(6f, 0.45f); SoundManager.play("big_explosion", vol = 0.5f)
                    // استدعاء عساكر إضافيين
                    se(100f, 100f, EnemyType.GRUNT, 50, 0, 140f)
                    se(800f, 100f, EnemyType.GRUNT, 50, 0, 140f)
                }
                if (hpPct < 0.33f && e.phase == 2) {
                    e.phase = 3; e.speed = 155f; e.armor = 0
                    showMsg("المرحلة الأخيرة!  الملكة مجنونة!")
                    triggerShake(9f, 0.65f); SoundManager.play("big_explosion", vol = 0.8f)
                }
            }

            val dx = playerPos.x - e.pos.x; val dy = playerPos.y - e.pos.y
            val dist = sqrt(dx * dx + dy * dy)
            e.angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

            val stopDist = when (e.type) { EnemyType.SNIPER -> 265f; EnemyType.BOSS_QUEEN -> 140f; EnemyType.HEAVY -> 105f; else -> 90f }

            e.strafeTimer -= dt
            if (e.strafeTimer <= 0f) { e.strafeDir = -e.strafeDir; e.strafeTimer = MathUtils.random(0.55f, 1.8f) }

            if (dist > stopDist) {
                val nx = dx / dist; val ny = dy / dist
                e.pos.x = (e.pos.x + nx * e.speed * dt).coerceIn(e.radius, WORLD_W - e.radius)
                e.pos.y = (e.pos.y + ny * e.speed * dt).coerceIn(e.radius, WORLD_H - e.radius)
            } else {
                val perpX = -sin(Math.toRadians(e.angle.toDouble())).toFloat() * e.strafeDir
                val perpY =  cos(Math.toRadians(e.angle.toDouble())).toFloat() * e.strafeDir
                e.pos.x = (e.pos.x + perpX * e.speed * 0.55f * dt).coerceIn(e.radius, WORLD_W - e.radius)
                e.pos.y = (e.pos.y + perpY * e.speed * 0.55f * dt).coerceIn(e.radius, WORLD_H - e.radius)
            }

            if (e.type == EnemyType.HEAVY || e.type == EnemyType.BOSS_QUEEN) {
                e.footDustT -= dt
                if (e.footDustT <= 0f) {
                    e.footDustT = 0.22f
                    spawnDust(Vector2(e.pos.x + MathUtils.random(-10f, 10f), e.pos.y - e.radius * 0.7f),
                        MathUtils.random(-15f, 15f), MathUtils.random(-8f, 4f), 0.5f, 0.45f, 0.38f, 0.4f)
                }
            }

            e.fireCD -= dt
            val shootRange = when (e.type) { EnemyType.SNIPER -> 520f; EnemyType.BOSS_QUEEN -> 440f; else -> 390f }
            if (e.fireCD <= 0f && dist < shootRange) {
                shootEnemy(e)
                e.fireCD = when (e.type) {
                    EnemyType.SNIPER    -> MathUtils.random(1.8f, 2.8f)
                    EnemyType.HEAVY     -> MathUtils.random(0.9f, 1.5f)
                    EnemyType.BOSS_QUEEN -> when (e.phase) { 3 -> 0.16f; 2 -> 0.25f; else -> 0.40f }
                    EnemyType.FLANKER   -> MathUtils.random(0.6f, 1.1f)
                    else                -> MathUtils.random(0.9f, 1.7f)
                }
            }
        }
        enemies.removeAll(toRemove)
    }

    private fun shootEnemy(e: Enemy) {
        val shots = when {
            e.type == EnemyType.BOSS_QUEEN && e.phase == 3 -> 6
            e.type == EnemyType.BOSS_QUEEN && e.phase == 2 -> 4
            e.type == EnemyType.BOSS_QUEEN -> 2
            e.type == EnemyType.HEAVY -> 2
            else -> 1
        }
        val bspd = when (e.type) { EnemyType.SNIPER -> 720f; EnemyType.BOSS_QUEEN -> 300f; else -> 265f }
        val dmg  = when (e.type) { EnemyType.SNIPER -> 24; EnemyType.BOSS_QUEEN -> 15; EnemyType.HEAVY -> 13; else -> 10 }
        val (br, bg, bb) = when (e.type) {
            EnemyType.SNIPER    -> Triple(0f, 0.8f, 1f)
            EnemyType.BOSS_QUEEN -> Triple(0.8f, 0f, 1f)
            EnemyType.HEAVY     -> Triple(1f, 0.5f, 0f)
            else                -> Triple(1f, 0.15f, 0.08f)
        }
        repeat(shots) { si ->
            val spr = if (shots > 1) (si - shots / 2) * 24f else MathUtils.random(-4f, 4f)
            val rad = Math.toRadians((e.angle + spr).toDouble())
            bullets.add(Bullet(
                pos = Vector2(e.pos).add(cos(rad).toFloat() * (e.radius + 5f), sin(rad).toFloat() * (e.radius + 5f)),
                vel = Vector2(cos(rad).toFloat() * bspd, sin(rad).toFloat() * bspd),
                isPlayer = false, damage = dmg, cr = br, cg = bg, cb = bb
            ))
        }
    }

    private fun updateBullets(dt: Float) {
        val rem = mutableListOf<Bullet>()
        bullets.forEach { b ->
            if (b in rem) return@forEach
            b.trail.add(0, Vector2(b.pos))
            if (b.trail.size > 8) b.trail.removeAt(b.trail.size - 1)
            b.pos.x += b.vel.x * dt; b.pos.y += b.vel.y * dt; b.life -= dt

            var dead = b.life <= 0f || b.pos.x < -15f || b.pos.x > WORLD_W + 15f ||
                       b.pos.y < -15f || b.pos.y > WORLD_H + 15f

            if (!dead) obstacles.forEach { o ->
                if (!dead && b.pos.x in o.x..o.x + o.w && b.pos.y in o.y..o.y + o.h) {
                    spawnHitFX(b.pos, 0.65f, 0.58f, 0.42f, 4); dead = true
                }
            }

            if (b.isPlayer && !dead) {
                enemies.filter { it.hp > 0 && it.deathTimer < 0f }.forEach { e ->
                    if (!dead && b.pos.dst(e.pos) < e.radius + 9f) {
                        val fd = (b.damage - e.armor).coerceAtLeast(2)
                        e.hp -= fd; e.hitFlash = 0.10f
                        combo++; comboTimer = COMBO_WIN; comboFlash = 0.6f
                        streakTimer = STREAK_WIN
                        score += fd * (1 + combo / 5)
                        dmgNumbers.add(DmgNum(Vector2(e.pos.x + MathUtils.random(-15f, 15f), e.pos.y + e.radius + 12f), fd, 1f, b.isCrit))
                        spawnHitFX(b.pos, if (b.isCrit) 0.9f else 1f, if (b.isCrit) 0.2f else 0.9f, if (b.isCrit) 1f else 0.2f)
                        SoundManager.play("hit_enemy", pitch = MathUtils.random(0.9f, 1.15f), vol = 0.6f)
                        if (e.hp <= 0) killEnemy(e)
                        dead = true
                    }
                }
            } else if (!b.isPlayer && !dead) {
                if (invTimer <= 0f && b.pos.dst(playerPos) < PLAYER_R + 9f) {
                    var dmg = b.damage
                    if (playerShield > 0) {
                        val ab = dmg.coerceAtMost(playerShield)
                        playerShield -= ab; dmg -= ab
                        shieldRechargeTimer = SHIELD_RECHARGE_DELAY
                        SoundManager.play("shield_hit", vol = 0.7f)
                        spawnHitFX(playerPos, 0.3f, 0.7f, 1f, 5)
                    }
                    if (dmg > 0) {
                        playerHP -= dmg; invTimer = 0.22f; damageFlash = 1f
                        shieldRechargeTimer = SHIELD_RECHARGE_DELAY
                        spawnHitFX(playerPos, 1f, 0.1f, 0.1f)
                        SoundManager.play("hit_player", vol = 0.75f)
                        triggerShake(4.5f, 0.25f)
                    }
                    if (playerHP <= 0) {
                        playerHP = 0; playerAlive = false; gameOver = true
                        spawnDeathFX(playerPos, 0.2f, 0.5f, 1f)
                        SoundManager.playDeathPlayer()
                        deathSlowActive = true; deathSlowTimer = 1.5f
                        triggerShake(10f, 0.8f)
                        PlayerProfile.addRecord(score, wave, killCount, playerLevel)
                    }
                    dead = true
                }
            }
            if (dead) rem.add(b)
        }
        bullets.removeAll(rem)
    }

    private fun killEnemy(e: Enemy) {
        val xp = when (e.type) { EnemyType.BOSS_QUEEN -> 400; EnemyType.HEAVY -> 90; EnemyType.SNIPER -> 65; EnemyType.FLANKER -> 55; else -> 40 }
        gainXP(xp)
        score += when (e.type) { EnemyType.BOSS_QUEEN -> 2000; EnemyType.HEAVY -> 300; EnemyType.SNIPER -> 200; else -> 120 }
        killCount++; e.deathTimer = 0.6f
        val (dr, dg, db) = when (e.type) { EnemyType.BOSS_QUEEN -> Triple(0.8f, 0f, 1f); EnemyType.HEAVY -> Triple(1f, 0.4f, 0f); else -> Triple(1f, 0.2f, 0.1f) }
        spawnDeathFX(e.pos, dr, dg, db)
        val isBoss = e.type == EnemyType.BOSS_QUEEN
        triggerShake(if (isBoss) 10f else 3.5f, if (isBoss) 0.65f else 0.20f)
        SoundManager.playExplosion(if (isBoss) 1f else 0.55f)

        // Kill Streak
        streakKills++
        if (streakKills >= 5 && streakTimer > 0f) {
            streakMsg = "قنص خمسة!"
            streakFlash = 2.5f; SoundManager.play("levelup", vol = 0.7f)
        } else if (streakKills >= 3 && streakTimer > 0f) {
            streakMsg = "ثلاثية خطرة!"
            streakFlash = 2.0f
        }

        // رصاصة محتملة من العدو الميت
        if (MathUtils.random() < 0.35f) {
            when {
                e.type == EnemyType.HEAVY   -> pickups.add(Pickup(Vector2(e.pos), PickupType.AMMO_SHOTGUN))
                e.type == EnemyType.SNIPER  -> pickups.add(Pickup(Vector2(e.pos), PickupType.AMMO_MINIGUN))
                e.type == EnemyType.FLANKER -> if (MathUtils.random() < 0.4f) pickups.add(Pickup(Vector2(e.pos), PickupType.HEALTH))
            }
        }
    }

    private fun updateGrenades(dt: Float) {
        val rem = mutableListOf<Grenade>()
        grenades.forEach { g ->
            if (g.exploding) {
                g.explodeTimer += dt
                g.radius = g.explodeTimer * GRENADE_RADIUS * 3f
                if (g.explodeTimer > 0.35f) rem.add(g)
                return@forEach
            }

            g.vel.y -= GRAVITY * dt
            g.pos.x += g.vel.x * dt
            g.pos.y += g.vel.y * dt
            g.fuse -= dt

            // ارتداد عن الحواف
            if (g.pos.x < 12f || g.pos.x > WORLD_W - 12f) { g.vel.x *= -0.62f; g.bounces++ }
            if (g.pos.y < 12f || g.pos.y > WORLD_H - 12f) { g.vel.y *= -0.62f; g.bounces++ }
            g.pos.x = g.pos.x.coerceIn(12f, WORLD_W - 12f)
            g.pos.y = g.pos.y.coerceIn(12f, WORLD_H - 12f)

            if (g.fuse <= 0f || g.bounces >= 3) {
                // انفجار!
                g.exploding = true
                triggerShake(8f, 0.5f)
                SoundManager.playExplosion(0.9f)
                spawnDeathFX(g.pos, 1f, 0.6f, 0.1f)
                // أذى منطقة
                enemies.filter { it.hp > 0 && it.deathTimer < 0f }.forEach { e ->
                    val dist = g.pos.dst(e.pos)
                    if (dist < GRENADE_RADIUS) {
                        val dmg = (120f * (1f - dist / GRENADE_RADIUS)).toInt().coerceAtLeast(20)
                        val fd = (dmg - e.armor).coerceAtLeast(5)
                        e.hp -= fd; e.hitFlash = 0.20f
                        dmgNumbers.add(DmgNum(Vector2(e.pos.x, e.pos.y + e.radius + 12f), fd, 1.2f, true))
                        score += fd
                        if (e.hp <= 0) killEnemy(e)
                    }
                }
                // أذى للاعب إذا قريب
                if (g.pos.dst(playerPos) < GRENADE_RADIUS * 0.7f) {
                    val pd = ((80f * (1f - g.pos.dst(playerPos) / (GRENADE_RADIUS * 0.7f)))).toInt()
                    playerHP = (playerHP - pd).coerceAtLeast(0)
                    damageFlash = 0.8f; triggerShake(5f, 0.3f)
                    SoundManager.play("hit_player")
                    if (playerHP <= 0) { playerAlive = false; gameOver = true; SoundManager.playDeathPlayer() }
                }
            }
        }
        grenades.removeAll(rem)
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            if (!p.isDecal) { p.pos.x += p.vel.x * dt; p.pos.y += p.vel.y * dt; p.vel.x *= 0.91f; p.vel.y *= 0.91f }
            p.life -= dt
            if (p.life <= 0f) iter.remove()
        }
    }

    private fun checkPickups() {
        pickups.filter { it.alive }.forEach { p ->
            if (playerPos.dst(p.pos) < PLAYER_R + 18f) {
                p.alive = false
                when (p.type) {
                    PickupType.HEALTH        -> { playerHP = (playerHP + 35).coerceAtMost(MAX_HP); showMsg("+٣٥ صحة"); SoundManager.play("pickup") }
                    PickupType.SHIELD        -> { playerShield = MAX_SHIELD; shieldRechargeTimer = 0f; showMsg("درع مكتمل"); SoundManager.play("pickup", pitch = 1.2f) }
                    PickupType.AMMO_SHOTGUN  -> { shotgunAmmo += 30; currentWeapon = WeaponType.SHOTGUN; showMsg("شوتغان جاهز!"); SoundManager.play("pickup", pitch = 0.9f); SoundManager.playReload() }
                    PickupType.AMMO_MINIGUN  -> { minigunAmmo += 70; currentWeapon = WeaponType.MINIGUN; showMsg("مينيغان!"); SoundManager.play("pickup", pitch = 0.85f); SoundManager.playReload() }
                    PickupType.GRENADE       -> { grenadeCount += 3; showMsg("+٣ قنابل!"); SoundManager.play("pickup", pitch = 0.8f) }
                }
                spawnHitFX(p.pos, 0.2f, 1f, 0.4f, 10); score += 75
            }
        }
    }

    private fun checkWaveEnd(dt: Float) {
        waveMsgT -= dt
        if (enemies.all { it.hp <= 0 || it.deathTimer >= 0f }) {
            waveDelay += dt
            if (waveDelay > 2.8f) {
                wave++
                if (wave > MAX_WAVES) {
                    playerWon = true; gameOver = true
                    SoundManager.playVictory()
                    PlayerProfile.addRecord(score, wave - 1, killCount, playerLevel)
                } else {
                    showMsg("الموجة ${wave - 1} منتهية!  استعد...")
                    SoundManager.playWaveClear()
                    spawnWave()
                }
            }
        }
    }

    private fun updateComboAndNums(dt: Float) {
        comboTimer -= dt; if (comboTimer <= 0f) { combo = 0 }
        streakTimer -= dt; if (streakTimer <= 0f) { streakKills = 0 }
        comboFlash -= dt; levelUpFlash -= dt; streakFlash -= dt
        val iter = dmgNumbers.iterator()
        while (iter.hasNext()) { val d = iter.next(); d.pos.y += 48f * dt; d.life -= dt; if (d.life <= 0f) iter.remove() }
    }

    private fun gainXP(amount: Int) {
        playerXP += amount * (1 + combo / 8)
        if (playerXP >= xpToNext) {
            playerXP -= xpToNext; playerLevel++
            xpToNext = (xpToNext * 1.4f).toInt()
            levelUpFlash = 2.0f; levelUpMsg = "LEVEL UP! $playerLevel"
            playerHP = (playerHP + 25).coerceAtMost(MAX_HP); playerShield = MAX_SHIELD
            triggerShake(3f, 0.3f); SoundManager.play("levelup")
        }
    }

    private fun triggerShake(intensity: Float, dur: Float) {
        if (intensity > shakeIntensity) { shakeIntensity = intensity; shakeTimer = dur }
    }

    // ══════════════════════════════════════════════════════════════════
    //  FX
    // ══════════════════════════════════════════════════════════════════
    private fun spawnHitFX(pos: Vector2, r: Float, g: Float, b: Float, count: Int = 7) {
        repeat(count) {
            val ang = MathUtils.random(0f, 360f); val spd = MathUtils.random(70f, 240f)
            val rad = Math.toRadians(ang.toDouble())
            particles.add(Particle(Vector2(pos), Vector2(cos(rad).toFloat() * spd, sin(rad).toFloat() * spd),
                MathUtils.random(0.18f, 0.42f), 0.42f, r, g, b, MathUtils.random(3f, 8f)))
        }
    }

    private fun spawnDeathFX(pos: Vector2, r: Float, g: Float, b: Float) {
        repeat(28) {
            val ang = MathUtils.random(0f, 360f); val spd = MathUtils.random(100f, 480f)
            val rad = Math.toRadians(ang.toDouble())
            particles.add(Particle(Vector2(pos), Vector2(cos(rad).toFloat() * spd, sin(rad).toFloat() * spd),
                MathUtils.random(0.4f, 1.2f), 1.2f, r, g, b, MathUtils.random(4f, 18f)))
        }
        repeat(12) {
            val ang = MathUtils.random(0f, 360f); val d = MathUtils.random(5f, 35f)
            val rad = Math.toRadians(ang.toDouble())
            particles.add(Particle(
                Vector2(pos.x + cos(rad).toFloat() * d, pos.y + sin(rad).toFloat() * d),
                Vector2(0f, 0f), 10f, 10f, 0.38f, 0.02f, 0.02f, MathUtils.random(7f, 22f), isDecal = true))
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Draw
    // ══════════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════════
    //  خلفية السماء الفضائية داخل الساحة
    // ══════════════════════════════════════════════════════════════════

    // نجوم داخل الساحة (طبقتان)
    private data class ArenaStar(val x: Float, val y: Float, val r: Float,
                                  val bright: Float, val layer: Int, val twinkleOff: Float)
    private val arenaStars = List(140) {
        val lay = MathUtils.random(0, 1)
        ArenaStar(MathUtils.random(0f, 900f), MathUtils.random(0f, 560f),
            MathUtils.random(0.5f, 2.5f) * (0.6f + lay * 0.35f),
            MathUtils.random(0.3f, 1f), lay, MathUtils.random(0f, 6.28f))
    }

    // سدم الساحة
    private val arenaNebulae = listOf(
        floatArrayOf(100f, 430f, 200f, 0.22f, 0.04f, 0.58f, 0.10f),
        floatArrayOf(820f, 150f, 250f, 0.04f, 0.18f, 0.62f, 0.09f),
        floatArrayOf(470f, 520f, 160f, 0.55f, 0.04f, 0.20f, 0.08f),
        floatArrayOf(680f, 440f, 140f, 0.04f, 0.45f, 0.36f, 0.07f),
        floatArrayOf(200f, 80f,  175f, 0.40f, 0.10f, 0.55f, 0.06f)
    )

    private fun drawBackground() {
        shape.begin(ShapeRenderer.ShapeType.Filled)

        // ── سماء فضائية ─────────────────────────────────────────────
        shape.color = Color(0.02f, 0.02f, 0.07f, 1f); shape.rect(0f, WORLD_H * 0.5f, WORLD_W, WORLD_H * 0.5f)
        shape.color = Color(0.04f, 0.04f, 0.12f, 1f); shape.rect(0f, 0f, WORLD_W, WORLD_H * 0.5f)

        // سدم
        arenaNebulae.forEach { n ->
            shape.color = Color(n[3], n[4], n[5], n[6] * 1.25f); shape.circle(n[0], n[1], n[2])
            shape.color = Color(n[3], n[4], n[5], n[6] * 0.55f); shape.circle(n[0], n[1], n[2] * 1.55f)
        }

        // نجوم
        arenaStars.forEach { s ->
            val ta = (0.48f + 0.52f * sin(animTime * 2.2f + s.twinkleOff).toFloat()) * s.bright
            shape.color = when (s.layer) {
                0    -> Color(ta * 0.78f, ta * 0.82f, ta, ta)
                else -> Color(ta, ta * 0.96f, ta * 0.88f, ta)
            }
            shape.circle(s.x, s.y, s.r)
            if (s.r > 1.7f) {
                shape.color = Color(ta, ta, ta, ta * 0.18f); shape.circle(s.x, s.y, s.r * 2.8f)
            }
        }

        // ── طبقة أرضية بالساحة (grid شبه شفاف فوق السماء) ───────────
        shape.color = Color(0.08f, 0.10f, 0.18f, 0.38f); shape.rect(0f, 0f, WORLD_W, WORLD_H)

        // إضاءات زاويا ملونة
        val lights = listOf(
            floatArrayOf(0.2f, 0.4f, 1f, 0.07f, 0f, 0f),
            floatArrayOf(1f, 0.3f, 0.2f, 0.06f, WORLD_W, 0f),
            floatArrayOf(0.2f, 1f, 0.3f, 0.05f, 0f, WORLD_H),
            floatArrayOf(1f, 0.1f, 0.8f, 0.06f, WORLD_W, WORLD_H)
        )
        lights.forEach { l -> shape.color = Color(l[0], l[1], l[2], l[3]); shape.circle(l[4], l[5], 350f) }
        shape.end()

        // grid خفيف فوق السماء
        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Color(0.35f, 0.45f, 0.75f, 0.055f)
        var gx = 0f; while (gx <= WORLD_W) { shape.line(gx, 0f, gx, WORLD_H); gx += 55f }
        var gy = 0f; while (gy <= WORLD_H) { shape.line(0f, gy, WORLD_W, gy); gy += 55f }
        Gdx.gl.glLineWidth(3f)
        shape.color = Color(0.28f, 0.32f, 0.52f, 0.88f); shape.rect(0f, 0f, WORLD_W, WORLD_H)
        Gdx.gl.glLineWidth(1f)
        shape.end()

        // vignette حواف
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.52f)
        val vw = 62f
        shape.rect(0f, 0f, vw, WORLD_H); shape.rect(WORLD_W - vw, 0f, vw, WORLD_H)
        shape.rect(0f, 0f, WORLD_W, vw); shape.rect(0f, WORLD_H - vw, WORLD_W, vw)
        // حدود الساحة
        shape.color = Color(0.25f, 0.28f, 0.48f, 1f)
        shape.rect(-14f, -14f, WORLD_W + 28f, 14f); shape.rect(-14f, WORLD_H, WORLD_W + 28f, 14f)
        shape.rect(-14f, -14f, 14f, WORLD_H + 28f); shape.rect(WORLD_W, -14f, 14f, WORLD_H + 28f)
        shape.end()
    }

    private fun drawObstacles() {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        obstacles.forEach { o ->
            shape.color = Color(0f, 0f, 0f, 0.35f); shape.rect(o.x + 7f, o.y - 7f, o.w, o.h)
            shape.color = Color(0.32f, 0.30f, 0.27f, 1f); shape.rect(o.x, o.y, o.w, o.h)
            shape.color = Color(0.46f, 0.42f, 0.38f, 1f); shape.rect(o.x, o.y + o.h - 10f, o.w, 10f)
            shape.color = Color(0.58f, 0.54f, 0.48f, 0.55f); shape.rect(o.x, o.y, 4f, o.h)
        }
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2f)
        obstacles.forEach { o -> shape.color = Color(0.58f, 0.53f, 0.46f, 0.5f); shape.rect(o.x, o.y, o.w, o.h) }
        Gdx.gl.glLineWidth(1f); shape.end()
    }

    private fun drawWorld() {
        shape.begin(ShapeRenderer.ShapeType.Filled)

        // بقع دم
        particles.filter { it.isDecal }.forEach { p ->
            val a = (p.life / p.maxLife * 0.65f).coerceIn(0f, 0.65f)
            shape.color = Color(p.cr, p.cg, p.cb, a); shape.circle(p.pos.x, p.pos.y, p.size)
        }

        // قنابل في الهواء
        grenades.forEach { g ->
            if (!g.exploding) {
                shape.color = Color(0.2f, 0.2f, 0.2f, 1f); shape.circle(g.pos.x, g.pos.y, 9f)
                shape.color = Color(0.8f, 0.6f, 0.1f, 0.7f); shape.circle(g.pos.x, g.pos.y, 7f)
            } else {
                val prog = g.explodeTimer / 0.35f
                shape.color = Color(1f, 0.6f, 0.1f, (1f - prog) * 0.9f); shape.circle(g.pos.x, g.pos.y, g.radius)
                shape.color = Color(1f, 1f, 1f, (1f - prog) * 0.4f); shape.circle(g.pos.x, g.pos.y, g.radius * 0.5f)
            }
        }

        // بيك آبات
        pickups.filter { it.alive }.forEach { p ->
            val bob = sin(animTime * 3.5f).toFloat() * 5f; val py = p.pos.y + bob
            when (p.type) {
                PickupType.HEALTH -> {
                    shape.color = Color(0.1f, 1f, 0.3f, 0.18f); shape.circle(p.pos.x, py, 24f)
                    shape.color = Color(0.1f, 0.85f, 0.25f, 1f); shape.circle(p.pos.x, py, 16f)
                    shape.color = Color(1f, 1f, 1f, 0.9f)
                    shape.rect(p.pos.x - 2.5f, py - 9f, 5f, 18f); shape.rect(p.pos.x - 9f, py - 2.5f, 18f, 5f)
                }
                PickupType.SHIELD -> {
                    shape.color = Color(0.2f, 0.6f, 1f, 0.18f); shape.circle(p.pos.x, py, 24f)
                    shape.color = Color(0.1f, 0.5f, 0.95f, 1f); shape.circle(p.pos.x, py, 16f)
                    shape.color = Color(1f, 1f, 1f, 0.9f); shape.rect(p.pos.x - 7f, py - 2f, 14f, 12f); shape.rect(p.pos.x - 2f, py - 8f, 4f, 6f)
                }
                PickupType.AMMO_SHOTGUN -> {
                    shape.color = Color(1f, 0.4f, 0.1f, 0.18f); shape.circle(p.pos.x, py, 24f)
                    shape.color = Color(0.9f, 0.35f, 0.05f, 1f); shape.circle(p.pos.x, py, 16f)
                    shape.color = Color(1f, 1f, 1f, 0.9f)
                    for (si in -1..1) shape.rect(p.pos.x - 2f + si * 7f, py - 8f, 4f, 16f)
                }
                PickupType.AMMO_MINIGUN -> {
                    shape.color = Color(1f, 0.9f, 0.1f, 0.18f); shape.circle(p.pos.x, py, 24f)
                    shape.color = Color(0.95f, 0.80f, 0.05f, 1f); shape.circle(p.pos.x, py, 16f)
                    shape.color = Color(0.1f, 0.1f, 0.1f, 0.9f); shape.circle(p.pos.x, py, 7f)
                    shape.color = Color(1f, 1f, 1f, 0.5f)
                    for (ai in 0..5) { val ar = Math.toRadians(ai * 60.0).toFloat(); shape.rect(p.pos.x + cos(ar) * 8f - 1.5f, py + sin(ar) * 8f - 5f, 3f, 10f) }
                }
                PickupType.GRENADE -> {
                    // أيقونة قنبلة خضراء
                    shape.color = Color(0.1f, 0.9f, 0.5f, 0.18f); shape.circle(p.pos.x, py, 26f)
                    shape.color = Color(0.08f, 0.75f, 0.35f, 1f); shape.circle(p.pos.x, py, 16f)
                    shape.color = Color(0.6f, 0.4f, 0.1f, 1f); shape.rect(p.pos.x - 3f, py + 10f, 6f, 9f)
                    shape.color = Color(1f, 1f, 1f, 0.7f)
                    // علامة G
                    shape.rect(p.pos.x - 6f, py - 6f, 12f, 3f)
                    shape.rect(p.pos.x - 6f, py + 4f, 12f, 3f)
                    shape.rect(p.pos.x - 6f, py - 6f, 3f, 13f)
                    shape.rect(p.pos.x + 1f, py - 1f, 5f, 3f)
                }
            }
        }

        // جسيمات طائرة
        particles.filter { !it.isDecal }.forEach { p ->
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            shape.color = Color(p.cr, p.cg, p.cb, a); shape.circle(p.pos.x, p.pos.y, p.size * a + 1f)
        }
        shape.end()

        // ذيول الرصاصات
        shape.begin(ShapeRenderer.ShapeType.Line)
        bullets.forEach { b ->
            for (ti in 0 until b.trail.size - 1) {
                val a = (1f - ti.toFloat() / b.trail.size) * 0.55f
                shape.color = Color(b.cr, b.cg, b.cb, a)
                Gdx.gl.glLineWidth(if (b.isPlayer) 3f else 2f)
                shape.line(b.trail[ti], b.trail[ti + 1])
            }
        }
        Gdx.gl.glLineWidth(1f); shape.end()

        shape.begin(ShapeRenderer.ShapeType.Filled)

        // رصاصات
        bullets.forEach { b ->
            val r = if (b.isPlayer) 7f else 6f
            shape.color = Color(b.cr, b.cg, b.cb, 1f); shape.circle(b.pos.x, b.pos.y, r)
            shape.color = Color(b.cr, b.cg, b.cb, 0.35f); shape.circle(b.pos.x, b.pos.y, r + 5f)
        }

        // أنيميشن موت الأعداء
        enemies.filter { it.deathTimer >= 0f }.forEach { e ->
            val prog = 1f - e.deathTimer / 0.60f
            shape.color = Color(1f, 0.4f, 0.1f, (1f - prog) * 0.9f)
            shape.circle(e.pos.x, e.pos.y, e.radius * (1f + prog * 3f))
        }

        // رسم الأعداء الأحياء
        enemies.filter { it.hp > 0 && it.deathTimer < 0f }.forEach { e ->
            val flash = e.hitFlash > 0f; val hpPct = (e.hp.toFloat() / e.maxHp).coerceIn(0f, 1f)
            when (e.type) {
                EnemyType.BOSS_QUEEN -> drawBossQueen(e, flash, hpPct)
                EnemyType.HEAVY      -> drawHeavy(e, flash, hpPct)
                EnemyType.SNIPER     -> drawSniper(e, flash, hpPct)
                EnemyType.FLANKER    -> drawFlanker(e, flash, hpPct)
                else                 -> drawGrunt(e, flash, hpPct)
            }
        }

        // اللاعب
        if (playerAlive) drawPlayer()
        shape.end()

        // أرقام الضرر
        if (dmgNumbers.isNotEmpty()) {
            game.batch.begin()
            dmgNumbers.forEach { d ->
                val a = d.life.coerceIn(0f, 1f)
                val txt = if (d.isCrit) "CRIT ${d.value}" else "${d.value}"
                val h = if (d.isCrit) 30f else 22f
                ArabicText.drawCenter(game.batch, txt, d.pos.x, d.pos.y + 10f, h,
                    if (d.isCrit) 1f else 1f, if (d.isCrit) 0.2f else 0.88f, if (d.isCrit) 1f else 0.08f, a)
            }
            game.batch.end()
        }

        // مؤشرات الأعداء خارج الشاشة
        drawOffScreenIndicators()
    }

    // ── رسم الشخصيات ──────────────────────────────────────────────
    private fun drawGrunt(e: Enemy, flash: Boolean, hpPct: Float) {
        val fc = if (flash) Color(1f, 1f, 1f, 1f) else Color(0.78f, 0.1f, 0.08f, 1f)
        shape.color = Color(fc.r * 0.5f, fc.g * 0.5f, fc.b * 0.5f, 0.4f); shape.circle(e.pos.x, e.pos.y - 3f, e.radius + 5f)
        shape.color = fc; shape.circle(e.pos.x, e.pos.y, e.radius)
        drawEnemyDetails(e, hpPct, Color(1f, 0.1f, 0.08f, 0.8f))
    }

    private fun drawHeavy(e: Enemy, flash: Boolean, hpPct: Float) {
        val fc = if (flash) Color(1f, 1f, 1f, 1f) else Color(0.82f, 0.42f, 0.08f, 1f)
        shape.color = Color(fc.r * 0.5f, fc.g * 0.5f, fc.b * 0.5f, 0.45f); shape.circle(e.pos.x, e.pos.y - 4f, e.radius + 8f)
        shape.color = fc; shape.circle(e.pos.x, e.pos.y, e.radius)
        // طبقة درع
        shape.color = Color(0.4f, 0.38f, 0.32f, 0.7f); shape.arc(e.pos.x, e.pos.y, e.radius, e.angle - 90f, 180f)
        drawEnemyDetails(e, hpPct, Color(1f, 0.5f, 0.1f, 0.8f))
    }

    private fun drawSniper(e: Enemy, flash: Boolean, hpPct: Float) {
        val fc = if (flash) Color(1f, 1f, 1f, 1f) else Color(0.08f, 0.72f, 0.95f, 1f)
        shape.color = Color(fc.r * 0.5f, fc.g * 0.5f, fc.b * 0.5f, 0.4f); shape.circle(e.pos.x, e.pos.y - 3f, e.radius + 5f)
        shape.color = fc; shape.circle(e.pos.x, e.pos.y, e.radius)
        // خط بصر الليزر
        shape.color = Color(0.1f, 0.9f, 1f, 0.18f)
        val rad = Math.toRadians(e.angle.toDouble())
        shape.line(e.pos.x, e.pos.y, e.pos.x + cos(rad).toFloat() * 220f, e.pos.y + sin(rad).toFloat() * 220f)
        drawEnemyDetails(e, hpPct, Color(0.1f, 0.7f, 1f, 0.8f))
    }

    private fun drawFlanker(e: Enemy, flash: Boolean, hpPct: Float) {
        val fc = if (flash) Color(1f, 1f, 1f, 1f) else Color(0.65f, 0.08f, 0.85f, 1f)
        // شكل مثلث للمتسلل
        shape.color = Color(fc.r * 0.5f, fc.g * 0.5f, fc.b * 0.5f, 0.4f); shape.circle(e.pos.x, e.pos.y - 3f, e.radius + 4f)
        shape.color = fc; shape.circle(e.pos.x, e.pos.y, e.radius)
        drawEnemyDetails(e, hpPct, Color(0.7f, 0.1f, 1f, 0.8f))
    }

    private fun drawBossQueen(e: Enemy, flash: Boolean, hpPct: Float) {
        val fc = if (flash) Color(1f, 1f, 1f, 1f)
                 else when (e.phase) { 3 -> Color(1f, 0.1f, 0.1f, 1f); 2 -> Color(0.85f, 0.1f, 0.85f, 1f); else -> Color(0.6f, 0f, 0.9f, 1f) }
        // توهج خارجي للبوص
        val glowA = (0.15f + sin(animTime * 3.0f).toFloat() * 0.08f).coerceIn(0f, 0.5f)
        shape.color = Color(fc.r, fc.g, fc.b, glowA); shape.circle(e.pos.x, e.pos.y, e.radius + 22f)
        shape.color = Color(fc.r * 0.7f, fc.g * 0.7f, fc.b * 0.7f, 0.6f); shape.circle(e.pos.x, e.pos.y, e.radius + 8f)
        shape.color = fc; shape.circle(e.pos.x, e.pos.y, e.radius)
        // تاج للبوص
        shape.color = Color(1f, 0.85f, 0.1f, 0.9f)
        for (k in 0..4) {
            val ka = Math.toRadians(k * 72.0 + animTime * 60.0).toFloat()
            shape.circle(e.pos.x + cos(ka) * (e.radius + 10f), e.pos.y + sin(ka) * (e.radius + 10f), 5f)
        }
        // ليزر في المرحلة 3
        if (e.phase == 3) {
            val rad = Math.toRadians(e.angle.toDouble())
            val la = (0.6f + 0.4f * sin(animTime * 15f).toFloat()).coerceIn(0f, 1f)
            shape.color = Color(1f, 0f, 0.5f, la * 0.7f)
            shape.line(e.pos.x, e.pos.y, e.pos.x + cos(rad).toFloat() * 350f, e.pos.y + sin(rad).toFloat() * 350f)
        }

        // شريط HP البوص (أكبر وأوضح)
        val bw = e.radius * 2.8f; val bh = 14f
        val bx = e.pos.x - bw / 2f; val by = e.pos.y + e.radius + 14f
        shape.color = Color(0f, 0f, 0f, 0.8f); shape.rect(bx - 3f, by - 3f, bw + 6f, bh + 6f)
        shape.color = Color(0.15f, 0f, 0.15f, 1f); shape.rect(bx, by, bw, bh)
        val hpCol = when {
            hpPct > 0.55f -> Color(0.6f, 0f, 0.9f, 1f)
            hpPct > 0.30f -> Color(1f, 0.4f, 0f, 1f)
            else          -> Color(1f, 0.05f, 0.05f, 1f)
        }
        shape.color = hpCol; shape.rect(bx, by, bw * hpPct, bh)
        shape.color = Color(1f, 1f, 1f, 0.20f); shape.rect(bx, by + bh * 0.7f, bw * hpPct, bh * 0.3f)
    }

    private fun drawEnemyDetails(e: Enemy, hpPct: Float, barColor: Color) {
        // بندقية
        val rad = Math.toRadians(e.angle.toDouble())
        shape.color = Color(0.2f, 0.2f, 0.25f, 0.9f)
        shape.rectLine(e.pos.x, e.pos.y,
            e.pos.x + cos(rad).toFloat() * (e.radius + 18f),
            e.pos.y + sin(rad).toFloat() * (e.radius + 18f), 5f)
        // عيون
        val perpRad = rad + Math.PI / 2.0
        val ex1 = e.pos.x + cos(perpRad).toFloat() * 7f + cos(rad).toFloat() * 5f
        val ey1 = e.pos.y + sin(perpRad).toFloat() * 7f + sin(rad).toFloat() * 5f
        val ex2 = e.pos.x - cos(perpRad).toFloat() * 7f + cos(rad).toFloat() * 5f
        val ey2 = e.pos.y - sin(perpRad).toFloat() * 7f + sin(rad).toFloat() * 5f
        shape.color = Color(1f, 0.9f, 0.2f, 1f); shape.circle(ex1, ey1, 4f); shape.circle(ex2, ey2, 4f)
        // شريط HP صغير
        val bw = e.radius * 2.2f
        val bx = e.pos.x - bw / 2f; val by = e.pos.y + e.radius + 8f
        shape.color = Color(0f, 0f, 0f, 0.7f); shape.rect(bx - 1f, by - 1f, bw + 2f, 7f)
        shape.color = Color(0.12f, 0.04f, 0.04f, 1f); shape.rect(bx, by, bw, 5f)
        shape.color = barColor; shape.rect(bx, by, bw * hpPct, 5f)
    }

    private fun drawPlayer() {
        val pCol = listOf(
            Color(0.18f, 0.48f, 0.98f, 1f), Color(0.15f, 0.78f, 0.12f, 1f),
            Color(0.88f, 0.10f, 0.10f, 1f), Color(0.95f, 0.80f, 0.05f, 1f), Color(0.55f, 0.04f, 0.80f, 1f)
        )[PlayerProfile.color.coerceIn(0, 4)]

        val rad     = Math.toRadians(playerAngle.toDouble())
        val cosA    = cos(rad).toFloat()
        val sinA    = sin(rad).toFloat()
        val perpX   = -sinA;  val perpY = cosA
        val cx = playerPos.x; val cy = playerPos.y + bodyBob

        // ── مؤشر Dash cooldown (قوس دائري) ──────────────────────────
        if (dashCooldown > 0f) {
            val prog = dashCooldown / DASH_CD
            shape.color = Color(0.3f, 0.6f, 1f, 0.20f); shape.circle(cx, cy, PLAYER_R + 15f)
            shape.color = Color(0.3f, 0.6f, 1f, 0.55f)
            shape.arc(cx, cy, PLAYER_R + 13f, 90f, (1f - prog) * 360f)
        }

        // ── ظل أرضي بيضاوي ──────────────────────────────────────────
        val shadowA = if (isDashing) 0.12f else 0.28f
        shape.color = Color(0f, 0f, 0f, shadowA)
        shape.ellipse(cx - PLAYER_R * 0.9f, playerPos.y - PLAYER_R * 0.45f, PLAYER_R * 1.8f, PLAYER_R * 0.55f)

        val invBlink = invTimer > 0f && (animTime * 14f).toInt() % 2 == 0
        if (invBlink) return

        // ── أرجل (مشي واقعي) ────────────────────────────────────────
        val legLen    = PLAYER_R * 1.05f
        val legWidth  = 5.5f
        val legOffset = PLAYER_R * 0.42f
        // أرجل 4 (زوجان)
        for (side in listOf(-1f, 1f)) {
            val phase = if (side > 0f) legPhase else legPhase + Math.PI.toFloat()
            val swing = sin(phase) * legLen * 0.55f
            val lx1 = cx + perpX * legOffset * side
            val ly1 = cy + perpY * legOffset * side
            val lx2 = lx1 - cosA * (legLen * 0.55f) + perpX * swing * 0.35f
            val ly2 = ly1 - sinA * (legLen * 0.55f) + perpY * swing * 0.35f
            val lx3 = lx2 - cosA * (legLen * 0.45f) + perpX * swing
            val ly3 = ly2 - sinA * (legLen * 0.45f) + perpY * swing
            // ساق علوية
            shape.color = Color(pCol.r * 0.55f, pCol.g * 0.55f, pCol.b * 0.80f, 0.92f)
            shape.rectLine(lx1, ly1, lx2, ly2, legWidth)
            // ساق سفلية
            shape.color = Color(pCol.r * 0.40f, pCol.g * 0.40f, pCol.b * 0.65f, 0.88f)
            shape.rectLine(lx2, ly2, lx3, ly3, legWidth - 1f)
            // قدم
            shape.color = Color(0.18f, 0.18f, 0.22f, 0.95f)
            shape.circle(lx3, ly3, 4.5f)
        }

        // ── ذراع خلفية (خلف الجسم) ───────────────────────────────────
        val armBackX = cx - cosA * 4f + perpX * (-PLAYER_R * 0.48f)
        val armBackY = cy - sinA * 4f + perpY * (-PLAYER_R * 0.48f)
        val armBackEnd = Vector2(armBackX + cosA * 14f, armBackY + sinA * 14f)
        shape.color = Color(pCol.r * 0.45f, pCol.g * 0.45f, pCol.b * 0.72f, 0.80f)
        shape.rectLine(armBackX, armBackY, armBackEnd.x, armBackEnd.y, 5f)

        // ── جسم المقاتل الرئيسي ──────────────────────────────────────
        // توهج خارجي
        shape.color = Color(pCol.r, pCol.g, pCol.b, 0.14f + sin(breathCycle).toFloat() * 0.04f)
        shape.circle(cx, cy, PLAYER_R + 14f)
        // هالة الدرع (إذا كان الدرع نشطاً)
        if (playerShield > 0) {
            val shA = (playerShield.toFloat() / MAX_SHIELD) * 0.30f
            shape.color = Color(0.3f, 0.65f, 1f, shA + sin(animTime * 5f).toFloat() * 0.06f)
            shape.circle(cx, cy, PLAYER_R + 8f)
        }
        // جسم الكتلة الرئيسية
        shape.color = Color(pCol.r * 0.55f, pCol.g * 0.55f, pCol.b * 0.85f, 0.65f)
        shape.circle(cx, cy, PLAYER_R + 3f)
        shape.color = pCol
        shape.circle(cx, cy, PLAYER_R)
        // بطن/ظهر أغمق
        shape.color = Color(pCol.r * 0.42f, pCol.g * 0.42f, pCol.b * 0.72f, 1f)
        shape.arc(cx, cy, PLAYER_R, playerAngle + 90f, 180f)
        // لمعة علوية
        shape.color = Color(1f, 1f, 1f, 0.18f)
        shape.arc(cx + cosA * 4f, cy + sinA * 4f, PLAYER_R * 0.55f, playerAngle - 40f, 100f)

        // ── خوذة / رأس ───────────────────────────────────────────────
        val headX = cx + cosA * (PLAYER_R * 0.38f); val headY = cy + sinA * (PLAYER_R * 0.38f)
        // خوذة قاعدة
        shape.color = Color(0.22f, 0.22f, 0.28f, 0.95f)
        shape.circle(headX, headY, PLAYER_R * 0.52f)
        // لون الخوذة
        shape.color = Color(pCol.r * 0.78f, pCol.g * 0.78f, pCol.b * 0.98f, 0.9f)
        shape.circle(headX, headY, PLAYER_R * 0.44f)
        // شبك الخوذة (قناع)
        shape.color = Color(0.10f, 0.10f, 0.15f, 0.75f)
        shape.arc(headX + cosA * 3f, headY + sinA * 3f, PLAYER_R * 0.3f, playerAngle - 65f, 130f)
        // عيون متوهجة
        val eyeRad  = Math.toRadians(playerAngle.toDouble())
        val eyePerp = eyeRad + Math.PI / 2.0
        val eyeR    = PLAYER_R * 0.28f
        val ex1 = headX + cos(eyePerp).toFloat() * eyeR + cosA * 2f
        val ey1 = headY + sin(eyePerp).toFloat() * eyeR + sinA * 2f
        val ex2 = headX - cos(eyePerp).toFloat() * eyeR + cosA * 2f
        val ey2 = headY - sin(eyePerp).toFloat() * eyeR + sinA * 2f
        // بريق العيون
        shape.color = Color(1f, 0.95f, 0.30f, 0.90f)
        shape.circle(ex1, ey1, 5.5f); shape.circle(ex2, ey2, 5.5f)
        shape.color = Color(1f, 1f, 1f, 0.60f)
        shape.circle(ex1 + cosA, ey1 + sinA, 2.2f); shape.circle(ex2 + cosA, ey2 + sinA, 2.2f)

        // ── ذراع أمامية + سلاح ───────────────────────────────────────
        val shoulderX = cx + cosA * 5f + perpX * (PLAYER_R * 0.45f)
        val shoulderY = cy + sinA * 5f + perpY * (PLAYER_R * 0.45f)
        // ذراع
        shape.color = Color(pCol.r * 0.68f, pCol.g * 0.68f, pCol.b * 0.90f, 0.90f)
        shape.rectLine(shoulderX, shoulderY,
            shoulderX + cosA * (PLAYER_R * 0.65f), shoulderY + sinA * (PLAYER_R * 0.65f), 6f)

        // السلاح مع ارتداد
        val recoilPush = -aimRecoil * 0.5f
        val gunBase = Vector2(cx + cosA * (PLAYER_R + recoilPush), cy + sinA * (PLAYER_R + recoilPush))
        val gunTip  = Vector2(cx + cosA * (PLAYER_R + 24f + recoilPush), cy + sinA * (PLAYER_R + 24f + recoilPush))

        when (currentWeapon) {
            WeaponType.SHOTGUN -> {
                // شوتغان — أعرض وأقصر
                shape.color = Color(0.22f, 0.20f, 0.18f, 1f)
                shape.rectLine(gunBase.x, gunBase.y, gunTip.x - cosA * 4f, gunTip.y - sinA * 4f, 9f)
                shape.color = Color(0.55f, 0.45f, 0.32f, 0.85f)
                shape.rectLine(gunBase.x, gunBase.y, gunTip.x - cosA * 10f, gunTip.y - sinA * 10f, 5f)
                // ماسورة مزدوجة
                shape.color = Color(0.30f, 0.28f, 0.25f, 1f)
                shape.rectLine(gunTip.x - cosA * 4f + perpX * 3f, gunTip.y - sinA * 4f + perpY * 3f,
                               gunTip.x + perpX * 3f, gunTip.y + perpY * 3f, 4f)
                shape.rectLine(gunTip.x - cosA * 4f - perpX * 3f, gunTip.y - sinA * 4f - perpY * 3f,
                               gunTip.x - perpX * 3f, gunTip.y - perpY * 3f, 4f)
            }
            WeaponType.MINIGUN -> {
                // ميني-غان — أطول مع دوران
                val spin = animTime * 22f
                shape.color = Color(0.25f, 0.22f, 0.18f, 1f)
                shape.rectLine(gunBase.x, gunBase.y, gunTip.x + cosA * 5f, gunTip.y + sinA * 5f, 8f)
                // براميل دوارة
                for (b in 0..2) {
                    val ba = Math.toRadians((b * 120.0 + spin)).toFloat()
                    shape.color = Color(0.45f, 0.40f, 0.35f, 0.9f)
                    shape.circle(gunTip.x + cos(ba) * 5f, gunTip.y + sin(ba) * 5f, 3.5f)
                }
                // حلقة ربط
                shape.color = Color(0.65f, 0.55f, 0.25f, 0.8f)
                shape.circle(gunBase.x + cosA * 8f, gunBase.y + sinA * 8f, 5f)
            }
            WeaponType.MELEE -> {
                // سكين / مشرط
                shape.color = Color(0.15f, 0.15f, 0.20f, 1f)
                shape.rectLine(gunBase.x, gunBase.y, gunTip.x - cosA * 8f, gunTip.y - sinA * 8f, 5f)
                shape.color = Color(0.82f, 0.82f, 0.90f, 1f)
                shape.triangle(
                    gunTip.x, gunTip.y,
                    gunTip.x - cosA * 14f + perpX * 5f, gunTip.y - sinA * 14f + perpY * 5f,
                    gunTip.x - cosA * 14f - perpX * 5f, gunTip.y - sinA * 14f - perpY * 5f
                )
            }
            else -> {
                // مسدس
                shape.color = Color(0.20f, 0.20f, 0.26f, 1f)
                shape.rectLine(gunBase.x, gunBase.y, gunTip.x, gunTip.y, 6f)
                // مقبض
                shape.color = Color(0.30f, 0.25f, 0.20f, 0.85f)
                shape.rectLine(gunBase.x + perpX * 2f, gunBase.y + perpY * 2f,
                    gunBase.x - perpX * 4f - cosA * 4f, gunBase.y - perpY * 4f - sinA * 4f, 4f)
                // حلقة الماسورة
                shape.color = Color(0.40f, 0.40f, 0.45f, 0.8f)
                shape.circle(gunTip.x, gunTip.y, 3.5f)
            }
        }

        // ── وهج فوهة السلاح ──────────────────────────────────────────
        if (isMuzzleFlash) {
            val muzzleX = cx + cosA * (PLAYER_R + 26f + recoilPush)
            val muzzleY = cy + sinA * (PLAYER_R + 26f + recoilPush)
            // وهج رئيسي
            shape.color = Color(1f, 0.92f, 0.45f, 0.95f)
            shape.circle(muzzleX, muzzleY, 11f)
            shape.color = Color(1f, 1f, 1f, 0.85f)
            shape.circle(muzzleX, muzzleY, 5.5f)
            // شظايا النار
            repeat(if (currentWeapon == WeaponType.SHOTGUN) 5 else 3) { i ->
                val fa = rad + MathUtils.random(-0.7f, 0.7f)
                val fd = MathUtils.random(8f, 20f)
                shape.color = Color(1f, MathUtils.random(0.5f, 0.9f), 0.1f, MathUtils.random(0.4f, 0.8f))
                shape.circle(muzzleX + cos(fa).toFloat() * fd, muzzleY + sin(fa).toFloat() * fd, MathUtils.random(2f, 5f))
            }
        }
    }

    // ── مؤشرات خارج الشاشة ────────────────────────────────────────
    private fun drawOffScreenIndicators() {
        val margin = 28f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        enemies.filter { it.hp > 0 && it.deathTimer < 0f }.forEach { e ->
            val isOffScreen = e.pos.x < margin || e.pos.x > WORLD_W - margin ||
                              e.pos.y < margin || e.pos.y > WORLD_H - margin
            if (!isOffScreen) return@forEach

            val dx = e.pos.x - WORLD_W / 2f; val dy = e.pos.y - WORLD_H / 2f
            val ang = atan2(dy.toDouble(), dx.toDouble()).toFloat()
            // نقطة على حافة الشاشة
            val edgeX = (WORLD_W / 2f + cos(ang) * (WORLD_W / 2f - 24f)).coerceIn(20f, WORLD_W - 20f)
            val edgeY = (WORLD_H / 2f + sin(ang) * (WORLD_H / 2f - 24f)).coerceIn(20f, WORLD_H - 20f)

            val col = when (e.type) {
                EnemyType.BOSS_QUEEN -> Color(0.9f, 0.1f, 0.9f, 0.9f)
                EnemyType.HEAVY      -> Color(1f, 0.5f, 0.1f, 0.9f)
                EnemyType.SNIPER     -> Color(0.1f, 0.7f, 1f, 0.9f)
                else                 -> Color(1f, 0.15f, 0.15f, 0.9f)
            }
            // مثلث سهم
            shape.color = col
            val cosA = cos(ang); val sinA = sin(ang)
            val size = 12f
            shape.triangle(
                (edgeX + cosA * size).toFloat(), (edgeY + sinA * size).toFloat(),
                (edgeX - sinA * size * 0.5f - cosA * size * 0.5f).toFloat(), (edgeY + cosA * size * 0.5f - sinA * size * 0.5f).toFloat(),
                (edgeX + sinA * size * 0.5f - cosA * size * 0.5f).toFloat(), (edgeY - cosA * size * 0.5f - sinA * size * 0.5f).toFloat()
            )
            // نقطة مركزية
            shape.color = Color(1f, 1f, 1f, 0.7f); shape.circle(edgeX, edgeY, 4f)
        }
        shape.end()
    }

    // ── دم الشاشة ─────────────────────────────────────────────────
    private fun drawBloodOverlay() {
        if (bloodOverlay <= 0.01f && damageFlash <= 0.01f) return
        // Camera reset to center for overlay
        val savedCam = cam.position.cpy()
        cam.position.set(WORLD_W / 2f, WORLD_H / 2f, 0f)
        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)
        // فلاش أحمر عند الإصابة
        if (damageFlash > 0f) {
            damageFlash -= Gdx.graphics.deltaTime * 5f
            shape.color = Color(1f, 0f, 0f, (damageFlash * 0.45f).coerceIn(0f, 0.45f))
            shape.rect(0f, 0f, WORLD_W, WORLD_H)
        }
        // دم على الحواف عند HP منخفض
        if (bloodOverlay > 0.01f) {
            val pulse = bloodPulse * 0.3f
            val a = bloodOverlay * (0.7f + pulse)
            val vw = 90f + bloodOverlay * 120f
            shape.color = Color(0.6f, 0f, 0f, a * 0.6f)
            shape.rect(0f, 0f, vw, WORLD_H); shape.rect(WORLD_W - vw, 0f, vw, WORLD_H)
            shape.rect(0f, 0f, WORLD_W, vw); shape.rect(0f, WORLD_H - vw, WORLD_W, vw)
        }
        shape.end()

        // إعادة الكاميرا
        cam.position.set(savedCam)
        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined
    }

    // ── HUD ───────────────────────────────────────────────────────
    private fun drawHUD(dt: Float) {
        // Reset camera for HUD
        val savedCam = cam.position.cpy()
        cam.position.set(WORLD_W / 2f, WORLD_H / 2f, 0f)
        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        game.batch.begin()

        // النتيجة والموجة
        ArabicText.drawCenter(game.batch, "نقاط: $score", WORLD_W / 2f, WORLD_H - 8f,
            26f, 1f, 0.9f, 0.3f, 1f)
        ArabicText.drawCenter(game.batch, "موجة $wave / $MAX_WAVES", WORLD_W * 0.72f, WORLD_H - 8f,
            24f, 0.65f, 0.90f, 1f, 1f)
        val alive = enemies.count { it.hp > 0 && it.deathTimer < 0f }
        ArabicText.draw(game.batch, "أعداء: $alive", WORLD_W - 150f, WORLD_H - 8f, 22f, 1f, 0.45f, 0.45f, 1f)
        game.batch.end()

        // HP
        val bx = 12f; val by = 10f; val bw = WORLD_W * 0.22f; val bh = 22f
        val hPct = (playerHP.toFloat() / MAX_HP).coerceIn(0f, 1f)
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.75f); shape.rect(bx - 3f, by - 3f, bw + 6f, bh + 6f)
        shape.color = Color(0.12f, 0.04f, 0.04f, 1f); shape.rect(bx, by, bw, bh)
        shape.color = when { hPct > 0.55f -> Color(0.10f, 0.88f, 0.18f, 1f); hPct > 0.28f -> Color(1f, 0.75f, 0f, 1f); else -> Color(0.95f, 0.08f, 0.08f, 1f) }
        shape.rect(bx, by, bw * hPct, bh)
        shape.color = Color(1f, 1f, 1f, 0.18f); shape.rect(bx, by + bh * 0.72f, bw * hPct, bh * 0.28f)
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Color(1f, 1f, 1f, 0.32f); shape.rect(bx, by, bw, bh)
        shape.end()

        // درع
        val sPct = playerShield.toFloat() / MAX_SHIELD
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.05f, 0.1f, 0.2f, 0.8f); shape.rect(bx, by + bh + 4f, bw, 10f)
        if (sPct > 0f) {
            shape.color = if (shieldRechargeTimer > 0f) Color(0.2f, 0.4f, 0.7f, 0.6f) else Color(0.25f, 0.60f, 1f, 0.9f)
            shape.rect(bx, by + bh + 4f, bw * sPct, 10f)
        }
        shape.end()

        game.batch.begin()
        ArabicText.draw(game.batch, "HP $playerHP", bx + 5f, by + bh, 18f, 1f, 1f, 1f, 1f)
        ArabicText.draw(game.batch, "درع $playerShield", bx + 5f, by + bh + 22f, 16f, 0.5f, 0.8f, 1f, 0.9f)
        game.batch.end()

        drawWeaponHUD()
        drawXPBar()
        drawGrenadeButton()
        drawDashButton()
        drawJoystick()

        game.batch.begin()

        // Combo
        if (combo >= 2) {
            val ca = (comboTimer / COMBO_WIN).coerceIn(0f, 1f)
            ArabicText.drawCenter(game.batch, "x$combo   كومبو!", WORLD_W / 2f, WORLD_H * 0.16f,
                36f, 1f, 0.55f + comboFlash * 0.45f, 0.1f, ca)
        }

        // Kill streak
        if (streakFlash > 0f) {
            val sa = (streakFlash / 2.5f).coerceIn(0f, 1f)
            ArabicText.drawCenter(game.batch, streakMsg, WORLD_W / 2f, WORLD_H * 0.24f,
                40f, 1f, 0.9f, 0.1f, sa)
        }

        // رسالة الموجة
        if (waveMsgT > 0f) {
            val a = (waveMsgT / 3.2f).coerceIn(0f, 1f)
            ArabicText.drawCenter(game.batch, waveMsg, WORLD_W / 2f, WORLD_H * 0.62f, 42f, 0.2f, 1f, 0.45f, a)
        }

        // رفع مستوى
        if (levelUpFlash > 0f) {
            val a = (levelUpFlash / 2f).coerceIn(0f, 1f)
            ArabicText.drawCenter(game.batch, "ارتفعت مستوى! ★ $playerLevel", WORLD_W / 2f, WORLD_H * 0.52f,
                44f, 1f, 0.9f, 0.1f, a)
        }

        // تحذير سخونة مينيغان
        if (currentWeapon == WeaponType.MINIGUN && weaponHeat > 0.75f) {
            val ha = abs(sin(animTime * 10f)).toFloat()
            ArabicText.drawCenter(game.batch, "تحذير: سخونة!", WORLD_W * 0.83f, WORLD_H * 0.36f, 26f, 1f, 0.2f, 0f, ha)
        }

        // Game Over / Win
        if (gameOver) {
            game.batch.end()
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.color = Color(0f, 0f, 0f, 0.88f); shape.rect(0f, 0f, WORLD_W, WORLD_H)
            shape.end()
            game.batch.begin()
            val isNewRecord = score >= PlayerProfile.highScore && score > 0
            if (playerWon) {
                ArabicText.drawCenter(game.batch, "انتصرت يا ${PlayerProfile.name}!", WORLD_W / 2f, WORLD_H * 0.74f, 56f, 0.2f, 1f, 0.4f, 1f)
            } else {
                ArabicText.drawCenter(game.batch, "انتهت اللعبة", WORLD_W / 2f, WORLD_H * 0.74f, 60f, 1f, 0.1f, 0.08f, 1f)
            }
            ArabicText.drawCenter(game.batch, "النقاط: $score", WORLD_W / 2f, WORLD_H * 0.59f, 44f, 1f, 0.88f, 0.1f, 1f)
            if (isNewRecord) {
                val blinkA2 = abs(sin(animTime * 4f)).toFloat()
                ArabicText.drawCenter(game.batch, "🏆 رقم قياسي جديد!", WORLD_W / 2f, WORLD_H * 0.50f, 34f, 1f, 0.88f, 0.05f, blinkA2)
            } else {
                ArabicText.drawCenter(game.batch, "أعلى نقطة: ${PlayerProfile.highScore}", WORLD_W / 2f, WORLD_H * 0.50f, 28f, 0.55f, 0.80f, 1f, 1f)
            }
            ArabicText.drawCenter(game.batch, "قتلات: $killCount   مستوى: $playerLevel   موجة: $wave",
                WORLD_W / 2f, WORLD_H * 0.40f, 26f, 0.7f, 0.7f, 0.7f, 1f)
            val blinkA = abs(sin(animTime * 2f)).toFloat()
            ArabicText.drawCenter(game.batch, "اضغط للعودة للقائمة", WORLD_W / 2f, WORLD_H * 0.28f, 28f, 0.6f, 0.88f, 1f, blinkA)
        }
        game.batch.end()

        // ميني-ماب
        drawMinimap()

        // إعادة الكاميرا
        cam.position.set(savedCam)
        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined
    }

    private fun drawWeaponHUD() {
        val wx = WORLD_W - 158f; val wy = 10f; val ww = 145f; val wh = 54f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.7f); shape.rect(wx - 4f, wy - 4f, ww + 8f, wh + 8f)
        shape.color = Color(0.06f, 0.06f, 0.09f, 1f); shape.rect(wx, wy, ww, wh)
        val wCol = when (currentWeapon) {
            WeaponType.SHOTGUN -> Color(1f, 0.4f, 0.1f, 1f)
            WeaponType.MINIGUN -> Color(1f, 0.85f, 0.05f, 1f)
            WeaponType.MELEE   -> Color(1f, 0.3f, 0.5f, 1f)
            else               -> Color(0.55f, 0.78f, 1f, 1f)
        }
        shape.color = Color(wCol.r, wCol.g, wCol.b, 0.18f); shape.rect(wx, wy, ww, wh)
        if (currentWeapon == WeaponType.MINIGUN && weaponHeat > 0f) {
            shape.color = Color(1f, 0.3f * (1f - weaponHeat), 0f, 0.9f)
            shape.rect(wx, wy, ww * weaponHeat, 5f)
        }
        if (switchBtnFlash > 0f) {
            shape.color = Color(1f, 1f, 1f, switchBtnFlash * 0.3f); shape.rect(wx, wy, ww, wh)
        }
        // زر التبديل
        shape.color = Color(0.2f, 0.2f, 0.28f, 0.85f); shape.circle(SWITCH_BTN_X, SWITCH_BTN_Y, SWITCH_BTN_R)
        shape.color = Color(wCol.r, wCol.g, wCol.b, 0.6f); shape.circle(SWITCH_BTN_X, SWITCH_BTN_Y, SWITCH_BTN_R - 8f)
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2f)
        shape.color = Color(wCol.r, wCol.g, wCol.b, 0.7f); shape.circle(SWITCH_BTN_X, SWITCH_BTN_Y, SWITCH_BTN_R)
        Gdx.gl.glLineWidth(1f); shape.end()

        game.batch.begin()
        val weaponName = when (currentWeapon) { WeaponType.PISTOL -> "مسدس"; WeaponType.SHOTGUN -> "شوتغان"; WeaponType.MINIGUN -> "مينيغان"; WeaponType.MELEE -> "مشرط" }
        ArabicText.draw(game.batch, weaponName, wx + 8f, wy + wh - 4f, 22f, wCol.r, wCol.g, wCol.b, 1f)
        val ammoTxt = when (currentWeapon) { WeaponType.PISTOL -> "∞"; WeaponType.SHOTGUN -> "$shotgunAmmo"; WeaponType.MINIGUN -> "$minigunAmmo"; WeaponType.MELEE -> "-" }
        ArabicText.draw(game.batch, ammoTxt, wx + 8f, wy + wh - 28f, 20f, 1f, 1f, 1f, 1f)
        if (shotgunAmmo > 0 && currentWeapon != WeaponType.SHOTGUN)
            ArabicText.draw(game.batch, "SHG:$shotgunAmmo", wx + 8f, wy + 20f, 16f, 1f, 0.4f, 0.1f, 0.7f)
        if (minigunAmmo > 0 && currentWeapon != WeaponType.MINIGUN)
            ArabicText.draw(game.batch, "MG:$minigunAmmo", wx + 80f, wy + 20f, 16f, 1f, 0.85f, 0.05f, 0.7f)
        game.batch.end()
    }

    private fun drawXPBar() {
        val bx = 12f; val by = 58f; val bw = WORLD_W * 0.22f; val bh = 9f
        val xpPct = if (xpToNext > 0) (playerXP.toFloat() / xpToNext).coerceIn(0f, 1f) else 0f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.04f, 0.05f, 0.14f, 0.88f); shape.rect(bx - 2f, by - 2f, bw + 4f, bh + 4f)
        shape.color = Color(0.06f, 0.06f, 0.18f, 1f); shape.rect(bx, by, bw, bh)
        shape.color = Color(0.45f, 0.3f, 1f, 1f); shape.rect(bx, by, bw * xpPct, bh)
        shape.color = Color(1f, 1f, 1f, 0.22f); shape.rect(bx, by + 6f, bw * xpPct, 3f)
        shape.end()
        game.batch.begin()
        ArabicText.draw(game.batch, "LV$playerLevel  XP$playerXP/$xpToNext", bx, by + 24f, 18f, 0.7f, 0.6f, 1f, 0.9f)
        game.batch.end()
    }

    private fun drawGrenadeButton() {
        val hasGren = grenadeCount > 0
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.7f); shape.circle(GREN_BTN_X, GREN_BTN_Y, GREN_BTN_R + 4f)
        shape.color = if (hasGren) Color(0.08f, 0.55f, 0.25f, 0.9f) else Color(0.15f, 0.15f, 0.15f, 0.7f)
        shape.circle(GREN_BTN_X, GREN_BTN_Y, GREN_BTN_R)
        if (hasGren) {
            shape.color = Color(0.8f, 0.6f, 0.1f, 0.9f); shape.circle(GREN_BTN_X, GREN_BTN_Y, GREN_BTN_R - 8f)
            shape.color = Color(0.3f, 0.25f, 0.05f, 1f); shape.rect(GREN_BTN_X - 3f, GREN_BTN_Y + GREN_BTN_R - 14f, 6f, 9f)
        }
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2f)
        shape.color = if (hasGren) Color(0.2f, 1f, 0.5f, 0.8f) else Color(0.3f, 0.3f, 0.3f, 0.5f)
        shape.circle(GREN_BTN_X, GREN_BTN_Y, GREN_BTN_R)
        Gdx.gl.glLineWidth(1f); shape.end()
        if (grenadeCount > 0) {
            game.batch.begin()
            ArabicText.drawCenter(game.batch, "x$grenadeCount", GREN_BTN_X, GREN_BTN_Y - GREN_BTN_R - 8f, 20f, 0.2f, 1f, 0.5f, 1f)
            game.batch.end()
        }
    }

    private fun drawDashButton() {
        val ready = dashCooldown <= 0f
        val prog  = if (dashCooldown > 0f) 1f - (dashCooldown / DASH_CD) else 1f
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.65f); shape.circle(DASH_BTN_X, DASH_BTN_Y, DASH_BTN_R + 4f)
        shape.color = if (ready) Color(0.10f, 0.50f, 0.90f, 0.88f) else Color(0.12f, 0.12f, 0.18f, 0.70f)
        shape.circle(DASH_BTN_X, DASH_BTN_Y, DASH_BTN_R)
        if (dashBtnFlash > 0f) {
            shape.color = Color(1f, 1f, 1f, dashBtnFlash * 0.40f); shape.circle(DASH_BTN_X, DASH_BTN_Y, DASH_BTN_R)
        }
        shape.end()
        // قوس التعبئة
        if (!ready) {
            shape.begin(ShapeRenderer.ShapeType.Line)
            Gdx.gl.glLineWidth(3f)
            shape.color = Color(0.4f, 0.75f, 1f, 0.85f)
            shape.arc(DASH_BTN_X, DASH_BTN_Y, DASH_BTN_R + 1f, 90f, prog * 360f)
            Gdx.gl.glLineWidth(1f)
            shape.end()
        } else {
            shape.begin(ShapeRenderer.ShapeType.Line)
            Gdx.gl.glLineWidth(2f)
            shape.color = Color(0.5f, 0.85f, 1f, 0.70f); shape.circle(DASH_BTN_X, DASH_BTN_Y, DASH_BTN_R)
            Gdx.gl.glLineWidth(1f)
            shape.end()
        }
        // أيقونة سهم مزدوج داخل الزر
        shape.begin(ShapeRenderer.ShapeType.Filled)
        val a  = if (ready) Color(1f, 1f, 1f, 0.95f) else Color(0.4f, 0.5f, 0.6f, 0.6f)
        shape.color = a
        shape.triangle(DASH_BTN_X + 12f, DASH_BTN_Y, DASH_BTN_X + 2f, DASH_BTN_Y + 10f, DASH_BTN_X + 2f, DASH_BTN_Y - 10f)
        shape.triangle(DASH_BTN_X - 12f, DASH_BTN_Y, DASH_BTN_X - 2f, DASH_BTN_Y + 10f, DASH_BTN_X - 2f, DASH_BTN_Y - 10f)
        shape.end()
        game.batch.begin()
        ArabicText.drawCenter(game.batch, "داش", DASH_BTN_X, DASH_BTN_Y - DASH_BTN_R - 8f, 18f,
            0.4f, 0.78f, 1f, if (ready) 1f else 0.5f)
        game.batch.end()
    }

    private fun drawJoystick() {
        if (!joyActive || joyAppearAlpha <= 0.01f) return
        val a = joyAppearAlpha

        // ── الحلقة الخارجية (Base Ring) ─────────────────────────────
        // طبقات متعددة لمظهر زجاجي لامع
        shape.begin(ShapeRenderer.ShapeType.Filled)

        // توهج خارجي كبير
        shape.color = Color(0.15f, 0.60f, 1f, 0.08f * a)
        shape.circle(joyCenter.x, joyCenter.y, 72f)

        // خلفية شبه شفافة (زجاجية)
        shape.color = Color(0.05f, 0.10f, 0.25f, 0.45f * a)
        shape.circle(joyCenter.x, joyCenter.y, 58f)

        // طبقة لمعة داخلية
        shape.color = Color(0.20f, 0.55f, 1f, 0.12f * a)
        shape.circle(joyCenter.x, joyCenter.y, 52f)

        shape.end()

        // حلقات الإطار
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2.5f)
        shape.color = Color(0.35f, 0.75f, 1f, 0.70f * a)
        shape.circle(joyCenter.x, joyCenter.y, 58f)
        Gdx.gl.glLineWidth(1f)
        shape.color = Color(0.50f, 0.85f, 1f, 0.30f * a)
        shape.circle(joyCenter.x, joyCenter.y, 45f)
        Gdx.gl.glLineWidth(1f)
        shape.end()

        // نقاط الاتجاهات الأربعة
        shape.begin(ShapeRenderer.ShapeType.Filled)
        for (d in 0 until 4) {
            val da = Math.toRadians(d * 90.0 - 45.0)
            val dx = cos(da).toFloat() * 50f; val dy = sin(da).toFloat() * 50f
            shape.color = Color(0.4f, 0.78f, 1f, 0.25f * a)
            shape.circle(joyCenter.x + dx, joyCenter.y + dy, 4f)
        }
        // خطوط اتجاه (+)
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(1.5f)
        for (d in 0 until 4) {
            val da = Math.toRadians(d * 90.0)
            shape.color = Color(0.45f, 0.80f, 1f, 0.22f * a)
            shape.line(
                joyCenter.x + cos(da).toFloat() * 18f,
                joyCenter.y + sin(da).toFloat() * 18f,
                joyCenter.x + cos(da).toFloat() * 52f,
                joyCenter.y + sin(da).toFloat() * 52f
            )
        }
        Gdx.gl.glLineWidth(1f)
        shape.end()

        // ── الإبهام (Thumb) — الكرة المتحركة ───────────────────────
        val raw = Vector2(joyCurrent).sub(joyCenter)
        val rawLen = raw.len().coerceAtMost(52f)
        val thumbDir = if (raw.len() > 1f) raw.nor() else Vector2(0f, 0f)
        val thumbX = joyCenter.x + thumbDir.x * rawLen
        val thumbY = joyCenter.y + thumbDir.y * rawLen

        // حساب نسبة الانحراف
        val pullRatio = (rawLen / 52f).coerceIn(0f, 1f)

        shape.begin(ShapeRenderer.ShapeType.Filled)

        // ظل الإبهام
        shape.color = Color(0f, 0f, 0f, 0.30f * a)
        shape.circle(thumbX + 2f, thumbY - 2f, 26f)

        // توهج خارجي الإبهام (يشتعل عند الضغط الكامل)
        val glowR = 0.2f + pullRatio * 0.8f
        val glowG = 0.65f - pullRatio * 0.3f
        shape.color = Color(glowR, glowG, 1f, (0.18f + pullRatio * 0.25f) * a)
        shape.circle(thumbX, thumbY, 30f)

        // جسم الإبهام الزجاجي
        shape.color = Color(0.15f + pullRatio * 0.3f, 0.55f, 1f, 0.78f * a)
        shape.circle(thumbX, thumbY, 22f)

        // طبقة وسطى أفتح
        shape.color = Color(0.35f + pullRatio * 0.4f, 0.72f, 1f, 0.65f * a)
        shape.circle(thumbX, thumbY, 16f)

        // نقطة مركزية لامعة
        shape.color = Color(0.85f, 0.95f, 1f, 0.90f * a)
        shape.circle(thumbX, thumbY, 7f)

        // لمعة علوية (highlight)
        shape.color = Color(1f, 1f, 1f, 0.55f * a)
        shape.circle(thumbX - 5f, thumbY + 7f, 5f)

        // خط بين المركز والإبهام (يُظهر الشد)
        if (pullRatio > 0.05f) {
            shape.color = Color(0.4f, 0.80f, 1f, 0.20f * a * pullRatio)
            shape.circle(joyCenter.x, joyCenter.y, 5f)
        }
        shape.end()

        // حلقة الإبهام
        shape.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2f)
        shape.color = Color(0.55f + pullRatio * 0.4f, 0.88f, 1f, 0.85f * a)
        shape.circle(thumbX, thumbY, 22f)
        Gdx.gl.glLineWidth(1f)
        shape.end()
    }

    private fun drawMinimap() {
        val mx = WORLD_W - 96f; val my = WORLD_H - 112f; val mw = 86f; val mh = 66f
        val sx = mw / WORLD_W; val sy = mh / WORLD_H
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.62f); shape.rect(mx - 3f, my - 3f, mw + 6f, mh + 6f)
        shape.color = Color(0.08f, 0.09f, 0.12f, 0.92f); shape.rect(mx, my, mw, mh)
        obstacles.forEach { o -> shape.color = Color(0.35f, 0.32f, 0.28f, 0.7f); shape.rect(mx + o.x * sx, my + o.y * sy, o.w * sx, o.h * sy) }
        enemies.filter { it.hp > 0 && it.deathTimer < 0f }.forEach { e ->
            shape.color = when (e.type) {
                EnemyType.BOSS_QUEEN -> Color(0.9f, 0.1f, 0.9f, 1f)
                EnemyType.HEAVY      -> Color(1f, 0.5f, 0.1f, 1f)
                EnemyType.SNIPER     -> Color(0.1f, 0.7f, 1f, 1f)
                else                 -> Color(0.9f, 0.1f, 0.1f, 1f)
            }
            shape.circle(mx + e.pos.x * sx, my + e.pos.y * sy, if (e.type == EnemyType.BOSS_QUEEN) 5.5f else 3.5f)
        }
        pickups.filter { it.alive }.forEach { p ->
            shape.color = Color(0.1f, 1f, 0.3f, 0.8f); shape.circle(mx + p.pos.x * sx, my + p.pos.y * sy, 2.5f)
        }
        val ppx = mx + playerPos.x * sx; val ppy = my + playerPos.y * sy
        shape.color = Color(0.3f, 0.6f, 1f, 1f); shape.circle(ppx, ppy, 4.5f)
        shape.color = Color.WHITE; shape.circle(ppx, ppy, 2f)
        shape.end()
        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.color = Color(0.42f, 0.42f, 0.58f, 0.6f); shape.rect(mx, my, mw, mh)
        shape.end()
    }

    // ══════════════════════════════════════════════════════════════════
    //  Restart
    // ══════════════════════════════════════════════════════════════════
    private fun restartGame() {
        playerPos.set(WORLD_W / 2f, WORLD_H / 2f)
        playerHP = MAX_HP; playerShield = 0; playerAlive = true
        gameOver = false; playerWon = false
        score = 0; wave = 1; killCount = 0
        playerXP = 0; playerLevel = 1; xpToNext = 200
        combo = 0; comboTimer = 0f; comboFlash = 0f; streakKills = 0
        levelUpFlash = 0f; damageFlash = 0f; bloodOverlay = 0f
        currentWeapon = WeaponType.PISTOL; shotgunAmmo = 0; minigunAmmo = 0
        grenadeCount = 0; weaponHeat = 0f
        fireTimer = 0f; dashCooldown = 0f; isDashing = false; shieldRechargeTimer = 0f
        isMuzzleFlash = false; muzzleTimer = 0f; deathSlowActive = false
        // أنيميشن
        legPhase = 0f; bodyBob = 0f; aimRecoil = 0f; breathCycle = 0f; dashBtnFlash = 0f
        joyAppearAlpha = 0f; idleTimer = 0f; autoMoveDir.set(0f, 0f); autoMoveTimer = 0f
        bullets.clear(); particles.clear(); dmgNumbers.clear(); grenades.clear()
        spawnWave()
    }

    private fun cos(a: Double) = Math.cos(a)
    private fun sin(a: Double) = Math.sin(a)

    override fun resize(w: Int, h: Int) { viewport.update(w, h, true) }
    override fun pause()   {}
    override fun resume()  {}
    override fun hide()    {}
    override fun dispose() {
        shape.dispose()
        fontNum.dispose()
        SoundManager.dispose()
    }
}

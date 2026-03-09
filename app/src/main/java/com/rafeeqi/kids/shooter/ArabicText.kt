package com.rafeeqi.kids.shooter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * ArabicText — يحل مشكلة ظهور الرموز بدلاً من العربية
 * يستخدم Android Canvas لرسم النص العربي بشكل صحيح RTL
 * ثم يحوله إلى Texture تقدر libGDX تعرضه
 *
 * استخدام:
 *   ArabicText.draw(batch, "مرحبا", x, y, height=32f, r, g, b, a)
 *   ArabicText.drawCenter(batch, "مرحبا", centerX, y, height=32f, r, g, b, a)
 */
object ArabicText {

    private data class CacheKey(val text: String, val size: Int)
    private data class Entry(val tex: Texture, val w: Float, val h: Float)

    private val cache   = HashMap<CacheKey, Entry>()
    private var typeface: Typeface = Typeface.DEFAULT

    // ── Init ──────────────────────────────────────────────────────
    fun init() {
        typeface = try {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    // ── Public draw methods ───────────────────────────────────────

    /** رسم نص يسار → يمين، القمة عند (x, y) */
    fun draw(
        batch: SpriteBatch, text: String,
        x: Float, y: Float,
        height: Float = 28f,
        r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f
    ) {
        if (text.isBlank()) return
        val e = get(text, height)
        batch.setColor(r, g, b, a)
        batch.draw(e.tex, x, y - e.h * 0.8f, e.w, e.h)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /** رسم نص في المنتصف أفقياً، القمة عند (cx, y) */
    fun drawCenter(
        batch: SpriteBatch, text: String,
        cx: Float, y: Float,
        height: Float = 28f,
        r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f
    ) {
        if (text.isBlank()) return
        val e = get(text, height)
        batch.setColor(r, g, b, a)
        batch.draw(e.tex, cx - e.w * 0.5f, y - e.h * 0.8f, e.w, e.h)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /** عرض النص (لحساب المواضع) */
    fun width(text: String, height: Float = 28f): Float =
        if (text.isBlank()) 0f else get(text, height).w

    // ── Internal ──────────────────────────────────────────────────
    private fun get(text: String, height: Float): Entry {
        val key = CacheKey(text, height.toInt().coerceIn(8, 400))
        return cache.getOrPut(key) { bake(text, height) }
    }

    private fun bake(text: String, targetH: Float): Entry {
        val renderSz = (targetH * 2.8f).coerceAtLeast(18f)

        val paint = Paint().apply {
            isAntiAlias = true
            textSize    = renderSz
            color       = android.graphics.Color.WHITE
            typeface    = this@ArabicText.typeface
            textAlign   = Paint.Align.LEFT
        }

        val fm = paint.fontMetrics
        val bmpH = (-fm.ascent + fm.descent + 6f).toInt().coerceAtLeast(8)
        val bmpW = (paint.measureText(text) + renderSz * 0.4f).toInt().coerceAtLeast(8)

        val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.TRANSPARENT)
        Canvas(bmp).drawText(text, renderSz * 0.2f, -fm.ascent + 3f, paint)

        // Android Bitmap → libGDX Pixmap
        val pm = Pixmap(bmpW, bmpH, Pixmap.Format.RGBA8888)
        val px = IntArray(bmpW * bmpH)
        bmp.getPixels(px, 0, bmpW, 0, 0, bmpW, bmpH)
        for (i in px.indices) {
            val p  = px[i]
            val aa = (p ushr 24) and 0xFF
            val rr = (p shr  16) and 0xFF
            val gg = (p shr   8) and 0xFF
            val bb = p and 0xFF
            pm.drawPixel(i % bmpW, i / bmpW,
                (rr shl 24) or (gg shl 16) or (bb shl 8) or aa)
        }
        bmp.recycle()

        val tex = Texture(pm)
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pm.dispose()

        val scale = targetH / bmpH
        return Entry(tex, bmpW * scale, targetH)
    }

    fun dispose() {
        cache.values.forEach { it.tex.dispose() }
        cache.clear()
    }
}

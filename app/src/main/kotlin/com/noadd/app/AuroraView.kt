package com.noadd.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

/**
 * Fundo do app: gradiente vertical azul-marinho com dois brilhos radiais
 * (azul e ciano) que dão a profundidade necessária para o vidro por cima.
 * Tudo desenhado com shaders — nenhuma imagem no APK.
 */
class AuroraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val topColor = color(R.attr.noAddCanvasTop, Color.BLACK)
    private val bottomColor = color(R.attr.noAddCanvasBottom, Color.BLACK)
    private val glowPrimary = color(R.attr.noAddGlowPrimary, Color.TRANSPARENT)
    private val glowSecondary = color(R.attr.noAddGlowSecondary, Color.TRANSPARENT)

    private var base: Shader? = null
    private var glowNear: Shader? = null
    private var glowFar: Shader? = null

    private fun color(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) value.data else fallback
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        base = LinearGradient(0f, 0f, 0f, h.toFloat(), topColor, bottomColor, Shader.TileMode.CLAMP)
        glowNear = RadialGradient(
            w * 0.12f,
            h * 0.06f,
            w * 1.05f,
            glowPrimary,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        glowFar = RadialGradient(
            w * 0.96f,
            h * 0.48f,
            w * 0.95f,
            glowSecondary,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.shader = base
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = glowNear
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = glowFar
        canvas.drawRect(0f, 0f, w, h, paint)
    }
}

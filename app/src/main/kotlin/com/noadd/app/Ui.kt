package com.noadd.app

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets

/** Ajustes de janela, tema e barras do sistema, sem AndroidX. */
object Ui {

    fun themeRes(mode: Int): Int = when (mode) {
        Prefs.THEME_LIGHT -> R.style.Theme_NoAdd_Light
        Prefs.THEME_DARK -> R.style.Theme_NoAdd_Dark
        else -> R.style.Theme_NoAdd
    }

    /** Deixa o aurora passar atrás das barras a partir do Android 11. */
    fun prepareWindow(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val window = activity.window
        window.setDecorFitsSystemWindows(false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    /**
     * Com a janela edge-to-edge, o padding do conteúdo recebe a área segura
     * do sistema (barras e recorte de tela).
     */
    @Suppress("DEPRECATION")
    fun applySystemBarsPadding(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val start = view.paddingStart
        val top = view.paddingTop
        val end = view.paddingEnd
        val bottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            v.setPadding(start + bars.left, top + bars.top, end + bars.right, bottom + bars.bottom)
            insets
        }
        view.requestApplyInsets()
    }

    /**
     * Como [applySystemBarsPadding], mas com o mesmo respiro em cima e embaixo:
     * assim o conteúdo centralizado fica no centro exato da tela, mesmo com a
     * barra de status mais alta que a barra de navegação.
     */
    @Suppress("DEPRECATION")
    fun applySymmetricSystemBarsPadding(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val start = view.paddingStart
        val top = view.paddingTop
        val end = view.paddingEnd
        val bottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val vertical = maxOf(bars.top, bars.bottom)
            val horizontal = maxOf(bars.left, bars.right)
            v.setPadding(start + horizontal, top + vertical, end + horizontal, bottom + vertical)
            insets
        }
        view.requestApplyInsets()
    }
}

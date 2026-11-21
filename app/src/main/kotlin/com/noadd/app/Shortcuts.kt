package com.noadd.app

import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast

/** Atalho fixo na tela inicial para um número + apelido. */
object Shortcuts {

    fun requestPin(activity: Activity, e164: String, label: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(activity, R.string.toast_shortcut_unsupported, Toast.LENGTH_SHORT).show()
            return
        }
        val manager = activity.getSystemService(ShortcutManager::class.java)
        if (manager == null || !manager.isRequestPinShortcutSupported) {
            Toast.makeText(activity, R.string.toast_shortcut_unsupported, Toast.LENGTH_SHORT).show()
            return
        }
        val id = "num_$e164"
        if (manager.pinnedShortcuts.any { it.id == id }) {
            Toast.makeText(activity, R.string.toast_shortcut_exists, Toast.LENGTH_SHORT).show()
            return
        }
        val name = label.trim().ifEmpty { Phone.pretty(e164) }
        val target = Intent(activity, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_NUMBER, e164)
            putExtra(MainActivity.EXTRA_OPEN, true)
        }
        val info = ShortcutInfo.Builder(activity, id)
            .setShortLabel(name)
            .setLongLabel(activity.getString(R.string.shortcut_chat_long, name))
            .setIcon(Icon.createWithResource(activity, R.drawable.ic_shortcut))
            .setIntent(target)
            .build()
        val requested = manager.requestPinShortcut(info, null)
        if (requested) {
            Toast.makeText(activity, R.string.toast_shortcut_requested, Toast.LENGTH_SHORT).show()
        }
    }
}

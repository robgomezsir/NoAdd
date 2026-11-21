package com.noadd.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

/** Monta o link de conversa e conversa com o sistema operacional. */
object ChatIntents {

    const val PKG_WA = "com.whatsapp"
    const val PKG_W4B = "com.whatsapp.w4b"

    fun link(e164: String, message: String): String {
        val base = "https://wa.me/$e164"
        if (message.isBlank()) return base
        val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        return "$base?text=$encoded"
    }

    fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: Exception) {
        false
    }

    fun installedChats(context: Context): List<String> =
        listOf(PKG_WA, PKG_W4B).filter { isInstalled(context, it) }

    fun view(link: String, pkg: String?): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    fun share(text: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
    }
}

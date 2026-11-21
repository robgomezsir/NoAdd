package com.noadd.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Um número salvo na agenda. */
data class Entry(val number: String, val label: String, val pinned: Boolean)

/**
 * Tudo fica em SharedPreferences neste aparelho. Nenhuma outra camada de
 * armazenamento, nenhuma rede.
 */
object Prefs {

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private const val FILE = "noadd"

    private fun sp(c: Context): SharedPreferences = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun theme(c: Context): Int = sp(c).getInt("theme", THEME_SYSTEM)
    fun setTheme(c: Context, value: Int) {
        sp(c).edit().putInt("theme", value).apply()
    }

    fun countryIso(c: Context): String = sp(c).getString("country", "BR") ?: "BR"
    fun setCountryIso(c: Context, iso: String) {
        sp(c).edit().putString("country", iso).apply()
    }

    fun number(c: Context): String = sp(c).getString("number", "") ?: ""
    fun setNumber(c: Context, value: String) {
        sp(c).edit().putString("number", value).apply()
    }

    fun message(c: Context): String = sp(c).getString("message", "") ?: ""
    fun setMessage(c: Context, value: String) {
        sp(c).edit().putString("message", value).apply()
    }

    fun keepPrefix(c: Context): Boolean = sp(c).getBoolean("keep_prefix", true)
    fun setKeepPrefix(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("keep_prefix", on).apply()
    }

    fun saveRecents(c: Context): Boolean = sp(c).getBoolean("save_recents", true)
    fun setSaveRecents(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("save_recents", on).apply()
    }

    fun autoOpen(c: Context): Boolean = sp(c).getBoolean("auto_open", false)
    fun setAutoOpen(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("auto_open", on).apply()
    }

    fun telHandler(c: Context): Boolean = sp(c).getBoolean("tel_handler", false)
    fun setTelHandler(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("tel_handler", on).apply()
    }

    fun limit(c: Context): Int = sp(c).getInt("limit", 30).coerceIn(1, 500)
    fun setLimit(c: Context, value: Int) {
        sp(c).edit().putInt("limit", value.coerceIn(1, 500)).apply()
    }

    fun entries(c: Context): List<Entry> {
        val raw = sp(c).getString("entries", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<Entry>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val number = obj.optString("n")
                if (number.isNotEmpty()) {
                    list.add(Entry(number, obj.optString("l"), obj.optBoolean("p")))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(c: Context, list: List<Entry>) {
        val array = JSONArray()
        for (entry in list) {
            array.put(
                JSONObject()
                    .put("n", entry.number)
                    .put("l", entry.label)
                    .put("p", entry.pinned),
            )
        }
        sp(c).edit().putString("entries", array.toString()).apply()
    }

    /** Adiciona no topo, respeitando o limite de recentes (fixados não contam). */
    fun add(c: Context, number: String, label: String = "", pinned: Boolean = false) {
        if (number.isEmpty()) return
        if (!pinned && !saveRecents(c)) return
        val list = entries(c).toMutableList()
        // Um número fixado não precisa aparecer de novo nos recentes.
        if (list.any { it.number == number && it.pinned }) return
        list.removeAll { it.number == number && !it.pinned }
        list.add(0, Entry(number, label, pinned))
        val max = limit(c)
        var recents = 0
        val kept = list.filter { entry ->
            if (entry.pinned) {
                true
            } else {
                recents++
                recents <= max
            }
        }
        save(c, kept)
    }

    fun pin(c: Context, number: String, label: String) {
        val list = entries(c).toMutableList()
        list.removeAll { it.number == number }
        list.add(0, Entry(number, label, true))
        save(c, list)
    }

    fun unpin(c: Context, number: String) {
        val list = entries(c).map {
            if (it.number == number) Entry(it.number, it.label, false) else it
        }
        save(c, list)
    }

    fun remove(c: Context, number: String) {
        save(c, entries(c).filter { it.number != number })
    }

    fun clear(c: Context) {
        save(c, emptyList())
    }
}

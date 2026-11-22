package com.noadd.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistência do lote em SharedPreferences + JSON, no mesmo espírito do Prefs:
 * arquivo privado, sem banco, sem rede e sem backup.
 */
object BatchStore {

    private const val FILE = "noadd_batch"

    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ----- Contatos -----

    fun contacts(c: Context): List<BatchContact> {
        val raw = sp(c).getString("contacts", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<BatchContact>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val phone = obj.optString("p")
                if (phone.isEmpty()) continue
                list.add(
                    BatchContact(
                        id = obj.optString("id").ifEmpty { java.util.UUID.randomUUID().toString() },
                        name = obj.optString("n"),
                        phone = phone,
                        surname = obj.optString("sn"),
                        status = statusOf(obj.optString("s")),
                    ),
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveContacts(c: Context, list: List<BatchContact>) {
        val array = JSONArray()
        for (contact in list) {
            array.put(
                JSONObject()
                    .put("id", contact.id)
                    .put("n", contact.name)
                    .put("sn", contact.surname)
                    .put("p", contact.phone)
                    .put("s", contact.status.name),
            )
        }
        sp(c).edit().putString("contacts", array.toString()).apply()
    }

    private fun statusOf(value: String): BatchStatus = try {
        BatchStatus.valueOf(value)
    } catch (e: Exception) {
        BatchStatus.PENDING
    }

    // ----- Para revisar -----

    fun review(c: Context): List<ReviewContact> {
        val raw = sp(c).getString("review", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<ReviewContact>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val value = obj.optString("r")
                if (value.isEmpty()) continue
                list.add(
                    ReviewContact(
                        id = obj.optString("id").ifEmpty { java.util.UUID.randomUUID().toString() },
                        raw = value,
                        reason = obj.optString("reason", Batch.REASON_INVALID),
                    ),
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReview(c: Context, list: List<ReviewContact>) {
        val array = JSONArray()
        for (item in list) {
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("r", item.raw)
                    .put("reason", item.reason),
            )
        }
        sp(c).edit().putString("review", array.toString()).apply()
    }

    // ----- Mensagem e modelos -----

    fun message(c: Context): String = sp(c).getString("message", "") ?: ""
    fun setMessage(c: Context, value: String) {
        sp(c).edit().putString("message", value).apply()
    }

    /**
     * true depois que a saudação padrão foi oferecida. Sem isso o app não
     * distingue "nunca abriu" de "o usuário apagou a mensagem" e reinseriria
     * o texto padrão em cima da escolha dele.
     */
    fun messageSeeded(c: Context): Boolean = sp(c).getBoolean("message_seeded", false)
    fun setMessageSeeded(c: Context, value: Boolean) {
        sp(c).edit().putBoolean("message_seeded", value).apply()
    }

    // ----- Dados do treinamento -----

    fun training(c: Context): TrainingRow? {
        val raw = sp(c).getString("training", null) ?: return null
        return try {
            val obj = JSONObject(raw)
            TrainingRow(
                product = obj.optString("pr"),
                role = obj.optString("ca"),
                address = obj.optString("en"),
                time = obj.optString("ho"),
                date = obj.optString("da"),
                period = obj.optString("pe"),
                instructor = obj.optString("in"),
            )
        } catch (e: Exception) {
            null
        }
    }

    fun setTraining(c: Context, row: TrainingRow?) {
        if (row == null) {
            sp(c).edit().remove("training").apply()
            return
        }
        val obj = JSONObject()
            .put("pr", row.product)
            .put("ca", row.role)
            .put("en", row.address)
            .put("ho", row.time)
            .put("da", row.date)
            .put("pe", row.period)
            .put("in", row.instructor)
        sp(c).edit().putString("training", obj.toString()).apply()
    }

    /** Texto do modelo do RH, guardado à parte para aparecer como "Modelo de treinamento". */
    fun trainingTemplate(c: Context): String = sp(c).getString("training_template", "") ?: ""
    fun setTrainingTemplate(c: Context, value: String) {
        sp(c).edit().putString("training_template", value).apply()
    }

    fun templates(c: Context): List<String> {
        val raw = sp(c).getString("templates", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = ArrayList<String>(array.length())
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) list.add(value)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTemplates(c: Context, list: List<String>) {
        val array = JSONArray()
        for (value in list) array.put(value)
        sp(c).edit().putString("templates", array.toString()).apply()
    }

    // ----- Configurações do lote -----

    /** "" = ainda não escolhido, "default" = padrão do sistema, senão o pacote. */
    fun waPackage(c: Context): String = sp(c).getString("wa_package", "") ?: ""

    fun setWaPackage(c: Context, value: String) {
        sp(c).edit().putString("wa_package", value).apply()
    }

    fun autoAdvance(c: Context): Boolean = sp(c).getBoolean("auto_advance", false)
    fun setAutoAdvance(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("auto_advance", on).apply()
    }

    fun sessionLimit(c: Context): Int = sp(c).getInt("session_limit", 50).coerceIn(1, 500)
    fun setSessionLimit(c: Context, value: Int) {
        sp(c).edit().putInt("session_limit", value.coerceIn(1, 500)).apply()
    }

    /** 0 desliga o lembrete. */
    fun reminderEvery(c: Context): Int = sp(c).getInt("reminder_every", 0).coerceIn(0, 500)
    fun setReminderEvery(c: Context, value: Int) {
        sp(c).edit().putInt("reminder_every", value.coerceIn(0, 500)).apply()
    }

    fun disclaimerSeen(c: Context): Boolean = sp(c).getBoolean("disclaimer_seen", false)
    fun setDisclaimerSeen(c: Context, seen: Boolean) {
        sp(c).edit().putBoolean("disclaimer_seen", seen).apply()
    }

    // ----- Progresso -----

    fun sessionResolved(c: Context): Int = sp(c).getInt("session_resolved", 0).coerceAtLeast(0)
    fun setSessionResolved(c: Context, value: Int) {
        sp(c).edit().putInt("session_resolved", value.coerceAtLeast(0)).apply()
    }

    fun currentId(c: Context): String = sp(c).getString("current_id", "") ?: ""
    fun setCurrentId(c: Context, value: String) {
        sp(c).edit().putString("current_id", value).apply()
    }

    // ----- Limpeza -----

    /** Remove contatos, revisão e progresso; mantém mensagem, modelos e ajustes. */
    fun clearList(c: Context) {
        sp(c).edit()
            .remove("contacts")
            .remove("review")
            .remove("current_id")
            .remove("session_resolved")
            .remove("training")
            .apply()
    }

    /** Remove tudo do lote, inclusive mensagem, modelos, ajustes e aviso. */
    fun clearAll(c: Context) {
        sp(c).edit().clear().apply()
    }
}

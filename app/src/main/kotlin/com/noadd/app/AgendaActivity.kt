package com.noadd.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast

/** Recentes e fixados, com importação e exportação por texto. */
class AgendaActivity : Activity() {

    companion object {
        private const val REQ_SAVE = 10
    }

    private var items: List<Entry> = emptyList()
    private lateinit var adapter: EntryAdapter
    private var pendingExport = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Ui.themeRes(Prefs.theme(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)
        Ui.prepareWindow(this)
        Ui.applySystemBarsPadding(findViewById<View>(R.id.agendaContent))

        findViewById<View>(R.id.menuButton).setOnClickListener { showMenu() }

        val list = findViewById<ListView>(R.id.list)
        list.emptyView = findViewById<TextView>(R.id.empty)
        adapter = EntryAdapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            items.getOrNull(position)?.let { open(it) }
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            items.getOrNull(position)?.let { showActions(it) }
            true
        }
        reload()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    /** O menu vive no cabeçalho, ao lado do título. */
    private fun showMenu() {
        val popup = PopupMenu(this, findViewById<View>(R.id.menuButton))
        popup.menuInflater.inflate(R.menu.agenda, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_import -> {
                    askImport()
                    true
                }
                R.id.menu_export -> {
                    chooseExport()
                    true
                }
                R.id.menu_clear -> {
                    confirmClear()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_SAVE || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray()) }
            Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // ----- Lista -----

    private fun reload() {
        items = Prefs.entries(this).sortedByDescending { it.pinned }
        adapter.notifyDataSetChanged()
    }

    private fun open(entry: Entry) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(MainActivity.EXTRA_NUMBER, entry.number)
                .putExtra(MainActivity.EXTRA_LABEL, entry.label),
        )
        finish()
    }

    private fun showActions(entry: Entry) {
        val labels = arrayOf(
            getString(R.string.action_open_now),
            getString(if (entry.pinned) R.string.action_unpin else R.string.action_pin),
            getString(R.string.action_remove),
        )
        AlertDialog.Builder(this)
            .setTitle(if (entry.label.isBlank()) Phone.pretty(entry.number) else entry.label)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> openChatNow(entry)
                    1 -> if (entry.pinned) {
                        Prefs.unpin(this, entry.number)
                        reload()
                    } else {
                        askLabel(entry)
                    }
                    else -> {
                        Prefs.remove(this, entry.number)
                        reload()
                    }
                }
            }
            .show()
    }

    private fun openChatNow(entry: Entry) {
        try {
            startActivity(ChatIntents.view(ChatIntents.link(entry.number, ""), null))
            Prefs.add(this, entry.number, entry.label)
            reload()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.warning_no_app_title, Toast.LENGTH_SHORT).show()
        }
    }

    private fun askLabel(entry: Entry) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.pin_hint)
            setText(entry.label)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.pin_title)
            .setView(field(input))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Prefs.pin(this, entry.number, input.text.toString().trim())
                reload()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ----- Importar / exportar -----

    private fun askImport() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 5
            gravity = Gravity.TOP or Gravity.START
            hint = getString(R.string.import_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.import_title)
            .setView(field(input))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val count = importText(input.text.toString())
                reload()
                Toast.makeText(this, getString(R.string.toast_imported, count), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun importText(text: String): Int {
        val fallback = Countries.byIso(Prefs.countryIso(this)) ?: Countries.default
        var count = 0
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) continue
            var label = ""
            var raw = trimmed
            val cut = listOf(trimmed.indexOf(';'), trimmed.indexOf('\t')).filter { it > 0 }.minOrNull()
            if (cut != null) {
                label = trimmed.substring(0, cut).trim()
                raw = trimmed.substring(cut + 1).trim()
            }
            val e164 = Phone.normalize(raw, fallback)
            if (e164.length in 6..15) {
                Prefs.add(this, e164, label)
                count++
            }
        }
        return count
    }

    private fun chooseExport() {
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.toast_nothing_export, Toast.LENGTH_SHORT).show()
            return
        }
        val text = exportText()
        AlertDialog.Builder(this)
            .setTitle(R.string.export_title)
            .setItems(arrayOf(getString(R.string.export_share), getString(R.string.export_save))) { _, which ->
                if (which == 0) {
                    startActivity(Intent.createChooser(ChatIntents.share(text), getString(R.string.export_title)))
                } else {
                    saveFile(text)
                }
            }
            .show()
    }

    private fun exportText(): String {
        val builder = StringBuilder(getString(R.string.export_header)).append('\n')
        for (entry in items) {
            if (entry.label.isBlank()) {
                builder.append(entry.number)
            } else {
                builder.append(entry.label).append("; ").append(entry.number)
            }
            builder.append('\n')
        }
        return builder.toString()
    }

    private fun saveFile(text: String) {
        pendingExport = text
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "noadd-agenda.txt")
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_SAVE)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_title)
            .setMessage(R.string.clear_message)
            .setPositiveButton(R.string.action_clear_all) { _, _ ->
                Prefs.clear(this)
                reload()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ----- Utilidades -----

    private fun field(input: EditText): View {
        val box = FrameLayout(this)
        box.setPadding(dp(20), dp(8), dp(20), 0)
        box.addView(input)
        return box
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class EntryAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Entry = items[position]
        override fun getItemId(position: Int): Long = items[position].number.hashCode().toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_entry, parent, false)
            val entry = items[position]
            val number = Phone.pretty(entry.number)
            view.findViewById<TextView>(R.id.title).text = if (entry.label.isBlank()) number else entry.label
            val subtitle = when {
                entry.label.isBlank() && entry.pinned -> getString(R.string.agenda_pinned)
                entry.label.isBlank() -> ""
                entry.pinned -> "$number · ${getString(R.string.agenda_pinned)}"
                else -> number
            }
            val sub = view.findViewById<TextView>(R.id.subtitle)
            sub.text = subtitle
            sub.visibility = if (subtitle.isBlank()) View.GONE else View.VISIBLE
            return view
        }
    }
}

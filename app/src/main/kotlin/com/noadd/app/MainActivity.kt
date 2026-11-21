package com.noadd.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

/** Tela principal: país + número + mensagem, e as ações do dia a dia. */
class MainActivity : Activity() {

    companion object {
        const val EXTRA_NUMBER = "number"
        const val EXTRA_LABEL = "label"
        const val EXTRA_OPEN = "open"
        private const val REQ_AGENDA = 1
    }

    private lateinit var countryView: TextView
    private lateinit var phoneInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var hint: TextView
    private var country: Country = Countries.default
    private var lastLength = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Ui.themeRes(Prefs.theme(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Ui.prepareWindow(this)
        Ui.applySymmetricSystemBarsPadding(findViewById<View>(R.id.content))
        revealSheet()

        countryView = findViewById(R.id.countryView)
        phoneInput = findViewById(R.id.phoneInput)
        messageInput = findViewById(R.id.messageInput)
        hint = findViewById(R.id.hint)

        country = Countries.byIso(Prefs.countryIso(this)) ?: Countries.default
        renderCountry()
        setTelAlias(Prefs.telHandler(this))

        countryView.setOnClickListener { pickCountry() }
        phoneInput.setText(Prefs.number(this))
        messageInput.setText(Prefs.message(this))
        lastLength = phoneInput.text.length

        phoneInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                val pasted = abs(length - lastLength) > 4
                lastLength = length
                if (!pasted) return
                val e164 = Phone.normalize(s?.toString().orEmpty(), country)
                if (Prefs.autoOpen(this@MainActivity) && e164.isNotEmpty() && Phone.looksValid(e164)) {
                    hideHint()
                    openChat(e164)
                }
            }
        })

        findViewById<View>(R.id.menuButton).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.openButton).setOnClickListener { openFromForm() }
        findViewById<Button>(R.id.callButton).setOnClickListener { dialNumber() }
        findViewById<Button>(R.id.shareButton).setOnClickListener { shareLink() }
        findViewById<Button>(R.id.copyButton).setOnClickListener { copyLink() }
        findViewById<Button>(R.id.shortcutButton).setOnClickListener { createShortcut() }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        Prefs.setMessage(this, messageInput.text.toString())
        if (Prefs.keepPrefix(this)) {
            Prefs.setNumber(this, phoneInput.text.toString())
            Prefs.setCountryIso(this, country.iso)
        }
    }

    /** O menu vive dentro do painel de vidro, ancorado ao botão do topo. */
    private fun showMenu() {
        val popup = PopupMenu(this, findViewById<View>(R.id.menuButton))
        popup.menuInflater.inflate(R.menu.main, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_agenda -> {
                    @Suppress("DEPRECATION")
                    startActivityForResult(Intent(this, AgendaActivity::class.java), REQ_AGENDA)
                    true
                }
                R.id.menu_settings -> {
                    openSettings()
                    true
                }
                R.id.menu_about -> {
                    openAbout()
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
        if (requestCode != REQ_AGENDA || resultCode != RESULT_OK) return
        val number = data?.getStringExtra(EXTRA_NUMBER) ?: return
        val detected = Countries.match(number) ?: country
        country = detected
        Prefs.setCountryIso(this, detected.iso)
        renderCountry()
        phoneInput.setText(Phone.national(number, detected))
        hideHint()
    }

    // ----- Ações principais -----

    private fun openFromForm() {
        val e164 = currentE164() ?: return
        if (Phone.looksValid(e164)) hideHint() else showHint(R.string.warn_invalid)
        openChat(e164)
    }

    private fun openChat(e164: String) {
        val message = messageInput.text.toString().trim()
        val link = ChatIntents.link(e164, message)
        val chats = ChatIntents.installedChats(this)
        if (chats.isEmpty()) {
            askBrowser(link, e164)
            return
        }
        if (chats.size == 2) {
            val options = arrayOf(
                getString(R.string.whatsapp_name),
                getString(R.string.whatsapp_business_name),
                getString(R.string.chat_default),
            )
            AlertDialog.Builder(this)
                .setTitle(R.string.chooser_title)
                .setItems(options) { _, which ->
                    val pkg = when (which) {
                        0 -> ChatIntents.PKG_WA
                        1 -> ChatIntents.PKG_W4B
                        else -> null
                    }
                    launch(link, e164, pkg)
                }
                .show()
            return
        }
        launch(link, e164, null)
    }

    private fun launch(link: String, e164: String, pkg: String?) {
        try {
            startActivity(ChatIntents.view(link, pkg))
            rememberUse(e164)
        } catch (e: ActivityNotFoundException) {
            askBrowser(link, e164)
        }
    }

    private fun askBrowser(link: String, e164: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.warning_no_app_title)
            .setMessage(R.string.warning_no_app_message)
            .setPositiveButton(R.string.open_browser) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                    rememberUse(e164)
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.warning_no_app_title)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun rememberUse(e164: String) {
        Prefs.add(this, e164)
        if (Prefs.keepPrefix(this)) {
            Prefs.setNumber(this, phoneInput.text.toString())
            Prefs.setCountryIso(this, country.iso)
        } else {
            phoneInput.setText("")
            Prefs.setNumber(this, "")
            country = Countries.default
            Prefs.setCountryIso(this, country.iso)
            renderCountry()
        }
        Prefs.setMessage(this, messageInput.text.toString())
    }

    /** Deixa escolher entre chamar pelo WhatsApp ou pelo telefone. */
    private fun dialNumber() {
        val e164 = currentE164() ?: return
        val options = arrayOf(getString(R.string.whatsapp_name), getString(R.string.call_phone))
        AlertDialog.Builder(this)
            .setTitle(R.string.call_title)
            .setItems(options) { _, which ->
                if (which == 0) callOnWhatsApp(e164) else callOnPhone(e164)
            }
            .show()
    }

    /** WhatsApp não expõe chamada por intent: abre a conversa, onde fica o botão de ligar. */
    private fun callOnWhatsApp(e164: String) {
        val link = ChatIntents.link(e164, "")
        val chats = ChatIntents.installedChats(this)
        if (chats.isEmpty()) {
            askBrowser(link, e164)
            return
        }
        if (chats.size == 2) {
            val options = arrayOf(
                getString(R.string.whatsapp_name),
                getString(R.string.whatsapp_business_name),
                getString(R.string.chat_default),
            )
            AlertDialog.Builder(this)
                .setTitle(R.string.chooser_title)
                .setItems(options) { _, which ->
                    val pkg = when (which) {
                        0 -> ChatIntents.PKG_WA
                        1 -> ChatIntents.PKG_W4B
                        else -> null
                    }
                    openWhatsAppCall(link, e164, pkg)
                }
                .show()
            return
        }
        openWhatsAppCall(link, e164, null)
    }

    private fun openWhatsAppCall(link: String, e164: String, pkg: String?) {
        try {
            startActivity(ChatIntents.view(link, pkg))
            toast(R.string.call_whatsapp_hint)
        } catch (e: ActivityNotFoundException) {
            askBrowser(link, e164)
        }
    }

    /** Abre o discador do sistema com o número; discar não exige permissão. */
    private fun callOnPhone(e164: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$e164")))
        } catch (e: ActivityNotFoundException) {
            toast(R.string.toast_no_dialer)
        }
    }

    private fun shareLink() {
        val e164 = currentE164() ?: return
        val link = ChatIntents.link(e164, messageInput.text.toString().trim())
        startActivity(Intent.createChooser(ChatIntents.share(link), getString(R.string.action_share)))
    }

    private fun copyLink() {
        val e164 = currentE164() ?: return
        ChatIntents.copy(this, ChatIntents.link(e164, messageInput.text.toString().trim()))
    }

    private fun createShortcut() {
        val e164 = currentE164() ?: return
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.shortcut_hint)
        }
        val box = FrameLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.shortcut_title)
            .setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Shortcuts.requestPin(this, e164, input.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ----- País -----

    private fun renderCountry() {
        countryView.text = "${country.flag}  ${country.name}  +${country.dial}"
        countryView.contentDescription = getString(R.string.country_desc, country.name, country.dial)
    }

    private fun pickCountry() {
        val search = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = getString(R.string.country_search_hint)
            setSingleLine()
        }
        val list = ListView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
            addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(340)))
        }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        list.adapter = adapter
        var shown: List<Country> = emptyList()

        fun refresh(query: String) {
            shown = Countries.search(query)
            adapter.clear()
            adapter.addAll(shown.map { it.toString() })
            adapter.notifyDataSetChanged()
        }
        refresh("")

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refresh(s?.toString().orEmpty())
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.country_dialog_title)
            .setView(box)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        list.setOnItemClickListener { _, _, position, _ ->
            val picked = shown.getOrNull(position) ?: return@setOnItemClickListener
            country = picked
            Prefs.setCountryIso(this, picked.iso)
            renderCountry()
            dialog.dismiss()
        }
        dialog.show()
    }

    // ----- Configurações e Sobre -----

    private fun openSettings() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        val keepPrefix = view.findViewById<CheckBox>(R.id.setKeepPrefix)
        val saveRecents = view.findViewById<CheckBox>(R.id.setSaveRecents)
        val autoOpen = view.findViewById<CheckBox>(R.id.setAutoOpen)
        val tel = view.findViewById<CheckBox>(R.id.setTel)
        val themeSpinner = view.findViewById<Spinner>(R.id.setTheme)
        val limitInput = view.findViewById<EditText>(R.id.setLimit)

        keepPrefix.isChecked = Prefs.keepPrefix(this)
        saveRecents.isChecked = Prefs.saveRecents(this)
        autoOpen.isChecked = Prefs.autoOpen(this)
        tel.isChecked = Prefs.telHandler(this)
        themeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark)),
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        themeSpinner.setSelection(Prefs.theme(this).coerceIn(0, 2))
        limitInput.setText(Prefs.limit(this).toString())

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Prefs.setKeepPrefix(this, keepPrefix.isChecked)
                Prefs.setSaveRecents(this, saveRecents.isChecked)
                Prefs.setAutoOpen(this, autoOpen.isChecked)
                val telOn = tel.isChecked
                Prefs.setTelHandler(this, telOn)
                setTelAlias(telOn)
                Prefs.setLimit(this, limitInput.text.toString().toIntOrNull() ?: 30)
                val theme = themeSpinner.selectedItemPosition
                val changed = theme != Prefs.theme(this)
                Prefs.setTheme(this, theme)
                if (changed) recreate()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setTelAlias(enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, "$packageName.TelAlias"),
                state,
                PackageManager.DONT_KILL_APP,
            )
        } catch (e: Exception) {
            // Sem o alias no manifest, nada a fazer.
        }
    }

    private fun openAbout() {
        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(getString(R.string.about_body, versionName()))
            .setPositiveButton(android.R.string.ok, null)
        val support = getString(R.string.support_url).trim()
        if (support.isNotEmpty()) {
            builder.setNeutralButton(R.string.support_project) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(support)))
                } catch (e: ActivityNotFoundException) {
                    // Sem navegador: silencioso, nada de crash.
                }
            }
        }
        builder.show()
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    } catch (e: Exception) {
        ""
    }

    // ----- Entradas externas -----

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        var raw: String? = null
        var autoOpen = false
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val extra = intent.getStringExtra(EXTRA_NUMBER)
                if (!extra.isNullOrEmpty()) {
                    raw = extra
                    autoOpen = intent.getBooleanExtra(EXTRA_OPEN, false)
                } else {
                    val data = intent.data
                    if (data != null && data.scheme == "tel") {
                        raw = data.schemeSpecificPart
                        autoOpen = true
                    }
                }
            }
            Intent.ACTION_SEND -> {
                raw = Phone.extract(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty(), country)
            }
            Intent.ACTION_PROCESS_TEXT -> {
                raw = Phone.extract(
                    intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty(),
                    country,
                )
            }
        }
        val value = raw ?: return
        if (value.isEmpty()) return
        val e164 = if (value.all { it.isDigit() }) value else Phone.normalize(value, country)
        if (e164.isEmpty()) return
        val detected = Countries.match(e164)
        if (detected != null) {
            country = detected
            Prefs.setCountryIso(this, detected.iso)
            renderCountry()
        }
        phoneInput.setText(Phone.national(e164, country))
        hideHint()
        if (autoOpen) openChat(e164)
    }

    // ----- Utilidades -----

    private fun currentE164(): String? {
        val e164 = Phone.normalize(phoneInput.text.toString(), country)
        if (e164.length < 6) {
            toast(R.string.toast_number_required)
            return null
        }
        return e164
    }

    private fun showHint(textRes: Int) {
        hint.setText(textRes)
        hint.visibility = View.VISIBLE
    }

    private fun hideHint() {
        hint.visibility = View.GONE
    }

    private fun toast(textRes: Int) {
        Toast.makeText(this, textRes, Toast.LENGTH_SHORT).show()
    }

    /** Uma única entrada: o painel de vidro sobe e aparece. */
    private fun revealSheet() {
        val sheet = findViewById<View>(R.id.glassSheet)
        sheet.alpha = 0f
        sheet.translationY = 18 * resources.displayMetrics.density
        sheet.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

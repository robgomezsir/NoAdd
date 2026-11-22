package com.noadd.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.ContactsContract
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

/**
 * Envio em lote semi-manual: o app monta o link e abre a conversa; quem envia é
 * sempre o usuário, no WhatsApp. Sem acessibilidade, overlay ou automação.
 */
class BatchActivity : Activity() {

    companion object {
        private const val STEP_IMPORT = 0
        private const val STEP_MESSAGE = 1
        private const val STEP_QUEUE = 2

        private const val REQ_OPEN_DOCUMENT = 20
        private const val REQ_PICK_CONTACT = 21
        private const val REQ_SAVE_REPORT = 22
        private const val REQ_OPEN_TRAINING = 23

        private const val PREVIEW_COUNT = 5
        private const val LINK_LIMIT = 2000
        private const val RETURN_GUARD_MS = 400L
    }

    private lateinit var batchContent: View
    private lateinit var batchStep: TextView
    private lateinit var importPanel: View
    private lateinit var messagePanel: View
    private lateinit var queuePanel: View

    private lateinit var importSummary: TextView
    private lateinit var reviewSection: View
    private lateinit var reviewTitle: TextView
    private lateinit var reviewList: LinearLayout
    private lateinit var listTitle: TextView
    private lateinit var listPreview: LinearLayout
    private lateinit var listMore: TextView
    private lateinit var toMessage: Button

    private lateinit var messageInput: EditText
    private lateinit var previewName: TextView
    private lateinit var previewText: TextView
    private lateinit var messageCounter: TextView
    private lateinit var linkWarning: TextView
    private lateinit var loadTraining: Button
    private lateinit var trainingSummary: TextView
    private lateinit var previewNext: Button
    private lateinit var previewWarning: TextView

    private lateinit var progressText: TextView
    private lateinit var sessionText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var countsText: TextView
    private lateinit var limitCard: View
    private lateinit var limitText: TextView
    private lateinit var currentCard: View
    private lateinit var currentName: TextView
    private lateinit var currentPhone: TextView
    private lateinit var batchList: ListView
    private lateinit var batchEmpty: TextView
    private lateinit var openNextButton: Button

    private var contacts: MutableList<BatchContact> = ArrayList()
    private var review: MutableList<ReviewContact> = ArrayList()
    private lateinit var adapter: ContactAdapter

    private var step = STEP_IMPORT
    private var pendingReturnAt = 0L
    private var pendingReport = ""
    private var training: TrainingRow? = null
    private var previewIndex = 0

    private val saveMessageRunnable = Runnable {
        if (::messageInput.isInitialized) BatchStore.setMessage(this, messageInput.text.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Ui.themeRes(Prefs.theme(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch)
        Ui.prepareWindow(this)
        Ui.applySystemBarsPadding(findViewById<View>(R.id.batchContent))

        bindViews()
        loadState()
        adapter = ContactAdapter()
        batchList.adapter = adapter
        setupListeners()

        pendingReport = savedInstanceState?.getString("report").orEmpty()
        val savedStep = savedInstanceState?.getInt("step", -1) ?: -1
        val initialStep = if (savedStep in STEP_IMPORT..STEP_QUEUE) {
            savedStep
        } else if (contacts.isEmpty()) {
            STEP_IMPORT
        } else {
            STEP_QUEUE
        }
        showStep(initialStep)
        refreshAll()

        if (!BatchStore.disclaimerSeen(this)) showDisclaimer()
    }

    override fun onResume() {
        super.onResume()
        val launchedAt = pendingReturnAt
        pendingReturnAt = 0
        val returning = launchedAt > 0 && SystemClock.elapsedRealtime() - launchedAt >= RETURN_GUARD_MS
        if (returning && step == STEP_QUEUE && BatchStore.autoAdvance(this)) {
            val current = currentContact()
            if (current != null) {
                batchContent.postDelayed({
                    if (!isFinishing && currentContact()?.id == current.id) {
                        markCurrentSent(advance = true, quiet = true)
                    }
                }, 250)
            }
        }
        refreshAll()
    }

    override fun onPause() {
        super.onPause()
        if (::messageInput.isInitialized) {
            messageInput.removeCallbacks(saveMessageRunnable)
            BatchStore.setMessage(this, messageInput.text.toString())
        }
    }

    @Deprecated("Activity sem AndroidX: o botão voltar é tratado aqui.")
    override fun onBackPressed() {
        if (step == STEP_IMPORT) {
            super.onBackPressed()
        } else {
            showStep(step - 1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("step", step)
        outState.putString("report", pendingReport)
    }

    // ----- Montagem da tela -----

    private fun bindViews() {
        batchContent = findViewById(R.id.batchContent)
        batchStep = findViewById(R.id.batchStep)
        importPanel = findViewById(R.id.importPanel)
        messagePanel = findViewById(R.id.messagePanel)
        queuePanel = findViewById(R.id.queuePanel)

        importSummary = findViewById(R.id.importSummary)
        reviewSection = findViewById(R.id.reviewSection)
        reviewTitle = findViewById(R.id.reviewTitle)
        reviewList = findViewById(R.id.reviewList)
        listTitle = findViewById(R.id.listTitle)
        listPreview = findViewById(R.id.listPreview)
        listMore = findViewById(R.id.listMore)
        toMessage = findViewById(R.id.toMessage)

        messageInput = findViewById(R.id.batchMessage)
        previewName = findViewById(R.id.previewName)
        previewText = findViewById(R.id.previewText)
        messageCounter = findViewById(R.id.messageCounter)
        linkWarning = findViewById(R.id.linkWarning)
        loadTraining = findViewById(R.id.loadTraining)
        trainingSummary = findViewById(R.id.trainingSummary)
        previewNext = findViewById(R.id.previewNext)
        previewWarning = findViewById(R.id.previewWarning)

        progressText = findViewById(R.id.progressText)
        sessionText = findViewById(R.id.sessionText)
        progressBar = findViewById(R.id.progressBar)
        countsText = findViewById(R.id.countsText)
        limitCard = findViewById(R.id.limitCard)
        limitText = findViewById(R.id.limitText)
        currentCard = findViewById(R.id.currentCard)
        currentName = findViewById(R.id.currentName)
        currentPhone = findViewById(R.id.currentPhone)
        batchList = findViewById(R.id.batchList)
        batchEmpty = findViewById(R.id.batchEmpty)
        openNextButton = findViewById(R.id.openNext)
    }

    private fun loadState() {
        contacts = BatchStore.contacts(this).toMutableList()
        review = BatchStore.review(this).toMutableList()
        training = BatchStore.training(this)
        // A saudação padrão entra só na primeira abertura. Uma mensagem já
        // salva — inclusive vazia, apagada pelo usuário — é respeitada: o texto
        // padrão não volta por cima da escolha dele.
        val stored = BatchStore.message(this)
        val firstRun = !BatchStore.messageSeeded(this) && stored.isBlank()
        val template = if (firstRun) {
            BatchStore.setMessage(this, defaultTemplate())
            defaultTemplate()
        } else {
            stored
        }
        BatchStore.setMessageSeeded(this, true)
        messageInput.setText(template)
        refreshTraining()
    }

    private fun defaultTemplate(): String = getString(R.string.batch_default_message)

    private fun setupListeners() {
        findViewById<View>(R.id.batchBack).setOnClickListener { goBack() }
        findViewById<View>(R.id.batchMenu).setOnClickListener { showMenu() }

        findViewById<Button>(R.id.importFile).setOnClickListener {
            openDocument(REQ_OPEN_DOCUMENT)
        }
        findViewById<Button>(R.id.pasteList).setOnClickListener { showPasteDialog() }
        findViewById<Button>(R.id.pickContact).setOnClickListener { pickContact() }
        findViewById<Button>(R.id.addManual).setOnClickListener { showContactDialog(null) }
        toMessage.setOnClickListener {
            if (contacts.isEmpty()) {
                toast(R.string.batch_list_empty)
            } else {
                showStep(STEP_MESSAGE)
            }
        }

        findViewById<Button>(R.id.insertName).setOnClickListener { insertVariable("{nome}") }
        findViewById<Button>(R.id.insertFirst).setOnClickListener { insertVariable("{primeiro_nome}") }
        findViewById<Button>(R.id.backToImport).setOnClickListener { showStep(STEP_IMPORT) }
        findViewById<Button>(R.id.toQueue).setOnClickListener {
            if (contacts.isEmpty()) {
                toast(R.string.batch_list_empty)
            } else {
                saveMessage()
                showStep(STEP_QUEUE)
            }
        }
        findViewById<Button>(R.id.templatesButton).setOnClickListener { showTemplates() }
        loadTraining.setOnClickListener { openDocument(REQ_OPEN_TRAINING) }
        previewNext.setOnClickListener {
            if (contacts.isEmpty()) return@setOnClickListener
            previewIndex = (previewIndex + 1) % contacts.size
            refreshMessagePreview()
        }

        findViewById<Button>(R.id.markSent).setOnClickListener {
            markCurrentSent(advance = true, quiet = false)
        }
        findViewById<Button>(R.id.skip).setOnClickListener { markCurrentSkipped() }
        findViewById<Button>(R.id.openAgain).setOnClickListener {
            val current = currentContact()
            if (current == null) toast(R.string.batch_no_pending) else openContact(current)
        }
        findViewById<Button>(R.id.newSession).setOnClickListener { startNewSession() }
        openNextButton.setOnClickListener { openNext() }

        batchList.setOnItemClickListener { _, _, position, _ ->
            contacts.getOrNull(position)?.let { showContactActions(it) }
        }

        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                messageInput.removeCallbacks(saveMessageRunnable)
                messageInput.postDelayed(saveMessageRunnable, 400)
                refreshMessagePreview()
            }
        })
    }

    private fun showStep(value: Int) {
        step = value
        importPanel.visibility = if (value == STEP_IMPORT) View.VISIBLE else View.GONE
        messagePanel.visibility = if (value == STEP_MESSAGE) View.VISIBLE else View.GONE
        queuePanel.visibility = if (value == STEP_QUEUE) View.VISIBLE else View.GONE
        batchStep.setText(
            when (value) {
                STEP_MESSAGE -> R.string.batch_step_message
                STEP_QUEUE -> R.string.batch_step_queue
                else -> R.string.batch_step_import
            },
        )
        currentFocus?.clearFocus()
        when (value) {
            STEP_IMPORT -> refreshImport()
            STEP_MESSAGE -> refreshMessagePreview()
            STEP_QUEUE -> refreshQueue()
        }
    }

    private fun goBack() {
        if (step == STEP_IMPORT) finish() else showStep(step - 1)
    }

    private fun refreshAll() {
        refreshImport()
        refreshMessagePreview()
        refreshQueue()
        refreshTraining()
    }

    // ----- Importação -----

    private fun openDocument(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "text/*",
                    "text/plain",
                    "text/csv",
                    "text/comma-separated-values",
                    "application/csv",
                    "application/vnd.ms-excel",
                ),
            )
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, requestCode)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.batch_file_error)
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_PICK_CONTACT)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.batch_pick_error)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            REQ_OPEN_DOCUMENT -> {
                val uri = data?.data ?: return
                importFile(uri)
            }
            REQ_PICK_CONTACT -> handlePickedContact(data)
            REQ_SAVE_REPORT -> saveReport(data?.data)
            REQ_OPEN_TRAINING -> importTraining(data?.data)
        }
    }

    private fun importFile(uri: Uri) {
        try {
            val text = readText(uri)
            if (text.isBlank()) {
                toast(R.string.batch_file_empty)
                return
            }
            importText(text)
        } catch (e: Exception) {
            toast(R.string.batch_file_error)
        }
    }

    /** UTF-8 com queda para Latin-1 (CSV antigo do Excel) e BOM UTF-16. */
    private fun readText(uri: Uri): String {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("empty stream")
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, Charsets.UTF_16LE).removePrefix("\uFEFF")
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, Charsets.UTF_16BE).removePrefix("\uFEFF")
        }
        var text = String(bytes, Charsets.UTF_8)
        if (text.contains('\uFFFD')) text = String(bytes, Charsets.ISO_8859_1)
        return text.removePrefix("\uFEFF")
    }

    private fun importTraining(uri: Uri?) {
        if (uri == null) return
        val text = try {
            readText(uri)
        } catch (e: Exception) {
            toast(R.string.batch_file_error)
            return
        }
        val parsed = Training.parse(text)
        val row = parsed.row
        if (row == null) {
            toast(R.string.batch_training_no_data)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_training_dialog_title)
            .setView(trainingDialogView(row, parsed))
            .setPositiveButton(R.string.batch_training_apply) { _, _ -> applyTraining(row) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Resumo para conferência: campo vazio aparece na cor de destaque. */
    private fun trainingDialogView(row: TrainingRow, parsed: ParsedTraining): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        if (parsed.rows > 1) {
            box.addView(hintText(getString(R.string.batch_training_many_rows, parsed.rows)))
        }
        if (parsed.missingColumns.isNotEmpty()) {
            box.addView(
                hintText(
                    getString(
                        R.string.batch_training_missing_columns,
                        parsed.missingColumns.joinToString(", ") { getString(Training.label(it)) },
                    ),
                ),
            )
        }
        for (field in TrainingField.values()) {
            val value = row.value(field)
            val view = TextView(this).apply {
                textSize = 14f
                setPadding(0, dp(8), 0, 0)
                setLineSpacing(dp(1).toFloat(), 1f)
            }
            val label = getString(Training.label(field)) + ": "
            if (value.isBlank()) {
                view.text = label + getString(R.string.batch_training_empty_value)
                view.setTextColor(themeColor(R.attr.noAddAccent))
            } else {
                view.text = label + value
                view.setTextColor(themeColor(R.attr.noAddInk))
            }
            box.addView(view)
        }
        return ScrollView(this).apply { addView(box) }
    }

    private fun hintText(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 12f
        setTextColor(themeColor(R.attr.noAddInkMuted))
        setPadding(0, dp(4), 0, 0)
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun applyTraining(row: TrainingRow) {
        training = row
        BatchStore.setTraining(this, row)
        val text = Training.fill(row)
        messageInput.setText(text)
        messageInput.setSelection(text.length)
        BatchStore.setMessage(this, text)
        BatchStore.setMessageSeeded(this, true)
        BatchStore.setTrainingTemplate(this, text)
        refreshTraining()
        refreshMessagePreview()
        toast(R.string.batch_training_applied)
    }

    private fun refreshTraining() {
        if (!::trainingSummary.isInitialized) return
        val row = training
        if (row == null) {
            trainingSummary.visibility = View.GONE
            return
        }
        val parts = listOf(row.product, row.role, row.date, row.time)
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(getString(R.string.batch_training_empty_value)) }
        trainingSummary.text = getString(
            R.string.batch_training_loaded,
            parts.joinToString(" · "),
        )
        trainingSummary.visibility = View.VISIBLE
    }

    private fun importText(text: String) {
        val country = Countries.byIso(Prefs.countryIso(this)) ?: Countries.default
        val parsed = Batch.parse(text, country)
        if (parsed.rows == 0) {
            toast(R.string.batch_file_empty)
            return
        }

        val known = contacts.map { it.phone }.toHashSet()
        var added = 0
        var duplicates = parsed.duplicates
        for (item in parsed.contacts) {
            if (!known.add(item.phone)) {
                duplicates++
                continue
            }
            contacts.add(
                BatchContact(name = item.name, surname = item.surname, phone = item.phone),
            )
            added++
        }
        for (item in parsed.invalid) {
            if (review.none { it.raw == item.raw }) {
                review.add(ReviewContact(raw = item.raw, reason = item.reason))
            }
        }
        persistList()
        refreshAll()

        importSummary.text = getString(
            R.string.batch_summary,
            added,
            parsed.invalid.size,
            duplicates,
        )
        importSummary.visibility = View.VISIBLE

        when {
            added > 0 -> toast(getString(R.string.batch_imported_toast, added))
            parsed.invalid.isNotEmpty() -> toast(R.string.batch_contact_invalid)
            else -> toast(R.string.batch_nothing_imported)
        }
    }

    private fun showPasteDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 6
            gravity = Gravity.TOP or Gravity.START
            hint = getString(R.string.batch_import_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_import_dialog_title)
            .setView(field(input))
            .setPositiveButton(android.R.string.ok) { _, _ -> importText(input.text.toString()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showContactDialog(existing: BatchContact?) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val nameInput = EditText(this).apply {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
            hint = getString(R.string.shortcut_hint)
            setText(existing?.name.orEmpty())
        }
        val surnameInput = EditText(this).apply {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
            hint = getString(R.string.batch_surname_hint)
            setText(existing?.surname.orEmpty())
        }
        val phoneInput = EditText(this).apply {
            id = View.generateViewId()
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine()
            hint = getString(R.string.hint_number)
            setText(existing?.let { Phone.pretty(it.phone) }.orEmpty())
        }
        box.addView(fieldLabel(getString(R.string.batch_manual_name), nameInput.id))
        box.addView(nameInput)
        box.addView(fieldLabel(getString(R.string.batch_manual_surname), surnameInput.id))
        box.addView(surnameInput)
        box.addView(fieldLabel(getString(R.string.batch_manual_phone), phoneInput.id))
        box.addView(phoneInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) R.string.batch_manual_title else R.string.batch_edit_title)
            .setView(box)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                val surname = surnameInput.text.toString().trim()
                val raw = phoneInput.text.toString().trim()
                if (raw.isEmpty()) {
                    toast(R.string.batch_manual_phone)
                    return@setOnClickListener
                }
                val ok = if (existing == null) {
                    addContact(name, surname, raw)
                } else {
                    updateContact(existing, name, surname, raw)
                }
                if (ok) dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun addContact(name: String, surname: String, raw: String): Boolean {
        val country = Countries.byIso(Prefs.countryIso(this)) ?: Countries.default
        val e164 = Phone.normalize(raw, country)
        if (e164.isEmpty() || !Phone.looksValid(e164)) {
            val reason = if (e164.isEmpty()) Batch.REASON_EMPTY else Batch.REASON_INVALID
            if (review.none { it.raw == raw }) {
                review.add(ReviewContact(raw = raw, reason = reason))
            }
            persistList()
            refreshAll()
            toast(R.string.batch_contact_invalid)
            return true
        }
        if (contacts.any { it.phone == e164 }) {
            toast(R.string.batch_duplicate)
            return false
        }
        contacts.add(BatchContact(name = name, surname = surname, phone = e164))
        persistList()
        refreshAll()
        toast(R.string.batch_contact_added)
        return true
    }

    private fun updateContact(
        existing: BatchContact,
        name: String,
        surname: String,
        raw: String,
    ): Boolean {
        val country = Countries.byIso(Prefs.countryIso(this)) ?: Countries.default
        val e164 = Phone.normalize(raw, country)
        if (e164.isEmpty() || !Phone.looksValid(e164)) {
            toast(R.string.batch_edit_invalid)
            return false
        }
        if (contacts.any { it.id != existing.id && it.phone == e164 }) {
            toast(R.string.batch_duplicate)
            return false
        }
        val index = contacts.indexOfFirst { it.id == existing.id }
        if (index < 0) return false
        contacts[index] = contacts[index].copy(name = name, surname = surname, phone = e164)
        persistList()
        refreshAll()
        toast(R.string.batch_edit_saved)
        return true
    }

    private fun handlePickedContact(data: Intent?) {
        val uri = data?.data ?: return
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0) ?: ""
                    val number = cursor.getString(1) ?: ""
                    if (number.isBlank()) {
                        toast(R.string.batch_contact_invalid)
                    } else {
                        val parts = Batch.splitName(name)
                        addContact(parts.first, parts.second, number)
                    }
                }
            }
        } catch (e: Exception) {
            toast(R.string.batch_pick_error)
        }
    }

    private fun showReviewActions(item: ReviewContact) {
        val labels = arrayOf(
            getString(R.string.batch_review_fix),
            getString(R.string.batch_review_discard),
        )
        AlertDialog.Builder(this)
            .setTitle(item.raw)
            .setItems(labels) { _, which ->
                if (which == 0) showReviewFixDialog(item) else discardReview(item)
            }
            .show()
    }

    private fun showReviewFixDialog(item: ReviewContact) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine()
            hint = getString(R.string.hint_number)
            setText(item.raw)
            setSelection(text.length)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.batch_review_fix)
            .setView(field(input))
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val country = Countries.byIso(Prefs.countryIso(this)) ?: Countries.default
                val e164 = Phone.normalize(input.text.toString(), country)
                if (e164.isEmpty() || !Phone.looksValid(e164)) {
                    toast(R.string.batch_edit_invalid)
                    return@setOnClickListener
                }
                if (contacts.any { it.phone == e164 }) {
                    toast(R.string.batch_duplicate)
                    return@setOnClickListener
                }
                contacts.add(BatchContact(name = "", phone = e164))
                review.removeAll { it.id == item.id }
                persistList()
                refreshAll()
                toast(R.string.batch_review_corrected)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun discardReview(item: ReviewContact) {
        review.removeAll { it.id == item.id }
        persistList()
        refreshAll()
        toast(R.string.batch_review_removed)
    }

    private fun refreshImport() {
        listTitle.text = getString(R.string.batch_list_title, contacts.size)
        listPreview.removeAllViews()
        if (contacts.isEmpty()) {
            val empty = TextView(this).apply {
                text = getString(R.string.batch_list_empty)
                setTextColor(themeColor(R.attr.noAddInkMuted))
                textSize = 13f
                setPadding(0, dp(6), 0, 0)
            }
            listPreview.addView(empty)
            listMore.visibility = View.GONE
        } else {
            for (contact in contacts.take(PREVIEW_COUNT)) {
                val row = layoutInflater.inflate(R.layout.item_batch_preview, listPreview, false)
                row.findViewById<TextView>(R.id.previewName).text =
                    contact.fullName.ifBlank { Phone.pretty(contact.phone) }
                row.findViewById<TextView>(R.id.previewPhone).text =
                    if (contact.fullName.isBlank()) "" else Phone.pretty(contact.phone)
                listPreview.addView(row)
            }
            val remaining = contacts.size - PREVIEW_COUNT
            listMore.visibility = if (remaining > 0) View.VISIBLE else View.GONE
            if (remaining > 0) listMore.text = getString(R.string.batch_list_more, remaining)
        }

        reviewSection.visibility = if (review.isEmpty()) View.GONE else View.VISIBLE
        reviewTitle.text = getString(R.string.batch_review_title, review.size)
        reviewList.removeAllViews()
        for (item in review) {
            val row = layoutInflater.inflate(R.layout.item_entry, reviewList, false)
            row.findViewById<TextView>(R.id.title).text = item.raw
            row.findViewById<TextView>(R.id.subtitle).text = getString(Batch.reasonLabel(item.reason))
            row.setOnClickListener { showReviewActions(item) }
            reviewList.addView(row)
        }

        toMessage.isEnabled = contacts.isNotEmpty()
        toMessage.alpha = if (contacts.isNotEmpty()) 1f else 0.5f
    }

    // ----- Mensagem -----

    private fun insertVariable(variable: String) {
        val start = messageInput.selectionStart
        val end = messageInput.selectionEnd
        if (start >= 0 && end >= start) {
            messageInput.text.replace(start, end, variable)
        } else {
            messageInput.append(variable)
        }
        messageInput.requestFocus()
    }

    private fun refreshMessagePreview() {
        val template = messageInput.text.toString()
        val contact = if (contacts.isEmpty()) null else contacts[previewIndex % contacts.size]
        previewNext.visibility = if (contacts.size > 1) View.VISIBLE else View.GONE

        if (contact == null) {
            previewName.text = getString(
                R.string.batch_preview_name,
                getString(R.string.batch_preview_no_name),
            )
            previewText.text = getString(R.string.batch_preview_empty)
            messageCounter.text = getString(R.string.batch_counter, template.length, template.length)
            linkWarning.visibility = View.GONE
            refreshMessageNotes(template)
            return
        }

        previewName.text = getString(
            R.string.batch_preview_name,
            contact.fullName.ifBlank { getString(R.string.batch_preview_no_name) },
        )
        val preview = Batch.personalize(template, contact)
        previewText.text = if (template.isBlank()) {
            getString(R.string.batch_preview_empty)
        } else {
            preview.ifBlank { getString(R.string.batch_preview_empty) }
        }
        val linkLength = Batch.linkLength(contact, template)
        messageCounter.text = getString(R.string.batch_counter, template.length, linkLength)
        linkWarning.visibility = if (linkLength > LINK_LIMIT) View.VISIBLE else View.GONE
        refreshMessageNotes(template)
    }

    /** Campos vazios do treinamento e variáveis desconhecidas, sem esconder nada. */
    private fun refreshMessageNotes(template: String) {
        if (!::previewWarning.isInitialized) return
        val notes = ArrayList<String>()
        val row = training
        if (row != null && row.missing.isNotEmpty()) {
            notes.add(
                getString(
                    R.string.batch_training_empty_fields,
                    row.missing.joinToString(", ") { getString(Training.label(it)) },
                ),
            )
        }
        val unknown = Training.unknownVariables(template)
        if (unknown.isNotEmpty()) {
            notes.add(
                getString(R.string.batch_unknown_vars, unknown.joinToString(", ") { "{$it}" }),
            )
        }
        previewWarning.text = notes.joinToString("\n")
        previewWarning.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun saveMessage() {
        BatchStore.setMessage(this, messageInput.text.toString())
    }

    private fun showTemplates() {
        val trainingTemplate = BatchStore.trainingTemplate(this)
        val templates = BatchStore.templates(this).toMutableList()
        // "Modelo de treinamento" fica no topo, com nome, em vez do texto cru.
        val labels = ArrayList<String>()
        val values = ArrayList<String>()
        if (trainingTemplate.isNotBlank()) {
            labels.add(getString(R.string.batch_training_template_name))
            values.add(trainingTemplate)
        }
        for (value in templates) {
            labels.add(value)
            values.add(value)
        }
        if (values.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.batch_templates_title)
                .setMessage(R.string.batch_templates_empty)
                .setPositiveButton(R.string.batch_template_save) { _, _ -> saveCurrentTemplate() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        val list = ListView(this)
        val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        list.adapter = listAdapter
        val box = FrameLayout(this).apply {
            setPadding(dp(16), dp(8), dp(16), 0)
            addView(
                list,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(320),
                ),
            )
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.batch_templates_title)
            .setView(box)
            .setPositiveButton(R.string.batch_template_save) { _, _ -> saveCurrentTemplate() }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        list.setOnItemClickListener { _, _, position, _ ->
            val value = values.getOrNull(position) ?: return@setOnItemClickListener
            messageInput.setText(value)
            messageInput.setSelection(messageInput.text.length)
            BatchStore.setMessage(this, value)
            BatchStore.setMessageSeeded(this, true)
            refreshMessagePreview()
            dialog.dismiss()
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            val value = values.getOrNull(position) ?: return@setOnItemLongClickListener true
            confirmDeleteTemplate(value) {
                if (trainingTemplate.isNotBlank() && position == 0) {
                    BatchStore.setTrainingTemplate(this, "")
                } else {
                    val index = templates.indexOfFirst { it == value }
                    if (index >= 0) {
                        templates.removeAt(index)
                        BatchStore.saveTemplates(this, templates)
                    }
                }
                labels.removeAt(position)
                values.removeAt(position)
                listAdapter.notifyDataSetChanged()
                toast(R.string.batch_template_deleted)
            }
            true
        }
        dialog.show()
    }

    private fun saveCurrentTemplate() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) {
            toast(R.string.batch_preview_empty)
            return
        }
        val list = BatchStore.templates(this).toMutableList()
        if (list.none { it == text }) {
            list.add(0, text)
            BatchStore.saveTemplates(this, list)
        }
        toast(R.string.batch_template_saved)
    }

    private fun confirmDeleteTemplate(value: String, onDelete: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_template_delete_title)
            .setMessage(R.string.batch_template_delete_message)
            .setPositiveButton(R.string.action_remove) { _, _ -> onDelete() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ----- Fila -----

    private fun refreshQueue() {
        val total = contacts.size
        val sent = contacts.count { it.status == BatchStatus.SENT }
        val skipped = contacts.count { it.status == BatchStatus.SKIPPED }
        val pending = total - sent - skipped
        val resolved = sent + skipped

        progressText.text = getString(R.string.batch_progress, resolved, total)
        progressBar.progress = if (total == 0) 0 else resolved * 100 / total
        progressText.contentDescription = getString(R.string.batch_progress_desc, resolved, total)
        countsText.text = getString(R.string.batch_counts, sent, skipped, pending)

        val session = BatchStore.sessionResolved(this)
        val limit = BatchStore.sessionLimit(this)
        sessionText.text = getString(R.string.batch_session_text, session, limit)
        limitCard.visibility = if (session >= limit && pending > 0) View.VISIBLE else View.GONE
        limitText.text = getString(R.string.batch_session_limit_message, session, limit)

        val current = currentContact()
        if (current == null) {
            currentCard.visibility = View.GONE
        } else {
            currentCard.visibility = View.VISIBLE
            val status = getString(Batch.statusLabel(current.status))
            currentName.text = current.fullName.ifBlank { Phone.pretty(current.phone) }
            currentPhone.text = if (current.fullName.isBlank()) {
                status
            } else {
                "${Phone.pretty(current.phone)} · $status"
            }
        }

        adapter.notifyDataSetChanged()
        batchEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        batchEmpty.setText(R.string.batch_queue_empty)

        when {
            contacts.isEmpty() -> {
                openNextButton.setText(R.string.batch_open_next)
                openNextButton.isEnabled = false
            }
            pending == 0 -> {
                openNextButton.setText(R.string.batch_queue_done_short)
                openNextButton.isEnabled = false
            }
            else -> {
                openNextButton.setText(R.string.batch_open_next)
                openNextButton.isEnabled = true
            }
        }
        openNextButton.alpha = if (openNextButton.isEnabled) 1f else 0.5f
    }

    private fun currentContact(): BatchContact? {
        val id = BatchStore.currentId(this)
        if (id.isEmpty()) return null
        return contacts.firstOrNull { it.id == id && it.status == BatchStatus.OPENED }
    }

    private fun showContactActions(contact: BatchContact) {
        val labels = ArrayList<String>()
        val actions = ArrayList<() -> Unit>()

        labels.add(getString(R.string.batch_contact_open))
        actions.add { openContact(contact) }

        if (contact.status != BatchStatus.SENT) {
            labels.add(getString(R.string.batch_contact_mark_sent))
            actions.add {
                applyStatus(contact, BatchStatus.SENT)
                toast(R.string.batch_sent_toast)
                refreshAll()
            }
        }
        if (contact.status != BatchStatus.PENDING) {
            labels.add(getString(R.string.batch_contact_mark_pending))
            actions.add {
                applyStatus(contact, BatchStatus.PENDING)
                refreshAll()
            }
        }
        if (contact.status != BatchStatus.SKIPPED) {
            labels.add(getString(R.string.batch_contact_skip))
            actions.add {
                applyStatus(contact, BatchStatus.SKIPPED)
                toast(R.string.batch_skipped_toast)
                refreshAll()
            }
        }
        labels.add(getString(R.string.batch_contact_edit))
        actions.add { showContactDialog(contact) }
        labels.add(getString(R.string.batch_contact_remove))
        actions.add { removeContact(contact) }

        AlertDialog.Builder(this)
            .setTitle(contact.fullName.ifBlank { Phone.pretty(contact.phone) })
            .setItems(labels.toTypedArray()) { _, which -> actions.getOrNull(which)?.invoke() }
            .show()
    }

    private fun applyStatus(contact: BatchContact, status: BatchStatus) {
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index < 0) return
        val old = contacts[index]
        val oldResolved = old.status == BatchStatus.SENT || old.status == BatchStatus.SKIPPED
        val newResolved = status == BatchStatus.SENT || status == BatchStatus.SKIPPED
        if (oldResolved != newResolved) {
            val session = BatchStore.sessionResolved(this)
            BatchStore.setSessionResolved(
                this,
                if (newResolved) session + 1 else (session - 1).coerceAtLeast(0),
            )
        }
        contacts[index] = old.copy(status = status)
        if (status == BatchStatus.OPENED) {
            BatchStore.setCurrentId(this, contact.id)
        } else if (BatchStore.currentId(this) == contact.id) {
            BatchStore.setCurrentId(this, "")
        }
        BatchStore.saveContacts(this, contacts)
    }

    private fun removeContact(contact: BatchContact) {
        val wasResolved = contact.status == BatchStatus.SENT || contact.status == BatchStatus.SKIPPED
        contacts.removeAll { it.id == contact.id }
        if (wasResolved) {
            BatchStore.setSessionResolved(
                this,
                (BatchStore.sessionResolved(this) - 1).coerceAtLeast(0),
            )
        }
        if (BatchStore.currentId(this) == contact.id) BatchStore.setCurrentId(this, "")
        BatchStore.saveContacts(this, contacts)
        refreshAll()
        toast(R.string.batch_contact_removed)
    }

    private fun markCurrentSent(advance: Boolean, quiet: Boolean) {
        val current = currentContact() ?: return
        applyStatus(current, BatchStatus.SENT)
        if (!quiet) toast(R.string.batch_sent_toast)
        refreshAll()
        if (advance) advanceAfterSent()
    }

    private fun markCurrentSkipped() {
        val current = currentContact() ?: return
        applyStatus(current, BatchStatus.SKIPPED)
        toast(R.string.batch_skipped_toast)
        refreshAll()
    }

    private fun advanceAfterSent() {
        if (!contacts.any { it.status == BatchStatus.PENDING || it.status == BatchStatus.OPENED }) {
            refreshQueue()
            return
        }
        val limit = BatchStore.sessionLimit(this)
        val session = BatchStore.sessionResolved(this)
        if (session >= limit) {
            refreshQueue()
            return
        }
        val every = BatchStore.reminderEvery(this)
        if (every > 0 && session % every == 0) {
            showReminder { openNextInternal() }
            return
        }
        openNextInternal()
    }

    private fun openNext() {
        val unresolved = contacts.any {
            it.status == BatchStatus.PENDING || it.status == BatchStatus.OPENED
        }
        if (!unresolved) {
            toast(R.string.batch_no_pending)
            return
        }
        val limit = BatchStore.sessionLimit(this)
        if (BatchStore.sessionResolved(this) >= limit) {
            showLimitDialog {
                startNewSession()
                openNextInternal()
            }
            return
        }
        openNextInternal()
    }

    private fun openNextInternal() {
        if (step != STEP_QUEUE) showStep(STEP_QUEUE)
        val next = currentContact()
            ?: contacts.firstOrNull { it.status == BatchStatus.PENDING }
        if (next == null) {
            toast(R.string.batch_no_pending)
            return
        }
        openContact(next)
    }

    private fun openContact(contact: BatchContact, enforceLimit: Boolean = true) {
        val unresolved = contact.status == BatchStatus.PENDING || contact.status == BatchStatus.OPENED
        val limit = BatchStore.sessionLimit(this)
        if (enforceLimit && unresolved && BatchStore.sessionResolved(this) >= limit) {
            showLimitDialog {
                startNewSession()
                openContact(contact, enforceLimit = false)
            }
            return
        }

        val template = messageInput.text.toString()
        val message = Batch.personalize(template, contact)
        val link = ChatIntents.link(contact.phone, message)
        val chats = ChatIntents.installedChats(this)
        if (chats.isEmpty()) {
            askBrowser(link, contact)
            return
        }

        val stored = BatchStore.waPackage(this)
        when {
            stored == "default" -> launchChat(link, contact, null)
            stored.isNotEmpty() && chats.contains(stored) -> launchChat(link, contact, stored)
            chats.size == 1 -> launchChat(link, contact, chats[0])
            else -> chooseWhatsApp(link, contact)
        }
    }

    private fun chooseWhatsApp(link: String, contact: BatchContact) {
        val labels = arrayOf(
            getString(R.string.whatsapp_name),
            getString(R.string.whatsapp_business_name),
            getString(R.string.chat_default),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_choose_wa_title)
            .setItems(labels) { _, which ->
                val value = when (which) {
                    0 -> ChatIntents.PKG_WA
                    1 -> ChatIntents.PKG_W4B
                    else -> "default"
                }
                BatchStore.setWaPackage(this, value)
                launchChat(link, contact, if (value == "default") null else value)
            }
            .setOnCancelListener { refreshQueue() }
            .show()
    }

    private fun launchChat(link: String, contact: BatchContact, pkg: String?) {
        val previous = contact.status
        try {
            pendingReturnAt = SystemClock.elapsedRealtime()
            applyStatus(contact, BatchStatus.OPENED)
            BatchStore.setCurrentId(this, contact.id)
            refreshAll()
            startActivity(ChatIntents.view(link, pkg))
        } catch (e: ActivityNotFoundException) {
            pendingReturnAt = 0
            applyStatus(contact, previous)
            refreshAll()
            askBrowser(link, contact)
        } catch (e: Exception) {
            pendingReturnAt = 0
            applyStatus(contact, previous)
            refreshAll()
            toast(R.string.batch_open_failed)
        }
    }

    private fun askBrowser(link: String, contact: BatchContact) {
        AlertDialog.Builder(this)
            .setTitle(R.string.warning_no_app_title)
            .setMessage(R.string.warning_no_app_message)
            .setPositiveButton(R.string.open_browser) { _, _ ->
                val previous = contact.status
                try {
                    pendingReturnAt = SystemClock.elapsedRealtime()
                    applyStatus(contact, BatchStatus.OPENED)
                    BatchStore.setCurrentId(this, contact.id)
                    refreshAll()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                } catch (e: ActivityNotFoundException) {
                    pendingReturnAt = 0
                    applyStatus(contact, previous)
                    refreshAll()
                    toast(R.string.batch_open_failed)
                } catch (e: Exception) {
                    pendingReturnAt = 0
                    applyStatus(contact, previous)
                    refreshAll()
                    toast(R.string.batch_open_failed)
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> refreshQueue() }
            .show()
    }

    private fun showLimitDialog(onContinue: () -> Unit) {
        val session = BatchStore.sessionResolved(this)
        val limit = BatchStore.sessionLimit(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_settings_limit)
            .setMessage(getString(R.string.batch_session_limit_message, session, limit))
            .setPositiveButton(R.string.batch_session_new) { _, _ ->
                startNewSession()
                onContinue()
            }
            .setNegativeButton(R.string.batch_session_later, null)
            .show()
    }

    private fun showReminder(onContinue: () -> Unit) {
        val session = BatchStore.sessionResolved(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_reminder_title)
            .setMessage(getString(R.string.batch_reminder_message, session))
            .setPositiveButton(R.string.batch_continue) { _, _ -> onContinue() }
            .setNegativeButton(R.string.batch_pause, null)
            .show()
    }

    private fun startNewSession() {
        BatchStore.setSessionResolved(this, 0)
        refreshAll()
    }

    // ----- Menu, ajustes, exportação e limpeza -----

    private fun showMenu() {
        val popup = PopupMenu(this, findViewById<View>(R.id.batchMenu))
        popup.menuInflater.inflate(R.menu.batch, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_batch_settings -> {
                    showBatchSettings()
                    true
                }
                R.id.menu_batch_templates -> {
                    showTemplates()
                    true
                }
                R.id.menu_batch_export -> {
                    exportReport()
                    true
                }
                R.id.menu_batch_clear_list -> {
                    confirmClearList()
                    true
                }
                R.id.menu_batch_clear_all -> {
                    confirmClearAll()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showBatchSettings() {
        val view = layoutInflater.inflate(R.layout.dialog_batch_settings, null)
        val autoAdvance = view.findViewById<CheckBox>(R.id.setAutoAdvance)
        val limitInput = view.findViewById<EditText>(R.id.setSessionLimit)
        val reminderInput = view.findViewById<EditText>(R.id.setReminder)
        val waSpinner = view.findViewById<Spinner>(R.id.setWa)

        autoAdvance.isChecked = BatchStore.autoAdvance(this)
        limitInput.setText(BatchStore.sessionLimit(this).toString())
        reminderInput.setText(BatchStore.reminderEvery(this).toString())

        val chats = ChatIntents.installedChats(this)
        val labels = ArrayList<String>()
        val values = ArrayList<String>()
        if (chats.contains(ChatIntents.PKG_WA)) {
            labels.add(getString(R.string.whatsapp_name))
            values.add(ChatIntents.PKG_WA)
        }
        if (chats.contains(ChatIntents.PKG_W4B)) {
            labels.add(getString(R.string.whatsapp_business_name))
            values.add(ChatIntents.PKG_W4B)
        }
        labels.add(getString(R.string.chat_default))
        values.add("default")

        waSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels,
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val stored = BatchStore.waPackage(this)
        val storedIndex = values.indexOf(stored)
        waSpinner.setSelection(if (storedIndex >= 0) storedIndex else values.indexOf("default"))

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.batch_settings_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val limit = limitInput.text.toString().toIntOrNull()
                if (limit == null || limit !in 1..500) {
                    toast(R.string.batch_settings_invalid_limit)
                    return@setOnClickListener
                }
                val reminder = reminderInput.text.toString().toIntOrNull()
                if (reminder == null || reminder !in 0..500) {
                    toast(R.string.batch_settings_invalid_reminder)
                    return@setOnClickListener
                }
                BatchStore.setAutoAdvance(this, autoAdvance.isChecked)
                BatchStore.setSessionLimit(this, limit)
                BatchStore.setReminderEvery(this, reminder)
                BatchStore.setWaPackage(this, values[waSpinner.selectedItemPosition])
                toast(R.string.batch_settings_saved)
                refreshAll()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun exportReport() {
        if (contacts.isEmpty()) {
            toast(R.string.batch_export_empty)
            return
        }
        val csv = buildReport()
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_export_title)
            .setItems(
                arrayOf(
                    getString(R.string.batch_export_share),
                    getString(R.string.batch_export_save),
                ),
            ) { _, which ->
                if (which == 0) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.batch_export_subject))
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.batch_export_title)))
                } else {
                    saveReportFile(csv)
                }
            }
            .show()
    }

    private fun buildReport(): String {
        val builder = StringBuilder()
        builder.append(getString(R.string.batch_export_header_name)).append(';')
            .append(getString(R.string.batch_export_header_phone)).append(';')
            .append(getString(R.string.batch_export_header_status)).append('\n')
        for (contact in contacts) {
            builder.append(Batch.csv(contact.fullName)).append(';')
                .append(Batch.csv(Phone.pretty(contact.phone))).append(';')
                .append(Batch.csv(getString(Batch.statusLabel(contact.status)))).append('\n')
        }
        return builder.toString()
    }

    private fun saveReportFile(csv: String) {
        pendingReport = csv
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "noadd-lote.csv")
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_SAVE_REPORT)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.batch_export_failed)
        }
    }

    private fun saveReport(uri: Uri?) {
        if (uri == null) return
        try {
            contentResolver.openOutputStream(uri)?.use {
                it.write(pendingReport.toByteArray(Charsets.UTF_8))
            }
            toast(R.string.batch_export_saved)
        } catch (e: Exception) {
            toast(R.string.batch_export_failed)
        }
    }

    private fun confirmClearList() {
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_clear_list_title)
            .setMessage(R.string.batch_clear_list_message)
            .setPositiveButton(R.string.batch_clear_list_action) { _, _ ->
                contacts.clear()
                review.clear()
                training = null
                previewIndex = 0
                BatchStore.clearList(this)
                importSummary.visibility = View.GONE
                showStep(STEP_IMPORT)
                refreshAll()
                toast(R.string.batch_cleared)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_clear_all_title)
            .setMessage(R.string.batch_clear_all_message)
            .setPositiveButton(R.string.batch_clear_all_action) { _, _ ->
                BatchStore.clearAll(this)
                contacts.clear()
                review.clear()
                training = null
                previewIndex = 0
                BatchStore.setMessage(this, defaultTemplate())
                BatchStore.setMessageSeeded(this, true)
                messageInput.setText(defaultTemplate())
                importSummary.visibility = View.GONE
                showStep(STEP_IMPORT)
                refreshAll()
                toast(R.string.batch_cleared)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDisclaimer() {
        AlertDialog.Builder(this)
            .setTitle(R.string.batch_disclaimer_title)
            .setMessage(R.string.batch_disclaimer_message)
            .setPositiveButton(R.string.batch_disclaimer_ok) { _, _ ->
                BatchStore.setDisclaimerSeen(this, true)
            }
            .setNegativeButton(R.string.batch_disclaimer_cancel) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    // ----- Utilidades -----

    private fun persistList() {
        BatchStore.saveContacts(this, contacts)
        BatchStore.saveReview(this, review)
    }

    private fun field(input: EditText): View = FrameLayout(this).apply {
        setPadding(dp(20), dp(8), dp(20), 0)
        addView(input)
    }

    private fun fieldLabel(text: String, forId: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(themeColor(R.attr.noAddInkMuted))
        labelFor = forId
        setPadding(0, dp(10), 0, 0)
    }

    private fun themeColor(attr: Int): Int {
        val typed = obtainStyledAttributes(intArrayOf(attr))
        val color = typed.getColor(0, Color.WHITE)
        typed.recycle()
        return color
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(textRes: Int) {
        Toast.makeText(this, textRes, Toast.LENGTH_SHORT).show()
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private inner class ContactAdapter : BaseAdapter() {
        override fun getCount(): Int = contacts.size
        override fun getItem(position: Int): BatchContact = contacts[position]
        override fun getItemId(position: Int): Long = contacts[position].id.hashCode().toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView
                ?: layoutInflater.inflate(R.layout.item_batch_contact, parent, false)
            val contact = contacts[position]
            val pretty = Phone.pretty(contact.phone)
            val name = view.findViewById<TextView>(R.id.contactName)
            val phone = view.findViewById<TextView>(R.id.contactPhone)
            val status = view.findViewById<TextView>(R.id.contactStatus)

            name.text = contact.fullName.ifBlank { pretty }
            phone.text = if (contact.fullName.isBlank()) "" else pretty
            phone.visibility = if (phone.text.isBlank()) View.GONE else View.VISIBLE
            status.text = getString(Batch.statusLabel(contact.status))
            status.setTextColor(
                if (contact.status == BatchStatus.SENT || contact.status == BatchStatus.OPENED) {
                    themeColor(R.attr.noAddAccent)
                } else {
                    themeColor(R.attr.noAddInkMuted)
                },
            )
            view.contentDescription = getString(
                R.string.batch_contact_desc,
                name.text,
                pretty,
                status.text,
            )
            return view
        }
    }
}

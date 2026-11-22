package com.noadd.app

import java.text.Normalizer
import java.util.Locale
import java.util.UUID

/** Status de um contato dentro da fila do lote. */
enum class BatchStatus { PENDING, OPENED, SENT, SKIPPED }

/** Um contato da fila. O telefone fica em E.164 sem o "+". */
data class BatchContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val status: BatchStatus = BatchStatus.PENDING,
    val surname: String = "",
) {
    /** "Nome Sobrenome", sem espaços sobrando quando falta uma das partes. */
    val fullName: String
        get() = listOf(name.trim(), surname.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}

/** Um número que não pôde ser lido e ficou para revisão. */
data class ReviewContact(
    val id: String = UUID.randomUUID().toString(),
    val raw: String,
    val reason: String,
)

/** Resultado da leitura de um texto/arquivo, antes de juntar com a lista atual. */
data class ParsedImport(
    val contacts: List<ParsedContact>,
    val invalid: List<ParsedInvalid>,
    val duplicates: Int,
    val rows: Int,
)

data class ParsedContact(val name: String, val surname: String, val phone: String)
data class ParsedInvalid(val raw: String, val reason: String)

/**
 * Regras puras do lote: leitura de CSV/TXT/colagem do Excel, personalização da
 * mensagem e formatação do relatório. Sem Android, para poder ser testado à parte.
 */
object Batch {

    const val REASON_EMPTY = "empty"
    const val REASON_INVALID = "invalid"

    /**
     * Aceita:
     * - uma linha por telefone;
     * - "nome;telefone" ou "telefone;nome", com ";" ou ",";
     * - colunas coladas do Excel (tabulação);
     * - várias linhas, com ou sem cabeçalho.
     */
    fun parse(text: String, country: Country): ParsedImport {
        val clean = text
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val lines = clean.split('\n')
        val delimiter = detectDelimiter(lines)

        val contacts = ArrayList<ParsedContact>()
        val invalid = ArrayList<ParsedInvalid>()
        val seen = HashSet<String>()
        var duplicates = 0
        var rows = 0
        var firstContent = true
        var columns: ColumnMap? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith('#')) continue

            // Mantém a posição das colunas para casar com o cabeçalho.
            val fields = if (delimiter == null) {
                listOf(line)
            } else {
                splitRecord(line, delimiter).map { it.trim() }
            }
            val values = fields.filter { it.isNotEmpty() }

            // Cabeçalho comum ("Nome;Telefone"): a primeira linha sem nenhum
            // dígito é descartada, desde que exista mais conteúdo depois.
            if (firstContent) {
                firstContent = false
                if (line.none { it.isDigit() } &&
                    lines.count { it.isNotBlank() } > 1 &&
                    looksLikeHeader(line)
                ) {
                    columns = mapColumns(fields)
                    continue
                }
            }

            rows++
            if (values.isEmpty()) {
                invalid.add(ParsedInvalid(line, REASON_EMPTY))
                continue
            }

            val phoneFields = values.filter { looksLikePhone(it) }
            if (phoneFields.isEmpty()) {
                invalid.add(ParsedInvalid(line, REASON_INVALID))
                continue
            }

            // "11999999999,11988888888": todos os campos são telefones.
            val multiplePhones = values.size > 1 && values.all { looksLikePhone(it) }
            val mappedPhone = columns?.phoneIndex
                ?.let { fields.getOrNull(it) }
                ?.takeIf { looksLikePhone(it) }
            val chosen = when {
                mappedPhone != null -> listOf(mappedPhone)
                multiplePhones -> values
                else -> listOf(phoneFields.maxByOrNull { it.count { ch -> ch.isDigit() } }!!)
            }
            // Nome e sobrenome saem das colunas do cabeçalho ("Nome",
            // "Sobrenome"); sem cabeçalho, o texto com letras é dividido.
            val mappedGiven = columns?.givenIndexes.orEmpty()
                .mapNotNull { fields.getOrNull(it) }
                .joinToString(" ")
                .let(::cleanName)
            val mappedFamily = columns?.familyIndexes.orEmpty()
                .mapNotNull { fields.getOrNull(it) }
                .joinToString(" ")
                .let(::cleanName)
            val parts: Pair<String, String> = when {
                multiplePhones && mappedPhone == null -> "" to ""
                mappedFamily.isNotEmpty() -> mappedGiven to mappedFamily
                mappedGiven.isNotEmpty() -> splitName(mappedGiven)
                else -> splitName(
                    cleanName(
                        values.firstOrNull {
                            it != chosen[0] && it.any { ch -> ch.isLetter() }
                        }.orEmpty(),
                    ),
                )
            }

            for (field in chosen) {
                val e164 = Phone.normalize(field, country)
                if (e164.isEmpty()) {
                    invalid.add(ParsedInvalid(field, REASON_EMPTY))
                    continue
                }
                if (!Phone.looksValid(e164)) {
                    invalid.add(ParsedInvalid(field, REASON_INVALID))
                    continue
                }
                if (!seen.add(e164)) {
                    duplicates++
                    continue
                }
                contacts.add(ParsedContact(parts.first, parts.second, e164))
            }
        }

        return ParsedImport(contacts, invalid, duplicates, rows)
    }

    /** Troca {nome} e {primeiro_nome}; se não houver nome, limpa espaços órfãos. */
    fun personalize(template: String, contact: BatchContact): String {
        val full = contact.fullName.trim()
        val first = firstName(full)
        var out = template
            .replace(Regex("\\{nome\\}", RegexOption.IGNORE_CASE), full)
            .replace(Regex("\\{primeiro_nome\\}", RegexOption.IGNORE_CASE), first)
        if (full.isEmpty()) {
            out = out
                .replace(Regex("[ \\t]+([,.;:!?])"), "$1")
                .replace(Regex("([,;:])\\s*[,;:]+"), "$1")
                .replace(Regex(",[ \\t]*([!?.])"), "$1")
                .replace(Regex("[ \\t]{2,}"), " ")
                .replace(Regex("^[\\s,;:]+"), "")
                .trim()
                .trimEnd(',', ';')
        }
        return out
    }

    fun firstName(name: String): String =
        name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()

    /** Separa um nome completo em nome e sobrenome, cortando no último espaço. */
    fun splitName(full: String): Pair<String, String> {
        val value = cleanName(full)
        if (value.isEmpty()) return "" to ""
        val cut = value.lastIndexOf(' ')
        if (cut <= 0) return value to ""
        return value.substring(0, cut).trim() to value.substring(cut + 1).trim()
    }

    /** Tamanho do link final depois do URL-encode (o WhatsApp aceita ~2000). */
    fun linkLength(contact: BatchContact, message: String): Int =
        ChatIntents.link(contact.phone, personalize(message, contact)).length

    fun statusLabel(status: BatchStatus): Int = when (status) {
        BatchStatus.PENDING -> R.string.batch_status_pending
        BatchStatus.OPENED -> R.string.batch_status_opened
        BatchStatus.SENT -> R.string.batch_status_sent
        BatchStatus.SKIPPED -> R.string.batch_status_skipped
    }

    fun reasonLabel(reason: String): Int = when (reason) {
        REASON_EMPTY -> R.string.batch_review_reason_empty
        else -> R.string.batch_review_reason_invalid
    }

    /** Campo CSV com aspas apenas quando necessário. */
    fun csv(value: String): String {
        val needsQuotes = value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }

    // ----- Leitura -----

    internal fun detectDelimiter(lines: List<String>): Char? {
        val sample = lines.asSequence().filter { it.isNotBlank() }.take(5).toList()
        if (sample.isEmpty()) return null
        var best: Char? = null
        var bestCount = 0
        for (candidate in charArrayOf('\t', ';', ',')) {
            var count = 0
            for (line in sample) count += countOutsideQuotes(line, candidate)
            if (count > bestCount) {
                best = candidate
                bestCount = count
            }
        }
        return best
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == delimiter && !inQuotes) {
                count++
            }
            i++
        }
        return count
    }

    /** Divide uma linha respeitando campos entre aspas (CSV). */
    internal fun splitRecord(line: String, delimiter: Char): List<String> {
        val out = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == delimiter && !inQuotes) {
                out.add(field.toString())
                field.setLength(0)
            } else {
                field.append(ch)
            }
            i++
        }
        out.add(field.toString())
        return out
    }

    private fun looksLikePhone(field: String): Boolean {
        if (field.any { it.isLetter() }) return false
        return field.count { it.isDigit() } >= 6
    }

    private val HEADER_WORDS = listOf(
        "nome", "name", "nombre",
        "telefone", "telefono", "phone", "celular", "movil", "fone",
        "contato", "contacto", "contact", "numero", "whatsapp", "email",
    )

    private val PHONE_HEADER_WORDS = listOf(
        "telefone", "telefono", "phone", "celular", "movil", "fone",
        "whatsapp", "numero", "number", "tel", "contato", "contacto", "contact",
    )

    private val NAME_HEADER_WORDS = listOf(
        "nome", "name", "nombre", "sobrenome", "surname", "apellido",
        "first", "last", "primeiro", "segundo",
    )

    private val SURNAME_HEADER_WORDS = listOf(
        "sobrenome", "surname", "last", "apellido", "segundo",
    )

    private data class ColumnMap(
        val givenIndexes: List<Int>,
        val familyIndexes: List<Int>,
        val phoneIndex: Int?,
    )

    /**
     * Usa os nomes do cabeçalho para achar as colunas certas. Assim uma planilha
     * com "Nome;Sobrenome;Telefone" vira "Nome Sobrenome", mesmo que a ordem mude.
     */
    private fun mapColumns(fields: List<String>): ColumnMap {
        val keys = fields.map { headerKey(it) }
        val phoneIndex = keys.indexOfFirst { key -> matches(key, PHONE_HEADER_WORDS) }
            .takeIf { it >= 0 }
        val candidates = keys.indices.filter { i ->
            i != phoneIndex && matches(keys[i], NAME_HEADER_WORDS)
        }
        val given = candidates.filter { !isSurname(keys[it]) }
        val family = candidates.filter { isSurname(keys[it]) }
        return ColumnMap(given, family, phoneIndex)
    }

    private fun matches(key: String, words: List<String>): Boolean {
        if (key.isEmpty()) return false
        return key.split(' ').any { token ->
            words.any { word -> token == word || token.startsWith(word) }
        }
    }

    private fun isSurname(key: String): Boolean = matches(key, SURNAME_HEADER_WORDS)

    internal fun headerKey(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun looksLikeHeader(line: String): Boolean {
        val normalized = Normalizer.normalize(line, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
        return HEADER_WORDS.any { normalized.contains(it) }
    }

    private fun cleanName(raw: String): String {
        val value = raw.trim().replace(Regex("\\s+"), " ")
        return if (value.any { it.isLetter() }) value else ""
    }
}

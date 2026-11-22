package com.noadd.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Campos do arquivo de treinamento, na ordem do resumo. */
enum class TrainingField { PRODUCT, ROLE, ADDRESS, TIME, DATE, PERIOD, INSTRUCTOR }

/** Uma linha do arquivo de treinamento, já com os textos como vieram. */
data class TrainingRow(
    val product: String = "",
    val role: String = "",
    val address: String = "",
    val time: String = "",
    val date: String = "",
    val period: String = "",
    val instructor: String = "",
) {
    fun value(field: TrainingField): String = when (field) {
        TrainingField.PRODUCT -> product
        TrainingField.ROLE -> role
        TrainingField.ADDRESS -> address
        TrainingField.TIME -> time
        TrainingField.DATE -> date
        TrainingField.PERIOD -> period
        TrainingField.INSTRUCTOR -> instructor
    }

    /** Campos vazios, seja por coluna ausente ou por célula em branco. */
    val missing: List<TrainingField>
        get() = TrainingField.values().filter { value(it).isBlank() }
}

/** Resultado da leitura: a linha aplicável, quantas linhas vieram e o que faltou. */
data class ParsedTraining(
    val row: TrainingRow?,
    val rows: Int,
    val missingColumns: List<TrainingField>,
)

/**
 * Leitura do segundo arquivo (dados do treinamento) e o modelo de mensagem do
 * RH. Sem Android e sem rede, para poder ser testado à parte.
 */
object Training {

    /** Modelo aplicado ao carregar a turma; só {nome} continua variável. */
    const val TEMPLATE = "Olá, {nome}, tudo bem? 😊\n" +
        "\n" +
        "Sou do RH da ATENTO! \n" +
        "Seguem as informações sobre seu Treinamento de {cargo}, com o produto {produto}.\n" +
        "\n" +
        "📋 Informações de Início de Trabalho:\n" +
        "\n" +
        "📍 Endereço: {endereco}\n" +
        "🕑 Horário: {hora}\n" +
        "📅 Início do Treinamento: {inicio} \n" +
        "🔄 Período: {periodo}\n" +
        "👤 Procurar por: {instrutor}\n" +
        "🪑 Aguarde na Recepção.\n" +
        "\n" +
        "⚠️ LEVAR RG PARA ENTRADA NO PRÉDIO E SOLICITAR LIBERAÇÃO AO CHEGAR.\n" +
        "\n" +
        "Aguardamos você!"

    /** Nomes das colunas do arquivo, na ordem em que o app as mostra. */
    private val VAR_NAMES = mapOf(
        TrainingField.PRODUCT to "produto",
        TrainingField.ROLE to "cargo",
        TrainingField.ADDRESS to "endereco",
        TrainingField.TIME to "hora",
        TrainingField.DATE to "inicio",
        TrainingField.PERIOD to "periodo",
        TrainingField.INSTRUCTOR to "instrutor",
    )

    private val ALIASES = mapOf(
        TrainingField.PRODUCT to listOf("produto", "product", "curso", "servico"),
        TrainingField.ROLE to listOf("cargo", "funcao", "role", "position", "puesto", "puesto de trabajo"),
        TrainingField.ADDRESS to listOf("endereco", "address", "direccion", "local", "localizacao"),
        TrainingField.TIME to listOf("hora", "horario", "time", "hour"),
        TrainingField.DATE to listOf("data", "date", "fecha", "inicio"),
        TrainingField.PERIOD to listOf("periodo", "period", "turno", "jornada"),
        TrainingField.INSTRUCTOR to listOf("instrutor", "instructor", "facilitador", "responsavel"),
    )

    /** Variáveis conhecidas; qualquer outra fica no texto e é sinalizada. */
    val VARIABLES = listOf(
        "nome", "primeiro_nome", "produto", "cargo",
        "endereco", "hora", "inicio", "periodo", "instrutor",
    )

    /**
     * Lê um CSV/TXT com PRODUTO, CARGO, ENDERECO, HORA, DATA, PERIODO e
     * INSTRUTOR. Aceita `;`, `,` ou tabulação, BOM, cabeçalho sem acento e em
     * qualquer ordem. Nesta versão usa a primeira linha de dados.
     */
    fun parse(text: String): ParsedTraining {
        val clean = text
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val lines = clean.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ParsedTraining(null, 0, emptyList())

        val delimiter = Batch.detectDelimiter(lines) ?: ';'
        val map = mapColumns(Batch.splitRecord(lines[0], delimiter).map { Batch.headerKey(it) })
        val dataLines = if (map != null) lines.drop(1) else lines
        if (dataLines.isEmpty()) return ParsedTraining(null, 0, emptyList())

        val fields = Batch.splitRecord(dataLines[0], delimiter)
        fun value(field: TrainingField): String {
            // Com cabeçalho, campo sem coluna é vazio — nunca a coluna vizinha.
            val index = if (map != null) map[field] else field.ordinal
            return fields.getOrNull(index ?: -1)?.trim().orEmpty()
        }

        val row = TrainingRow(
            product = value(TrainingField.PRODUCT),
            role = value(TrainingField.ROLE),
            address = value(TrainingField.ADDRESS),
            time = value(TrainingField.TIME),
            date = excelDate(value(TrainingField.DATE)),
            period = value(TrainingField.PERIOD),
            instructor = value(TrainingField.INSTRUCTOR),
        )
        val missing = if (map != null) {
            TrainingField.values().filter { !map.containsKey(it) }
        } else {
            emptyList()
        }
        return ParsedTraining(row, dataLines.size, missing)
    }

    /** Preenche o modelo com os dados da turma, mantendo {nome} para cada contato. */
    fun fill(row: TrainingRow, template: String = TEMPLATE): String {
        var out = template
        for (field in TrainingField.values()) {
            val name = VAR_NAMES[field] ?: continue
            val value = row.value(field)
            out = out.replace(Regex("\\{" + name + "\\}", RegexOption.IGNORE_CASE)) { value }
        }
        return out
    }

    /** Variáveis do texto que o app não conhece (continuam como estão). */
    fun unknownVariables(text: String): List<String> =
        Regex("\\{([^{}]+)\\}").findAll(text)
            .map { it.groupValues[1].trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() && it !in VARIABLES }
            .distinct()
            .toList()

    fun label(field: TrainingField): Int = when (field) {
        TrainingField.PRODUCT -> R.string.batch_training_field_product
        TrainingField.ROLE -> R.string.batch_training_field_role
        TrainingField.ADDRESS -> R.string.batch_training_field_address
        TrainingField.TIME -> R.string.batch_training_field_time
        TrainingField.DATE -> R.string.batch_training_field_date
        TrainingField.PERIOD -> R.string.batch_training_field_period
        TrainingField.INSTRUCTOR -> R.string.batch_training_field_instructor
    }

    // ----- Leitura -----

    /**
     * Casa o cabeçalho com os campos, sem diferenciar maiúsculas nem acentos,
     * em qualquer ordem. Devolve null quando não parece haver cabeçalho.
     */
    private fun mapColumns(keys: List<String>): Map<TrainingField, Int>? {
        val used = HashSet<Int>()
        val map = HashMap<TrainingField, Int>()
        for (field in TrainingField.values()) {
            val aliases = ALIASES[field].orEmpty()
            val index = keys.indices.firstOrNull { i ->
                i !in used && matches(keys[i], aliases)
            }
            if (index != null) {
                used.add(index)
                map[field] = index
            }
        }
        return if (map.size >= 2) map else null
    }

    private fun matches(key: String, aliases: List<String>): Boolean {
        if (key.isEmpty()) return false
        return key.split(' ').any { token ->
            aliases.any { alias -> token == alias || token.startsWith(alias) }
        }
    }

    /**
     * DATA é mantida como texto. Só quando vem o número de série do Excel
     * (20000..80000) ela é convertida para dd/MM/aaaa.
     */
    private fun excelDate(raw: String): String {
        val value = raw.trim()
        if (value.isEmpty() || value.any { !it.isDigit() }) return raw
        val serial = value.toLongOrNull() ?: return raw
        if (serial !in 20000L..80000L) return raw
        val millis = (serial - EXCEL_EPOCH_DAY) * MILLIS_PER_DAY
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(millis))
    }

    /** Série 25569 = 01/01/1970 na contagem do Excel (base 30/12/1899). */
    private const val EXCEL_EPOCH_DAY = 25569L
    private const val MILLIS_PER_DAY = 86_400_000L
}

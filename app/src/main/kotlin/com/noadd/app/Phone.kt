package com.noadd.app

/**
 * Normalização de números: aceita o que o usuário colar, aplica DDI do país
 * selecionado, corrige o 9º dígito brasileiro e nunca bloqueia — só avisa.
 */
object Phone {

    /** Número nacional (sem DDI) para mostrar no campo de entrada. */
    fun national(e164: String, country: Country): String =
        if (e164.startsWith(country.dial)) e164.substring(country.dial.length) else e164

    fun normalize(raw: String, country: Country): String {
        val text = raw.trim()
        if (text.isEmpty()) return ""
        var digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        var international = text.startsWith("+") || digits.startsWith("00")
        if (digits.startsWith("00")) digits = digits.substring(2)
        // Zeros à esquerda: tronco (0 antes do DDD) ou digitação com zero.
        while (digits.length > 1 && digits.startsWith('0')) digits = digits.substring(1)
        if (digits.isEmpty()) return ""

        if (!international && digits.startsWith(country.dial)) {
            // Já veio com DDI: DDD + número (mínimo 8 dígitos depois do DDI).
            international = digits.length - country.dial.length >= 8
        }

        val e164 = if (international) digits else country.dial + digits
        return brazilianNine(e164)
    }

    /**
     * Celulares brasileiros antigos (DDD + 8 dígitos, começando por 6-9)
     * recebem o nono dígito; números já com 11 dígitos não são alterados.
     */
    private fun brazilianNine(e164: String): String {
        if (!e164.startsWith("55")) return e164
        val national = e164.substring(2)
        if (national.length != 10) return e164
        val third = national[2]
        return if (third in '6'..'9') "55" + national.substring(0, 2) + "9" + national.substring(2) else e164
    }

    /** Validação básica de tamanho; nunca impede a abertura. */
    fun looksValid(e164: String): Boolean {
        if (e164.length !in 8..15) return false
        val country = Countries.match(e164) ?: return false
        val national = e164.length - country.dial.length
        if (country.dial == "55") return national == 10 || national == 11
        return national >= 4
    }

    /** Formata para exibição: +55 (11) 99999-9999 quando for Brasil. */
    fun pretty(e164: String): String {
        val country = Countries.match(e164)
        if (country != null) {
            val national = e164.substring(country.dial.length)
            if (country.dial == "55" && national.length == 11) {
                return "+55 (${national.substring(0, 2)}) ${national.substring(2, 7)}-${national.substring(7)}"
            }
            if (country.dial == "55" && national.length == 10) {
                return "+55 (${national.substring(0, 2)}) ${national.substring(2, 6)}-${national.substring(6)}"
            }
            return "+${country.dial} $national"
        }
        return "+$e164"
    }

    private val CANDIDATE = Regex("[+]?\\d[\\d\\s().-]{4,}\\d")

    /** Extrai o número mais provável de um texto compartilhado. */
    fun extract(text: String, country: Country): String {
        val candidates = CANDIDATE.findAll(text).map { it.value.trim() }.toList()
        if (candidates.isEmpty()) return ""
        val withPlus = candidates.firstOrNull { it.startsWith("+") }
        val chosen = withPlus ?: candidates.maxByOrNull { it.count { ch -> ch.isDigit() } } ?: return ""
        return normalize(chosen, country)
    }
}

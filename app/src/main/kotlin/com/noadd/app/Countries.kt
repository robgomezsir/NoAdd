package com.noadd.app

import java.text.Normalizer
import java.util.Locale

/** Um país da lista embutida: ISO-3166 alfa-2, DDI e nome em português. */
class Country(val iso: String, val dial: String, val name: String) {

    /** Bandeira em emoji, derivada do código ISO (nada fica salvo em recurso). */
    val flag: String by lazy {
        val sb = StringBuilder()
        for (ch in iso.uppercase(Locale.ROOT)) {
            sb.appendCodePoint(0x1F1E6 + (ch - 'A'))
        }
        sb.toString()
    }

    override fun toString(): String = "$flag  $name  +$dial"
}

/**
 * Lista de países embutida no código para não inflar o APK com recursos.
 * Formato: ISO|DDI|Nome, um por linha.
 */
object Countries {

    private val TABLE = """
        AF|93|Afeganistão
        ZA|27|África do Sul
        AL|355|Albânia
        DE|49|Alemanha
        AD|376|Andorra
        AO|244|Angola
        AI|1264|Anguila
        AG|1268|Antígua e Barbuda
        SA|966|Arábia Saudita
        DZ|213|Argélia
        AR|54|Argentina
        AM|374|Armênia
        AW|297|Aruba
        AU|61|Austrália
        AT|43|Áustria
        AZ|994|Azerbaijão
        BS|1242|Bahamas
        BD|880|Bangladesh
        BB|1246|Barbados
        BH|973|Bahrein
        BY|375|Bielorrússia
        BE|32|Bélgica
        BZ|501|Belize
        BJ|229|Benin
        BM|1441|Bermudas
        BO|591|Bolívia
        BA|387|Bósnia e Herzegovina
        BW|267|Botsuana
        BR|55|Brasil
        BN|673|Brunei
        BG|359|Bulgária
        BF|226|Burkina Faso
        BI|257|Burundi
        BT|975|Butão
        CV|238|Cabo Verde
        CM|237|Camarões
        KH|855|Camboja
        CA|1|Canadá
        QA|974|Catar
        KZ|7|Cazaquistão
        TD|235|Chade
        CL|56|Chile
        CN|86|China
        CO|57|Colômbia
        KM|269|Comores
        CG|242|Congo
        CD|243|Congo (RD)
        KP|850|Coreia do Norte
        KR|82|Coreia do Sul
        CI|225|Costa do Marfim
        CR|506|Costa Rica
        HR|385|Croácia
        CU|53|Cuba
        CW|599|Curaçao
        DK|45|Dinamarca
        DJ|253|Djibuti
        DM|1767|Dominica
        EG|20|Egito
        SV|503|El Salvador
        AE|971|Emirados Árabes Unidos
        EC|593|Equador
        ER|291|Eritreia
        SK|421|Eslováquia
        SI|386|Eslovênia
        ES|34|Espanha
        US|1|Estados Unidos
        EE|372|Estônia
        SZ|268|Essuatíni
        ET|251|Etiópia
        FJ|679|Fiji
        PH|63|Filipinas
        FI|358|Finlândia
        FR|33|França
        GA|241|Gabão
        GM|220|Gâmbia
        GH|233|Gana
        GE|995|Geórgia
        GI|350|Gibraltar
        GD|1473|Granada
        GR|30|Grécia
        GL|299|Groenlândia
        GP|590|Guadalupe
        GU|1671|Guam
        GT|502|Guatemala
        GY|592|Guiana
        GF|594|Guiana Francesa
        GN|224|Guiné
        GW|245|Guiné-Bissau
        GQ|240|Guiné Equatorial
        HT|509|Haiti
        HN|504|Honduras
        HK|852|Hong Kong
        HU|36|Hungria
        YE|967|Iêmen
        IN|91|Índia
        ID|62|Indonésia
        IR|98|Irã
        IQ|964|Iraque
        IE|353|Irlanda
        IS|354|Islândia
        IL|972|Israel
        IT|39|Itália
        JM|1876|Jamaica
        JP|81|Japão
        JO|962|Jordânia
        XK|383|Kosovo
        KW|965|Kuwait
        LA|856|Laos
        LS|266|Lesoto
        LV|371|Letônia
        LB|961|Líbano
        LR|231|Libéria
        LY|218|Líbia
        LI|423|Liechtenstein
        LT|370|Lituânia
        LU|352|Luxemburgo
        MO|853|Macau
        MK|389|Macedônia do Norte
        MG|261|Madagascar
        MY|60|Malásia
        MW|265|Malaui
        MV|960|Maldivas
        ML|223|Mali
        MT|356|Malta
        MA|212|Marrocos
        MQ|596|Martinica
        MU|230|Maurício
        MR|222|Mauritânia
        MX|52|México
        MM|95|Mianmar
        MZ|258|Moçambique
        MD|373|Moldávia
        MC|377|Mônaco
        MN|976|Mongólia
        ME|382|Montenegro
        MS|1664|Montserrat
        NA|264|Namíbia
        NR|674|Nauru
        NP|977|Nepal
        NI|505|Nicarágua
        NE|227|Níger
        NG|234|Nigéria
        NO|47|Noruega
        NC|687|Nova Caledônia
        NZ|64|Nova Zelândia
        OM|968|Omã
        NL|31|Países Baixos
        PW|680|Palau
        PA|507|Panamá
        PG|675|Papua-Nova Guiné
        PK|92|Paquistão
        PY|595|Paraguai
        PE|51|Peru
        PF|689|Polinésia Francesa
        PL|48|Polônia
        PR|1787|Porto Rico
        PT|351|Portugal
        KE|254|Quênia
        KG|996|Quirguistão
        GB|44|Reino Unido
        CF|236|República Centro-Africana
        DO|1809|República Dominicana
        CZ|420|República Tcheca
        RE|262|Reunião
        RO|40|Romênia
        RW|250|Ruanda
        RU|7|Rússia
        KN|1869|São Cristóvão e Névis
        SM|378|San Marino
        ST|239|São Tomé e Príncipe
        VC|1784|São Vicente e Granadinas
        LC|1758|Santa Lúcia
        WS|685|Samoa
        AS|1684|Samoa Americana
        SN|221|Senegal
        SC|248|Seicheles
        SL|232|Serra Leoa
        RS|381|Sérvia
        SG|65|Singapura
        SY|963|Síria
        SO|252|Somália
        LK|94|Sri Lanka
        SD|249|Sudão
        SS|211|Sudão do Sul
        SE|46|Suécia
        CH|41|Suíça
        SR|597|Suriname
        TH|66|Tailândia
        TW|886|Taiwan
        TJ|992|Tajiquistão
        TZ|255|Tanzânia
        TL|670|Timor-Leste
        TG|228|Togo
        TO|676|Tonga
        TT|1868|Trinidad e Tobago
        TN|216|Tunísia
        TM|993|Turcomenistão
        TR|90|Turquia
        TV|688|Tuvalu
        UA|380|Ucrânia
        UG|256|Uganda
        UY|598|Uruguai
        UZ|998|Uzbequistão
        VU|678|Vanuatu
        VA|379|Vaticano
        VE|58|Venezuela
        VN|84|Vietnã
        ZM|260|Zâmbia
        ZW|263|Zimbábue
    """.trimIndent()

    /** Nome sem acentos e em minúsculas, para busca e ordenação. */
    fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)

    val all: List<Country> by lazy {
        TABLE.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { row ->
                val parts = row.split('|')
                if (parts.size < 3) null else Country(parts[0], parts[1], parts[2])
            }
            .sortedWith(compareBy({ fold(it.name) }, { it.dial }))
            .toList()
    }

    val default: Country by lazy { byIso("BR") ?: all.first() }

    fun byIso(iso: String): Country? = all.firstOrNull { it.iso.equals(iso, true) }

    /** País cujo DDI é o prefixo mais longo do número internacional. */
    fun match(e164: String): Country? {
        var best: Country? = null
        for (c in all) {
            if (!e164.startsWith(c.dial)) continue
            val current = best
            if (current == null || c.dial.length > current.dial.length) best = c
        }
        return best
    }

    fun search(query: String): List<Country> {
        val q = fold(query.trim())
        if (q.isEmpty()) return all
        return all.filter { fold(it.name).contains(q) || it.dial.startsWith(q) || it.iso.equals(q, true) }
    }
}

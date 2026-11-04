package br.com.alertasilencioso.core

object PhoneNumbers {
    private const val KEY_DIGITS = 11

    // No Brasil o SMS vai para celular, e celular tem 11 dígitos: DDD (2) + 9 + número (8).
    // Quem recebe o alerta precisa ser celular -- fixo não recebe SMS. Dois erros reais já derrubaram
    // o envio aqui: faltar o DDD (a operadora não entrega) e faltar o 9 do celular (a Claro devolve
    // "inclua o 9 na frente do número"). Por isso exigimos os 11 dígitos.
    private const val MIN_BR_DIGITS = 11
    private const val MIN_INTL_DIGITS = 11

    /** Deixa só o "+" inicial e os dígitos, que é o que o envio de SMS precisa. */
    fun clean(raw: String): String {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+") && digits.isNotEmpty()) "+$digits" else digits
    }

    /** Chave para reconhecer o mesmo número escrito de jeitos diferentes ("+55 31 9..." e "(31) 9..."). */
    fun key(raw: String): String = raw.filter { it.isDigit() }.takeLast(KEY_DIGITS)

    /** Verdadeiro quando é um número de OUTRO país (tem + e não começa com o DDI 55). */
    private fun ehInternacional(raw: String): Boolean {
        val trimmed = raw.trim()
        return trimmed.startsWith("+") && !trimmed.filter { it.isDigit() }.startsWith("55")
    }

    /**
     * Só os dígitos NACIONAIS (DDD + número), tirando o DDI 55 quando ele veio junto.
     * Assim "+55 31 98909-2272" e "(31) 98909-2272" viram os mesmos 11 dígitos — e dá para flagrar
     * um +55 que está sem o 9 do celular (ex.: "+55 31 9348-3704").
     */
    private fun nacionais(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.startsWith("55") && d.length >= 12) d.substring(2) else d
    }

    /** Dá para tentar enviar? Internacional (outro país) com + e >= 11 dígitos; no Brasil, celular = 11. */
    fun isPlausible(raw: String): Boolean {
        if (ehInternacional(raw)) return raw.count { it.isDigit() } >= MIN_INTL_DIGITS
        return nacionais(raw).length == MIN_BR_DIGITS
    }

    /**
     * Aviso quando o número parece errado, para explicar antes de a pessoa salvar. Pega os dois erros que
     * já derrubaram o envio de verdade -- faltar o DDD e faltar o 9 do celular -- inclusive quando o
     * número vem com o +55 na frente (que antes escondia o problema).
     */
    fun formatWarning(raw: String): String? {
        if (raw.trim().isEmpty() || ehInternacional(raw)) return null
        return when (nacionais(raw).length) {
            11 -> null
            10 -> "Parece faltar o 9 do celular. Hoje o celular tem 11 dígitos (DDD + 9): (31) 99999-8888."
            in 8..9 -> "Parece faltar o DDD. Escreva com DDD, assim: (31) 99999-8888."
            in 1..7 -> "Número curto demais. Confira o DDD e o número."
            else -> "Número com dígitos demais. Confira."
        }
    }

    /**
     * Padroniza para GUARDAR: só os dígitos nacionais (sem +55, sem traço/parênteses/espaço) e, quando é
     * celular de 10 dígitos (DDD + número começando em 6-9), ACRESCENTA o 9 sozinho. Número de outro país
     * (com + e sem 55) fica como "+dígitos". Assim o que vem da agenda/WhatsApp já sai arrumado e igual.
     */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (ehInternacional(trimmed)) return "+" + trimmed.filter { it.isDigit() }
        var d = nacionais(trimmed)
        if (d.length == 10 && d[2] in '6'..'9') d = d.substring(0, 2) + "9" + d.substring(2)
        return d
    }

    /** Mostra sempre igual: "(31) 99999-8888". Internacional ou incompleto: mostra como está. */
    fun format(raw: String): String {
        val trimmed = raw.trim()
        if (ehInternacional(trimmed)) return trimmed
        val d = normalize(trimmed)
        return when (d.length) {
            11 -> "(${d.substring(0, 2)}) ${d.substring(2, 7)}-${d.substring(7)}"
            10 -> "(${d.substring(0, 2)}) ${d.substring(2, 6)}-${d.substring(6)}"
            else -> if (d.isEmpty()) trimmed else d
        }
    }
}

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

    /** Dá para tentar enviar? Número internacional (com +) ou pelo menos DDD + número no Brasil. */
    fun isPlausible(raw: String): Boolean {
        val trimmed = raw.trim()
        val digits = trimmed.count { it.isDigit() }
        return if (trimmed.startsWith("+")) digits >= MIN_INTL_DIGITS else digits >= MIN_BR_DIGITS
    }

    /**
     * Aviso quando o número parece errado, para explicar antes de a pessoa salvar. Os dois erros que já
     * derrubaram o envio de verdade: faltar o DDD (8-9 dígitos) e faltar o 9 do celular (10 dígitos, o
     * formato antigo). Em ambos a operadora recusa e o alerta não chega.
     */
    fun formatWarning(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("+")) return null
        val digits = trimmed.count { it.isDigit() }
        return when {
            digits == 10 -> "Parece faltar o 9 do celular. Hoje o celular tem 11 dígitos (DDD + 9): (31) 99999-8888."
            digits in 8..9 -> "Parece faltar o DDD. Escreva com DDD, assim: (31) 99999-8888."
            digits in 1..7 -> "Número curto demais. Confira o DDD e o número."
            else -> null
        }
    }
}

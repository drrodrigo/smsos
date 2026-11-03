package br.com.alertasilencioso.core

object PhoneNumbers {
    private const val KEY_DIGITS = 11

    // No Brasil o SMS precisa de DDD + número: celular tem 11 dígitos (DDD + 9) e o fixo antigo 10.
    // Número sem DDD não é entregue pela operadora -- foi o que fez os testes falharem.
    private const val MIN_BR_DIGITS = 10
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
     * Aviso quando o número parece errado, para explicar antes de a pessoa salvar. O caso mais comum e
     * mais perigoso é faltar o DDD: 8 ou 9 dígitos é só o número, sem a cidade, e o SMS não chega.
     */
    fun formatWarning(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("+")) return null
        val digits = trimmed.count { it.isDigit() }
        return when {
            digits in 8..9 -> "Parece faltar o DDD. Escreva com DDD, assim: (31) 99999-8888."
            digits in 1..7 -> "Número curto demais. Confira o DDD e o número."
            else -> null
        }
    }
}

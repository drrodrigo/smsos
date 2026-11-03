package br.com.alertasilencioso.core

object PhoneNumbers {
    private const val KEY_DIGITS = 11
    private const val MIN_DIGITS = 8

    /** Deixa só o "+" inicial e os dígitos, que é o que o envio de SMS precisa. */
    fun clean(raw: String): String {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+") && digits.isNotEmpty()) "+$digits" else digits
    }

    /** Chave para reconhecer o mesmo número escrito de jeitos diferentes ("+55 31 9..." e "(31) 9..."). */
    fun key(raw: String): String = raw.filter { it.isDigit() }.takeLast(KEY_DIGITS)

    fun isPlausible(raw: String): Boolean = raw.count { it.isDigit() } >= MIN_DIGITS
}

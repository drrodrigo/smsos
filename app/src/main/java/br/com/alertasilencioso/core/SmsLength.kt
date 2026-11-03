package br.com.alertasilencioso.core

/** Estimativa de quantos SMS uma mensagem ocupa (cada contato recebe essa quantidade). */
object SmsLength {
    private const val GSM_SINGLE = 160
    private const val GSM_PART = 153
    private const val UNICODE_SINGLE = 70
    private const val UNICODE_PART = 67

    // Estes caracteres existem no SMS comum, mas cada um conta como dois.
    private const val GSM_EXTENDED = "^{}\\[~]|€"

    fun parts(text: String): Int {
        if (text.isEmpty()) return 1
        val gsm = text.all { (it.code in 32..126 && it != '`') || it == '\n' || it in GSM_EXTENDED }
        val length = if (gsm) text.length + text.count { it in GSM_EXTENDED } else text.length
        val (single, part) = if (gsm) GSM_SINGLE to GSM_PART else UNICODE_SINGLE to UNICODE_PART
        return if (length <= single) 1 else (length + part - 1) / part
    }
}

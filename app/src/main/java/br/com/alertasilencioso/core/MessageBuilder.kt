package br.com.alertasilencioso.core

import br.com.alertasilencioso.data.Profile
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Monta os textos dos SMS. Os acentos são retirados para cada mensagem caber em um SMS só (160 letras). */
object MessageBuilder {
    const val DEFAULT_TEMPLATE =
        "ALERTA! {nome} acionou o botao de emergencia e pode estar em perigo. Local: {mapa} - {hora}"
    const val HEALTH_TEMPLATE =
        "ALERTA DE SAUDE! {nome} apertou o botao e pode estar passando mal. Local: {mapa} - {hora}"
    const val PLACEHOLDER_HELP =
        "{nome} = seu nome · {mapa} = link do mapa · {hora} = data e hora · {lugar} = lugar cadastrado"

    private const val FALLBACK_NAME = "Um contato seu"
    private const val NO_LOCATION = "localizacao indisponivel"
    private const val UNKNOWN_PLACE = "local nao cadastrado"
    private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")
    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    fun defaultTemplate(profile: Profile): String =
        if (profile == Profile.SAUDE) HEALTH_TEMPLATE else DEFAULT_TEMPLATE

    fun alert(
        template: String,
        name: String,
        position: GeoPoint?,
        placeNames: List<String>,
        timeMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = forSms(
        template.ifBlank { DEFAULT_TEMPLATE }
            .replace("{nome}", displayName(name))
            .replace("{mapa}", position?.let(::mapLink) ?: NO_LOCATION)
            .replace("{hora}", formatDateTime(timeMillis, zone))
            .replace("{lugar}", placeNames.joinToString(", ").ifEmpty { UNKNOWN_PLACE })
    )

    fun followUp(
        name: String,
        position: GeoPoint?,
        index: Int,
        total: Int,
        timeMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = forSms(
        "ATUALIZACAO $index/$total - ${displayName(name)}: localizacao atual " +
            "${position?.let(::mapLink) ?: NO_LOCATION} - ${formatTime(timeMillis, zone)}"
    )

    fun allClear(name: String, timeMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String = forSms(
        "${displayName(name)} avisou que esta tudo bem. Pode desconsiderar o alerta de emergencia. " +
            "- ${formatTime(timeMillis, zone)}"
    )

    fun testSms(): String =
        "Teste do SMSOS: se voce recebeu esta mensagem, o envio de SMS esta funcionando. Pode ignorar."

    fun mapLink(p: GeoPoint): String =
        String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", p.latitude, p.longitude)

    fun forSms(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

    fun formatDateTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(DATE_TIME)

    fun formatTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(TIME)

    private fun displayName(name: String) = name.trim().ifEmpty { FALLBACK_NAME }
}

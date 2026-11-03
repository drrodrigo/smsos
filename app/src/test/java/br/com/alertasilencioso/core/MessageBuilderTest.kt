package br.com.alertasilencioso.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MessageBuilderTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val time = ZonedDateTime.of(2026, 11, 3, 22, 41, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `mensagem padrao com nome, mapa e hora cabe em um SMS`() {
        val text = MessageBuilder.alert(
            MessageBuilder.DEFAULT_TEMPLATE, "João", GeoPoint(-19.468301, -42.536702), listOf("Casa"), time, zone,
        )
        assertEquals(
            "ALERTA! Joao acionou o botao de emergencia e pode estar em perigo. " +
                "Local: https://maps.google.com/?q=-19.468301,-42.536702 - 03/11 22:41",
            text,
        )
        assertEquals(1, SmsLength.parts(text))
    }

    @Test
    fun `sem localizacao e sem nome usa textos de reserva`() {
        val text = MessageBuilder.alert("{nome} em {lugar}: {mapa}", "  ", null, emptyList(), time, zone)
        assertEquals("Um contato seu em local nao cadastrado: localizacao indisponivel", text)
    }

    @Test
    fun `modelo vazio volta para a mensagem padrao`() {
        val text = MessageBuilder.alert("", "Ana", null, emptyList(), time, zone)
        assertTrue(text.startsWith("ALERTA! Ana acionou"))
    }

    @Test
    fun `acentos sao removidos`() {
        assertEquals("Atencao: voce esta na Sao Joao", MessageBuilder.forSms("Atenção: você está na São João"))
    }

    @Test
    fun `atualizacao e aviso de tudo bem`() {
        assertEquals(
            "ATUALIZACAO 2/3 - Ana: localizacao atual https://maps.google.com/?q=1.000000,2.000000 - 22:41",
            MessageBuilder.followUp("Ana", GeoPoint(1.0, 2.0), 2, 3, time, zone),
        )
        assertEquals(
            "Ana avisou que esta tudo bem. Pode desconsiderar o alerta de emergencia. - 22:41",
            MessageBuilder.allClear("Ana", time, zone),
        )
    }
}

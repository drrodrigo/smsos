package br.com.alertasilencioso.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmallParsersTest {

    @Test
    fun `coordenadas no formato do Google Maps`() {
        assertEquals(GeoPoint(-19.4683, -42.5367), Coordinates.parse("-19.4683, -42.5367"))
    }

    @Test
    fun `coordenadas dentro de um link`() {
        assertEquals(
            GeoPoint(-23.55052, -46.63331),
            Coordinates.parse("https://www.google.com/maps/@-23.55052,-46.63331,17z"),
        )
    }

    @Test
    fun `coordenadas com virgula decimal`() {
        assertEquals(GeoPoint(-19.4683, -42.5367), Coordinates.parse("-19,4683; -42,5367"))
    }

    @Test
    fun `texto sem coordenadas ou fora do mapa`() {
        assertNull(Coordinates.parse("minha casa"))
        assertNull(Coordinates.parse("95.0, 10.0"))
    }

    @Test
    fun `limpeza e comparacao de telefones`() {
        assertEquals("+5531999998888", PhoneNumbers.clean(" +55 (31) 99999-8888 "))
        assertEquals("31999998888", PhoneNumbers.clean("(31) 99999-8888"))
        assertEquals(PhoneNumbers.key("+55 31 99999-8888"), PhoneNumbers.key("(31) 99999-8888"))
    }

    @Test
    fun `numero so vale com DDD`() {
        // Com DDD (11 dígitos) ou internacional (com +): vale.
        assertTrue(PhoneNumbers.isPlausible("(31) 99476-9224"))
        assertTrue(PhoneNumbers.isPlausible("+1 202 555 0147"))
        // Sem DDD (só o celular) ou curto demais: não vale, e foi o que fez o SMS falhar.
        assertFalse(PhoneNumbers.isPlausible("99476-9224"))
        assertFalse(PhoneNumbers.isPlausible("190"))
        assertNotNull(PhoneNumbers.formatWarning("99476-9224"))
        assertNull(PhoneNumbers.formatWarning("(31) 99476-9224"))
        assertNull(PhoneNumbers.formatWarning(""))
        // Com DDD mas sem o 9 do celular (10 dígitos): a operadora recusa com "inclua o 9".
        assertFalse(PhoneNumbers.isPlausible("(31) 9833-1775"))
        assertNotNull(PhoneNumbers.formatWarning("(31) 9833-1775"))
    }

    @Test
    fun `contagem de SMS`() {
        assertEquals(1, SmsLength.parts("a".repeat(160)))
        assertEquals(2, SmsLength.parts("a".repeat(161)))
        assertEquals(1, SmsLength.parts("ã".repeat(70)))
        assertEquals(2, SmsLength.parts("ã".repeat(71)))
        // Caracteres como "{" contam dobrado.
        assertEquals(2, SmsLength.parts("{".repeat(81)))
    }
}

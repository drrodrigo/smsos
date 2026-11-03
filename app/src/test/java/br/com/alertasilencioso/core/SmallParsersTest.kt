package br.com.alertasilencioso.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(PhoneNumbers.isPlausible("9999-8888"))
        assertFalse(PhoneNumbers.isPlausible("190"))
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

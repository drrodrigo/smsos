package br.com.alertasilencioso.core

import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipientResolverTest {

    private val son = Contact(name = "Filho", phone = "+55 31 98888-0001")
    private val neighborHome = Contact(name = "Vizinho de casa", phone = "(31) 98888-0002")
    private val neighborOffice = Contact(name = "Vizinho do escritório", phone = "31988880003")

    private val home = Place(name = "Casa", latitude = -19.4683, longitude = -42.5367, radiusMeters = 200, contacts = listOf(neighborHome))
    private val office = Place(name = "Escritório", latitude = -19.4900, longitude = -42.5200, radiusMeters = 200, contacts = listOf(neighborOffice))

    @Test
    fun `em casa avisa o filho e os vizinhos de casa`() {
        val r = RecipientResolver.resolve(GeoPoint(-19.4684, -42.5368), 10f, listOf(son), listOf(home, office))
        assertEquals(listOf(son, neighborHome), r.recipients)
        assertEquals(listOf("Casa"), r.places.map { it.name })
        assertTrue(r.locationKnown)
    }

    @Test
    fun `fora de qualquer lugar avisa so quem e sempre avisado`() {
        val r = RecipientResolver.resolve(GeoPoint(-19.6, -42.7), 10f, listOf(son), listOf(home, office))
        assertEquals(listOf(son), r.recipients)
        assertTrue(r.places.isEmpty())
    }

    @Test
    fun `sem localizacao avisa todo mundo`() {
        val r = RecipientResolver.resolve(null, null, listOf(son), listOf(home, office))
        assertEquals(listOf(son, neighborHome, neighborOffice), r.recipients)
        assertFalse(r.locationKnown)
    }

    @Test
    fun `imprecisao do GPS amplia a area no maximo pelo tamanho do raio`() {
        // Ponto a cerca de 300 m do centro da casa.
        val point = GeoPoint(-19.4683 + 0.0027, -42.5367)
        assertFalse(RecipientResolver.contains(home, point, 10f))
        assertTrue(RecipientResolver.contains(home, point, 150f))
        // Mesmo com precisão de 5 km, não passa de 2x o raio (400 m).
        val far = GeoPoint(-19.4683 + 0.0050, -42.5367)
        assertFalse(RecipientResolver.contains(home, far, 5_000f))
    }

    @Test
    fun `lugar sem localizacao marcada nunca combina com uma posicao`() {
        val unmarked = Place(name = "Sem GPS", contacts = listOf(neighborHome))
        assertFalse(RecipientResolver.contains(unmarked, GeoPoint(0.0, 0.0), 10f))
    }

    @Test
    fun `mesmo numero escrito de formas diferentes recebe uma vez so`() {
        val sameAsSon = Contact(name = "Filho (trabalho)", phone = "031 98888-0001")
        val place = home.copy(contacts = listOf(sameAsSon, neighborHome))
        val r = RecipientResolver.resolve(GeoPoint(-19.4683, -42.5367), 5f, listOf(son), listOf(place))
        assertEquals(listOf(son, neighborHome), r.recipients)
    }

    @Test
    fun `distancia entre dois pontos conhecidos`() {
        // 0,001 grau de latitude é cerca de 111 m.
        val d = RecipientResolver.distanceMeters(GeoPoint(-19.0, -42.0), GeoPoint(-19.001, -42.0))
        assertEquals(111.2, d, 0.5)
    }
}

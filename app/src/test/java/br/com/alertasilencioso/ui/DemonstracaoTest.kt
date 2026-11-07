package br.com.alertasilencioso.ui

import br.com.alertasilencioso.data.Settings
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A demonstração mostra a mensagem com dados de EXEMPLO e não envia nada (função pura). */
class DemonstracaoTest {

    @Test
    fun `sem nome cadastrado usa um nome de exemplo e mostra o mapa`() {
        val (seguranca, saude) = textosDemo(Settings())
        assertTrue("devia usar um nome de exemplo", seguranca.contains("Maria"))
        assertTrue(saude.contains("Maria"))
        assertTrue("devia mostrar um link de mapa de exemplo", seguranca.contains("maps.google.com"))
    }

    @Test
    fun `seguranca e saude sao textos diferentes`() {
        val (seguranca, saude) = textosDemo(Settings())
        assertNotEquals(seguranca, saude)
        assertTrue(saude.contains("passando mal"))
    }

    @Test
    fun `usa o nome cadastrado quando existe`() {
        val (seguranca, _) = textosDemo(Settings(userName = "Rodrigo"))
        assertTrue(seguranca.contains("Rodrigo"))
    }
}

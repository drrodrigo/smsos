package br.com.alertasilencioso.relogio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** O JSON que o relógio manda e o que ele lê do celular (o lado do celular é o ContratoDoRelogioTest). */
class ContratoTest {

    @Test
    fun `o pedido leva o perfil e o id`() {
        val seg = JSONObject(Contrato.pedido(Perfil.SEGURANCA, "abc"))
        assertEquals("seguranca", seg.getString("perfil"))
        assertEquals("abc", seg.getString("id"))
        assertEquals(setOf("perfil", "id"), seg.keys().asSequence().toSet())

        assertEquals("saude", JSONObject(Contrato.pedido(Perfil.SAUDE, "x")).getString("perfil"))
    }

    @Test
    fun `le a resposta completa do celular`() {
        val r = Contrato.lerResultado("""{"id":"abc","ok":true,"teste":false,"contatos":3}""")
        assertEquals(Resultado("abc", ok = true, teste = false, contatos = 3, erro = null), r)
    }

    @Test
    fun `resposta com erro e sem contatos`() {
        val r = Contrato.lerResultado("""{"id":"abc","ok":false,"teste":true,"erro":"o celular não conseguiu acionar"}""")!!
        assertFalse(r.ok)
        assertEquals(true, r.teste)
        assertNull(r.contatos)
        assertEquals("o celular não conseguiu acionar", r.erro)
    }

    @Test
    fun `contatos e erro nulos viram null`() {
        val r = Contrato.lerResultado("""{"id":"abc","ok":true,"teste":false,"contatos":null,"erro":null}""")!!
        assertNull(r.contatos)
        assertNull(r.erro)
    }

    @Test
    fun `texto que nao e resposta (sem id, sem ok, quebrado) e ignorado`() {
        assertNull(Contrato.lerResultado("isto não é json"))
        assertNull(Contrato.lerResultado("""{"ok":true}"""))
        assertNull(Contrato.lerResultado("""{"id":"abc"}"""))
        assertNull(Contrato.lerResultado(""))
    }
}

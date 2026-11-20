package br.com.alertasilencioso.relogio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A regra do SOS no relógio (Kotlin puro): quando dispara, quando NÃO dispara e que dispara uma vez só. */
class MaquinaDoSosTest {
    private var proximo = 0
    private val maquina = MaquinaDoSos { "pedido-${++proximo}" }

    private fun enviados(efeitos: List<Efeito>) = efeitos.filterIsInstance<Efeito.Enviar>()
    private fun vibracoes(efeitos: List<Efeito>) = efeitos.filterIsInstance<Efeito.Vibrar>().map { it.tipo }

    @Test
    fun `de fabrica - contagem de 3 s, modo discreto ligado e SOS de Seguranca`() {
        val a = Ajustes()
        assertEquals(3, a.contagem)
        assertTrue(a.discreto)
        assertEquals(Perfil.SEGURANCA, a.perfilRapido)
        assertEquals(listOf(0, 3, 5), Ajustes.CONTAGENS)
    }

    @Test
    fun `contagem guardada estranha volta para 3 s`() {
        assertEquals(0, Ajustes.contagemValida(0))
        assertEquals(5, Ajustes.contagemValida(5))
        assertEquals(3, Ajustes.contagemValida(7))
        assertEquals(3, Ajustes.contagemValida(-1))
    }

    @Test
    fun `contagem 0 (na hora) dispara JA ao abrir`() {
        val efeitos = maquina.abrir(Perfil.SEGURANCA, 0, discreto = true)
        assertTrue(maquina.estado is Estado.Enviando)
        assertEquals(1, maquina.disparos)
        assertEquals(listOf(Efeito.Enviar(Perfil.SEGURANCA, "pedido-1")), enviados(efeitos))
        // Na hora: uma vibração só (a do disparo), sem a de "abriu".
        assertEquals(listOf(Vibracao.DISPAROU), vibracoes(efeitos))
    }

    @Test
    fun `3 s (padrao) - conta 3, 2, 1 e dispara sozinho no terceiro segundo`() {
        val abriu = maquina.abrir(Perfil.SEGURANCA, Ajustes().contagem, discreto = true)
        assertEquals(Estado.Contando(Perfil.SEGURANCA, 3, true), maquina.estado)
        assertEquals(listOf(Vibracao.ABRIU), vibracoes(abriu))
        assertTrue(enviados(abriu).isEmpty())

        assertTrue(maquina.tique().isEmpty())
        assertEquals(2, (maquina.estado as Estado.Contando).faltam)
        assertTrue(maquina.tique().isEmpty())
        assertEquals(1, (maquina.estado as Estado.Contando).faltam)
        assertEquals(0, maquina.disparos)

        val disparou = maquina.tique()
        assertTrue(maquina.estado is Estado.Enviando)
        assertEquals(1, maquina.disparos)
        assertEquals(listOf(Efeito.Enviar(Perfil.SEGURANCA, "pedido-1")), enviados(disparou))
        assertEquals(listOf(Vibracao.DISPAROU), vibracoes(disparou))
    }

    @Test
    fun `5 s dispara no quinto segundo`() {
        maquina.abrir(Perfil.SAUDE, 5, discreto = true)
        repeat(4) { maquina.tique() }
        assertEquals(0, maquina.disparos)
        assertEquals(Efeito.Enviar(Perfil.SAUDE, "pedido-1"), enviados(maquina.tique()).single())
    }

    @Test
    fun `cancelar ANTES do fim nao dispara (nem depois, com mais segundos passando)`() {
        maquina.abrir(Perfil.SEGURANCA, 3, discreto = true)
        maquina.tique()
        maquina.tique()
        maquina.cancelar()
        assertEquals(Estado.Escolha, maquina.estado)
        repeat(10) { assertTrue(maquina.tique().isEmpty()) }
        assertEquals(0, maquina.disparos)
    }

    @Test
    fun `dispara UMA vez so - segundo a mais, abrir de novo ou cancelar depois nao mandam outro`() {
        maquina.abrir(Perfil.SEGURANCA, 3, discreto = true)
        repeat(3) { maquina.tique() }
        assertEquals(1, maquina.disparos)

        repeat(10) { assertTrue(enviados(maquina.tique()).isEmpty()) }
        assertTrue(maquina.abrir(Perfil.SEGURANCA, 0, discreto = true).isEmpty())
        assertTrue(maquina.abrir(Perfil.SAUDE, 3, discreto = true).isEmpty())
        maquina.cancelar()
        assertTrue("o pedido que já saiu não volta", maquina.estado is Estado.Enviando)
        assertEquals(1, maquina.disparos)
    }

    @Test
    fun `abrir de novo NO MEIO da contagem nao recomeca nem dispara duas vezes`() {
        maquina.abrir(Perfil.SEGURANCA, 3, discreto = true)
        maquina.tique()
        assertTrue(maquina.abrir(Perfil.SEGURANCA, 3, discreto = true).isEmpty())
        assertEquals(2, (maquina.estado as Estado.Contando).faltam)
        maquina.tique()
        maquina.tique()
        assertEquals(1, maquina.disparos)
    }

    @Test
    fun `modo discreto NAO vibra a cada segundo, o visivel vibra`() {
        maquina.abrir(Perfil.SEGURANCA, 3, discreto = true)
        assertTrue(maquina.tique().isEmpty())

        val visivel = MaquinaDoSos { "x" }
        visivel.abrir(Perfil.SEGURANCA, 3, discreto = false)
        assertEquals(listOf(Vibracao.SEGUNDO), vibracoes(visivel.tique()))
    }

    @Test
    fun `trocar para Saude durante a contagem manda o SOS de Saude`() {
        maquina.abrir(Perfil.SEGURANCA, 3, discreto = false)
        maquina.trocarPerfil()
        maquina.tique()
        maquina.tique()
        assertEquals(Perfil.SAUDE, enviados(maquina.tique()).single().perfil)
    }

    @Test
    fun `o celular respondeu ok - pedido enviado, vibracao dupla`() {
        maquina.abrir(Perfil.SEGURANCA, 0, discreto = true)
        val efeitos = maquina.resposta(Resultado("pedido-1", ok = true, teste = false, contatos = 2, erro = null))
        assertEquals(Desfecho.ENVIADO, (maquina.estado as Estado.Pronto).desfecho)
        assertEquals(listOf(Vibracao.CHEGOU), vibracoes(efeitos))
    }

    @Test
    fun `celular em modo teste, sem contatos ou com erro`() {
        fun desfecho(r: Resultado) = MaquinaDoSos.desfechoDe(r)
        assertEquals(Desfecho.TESTE, desfecho(Resultado("a", ok = true, teste = true, contatos = 3, erro = null)))
        assertEquals(Desfecho.SEM_CONTATOS, desfecho(Resultado("a", ok = true, teste = false, contatos = 0, erro = null)))
        assertEquals(Desfecho.ENVIADO, desfecho(Resultado("a", ok = true, teste = false, contatos = null, erro = null)))
        assertEquals(Desfecho.NAO_SAIU, desfecho(Resultado("a", ok = false, teste = false, contatos = 2, erro = "x")))
        assertEquals(Vibracao.NAO_SAIU, MaquinaDoSos.vibracaoDo(Desfecho.SEM_CONTATOS))
        assertEquals(Vibracao.CHEGOU, MaquinaDoSos.vibracaoDo(Desfecho.TESTE))
    }

    @Test
    fun `resposta de OUTRO pedido e ignorada`() {
        maquina.abrir(Perfil.SEGURANCA, 0, discreto = true)
        assertTrue(maquina.resposta(Resultado("outro", ok = true, teste = false, contatos = 1, erro = null)).isEmpty())
        assertTrue(maquina.estado is Estado.Enviando)
        assertTrue(maquina.semResposta("outro").isEmpty())
        assertTrue(maquina.estado is Estado.Enviando)
    }

    @Test
    fun `sem resposta - NAO saiu (vibracao longa) e Tentar de novo manda outro pedido na hora`() {
        maquina.abrir(Perfil.SAUDE, 0, discreto = true)
        assertEquals(listOf(Vibracao.NAO_SAIU), vibracoes(maquina.semResposta("pedido-1")))
        assertEquals(Desfecho.NAO_SAIU, (maquina.estado as Estado.Pronto).desfecho)

        val denovo = maquina.tentarDeNovo()
        val pedido = enviados(denovo).single()
        assertEquals(Perfil.SAUDE, pedido.perfil)
        assertNotEquals("pedido-1", pedido.id)
        assertEquals(2, maquina.disparos)
        // Resposta atrasada do pedido antigo não vale para o novo.
        assertTrue(maquina.resposta(Resultado("pedido-1", ok = true, teste = false, contatos = 1, erro = null)).isEmpty())
    }

    @Test
    fun `Tentar de novo so existe depois do NAO saiu`() {
        maquina.abrir(Perfil.SEGURANCA, 0, discreto = true)
        maquina.resposta(Resultado("pedido-1", ok = true, teste = false, contatos = 1, erro = null))
        assertTrue(maquina.tentarDeNovo().isEmpty())
        assertEquals(1, maquina.disparos)
    }

    @Test
    fun `depois do resultado, abrir de novo comeca outra contagem`() {
        maquina.abrir(Perfil.SEGURANCA, 0, discreto = true)
        maquina.resposta(Resultado("pedido-1", ok = true, teste = false, contatos = 1, erro = null))
        maquina.abrir(Perfil.SEGURANCA, 3, discreto = true)
        assertEquals(Estado.Contando(Perfil.SEGURANCA, 3, true), maquina.estado)
    }

    @Test
    fun `a bateria - tela acesa SO na contagem e no envio`() {
        assertFalse(MaquinaDoSos.telaAcesa(Estado.Escolha))
        assertTrue(MaquinaDoSos.telaAcesa(Estado.Contando(Perfil.SEGURANCA, 3, true)))
        assertTrue(MaquinaDoSos.telaAcesa(Estado.Enviando(Perfil.SEGURANCA, "a", true)))
        for (d in Desfecho.entries) {
            assertFalse(d.name, MaquinaDoSos.telaAcesa(Estado.Pronto(Perfil.SEGURANCA, d, discreto = true)))
            assertFalse(d.name, MaquinaDoSos.telaAcesa(Estado.Pronto(Perfil.SEGURANCA, d, discreto = false)))
        }
    }

    @Test
    fun `modo discreto - tela escura, menos quando a pessoa precisa ler`() {
        assertTrue(MaquinaDoSos.telaEscura(Estado.Contando(Perfil.SEGURANCA, 3, true)))
        assertFalse(MaquinaDoSos.telaEscura(Estado.Contando(Perfil.SEGURANCA, 3, false)))
        assertTrue(MaquinaDoSos.telaEscura(Estado.Pronto(Perfil.SEGURANCA, Desfecho.ENVIADO, true)))
        assertFalse(MaquinaDoSos.telaEscura(Estado.Pronto(Perfil.SEGURANCA, Desfecho.NAO_SAIU, true)))
        assertFalse(MaquinaDoSos.telaEscura(Estado.Pronto(Perfil.SEGURANCA, Desfecho.SEM_CONTATOS, true)))
        assertFalse(MaquinaDoSos.telaEscura(Estado.Escolha))
    }

    @Test
    fun `vibracoes - dupla curta para chegou, longa para nao saiu`() {
        val chegou = MaquinaDoSos.padrao(Vibracao.CHEGOU)
        assertEquals("duas vibrações", 2, chegou.filterIndexed { i, _ -> i % 2 == 1 }.size)
        assertTrue(chegou.filterIndexed { i, _ -> i % 2 == 1 }.all { it <= 150 })
        val naoSaiu = MaquinaDoSos.padrao(Vibracao.NAO_SAIU)
        assertEquals("uma vibração só", 1, naoSaiu.filterIndexed { i, _ -> i % 2 == 1 }.size)
        assertTrue("longa", naoSaiu[1] >= 800)
    }
}

package br.com.alertasilencioso.alert

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.StateJson
import br.com.alertasilencioso.testutil.RobolectricJarLeak
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Os dois tipos de guardião (21/11): "Guardião SMS" (só o número) e "Guardião com app" (SMS + o celular toca). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SituacaoDoGuardiaoTest {

    @After
    fun fechaJarVazadoDoRobolectric() = RobolectricJarLeak.fechar()

    private val resposta = """
        {"guardioes":[
          {"telefone":"31984491586","status":"aceito","tem_app":true},
          {"telefone":"31988887777","status":"pendente","tem_app":false},
          {"telefone":"31977776666","status":"aceito","tem_app":false}
        ]}
    """.trimIndent()

    @Test
    fun `le a resposta do servidor - aceito com app, pendente e aceito sem aviso`() {
        val m = SituacaoDoGuardiao.lerDoServidor(resposta)!!
        assertEquals(SituacaoDoGuardiao.COM_APP, m["31984491586"])
        assertEquals(SituacaoDoGuardiao.CONVITE_ENVIADO, m["31988887777"])
        assertEquals(SituacaoDoGuardiao.ACEITOU_SEM_AVISO, m["31977776666"])
    }

    @Test
    fun `servidor antigo ou erro nao vira situacao - a tela usa o que o aparelho lembra`() {
        assertNull(SituacaoDoGuardiao.lerDoServidor("""{"erro":"acao_desconhecida"}"""))
        assertNull(SituacaoDoGuardiao.lerDoServidor("lixo"))
    }

    @Test
    fun `o numero casa em qualquer formato (com +55, traco, parenteses)`() {
        val m = SituacaoDoGuardiao.lerDoServidor(resposta)
        assertEquals(SituacaoDoGuardiao.COM_APP, SituacaoDoGuardiao.de("+55 (31) 98449-1586", m, emptySet()))
    }

    @Test
    fun `so o numero na lista e guardiao SMS - convite mandado daqui e convite enviado`() {
        assertEquals(SituacaoDoGuardiao.SO_SMS, SituacaoDoGuardiao.de("(31) 95555-4444", null, emptySet()))
        assertEquals(SituacaoDoGuardiao.CONVITE_ENVIADO, SituacaoDoGuardiao.de("(31) 95555-4444", null, setOf("31955554444")))
    }

    @Test
    fun `o servidor manda mais que a lembranca do aparelho`() {
        val m = SituacaoDoGuardiao.lerDoServidor(resposta)
        assertEquals(SituacaoDoGuardiao.COM_APP, SituacaoDoGuardiao.de("31984491586", m, setOf("31984491586")))
        // O servidor respondeu e não conhece este número: vale a lembrança (o convite pode ter sido de outro aparelho).
        assertEquals(SituacaoDoGuardiao.CONVITE_ENVIADO, SituacaoDoGuardiao.de("31955554444", m, setOf("31955554444")))
    }

    @Test
    fun `so o guardiao com app toca no celular`() {
        assertTrue(SituacaoDoGuardiao.COM_APP.tocaNoCelular)
        assertFalse(SituacaoDoGuardiao.SO_SMS.tocaNoCelular)
        assertFalse(SituacaoDoGuardiao.CONVITE_ENVIADO.tocaNoCelular)
        assertFalse(SituacaoDoGuardiao.ACEITOU_SEM_AVISO.tocaNoCelular)
    }

    @Test
    fun `os convites mandados daqui ficam guardados`() {
        val estado = AppState(convitesEnviados = setOf("31984491586"))
        assertEquals(setOf("31984491586"), StateJson.decode(StateJson.encode(estado)).convitesEnviados)
        assertEquals(emptySet<String>(), StateJson.decode("""{"settings":{}}""").convitesEnviados)
    }
}

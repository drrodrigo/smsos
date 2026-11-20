package br.com.alertasilencioso.relogio

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Place
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.testutil.RobolectricJarLeak
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** O lado do CELULAR: lê o pedido do relógio, aciona o MESMO SOS do app e responde ok / teste / contatos. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OuvinteDoRelogioTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun comecaDoZero() {
        Repository.init(context)
        Repository.update { AppState(settings = Settings(testMode = true, onboardingDone = true, dispositivo = "aparelho-teste")) }
    }

    // Ver RobolectricJarLeak: sem isso, o próximo Android simulado (outra versão de SDK) quebra ao abrir.
    @After
    fun fechaJarVazadoDoRobolectric() = RobolectricJarLeak.fechar()

    @Test
    fun `le o pedido do relogio (Seguranca e Saude)`() {
        assertEquals(ContratoDoRelogio.Pedido(Profile.SAUDE, "a1"), ContratoDoRelogio.lerPedido("""{"perfil":"saude","id":"a1"}"""))
        assertEquals(ContratoDoRelogio.Pedido(Profile.SEGURANCA, "b2"), ContratoDoRelogio.lerPedido("""{"perfil":"seguranca","id":"b2"}"""))
    }

    @Test
    fun `pedido estranho ainda e SOS - na duvida, Seguranca`() {
        assertEquals(ContratoDoRelogio.Pedido(Profile.SEGURANCA, ""), ContratoDoRelogio.lerPedido("lixo"))
        assertEquals(Profile.SEGURANCA, ContratoDoRelogio.lerPedido("""{"perfil":"outro","id":"c"}""").perfil)
    }

    @Test
    fun `a resposta leva id, ok, teste e contatos (erro so quando deu errado)`() {
        val ok = JSONObject(ContratoDoRelogio.resultado("x", ok = true, teste = false, contatos = 2))
        assertEquals("x", ok.getString("id"))
        assertTrue(ok.getBoolean("ok"))
        assertFalse(ok.getBoolean("teste"))
        assertEquals(2, ok.getInt("contatos"))
        assertFalse(ok.has("erro"))

        val semContagem = JSONObject(ContratoDoRelogio.resultado("x", ok = false, teste = true, contatos = null, erro = "falhou"))
        assertFalse(semContagem.has("contatos"))
        assertEquals("falhou", semContagem.getString("erro"))
    }

    @Test
    fun `aciona o SOS do perfil pedido e conta os guardioes sem repetir numero`() {
        Repository.update {
            it.copy(
                alwaysContacts = listOf(Contact(name = "Ana", phone = "31999998888"), Contact(name = "Beto", phone = "31988887777")),
                places = listOf(Place(name = "Casa", contacts = listOf(Contact(name = "Ana de novo", phone = "(31) 99999-8888")))),
            )
        }
        var acionado: Profile? = null
        val resposta = JSONObject(
            OuvinteDoRelogio.atender(context, ContratoDoRelogio.Pedido(Profile.SAUDE, "id-1")) { _, p -> acionado = p },
        )
        assertEquals(Profile.SAUDE, acionado)
        assertEquals("id-1", resposta.getString("id"))
        assertTrue(resposta.getBoolean("ok"))
        assertTrue("o celular está em modo teste", resposta.getBoolean("teste"))
        assertEquals(2, resposta.getInt("contatos"))
    }

    @Test
    fun `sem guardiao cadastrado responde contatos 0 (o relogio pede para cadastrar)`() {
        Repository.updateSettings { it.copy(testMode = false) }
        val resposta = JSONObject(OuvinteDoRelogio.atender(context, ContratoDoRelogio.Pedido(Profile.SEGURANCA, "id-2")) { _, _ -> })
        assertTrue(resposta.getBoolean("ok"))
        assertFalse(resposta.getBoolean("teste"))
        assertEquals(0, resposta.getInt("contatos"))
    }

    @Test
    fun `se o celular falhar ao acionar, responde ok false com o erro`() {
        val resposta = JSONObject(
            OuvinteDoRelogio.atender(context, ContratoDoRelogio.Pedido(Profile.SEGURANCA, "id-3")) { _, _ -> error("quebrou") },
        )
        assertFalse(resposta.getBoolean("ok"))
        assertTrue(resposta.has("erro"))
    }

    @Test
    fun `de verdade - o SOS do relogio entra no historico como Acionado pelo relogio (modo teste, nada enviado)`() {
        Repository.update { it.copy(alwaysContacts = listOf(Contact(name = "Ana", phone = "31999998888"))) }
        // Passa a "espera entre alertas" do AlertManager (outro teste pode ter acionado há pouco).
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(2))

        val resposta = JSONObject(OuvinteDoRelogio.atender(context, ContratoDoRelogio.Pedido(Profile.SEGURANCA, "id-4")))
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(resposta.getBoolean("ok"))
        val registro = Repository.state.value.history.first()
        assertTrue("devia estar marcado como teste", registro.test)
        assertTrue(registro.notes.toString(), registro.notes.contains("Acionado pelo relógio"))
        assertEquals("Ana", registro.deliveries.single().name)
    }
}

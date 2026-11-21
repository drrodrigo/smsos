package br.com.alertasilencioso.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.alertasilencioso.alert.SosRapido
import br.com.alertasilencioso.data.AlertRecord
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.data.StateJson
import br.com.alertasilencioso.testutil.RobolectricJarLeak
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * O atalho "SOS rápido" (botão lateral do Samsung): abriu = conta 3 s e dispara UMA vez; toque ou "voltar" na
 * contagem cancela (toque leve, não); sem guardião não dispara; o 2º ícone segue o ajuste.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SosRapidoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val disparos = mutableListOf<Profile>()
    private val acionarDeVerdade = SosRapido.acionar
    private var alertaEntraNoHistorico = false
    private var alertaAcabouDeSair = false

    @Before
    fun comecaDoZero() {
        Repository.init(context)
        Repository.update {
            AppState(
                settings = Settings(testMode = true, onboardingDone = true, dispositivo = "aparelho-teste"),
                alwaysContacts = listOf(Contact(name = "Fernanda", phone = "31999998888")),
            )
        }
        SosRapido.acionar = { _, perfil ->
            disparos += perfil
            if (alertaEntraNoHistorico) Repository.addAlert(registro())
            !alertaAcabouDeSair
        }
    }

    @After
    fun desfaz() {
        SosRapido.acionar = acionarDeVerdade
        RobolectricJarLeak.fechar()
    }

    private fun registro() = AlertRecord(
        timestamp = System.currentTimeMillis() + 60_000,
        test = true,
        latitude = null,
        longitude = null,
        accuracyMeters = null,
        placeNames = emptyList(),
        deliveries = emptyList(),
        message = "SOS",
    )

    private fun abrir(): ActivityController<SosRapidoActivity> = Robolectric.buildActivity(SosRapidoActivity::class.java).setup()

    private fun passar(segundos: Double) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis((segundos * 1000).toLong()))

    @Test
    fun `com os dois SOS ligados o atalho e Seguranca, com um so e o escolhido`() {
        assertEquals(Profile.SEGURANCA, SosRapido.perfil(Settings(dualSos = true, profile = Profile.SAUDE)))
        assertEquals(Profile.SAUDE, SosRapido.perfil(Settings(dualSos = false, profile = Profile.SAUDE)))
        assertEquals(Profile.SEGURANCA, SosRapido.perfil(Settings(dualSos = false, profile = Profile.SEGURANCA)))
    }

    @Test
    fun `abriu, conta 3 segundos e dispara uma vez so`() {
        val tela = abrir().get()
        assertEquals(SosRapidoActivity.Fase.CONTANDO, tela.fase)
        passar(2.9)
        assertTrue("antes dos 3 s nada sai", disparos.isEmpty())
        passar(0.2)
        assertEquals(listOf(Profile.SEGURANCA), disparos)
        assertEquals(SosRapidoActivity.Fase.ENVIANDO, tela.fase)
        passar(30.0)
        assertEquals("cada abertura dispara UMA vez", 1, disparos.size)
    }

    @Test
    fun `segurar o dedo na tela durante a contagem cancela - nada sai`() {
        val tela = abrir().get()
        passar(1.5)
        tela.segurou()
        assertTrue(tela.isFinishing)
        passar(10.0)
        assertTrue(disparos.isEmpty())
    }

    @Test
    fun `toque leve na contagem NAO cancela - dedo ou bolso encostando`() {
        val tela = abrir().get()
        passar(1.0)
        tela.tocou()
        assertFalse(tela.isFinishing)
        passar(2.1)
        assertEquals(listOf(Profile.SEGURANCA), disparos)
    }

    @Test
    fun `voltar durante a contagem cancela - nada sai`() {
        val tela = abrir().get()
        passar(1.0)
        tela.onBackPressedDispatcher.onBackPressed()
        assertTrue(tela.isFinishing)
        passar(10.0)
        assertTrue(disparos.isEmpty())
    }

    @Test
    fun `tocar ou segurar depois de disparar nao fecha nem dispara de novo`() {
        val tela = abrir().get()
        passar(3.1)
        tela.tocou()
        tela.segurou()
        assertFalse(tela.isFinishing)
        assertEquals(1, disparos.size)
    }

    @Test
    fun `sem guardiao cadastrado nao dispara e avisa`() {
        Repository.update { it.copy(alwaysContacts = emptyList(), places = emptyList()) }
        val tela = abrir().get()
        assertEquals(SosRapidoActivity.Fase.SEM_GUARDIAO, tela.fase)
        passar(10.0)
        assertTrue(disparos.isEmpty())
    }

    @Test
    fun `o alerta entrou no historico - mostra acionado e fecha sozinho`() {
        alertaEntraNoHistorico = true
        val tela = abrir().get()
        passar(3.6)
        assertEquals(SosRapidoActivity.Fase.PRONTO, tela.fase)
        assertFalse(tela.isFinishing)
        passar(SosRapido.MOSTRA_PRONTO_MS / 1000.0 + 0.1)
        assertTrue(tela.isFinishing)
    }

    @Test
    fun `sem o alerta no historico, a tela preta espera no maximo o teto e fecha`() {
        val tela = abrir().get()
        passar(3.1)
        passar(SosRapido.ESPERA_MAX_MS / 1000.0 - 1)
        assertEquals(SosRapidoActivity.Fase.ENVIANDO, tela.fase)
        passar(1.5)
        assertEquals(SosRapidoActivity.Fase.PRONTO, tela.fase)
    }

    @Test
    fun `um alerta acabou de sair (ex - pelo relogio) - diz que ja saiu, sem esperar nem mentir`() {
        alertaAcabouDeSair = true
        val tela = abrir().get()
        passar(3.1)
        assertEquals(SosRapidoActivity.Fase.JA_SAIU, tela.fase)
        assertEquals(1, disparos.size)
        passar(SosRapido.MOSTRA_PRONTO_MS * 2 / 1000.0 + 0.1)
        assertTrue(tela.isFinishing)
    }

    @Test
    fun `dois toques de novo depois de acabar comecam outra contagem`() {
        alertaEntraNoHistorico = true
        val controle = abrir()
        passar(3.6)
        assertEquals(SosRapidoActivity.Fase.PRONTO, controle.get().fase)
        controle.newIntent(android.content.Intent(context, SosRapidoActivity::class.java))
        assertEquals(SosRapidoActivity.Fase.CONTANDO, controle.get().fase)
    }

    @Test
    fun `o 2o icone segue o ajuste - nasce desligado`() {
        val atalho = ComponentName(context, SosRapidoActivity::class.java)
        val pm = context.packageManager
        assertFalse(SosRapido.atalhoLigado(context))
        SosRapido.aplicarAtalho(context, true)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, pm.getComponentEnabledSetting(atalho))
        assertTrue(SosRapido.atalhoLigado(context))
        SosRapido.aplicarAtalho(context, false)
        assertFalse(SosRapido.atalhoLigado(context))
    }

    @Test
    fun `o ajuste do SOS rapido e guardado (nasce desligado)`() {
        assertFalse(Settings().sosRapido)
        val ligado = AppState(settings = Settings(sosRapido = true))
        assertTrue(StateJson.decode(StateJson.encode(ligado)).settings.sosRapido)
        assertFalse(StateJson.decode("""{"settings":{}}""").settings.sosRapido)
    }
}

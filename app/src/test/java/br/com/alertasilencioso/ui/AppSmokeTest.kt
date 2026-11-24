package br.com.alertasilencioso.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.testutil.RobolectricJarLeak
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Abre cada tela num Android simulado para garantir que nenhuma trava ao aparecer. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [26, 35], qualifiers = "w411dp-h891dp")
class AppSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun comecaDoZero() {
        // Pula o cadastro do primeiro uso (se aparecer) e o passo a passo (se abrir sozinho);
        // os testes de telas partem da tela Início.
        if (compose.onAllNodesWithText("Pular por enquanto").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Pular por enquanto").performClick()
            compose.waitForIdle()
        }
        if (compose.onAllNodesWithText("Fazer depois").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Fazer depois").performClick()
        }
        Repository.update { AppState(settings = Settings(onboardingDone = true, registroVisto = true)) }
    }

    // Ver RobolectricJarLeak: sem isso, o próximo Android simulado (outra versão de SDK) quebra ao abrir.
    @After
    fun fechaJarVazadoDoRobolectric() = RobolectricJarLeak.fechar()

    private fun openTab(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()

    private fun scrollTo(text: String) =
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text, substring = true))

    @Test
    fun todasAsTelasAbrem() {
        // O simulador não libera permissão nenhuma, então a proteção aparece desligada.
        compose.onNodeWithText("Proteção DESLIGADA").assertExists()
        // Padrão: os dois SOS ligados, botão dividido Segurança/Saúde.
        compose.onNode(hasText("SEGURANÇA", substring = true)).assertExists()
        compose.onNode(hasText("SAÚDE", substring = true)).assertExists()
        scrollTo("Testar sem enviar SMS")
        scrollTo("Histórico")

        openTab("Guardiões")
        compose.onNodeWithText("Meus guardiões").assertExists()

        openTab("Lugares")
        compose.onNodeWithText("Adicionar lugar").performClick()
        compose.onNodeWithText("Editar lugar").assertExists()
        scrollTo("Excluir este lugar")
        scrollTo("Editar lugar")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Falta marcar a localização").assertExists()

        openTab("Ajustes")
        compose.onNodeWithText("Seu nome").assertExists()
        // O botão de pânico (o 2º ícone, SOS rápido) se liga aqui (Rodrigo, 24/11).
        scrollTo("Botão de pânico (ícone SOS rápido)")
        scrollTo("Testar o envio de SMS")

        openTab("Início")
        compose.onNode(hasText("SEGURANÇA", substring = true)).assertExists()
    }

    @Test
    fun passoAPassoLevaAteOTeste() {
        compose.onNodeWithText("Configurar passo a passo").performClick()
        compose.onNodeWithText("Vamos deixar seu botão de pânico pronto").assertExists()
        compose.onNodeWithText("Começar").performClick()

        compose.onNodeWithText("Qual é o seu nome?").assertExists()
        compose.onNodeWithText("Seu nome").performTextInput("Ana")
        compose.onNodeWithText("Continuar").performClick()

        compose.onNodeWithText("Quem deve ser avisado sempre?").assertExists()
        // O simulador não libera nenhuma permissão, então os passos seguintes são pulados um a um.
        var pulos = 0
        while (compose.onAllNodesWithText("Concluir").fetchSemanticsNodes().isEmpty() && pulos < 10) {
            if (compose.onAllNodesWithText("Perceber o botão de volume").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNode(hasText("Configuração restrita", substring = true)).assertExists()
            }
            compose.onNodeWithText("Pular por enquanto").performScrollTo().performClick()
            pulos++
        }
        compose.onNodeWithText("Tudo pronto! Vamos testar?").assertExists()
        compose.onNodeWithText("Concluir").performScrollTo().performClick()
        compose.onNode(hasText("SEGURANÇA", substring = true)).assertExists()
    }

    @Test
    fun contatoCadastradoApareceNaLista() {
        Repository.addAlwaysContact(Contact(name = "Filho Teste", phone = "(31) 98888-7777"))
        openTab("Guardiões")
        compose.onNodeWithText("Filho Teste").assertExists()
        compose.onNodeWithText("(31) 98888-7777").assertExists()
        // A janela de "Digitar" não é testada aqui: o simulador não desenha janelas de diálogo do Compose.
        compose.onNodeWithText("Digitar").assertExists()
        // Os dois tipos de guardião (21/11): só o número = guardião SMS, com o botão "Convidar".
        compose.onNodeWithText("📩 Guardião SMS · recebe só o SMS").assertExists()
        compose.onNodeWithText("Convidar").assertExists()
        // O ⓘ de cada tipo (a janela em si o simulador não desenha).
        compose.onNodeWithContentDescription("Como chega para o guardião SMS").assertExists()
        compose.onNodeWithContentDescription("Como toca no celular do guardião com app").assertExists()
        // 22/11: nada de "?" colorido (parecia erro ao lado do ✓ verde); a ajuda é o ⓘ cinza.
        assert(compose.onAllNodesWithText("?").fetchSemanticsNodes().isEmpty())
        // 24/11: a chave de pausar cada guardião (sem apagar).
        compose.onNodeWithText("Recebe os alertas").assertExists()
        compose.onNodeWithContentDescription("Recebe os alertas: Filho Teste").assertExists()
    }

    @Test
    fun guardiaoPausadoApareceComoPausado() {
        Repository.addAlwaysContact(Contact(name = "Filha Pausada", phone = "(31) 97777-6666", ativo = false))
        openTab("Guardiões")
        compose.onNodeWithText("Filha Pausada").assertExists()
        compose.onNodeWithText("⏸ Pausado: não recebe nada até você ligar de novo").assertExists()
    }

    @Test
    fun conviteMandadoDaquiApareceComoConviteEnviado() {
        Repository.addAlwaysContact(Contact(name = "Filha Teste", phone = "(31) 97777-6666"))
        Repository.update { it.copy(convitesEnviados = setOf("31977776666")) }
        openTab("Guardiões")
        compose.onNode(hasText("Convite enviado", substring = true)).assertExists()
        compose.onNodeWithText("Reenviar").assertExists()
    }

    @Test
    fun ajustesMostramExemploDaMensagem() {
        openTab("Ajustes")
        compose.onNodeWithText("Nome que aparece na mensagem").performTextInput("Rodrigo")
        scrollTo("ALERTA! Rodrigo acionou")
        compose.onNode(hasText("ALERTA! Rodrigo acionou", substring = true)).assertExists()
    }
}

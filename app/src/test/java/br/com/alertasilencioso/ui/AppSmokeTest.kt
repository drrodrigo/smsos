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
        // O passo a passo pode ter aberto sozinho; os testes de telas partem da tela Início.
        if (compose.onAllNodesWithText("Fazer depois").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Fazer depois").performClick()
        }
        Repository.update { AppState(settings = Settings(onboardingDone = true)) }
    }

    private fun openTab(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()

    private fun scrollTo(text: String) =
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text, substring = true))

    @Test
    fun todasAsTelasAbrem() {
        // O simulador não tem chip, então o aviso de cima é o de aparelho sem SMS.
        compose.onNodeWithText("Este aparelho não envia SMS").assertExists()
        compose.onNodeWithText("SEGURE AQUI").assertExists()
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
        scrollTo("Testar o envio de SMS")

        openTab("Início")
        compose.onNodeWithText("SEGURE AQUI").assertExists()
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
        compose.onNodeWithText("SEGURE AQUI").assertExists()
    }

    @Test
    fun contatoCadastradoApareceNaLista() {
        Repository.addAlwaysContact(Contact(name = "Filho Teste", phone = "(31) 98888-7777"))
        openTab("Guardiões")
        compose.onNodeWithText("Filho Teste").assertExists()
        compose.onNodeWithText("(31) 98888-7777").assertExists()
        // A janela de "Digitar" não é testada aqui: o simulador não desenha janelas de diálogo do Compose.
        compose.onNodeWithText("Digitar").assertExists()
    }

    @Test
    fun ajustesMostramExemploDaMensagem() {
        openTab("Ajustes")
        compose.onNodeWithText("Nome que aparece na mensagem").performTextInput("Rodrigo")
        scrollTo("ALERTA! Rodrigo acionou")
        compose.onNode(hasText("ALERTA! Rodrigo acionou", substring = true)).assertExists()
    }
}

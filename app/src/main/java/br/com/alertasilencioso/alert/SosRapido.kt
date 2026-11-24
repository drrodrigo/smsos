package br.com.alertasilencioso.alert

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import br.com.alertasilencioso.R
import br.com.alertasilencioso.core.RecipientResolver
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.ui.SosRapidoActivity

/**
 * As regras do atalho "SOS rápido" (o 2º ícone, [SosRapidoActivity]), fora da tela para os testes travarem.
 *
 * SEMPRE LIGADO, sem chave (24/11): é o que os 2 toques no botão de ligar abrem.
 *
 * Por que existe: o Samsung NÃO entrega o botão de volume a nenhum app com a tela apagada (testado no aparelho do
 * Rodrigo, 21/11). Mas o "pressionar duas vezes" do botão lateral abre um app escolhido por cima da tela
 * bloqueada. Com este atalho escolhido lá, dois toques no botão de ligar = tela preta, 3 s e o SOS sai, igual ao
 * relógio.
 */
object SosRapido {
    /** A contagem antes de disparar (como o relógio): dá tempo de cancelar um toque sem querer. */
    const val SEGUNDOS = 3

    /** Depois de acionar, a tela preta espera o alerta entrar no histórico (o GPS leva até ~20 s) até este teto. */
    const val ESPERA_MAX_MS = 25_000L

    /** Quanto o "SOS acionado" fica na tela antes de fechar. */
    const val MOSTRA_PRONTO_MS = 2_500L

    /**
     * Contra CURIOSOS (Rodrigo, 24/11): com o celular DESBLOQUEADO, o SOS rápido só dispara com [TOQUES_NO_ICONE]
     * toques seguidos NO ÍCONE, dentro de [JANELA_DOS_TOQUES_MS]. O 1º e o 2º toque não mostram nada (a tela fica
     * onde estava): quem tocou por curiosidade acha que o ícone não faz nada. (Antes era 1 toque + 2 na tela preta,
     * mas ele viu o furo: "a pessoa toca na tela preta para testar... e dispara do mesmo jeito".) Com o celular
     * BLOQUEADO (2 toques no botão de ligar), dispara direto: a pessoa pode nem olhar.
     */
    const val TOQUES_NO_ICONE = 3

    /**
     * Extra do atalho do ícone principal (res/xml/atalhos.xml): "atalho" = veio do BOTÃO LATERAL do Samsung (ou do
     * menu do ícone). Dispara direto, como com o celular bloqueado: a pessoa apertou de propósito.
     */
    const val EXTRA_ORIGEM = "br.com.alertasilencioso.ORIGEM"
    const val ORIGEM_ATALHO = "atalho"

    /** O que fazer quando a tela do SOS rápido é aberta. */
    enum class Abertura { DISPARA, CONTA_TOQUE, ABRE_O_APP }

    /**
     * 24/11: a lista do botão lateral do Samsung é por APP e abre a "porta de entrada" do pacote. Desde a 0.36.2 essa
     * porta é o SOS rápido (filtro MAIN + INFO no manifesto, que o Android escolhe antes do LAUNCHER). Quem abre por
     * ela chega com a categoria INFO ([aberturaDoPacote]): o botão lateral, mas também o "Abrir" da Play e o das
     * informações do app.
     *
     *  - Celular BLOQUEADO (o botão lateral com a tela apagada) ou o atalho com ORIGEM=atalho: DISPARA.
     *  - Porta do pacote com o celular DESBLOQUEADO ("Abrir" da Play, informações do app, botão lateral com o celular
     *    em uso): ABRE O APP de configuração, como antes (nunca dispara um SOS por engano ao abrir pela Play).
     *  - O ícone SOS rápido / o atalho na tela inicial, desbloqueado: CONTA OS TOQUES (3 seguidos, contra curiosos).
     */
    fun comoAbrir(bloqueado: Boolean, doAtalho: Boolean, aberturaDoPacote: Boolean): Abertura = when {
        bloqueado || doAtalho -> Abertura.DISPARA
        aberturaDoPacote -> Abertura.ABRE_O_APP
        else -> Abertura.CONTA_TOQUE
    }
    const val JANELA_DOS_TOQUES_MS = 3_000L

    private val toquesNoIcone = ArrayDeque<Long>()

    /** Conta um toque no ícone ([agora] = SystemClock.elapsedRealtime). Verdadeiro no 3º dentro da janela (e zera). */
    @Synchronized
    fun contarToqueNoIcone(agora: Long): Boolean {
        while (toquesNoIcone.isNotEmpty() && agora - toquesNoIcone.first() > JANELA_DOS_TOQUES_MS) toquesNoIcone.removeFirst()
        toquesNoIcone.addLast(agora)
        if (toquesNoIcone.size >= TOQUES_NO_ICONE) {
            toquesNoIcone.clear()
            return true
        }
        return false
    }

    /** Para os testes começarem do zero. */
    @Synchronized
    internal fun zerarToques() = toquesNoIcone.clear()

    /**
     * Quem aciona de verdade. Falso = não acionou porque um alerta acabou de sair (ex.: o relógio, segundos antes).
     * Os testes trocam para contar os disparos sem mandar nada.
     */
    internal var acionar: (Context, Profile) -> Boolean = { c, p -> AlertManager.trigger(c, TriggerSource.BOTAO_LATERAL, p) }

    /**
     * Qual SOS o atalho dispara: com os dois SOS ligados, SEGURANÇA (é o pânico, o mesmo do Volume −); com um só,
     * o que a pessoa escolheu ("Para que você usa").
     */
    fun perfil(settings: Settings): Profile = if (settings.dualSos) Profile.SEGURANCA else settings.profile

    /** Os mesmos guardiões que a tela inicial conta ("sempre avisar" + os dos lugares, sem repetir número). */
    fun guardioes(estado: AppState): Int =
        RecipientResolver.distinctByPhone(estado.alwaysContacts + estado.places.flatMap { it.contacts }).size

    /**
     * O 2º ícone fica SEMPRE ligado (24/11). Quem teve a versão com a chave de desligar (0.33 a 0.35.1) pode ter
     * ficado com ele desligado: aqui volta ao do manifesto (ligado), e o atalho fixado na tela inicial também.
     */
    fun garantirAtalho(context: Context) {
        runCatching {
            val pm = context.packageManager
            val atalho = ComponentName(context, SosRapidoActivity::class.java)
            if (pm.getComponentEnabledSetting(atalho) != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                pm.setComponentEnabledSetting(atalho, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
            }
        }.onFailure { Log.w("SosRapido", "Não ligou o atalho", it) }
        runCatching { ShortcutManagerCompat.enableShortcuts(context, listOf(atalhoDaTelaInicial(context))) }
            .onFailure { Log.w("SosRapido", "Não ligou o atalho da tela inicial", it) }
    }

    /** Id do atalho "SOS rápido" fixado na tela inicial (o botão "Pôr na tela inicial" dos Ajustes). */
    const val ID_DO_ATALHO = "sos-rapido"

    /**
     * Pede ao Android para pôr o "SOS rápido" na TELA INICIAL (Rodrigo, 24/11: "coloca na primeira tela, no lugar
     * que fica confortável para disparar"). O próprio celular mostra "Adicionar à tela inicial?" e a pessoa
     * confirma. Devolve falso quando a tela inicial do aparelho não aceita (aí a pessoa arrasta da lista de apps).
     */
    fun porNaTelaInicial(context: Context): Boolean = runCatching {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        ShortcutManagerCompat.requestPinShortcut(context, atalhoDaTelaInicial(context), null)
    }.getOrDefault(false)

    private fun atalhoDaTelaInicial(context: Context): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, ID_DO_ATALHO)
            .setShortLabel(context.getString(R.string.sos_rapido_nome))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, SosRapidoActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()

    /** O atalho está aparecendo (ligado de fato no aparelho)? */
    fun atalhoLigado(context: Context): Boolean = runCatching {
        val estado = context.packageManager.getComponentEnabledSetting(ComponentName(context, SosRapidoActivity::class.java))
        estado == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || estado == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }.getOrDefault(false)
}

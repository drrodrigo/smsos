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

    /** Contra curiosos: quantos toques na tela preta confirmam o SOS aberto pelo ícone com o celular desbloqueado. */
    const val TOQUES_PARA_CONFIRMAR = 2

    /** Quanto a tela preta espera a confirmação; sem ela, fecha e nada sai. */
    const val ESPERA_DA_CONFIRMACAO_MS = 5_000L

    /**
     * Pede os toques de confirmação? Só com o celular DESBLOQUEADO (alguém tocou no ícone) e o ajuste ligado. Com o
     * celular bloqueado (os 2 toques no botão de ligar, a tela apagada) dispara direto: a pessoa pode nem olhar.
     */
    fun pedeConfirmacao(settings: Settings, celularBloqueado: Boolean): Boolean =
        settings.sosRapidoConfirmar && !celularBloqueado

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
     * Mostra ou esconde o 2º ícone conforme o ajuste. Desligado volta ao do manifesto (enabled=false): o ícone some
     * da gaveta e da lista do botão lateral, e nada mais abre o atalho.
     */
    fun aplicarAtalho(context: Context, ligado: Boolean) {
        runCatching {
            val pm = context.packageManager
            val atalho = ComponentName(context, SosRapidoActivity::class.java)
            val desejado = if (ligado) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            if (pm.getComponentEnabledSetting(atalho) != desejado) {
                pm.setComponentEnabledSetting(atalho, desejado, PackageManager.DONT_KILL_APP)
            }
        }.onFailure { Log.w("SosRapido", "Não mudou o atalho", it) }
        // O que foi fixado na tela inicial acompanha: desligado, fica cinza e avisa onde religar.
        runCatching {
            if (ligado) {
                ShortcutManagerCompat.enableShortcuts(context, listOf(atalhoDaTelaInicial(context)))
            } else {
                ShortcutManagerCompat.disableShortcuts(
                    context,
                    listOf(ID_DO_ATALHO),
                    "O SOS rápido está desligado. Ligue em GuardiõesSOS > Ajustes.",
                )
            }
        }.onFailure { Log.w("SosRapido", "Não mudou o atalho da tela inicial", it) }
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
        context.packageManager.getComponentEnabledSetting(ComponentName(context, SosRapidoActivity::class.java)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)
}

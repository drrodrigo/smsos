package br.com.alertasilencioso.relogio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import br.com.alertasilencioso.alert.AlertManager
import br.com.alertasilencioso.alert.TriggerSource
import br.com.alertasilencioso.core.RecipientResolver
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Repository
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * O pedido de SOS do RELÓGIO chega aqui (caminho /guardioes/sos), mesmo com o app do celular fechado. Vira o MESMO
 * SOS do botão do app: [AlertManager.trigger] com [TriggerSource.RELOGIO] (modo teste, gateway, histórico, sirene
 * nos guardiões: tudo igual). Depois responde ao relógio (/guardioes/sos/resultado) com ok, teste e contatos.
 *
 * O relógio não manda SMS nem fala com o servidor: quem envia é sempre este celular, pela nossa estrutura.
 */
class OuvinteDoRelogio : WearableListenerService() {

    override fun onMessageReceived(evento: MessageEvent) {
        if (evento.path != ContratoDoRelogio.CAMINHO_SOS) return
        val pedido = ContratoDoRelogio.lerPedido(String(evento.data, Charsets.UTF_8))
        val app = applicationContext

        // O AlertManager trabalha na linha principal (GPS, vibração, telas). Esta chamada chega numa linha de fundo:
        // passa para a principal e espera acabar, para o serviço não morrer antes de acionar.
        var resposta = ContratoDoRelogio.resultado(pedido.id, ok = false, teste = false, contatos = null, erro = "o celular não respondeu")
        val pronto = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            try {
                resposta = atender(app, pedido)
            } finally {
                pronto.countDown()
            }
        }
        pronto.await(ESPERA_MS, TimeUnit.MILLISECONDS)

        try {
            Tasks.await(
                Wearable.getMessageClient(app).sendMessage(
                    evento.sourceNodeId,
                    ContratoDoRelogio.CAMINHO_RESULTADO,
                    resposta.toByteArray(Charsets.UTF_8),
                ),
                ESPERA_MS,
                TimeUnit.MILLISECONDS,
            )
        } catch (e: Exception) {
            // Sem a resposta, o relógio diz "não saiu" — mas o alerta, se acionou, já está saindo daqui.
            Log.w(TAG, "Não respondeu ao relógio", e)
        }
    }

    companion object {
        private const val TAG = "OuvinteDoRelogio"
        private const val ESPERA_MS = 5_000L

        /**
         * Aciona o SOS pedido pelo relógio e devolve a resposta (JSON do contrato). Chamar na linha principal.
         * [acionar] existe para os testes; no app é sempre o [AlertManager].
         */
        fun atender(
            context: Context,
            pedido: ContratoDoRelogio.Pedido,
            acionar: (Context, Profile) -> Unit = { c, p -> AlertManager.trigger(c, TriggerSource.RELOGIO, p) },
        ): String {
            Repository.init(context.applicationContext)
            val estado = Repository.state.value
            val teste = estado.settings.testMode
            // Os mesmos guardiões que a tela inicial conta ("sempre avisar" + os dos lugares, sem repetir número).
            val contatos = RecipientResolver.distinctByPhone(estado.alwaysContacts + estado.places.flatMap { it.contacts }).size
            return try {
                acionar(context, pedido.perfil)
                ContratoDoRelogio.resultado(pedido.id, ok = true, teste = teste, contatos = contatos)
            } catch (e: RuntimeException) {
                Log.e(TAG, "O SOS do relógio falhou", e)
                ContratoDoRelogio.resultado(pedido.id, ok = false, teste = teste, contatos = contatos, erro = "o celular não conseguiu acionar")
            }
        }
    }
}

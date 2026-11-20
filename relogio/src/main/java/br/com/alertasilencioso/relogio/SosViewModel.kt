package br.com.alertasilencioso.relogio

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Liga a [MaquinaDoSos] ao relógio de verdade: conta os segundos, manda o pedido pela ponte do Wear OS
 * (NodeClient + MessageClient), espera a resposta do celular (até 8 s), vibra e segura o relógio acordado só
 * enquanto conta e envia. Vive enquanto a tela do SOS vive (fechar a tela no meio da contagem = cancelar).
 */
class SosViewModel(app: Application) : AndroidViewModel(app), MessageClient.OnMessageReceivedListener {
    private val maquina = MaquinaDoSos { UUID.randomUUID().toString() }
    private val guarda = GuardaDosAjustes(app)

    private val _estado = MutableStateFlow(maquina.estado)
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private val _ajustes = MutableStateFlow(guarda.ler())
    val ajustes: StateFlow<Ajustes> = _ajustes.asStateFlow()

    private val _mostrandoAjustes = MutableStateFlow(false)
    val mostrandoAjustes: StateFlow<Boolean> = _mostrandoAjustes.asStateFlow()

    private var contagem: Job? = null
    private var espera: Job? = null
    private var acordado: PowerManager.WakeLock? = null

    init {
        runCatching { Wearable.getMessageClient(app).addListener(this) }
            .onFailure { Log.w(TAG, "Sem a ponte do Wear OS", it) }
    }

    /** O SOS rápido (botão de cima, ícone, complicação): o tipo, a contagem e o modo dos ajustes. */
    fun abrirSosRapido() {
        val a = _ajustes.value
        abrir(a.perfilRapido, a)
    }

    /** Os botões SEGURANÇA / SAÚDE depois de cancelar: a contagem de novo, para aquele tipo. */
    fun abrir(perfil: Perfil, a: Ajustes = _ajustes.value) {
        _mostrandoAjustes.value = false
        aplicar(maquina.abrir(perfil, a.contagem, a.discreto))
    }

    fun cancelar() = aplicar(maquina.cancelar())

    fun trocarPerfil() = aplicar(maquina.trocarPerfil())

    fun tentarDeNovo() = aplicar(maquina.tentarDeNovo())

    fun mostrarAjustes(mostrar: Boolean) {
        _mostrandoAjustes.value = mostrar
    }

    fun mudarAjustes(novo: Ajustes) {
        guarda.gravar(novo)
        _ajustes.value = guarda.ler()
    }

    /** A resposta do celular (caminho /guardioes/sos/resultado). */
    override fun onMessageReceived(evento: MessageEvent) {
        if (evento.path != Contrato.CAMINHO_RESULTADO) return
        val r = Contrato.lerResultado(String(evento.data, Charsets.UTF_8)) ?: return
        viewModelScope.launch { aplicar(maquina.resposta(r)) }
    }

    private fun aplicar(efeitos: List<Efeito>) {
        _estado.value = maquina.estado
        for (e in efeitos) {
            when (e) {
                is Efeito.Vibrar -> vibrar(getApplication(), e.tipo)
                is Efeito.Enviar -> enviar(e.perfil, e.id)
            }
        }
        val agora = maquina.estado
        if (agora is Estado.Contando) {
            if (contagem?.isActive != true) {
                contagem = viewModelScope.launch {
                    while (maquina.estado is Estado.Contando) {
                        delay(1_000)
                        aplicar(maquina.tique())
                    }
                }
            }
        } else {
            contagem?.cancel()
            contagem = null
        }
        if (agora !is Estado.Enviando) {
            espera?.cancel()
            espera = null
        }
        manterAcordado(MaquinaDoSos.telaAcesa(agora))
    }

    /** Manda o pedido a TODOS os celulares conectados; o primeiro que responder vale. */
    private fun enviar(perfil: Perfil, id: String) {
        espera?.cancel()
        espera = viewModelScope.launch {
            delay(MaquinaDoSos.ESPERA_DA_RESPOSTA_MS)
            aplicar(maquina.semResposta(id))
        }
        val app = getApplication<Application>()
        val corpo = Contrato.pedido(perfil, id).toByteArray(Charsets.UTF_8)
        try {
            Wearable.getNodeClient(app).connectedNodes
                .addOnSuccessListener { nos ->
                    if (nos.isNullOrEmpty()) {
                        aplicar(maquina.semResposta(id))
                        return@addOnSuccessListener
                    }
                    var falhas = 0
                    for (no in nos) {
                        Wearable.getMessageClient(app).sendMessage(no.id, Contrato.CAMINHO_SOS, corpo)
                            .addOnFailureListener {
                                Log.w(TAG, "O pedido não foi ao celular ${no.displayName}", it)
                                falhas++
                                if (falhas == nos.size) aplicar(maquina.semResposta(id))
                            }
                    }
                }
                .addOnFailureListener {
                    Log.w(TAG, "Não achou o celular", it)
                    aplicar(maquina.semResposta(id))
                }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Ponte do Wear OS indisponível", e)
            aplicar(maquina.semResposta(id))
        }
    }

    /** Segura o processador acordado (tela apagada pela mão, por exemplo) só enquanto conta e envia. */
    private fun manterAcordado(sim: Boolean) {
        if (sim) {
            val trava = acordado ?: getApplication<Application>().getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GuardioesSOS:relogio")
                ?.apply { setReferenceCounted(false) }
                ?.also { acordado = it }
            // Teto de segurança: mesmo que algo trave, solta sozinho (5 s de contagem + 8 s de espera + folga).
            if (trava?.isHeld == false) trava.acquire(30_000L)
        } else {
            acordado?.takeIf { it.isHeld }?.release()
        }
    }

    override fun onCleared() {
        runCatching { Wearable.getMessageClient(getApplication<Application>()).removeListener(this) }
        acordado?.takeIf { it.isHeld }?.release()
        super.onCleared()
    }

    companion object {
        private const val TAG = "GuardioesRelogio"

        /** Vibração "de alarme": o relógio não engole com a tela apagada ou no modo não perturbe. */
        fun vibrar(context: Context, tipo: Vibracao) {
            val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            } ?: return
            if (!vibrador.hasVibrator()) return
            val efeito = VibrationEffect.createWaveform(MaquinaDoSos.padrao(tipo), -1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrador.vibrate(efeito, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(efeito, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        }
    }
}

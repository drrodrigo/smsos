package br.com.alertasilencioso.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.alertasilencioso.alert.Feedback
import br.com.alertasilencioso.alert.SosRapido
import br.com.alertasilencioso.data.Repository

/**
 * O atalho "SOS rápido" (o 2º ícone). Abriu = tela PRETA, conta [SosRapido.SEGUNDOS] e dispara SOZINHO, como o
 * relógio. Toque na tela ou "voltar" durante a contagem = cancela (nada sai). Apertar o botão de ligar NÃO cancela:
 * a contagem segue com a tela apagada (é o "dois toques e guarda no bolso").
 *
 * Abre por cima da tela bloqueada (showWhenLocked/turnScreenOn no manifesto), para o "pressionar duas vezes" do
 * botão lateral do Samsung. Depois de disparar, a tela fica preta (brilho no mínimo, acesa) até o alerta entrar no
 * histórico: com o app em primeiro plano o GPS responde. Aí mostra "SOS acionado" e fecha.
 */
class SosRapidoActivity : ComponentActivity() {

    enum class Fase { CONTANDO, ENVIANDO, PRONTO, SEM_GUARDIAO }

    private val handler = Handler(Looper.getMainLooper())

    internal var fase by mutableStateOf(Fase.CONTANDO)
        private set
    internal var faltam by mutableIntStateOf(SosRapido.SEGUNDOS)
        private set
    private var teste by mutableStateOf(false)
    private var acionadoEm = 0L
    private var esperouMs = 0L

    private val tique = object : Runnable {
        override fun run() {
            if (fase != Fase.CONTANDO) return
            faltam -= 1
            if (faltam <= 0) disparar() else handler.postDelayed(this, UM_SEGUNDO)
        }
    }

    private val espera = object : Runnable {
        override fun run() {
            if (fase != Fase.ENVIANDO) return
            esperouMs += PASSO_DA_ESPERA
            val entrou = Repository.state.value.history.any { it.timestamp >= acionadoEm }
            if (entrou || esperouMs >= SosRapido.ESPERA_MAX_MS) pronto() else handler.postDelayed(this, PASSO_DA_ESPERA)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this) { fechar() }
        setContent { TelaDoSosRapido() }

        val salvo = savedInstanceState?.getString(FASE)?.let { runCatching { Fase.valueOf(it) }.getOrNull() }
        when (salvo) {
            null -> comecar()
            // Recriou no meio (raro: o manifesto segura giro e tema): continua de onde estava, sem disparar duas vezes.
            Fase.CONTANDO -> {
                faltam = savedInstanceState.getInt(FALTAM, SosRapido.SEGUNDOS)
                fase = Fase.CONTANDO
                brilho(discreto = true)
                handler.postDelayed(tique, UM_SEGUNDO)
            }
            Fase.ENVIANDO -> {
                acionadoEm = savedInstanceState.getLong(ACIONADO_EM)
                fase = Fase.ENVIANDO
                brilho(discreto = true)
                handler.postDelayed(espera, PASSO_DA_ESPERA)
            }
            Fase.PRONTO, Fase.SEM_GUARDIAO -> finish()
        }
    }

    /** Dois toques de novo com o atalho já aberto: se já acabou, começa outra contagem; no meio, nada muda. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (fase == Fase.PRONTO || fase == Fase.SEM_GUARDIAO) {
            handler.removeCallbacksAndMessages(null)
            comecar()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FASE, fase.name)
        outState.putInt(FALTAM, faltam)
        outState.putLong(ACIONADO_EM, acionadoEm)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun comecar() {
        val estado = Repository.state.value
        teste = estado.settings.testMode
        if (SosRapido.guardioes(estado) == 0) {
            // Sem guardião não há para quem mandar: diz isso com a tela NORMAL (precisa ser lido).
            fase = Fase.SEM_GUARDIAO
            brilho(discreto = false)
            return
        }
        fase = Fase.CONTANDO
        faltam = SosRapido.SEGUNDOS
        brilho(discreto = true)
        Feedback.vibrarAbriu(this)
        handler.removeCallbacks(tique)
        handler.postDelayed(tique, UM_SEGUNDO)
    }

    private fun disparar() {
        fase = Fase.ENVIANDO
        acionadoEm = System.currentTimeMillis()
        esperouMs = 0
        try {
            SosRapido.acionar(applicationContext, SosRapido.perfil(Repository.state.value.settings))
        } catch (e: RuntimeException) {
            Log.e(TAG, "O SOS rápido falhou ao acionar", e)
        }
        handler.postDelayed(espera, PASSO_DA_ESPERA)
    }

    private fun pronto() {
        fase = Fase.PRONTO
        handler.postDelayed({ finish() }, SosRapido.MOSTRA_PRONTO_MS)
    }

    /** Toque na tela: na contagem, CANCELA; no aviso final, fecha. Enviando, não faz nada (já saiu). */
    internal fun tocou() {
        if (fase != Fase.ENVIANDO) fechar()
    }

    /** "Voltar": cancela a contagem (se houver) e fecha. Enviando, o alerta segue (já saiu). */
    internal fun fechar() {
        handler.removeCallbacksAndMessages(null)
        finish()
    }

    /** Brilho no mínimo = a tela preta parece apagada. */
    private fun brilho(discreto: Boolean) {
        val valor = if (discreto) BRILHO_DISCRETO else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = window.attributes.apply { screenBrightness = valor }
    }

    @Composable
    private fun TelaDoSosRapido() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tocou() }
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (fase) {
                    Fase.CONTANDO -> {
                        Text("$faltam", color = APAGADO, fontSize = 64.sp, fontWeight = FontWeight.Light)
                        Spacer(Modifier.height(8.dp))
                        Linha("SOS em ${faltam}s · toque para cancelar")
                        if (teste) Linha("modo teste: não envia")
                    }
                    Fase.ENVIANDO -> Linha(if (teste) "acionando (modo teste)…" else "acionando…")
                    Fase.PRONTO -> Linha(if (teste) "Modo teste: nada foi enviado" else "SOS acionado ✓")
                    Fase.SEM_GUARDIAO -> {
                        Text(
                            "Nenhum guardião cadastrado",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "O SOS não foi enviado. Abra o GuardiõesSOS e cadastre quem deve ser avisado.",
                            color = Color(0xFFDDDDDD),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text("Toque para fechar", color = Color(0xFF999999), fontSize = 14.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun Linha(texto: String) = Text(texto, color = APAGADO, fontSize = 14.sp, textAlign = TextAlign.Center)

    private companion object {
        const val TAG = "SosRapido"
        const val UM_SEGUNDO = 1_000L
        const val PASSO_DA_ESPERA = 500L
        const val BRILHO_DISCRETO = 0.01f
        const val FASE = "fase"
        const val FALTAM = "faltam"
        const val ACIONADO_EM = "acionadoEm"

        /** Cinza bem escuro: quem segura o celular lê de perto; de longe, a tela parece apagada. */
        val APAGADO = Color(0xFF4A4A4A)
    }
}

package br.com.alertasilencioso.relogio

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/**
 * A tela do SOS no relógio. Abriu (ícone, "pressionar duas vezes" do botão de cima, complicação) = começa a
 * contar e dispara SOZINHO. Abrir de novo com ela no fundo começa outra contagem (onNewIntent).
 */
class TelaDoSos : ComponentActivity() {
    private val vm: SosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Girou/recriou a tela no meio da contagem: a contagem segue (está no ViewModel), não começa outra.
        if (savedInstanceState == null) vm.abrirSosRapido()

        setContent {
            val estado by vm.estado.collectAsStateWithLifecycle()
            val ajustes by vm.ajustes.collectAsStateWithLifecycle()
            val mostrandoAjustes by vm.mostrandoAjustes.collectAsStateWithLifecycle()
            ConteudoDoSos(
                estado = estado,
                ajustes = ajustes,
                mostrandoAjustes = mostrandoAjustes,
                acoes = Acoes(
                    cancelar = vm::cancelar,
                    trocarPerfil = vm::trocarPerfil,
                    escolher = { vm.abrir(it) },
                    tentarDeNovo = vm::tentarDeNovo,
                    mostrarAjustes = vm::mostrarAjustes,
                    mudarAjustes = vm::mudarAjustes,
                    // Enviando: sai da frente SEM fechar, para a resposta do celular ainda vibrar no pulso.
                    esconder = { moveTaskToBack(true) },
                    fechar = { finish() },
                ),
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                vm.estado.collect(::ajustarJanela)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.abrirSosRapido()
    }

    /** Tela acesa só na contagem e no envio (bateria); brilho no mínimo no modo discreto (parece apagada). */
    private fun ajustarJanela(e: Estado) {
        if (MaquinaDoSos.telaAcesa(e)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val brilho = if (MaquinaDoSos.telaEscura(e)) BRILHO_DISCRETO else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (window.attributes.screenBrightness != brilho) {
            window.attributes = window.attributes.apply { screenBrightness = brilho }
        }
    }

    private companion object {
        const val BRILHO_DISCRETO = 0.01f
    }
}

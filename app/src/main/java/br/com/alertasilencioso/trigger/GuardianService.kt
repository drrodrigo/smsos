package br.com.alertasilencioso.trigger

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import br.com.alertasilencioso.alert.AlertManager
import br.com.alertasilencioso.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fica ligado o tempo todo (o próprio Android mantém serviços de acessibilidade rodando, inclusive depois de
 * reiniciar o celular) e só observa os botões de volume. Não lê a tela nem o que é digitado.
 */
class GuardianService : AccessibilityService() {
    private var controller: TriggerController? = null
    private var screenOffListener: ScreenOffVolumeListener? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Repository.init(this)
        val trigger = TriggerController(this) { source, profile -> AlertManager.trigger(this, source, profile) }
        controller = trigger
        screenOffListener = ScreenOffVolumeListener(this, trigger::onScreenOffVolume).also { it.start() }
        _running.value = true
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = controller?.onAccessibilityKey(event) ?: false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        screenOffListener?.stop()
        screenOffListener = null
        controller?.shutdown()
        controller = null
        _running.value = false
    }

    companion object {
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()
    }
}

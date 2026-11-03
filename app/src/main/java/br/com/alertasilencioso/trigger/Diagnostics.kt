package br.com.alertasilencioso.trigger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** O que o app percebeu dos botões desde que foi aberto. Serve para a pessoa testar no próprio celular. */
object Diagnostics {
    data class Snapshot(
        val screenOnPresses: Int = 0,
        val screenOffPresses: Int = 0,
        val lastPressAt: Long? = null,
        val lastPressScreenOff: Boolean = false,
    )

    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun recordPress(screenOff: Boolean) = _state.update {
        val now = System.currentTimeMillis()
        if (screenOff) {
            it.copy(screenOffPresses = it.screenOffPresses + 1, lastPressAt = now, lastPressScreenOff = true)
        } else {
            it.copy(screenOnPresses = it.screenOnPresses + 1, lastPressAt = now, lastPressScreenOff = false)
        }
    }
}

package br.com.alertasilencioso.core

import br.com.alertasilencioso.data.TriggerMode

/**
 * Reconhece o gesto que dispara o alerta: segurar o botão por um tempo, ou apertá-lo várias vezes seguidas.
 *
 * Não depende do Android: recebe os horários (em ms) de quando o botão desce e sobe, e quem usa chama
 * [poll] por um timer enquanto o botão continua pressionado.
 */
class PressPatternDetector(var config: Config = Config()) {

    data class Config(
        val mode: TriggerMode = TriggerMode.HOLD,
        val holdMillis: Long = 10_000,
        val tapCount: Int = 5,
        val tapWindowMillis: Long = 4_000,
        /** Intervalo máximo entre sinais repetidos para ainda contarem como o mesmo aperto. */
        val maxPulseGapMillis: Long = 1_000,
    )

    private var pressedAt: Long? = null
    private var pulseStartedAt: Long? = null
    private var lastPulseAt: Long? = null
    private val taps = ArrayDeque<Long>()

    /** Se o gesto atual já disparou o alerta (para não disparar de novo nem repassar o volume). */
    var firedThisGesture = false
        private set

    val isPressed: Boolean get() = pressedAt != null

    /** O botão desceu. Retorna true se isso completou o gesto (modo de toques). */
    fun press(now: Long): Boolean {
        if (pressedAt != null) return false
        pressedAt = now
        firedThisGesture = false
        return config.mode == TriggerMode.TAPS && registerTap(now)
    }

    /** O botão subiu. Retorna quanto tempo ele ficou pressionado, ou null se não estava pressionado. */
    fun release(now: Long): Long? {
        val start = pressedAt ?: return null
        pressedAt = null
        return now - start
    }

    /** Verificação enquanto o botão está pressionado. Retorna true quando o tempo de segurar foi atingido. */
    fun poll(now: Long): Boolean {
        val start = pressedAt ?: return false
        if (config.mode != TriggerMode.HOLD || firedThisGesture) return false
        return now - start >= config.holdMillis && fire()
    }

    /**
     * Sinal repetido sem aviso de soltura: alguns aparelhos, com a tela apagada, só avisam que o volume
     * "mudou", várias vezes por segundo, enquanto o botão está segurado.
     */
    fun pulse(now: Long): Boolean {
        val last = lastPulseAt
        lastPulseAt = now
        val start = if (last == null || now - last > config.maxPulseGapMillis) {
            firedThisGesture = false
            now.also { pulseStartedAt = it }
        } else {
            pulseStartedAt ?: now
        }
        return when (config.mode) {
            TriggerMode.TAPS -> registerTap(now)
            TriggerMode.HOLD -> !firedThisGesture && now - start >= config.holdMillis && fire()
        }
    }

    private fun registerTap(now: Long): Boolean {
        taps.addLast(now)
        while (taps.isNotEmpty() && now - taps.first() > config.tapWindowMillis) taps.removeFirst()
        if (taps.size < config.tapCount) return false
        taps.clear()
        return fire()
    }

    private fun fire(): Boolean {
        firedThisGesture = true
        return true
    }
}

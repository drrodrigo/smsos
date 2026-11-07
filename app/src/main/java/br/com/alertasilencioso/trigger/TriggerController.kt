package br.com.alertasilencioso.trigger

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import br.com.alertasilencioso.alert.TriggerSource
import br.com.alertasilencioso.core.PressPatternDetector
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.TriggerKey
import br.com.alertasilencioso.data.TriggerMode

/**
 * Liga os sinais dos botões de volume ao [PressPatternDetector] e chama [onTrigger] quando o gesto se completa.
 *
 * Com a tela ligada, o botão escolhido é "segurado" pelo app: o volume não muda e nada aparece na tela.
 * Toques rápidos são repassados depois, para o volume continuar funcionando no dia a dia.
 */
class TriggerController(
    context: Context,
    private val onTrigger: (TriggerSource, Profile) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlertaSilencioso:botao")
        .apply { setReferenceCounted(false) }
    private val detector = PressPatternDetector()
    private val pollRunnable = Runnable { poll() }
    private val flushVolumeRunnable = Runnable { flushVolume() }

    private var pressSource = TriggerSource.SCREEN_ON
    private var pressProfile = Profile.SEGURANCA
    private var consumedKeyCode: Int? = null
    private var screenOffDirection = 0
    private var screenOffPressMatches = false
    private var pendingVolumeSteps = 0
    private var pendingVolumeDirection = AudioManager.ADJUST_SAME

    private val settings get() = Repository.state.value.settings

    /** Tela ligada (vem do serviço de acessibilidade). Retorna true quando o app fica com o evento. */
    fun onAccessibilityKey(event: KeyEvent): Boolean {
        val direction = volumeDirection(event.keyCode) ?: return false
        val now = SystemClock.elapsedRealtime()
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return consumedKeyCode == event.keyCode
                Diagnostics.recordPress(screenOff = false)
                // Durante ligações o volume tem outras funções (silenciar o toque, volume da chamada).
                if (!matchesTriggerKey(direction) || audio.mode != AudioManager.MODE_NORMAL) return false
                // Alguns aparelhos (ex.: Samsung M54) não entregam o ACTION_UP de forma confiável pela
                // acessibilidade. Sem a soltura, o aperto anterior ficava "preso" e os toques seguintes
                // não contavam (o gesto nunca completava). Se ainda houver um aperto pendente, solta ele
                // antes de registrar o novo — assim cada ACTION_DOWN conta como um toque.
                if (detector.isPressed) {
                    detector.release(now)
                    consumedKeyCode = null
                }
                consumedKeyCode = event.keyCode
                press(now, TriggerSource.SCREEN_ON, profileFor(direction))
                return true
            }
            KeyEvent.ACTION_UP -> {
                if (consumedKeyCode != event.keyCode) return false
                consumedKeyCode = null
                val heldFor = release(now)
                if (heldFor != null && heldFor < SHORT_PRESS_MILLIS && !detector.firedThisGesture) {
                    queueVolumeStep(direction)
                }
                return true
            }
        }
        return false
    }

    /** Tela apagada (vem da sessão de mídia): +1 ou -1 quando o botão desce, 0 quando ele é solto. */
    fun onScreenOffVolume(direction: Int) {
        if (direction !in AudioManager.ADJUST_LOWER..AudioManager.ADJUST_RAISE) return
        val now = SystemClock.elapsedRealtime()
        if (direction != 0) {
            Diagnostics.recordPress(screenOff = true)
            screenOffDirection = direction
            screenOffPressMatches = matchesTriggerKey(direction)
            if (!screenOffPressMatches) {
                forwardToMusic(direction)
                return
            }
            if (Repository.state.value.screenOffReleaseSupported) {
                press(now, TriggerSource.SCREEN_OFF, profileFor(direction))
            } else {
                // Enquanto não sabemos se o aparelho avisa a soltura, só conta como "segurado" se os sinais
                // continuarem chegando. Assim um toque rápido nunca vira um alerta falso.
                pressProfile = profileFor(direction)
                detector.config = detectorConfig()
                if (detector.pulse(now)) fire(TriggerSource.SCREEN_OFF)
            }
        } else {
            if (!Repository.state.value.screenOffReleaseSupported) {
                Repository.update { it.copy(screenOffReleaseSupported = true) }
            }
            if (!screenOffPressMatches) return
            val heldFor = release(now)
            if (heldFor != null && heldFor < SHORT_PRESS_MILLIS && !detector.firedThisGesture) {
                forwardToMusic(screenOffDirection)
            }
        }
    }

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun press(now: Long, source: TriggerSource, profile: Profile) {
        detector.config = detectorConfig()
        pressSource = source
        pressProfile = profile
        handler.removeCallbacks(flushVolumeRunnable)
        wakeLock.acquire(detector.config.holdMillis + WAKE_MARGIN_MILLIS)
        if (detector.press(now)) {
            fire(source)
            return
        }
        if (detector.config.mode == TriggerMode.HOLD) handler.postDelayed(pollRunnable, detector.config.holdMillis)
    }

    private fun release(now: Long): Long? {
        handler.removeCallbacks(pollRunnable)
        val heldFor = detector.release(now)
        if (detector.config.mode == TriggerMode.HOLD && wakeLock.isHeld) wakeLock.release()
        return heldFor
    }

    private fun poll() {
        val now = SystemClock.elapsedRealtime()
        when {
            detector.poll(now) -> fire(pressSource)
            detector.isPressed -> handler.postDelayed(pollRunnable, POLL_RETRY_MILLIS)
        }
    }

    private fun fire(source: TriggerSource) {
        handler.removeCallbacks(pollRunnable)
        handler.removeCallbacks(flushVolumeRunnable)
        pendingVolumeSteps = 0
        onTrigger(source, pressProfile)
        if (wakeLock.isHeld) wakeLock.release()
    }

    // No modo de toques, espera um pouco antes de mudar o volume: se vierem mais toques, pode ser o alerta.
    private fun queueVolumeStep(direction: Int) {
        pendingVolumeDirection = direction
        pendingVolumeSteps++
        handler.removeCallbacks(flushVolumeRunnable)
        val delay = if (detector.config.mode == TriggerMode.TAPS) TAPS_VOLUME_DELAY_MILLIS else 0L
        handler.postDelayed(flushVolumeRunnable, delay)
    }

    private fun flushVolume() {
        repeat(pendingVolumeSteps) {
            audio.adjustSuggestedStreamVolume(
                pendingVolumeDirection, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI,
            )
        }
        pendingVolumeSteps = 0
    }

    // Com a tela apagada, se houver música tocando, o botão continua controlando o volume dela.
    private fun forwardToMusic(direction: Int) {
        if (audio.isMusicActive) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
    }

    // No modo duplo os dois lados armam (a direção escolhe o tipo); no modo simples, vale o botão escolhido.
    private fun matchesTriggerKey(direction: Int): Boolean {
        if (settings.dualSos) return true
        return when (settings.triggerKey) {
            TriggerKey.VOLUME_UP -> direction > 0
            TriggerKey.VOLUME_DOWN -> direction < 0
            TriggerKey.ANY -> true
        }
    }

    // Volume − = Segurança, Volume + = Saúde (só no modo duplo). No simples, usa o tipo configurado.
    private fun profileFor(direction: Int): Profile = when {
        !settings.dualSos -> settings.profile
        direction > 0 -> Profile.SAUDE
        else -> Profile.SEGURANCA
    }

    private fun volumeDirection(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
        KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
        else -> null
    }

    private fun detectorConfig() = settings.let {
        PressPatternDetector.Config(mode = it.triggerMode, holdMillis = it.holdSeconds * 1_000L, tapCount = it.tapCount)
    }

    private companion object {
        const val SHORT_PRESS_MILLIS = 1_000L
        const val POLL_RETRY_MILLIS = 200L
        const val TAPS_VOLUME_DELAY_MILLIS = 1_000L
        const val WAKE_MARGIN_MILLIS = 5_000L
    }
}

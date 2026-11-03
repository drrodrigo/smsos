package br.com.alertasilencioso.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.PowerManager
import androidx.core.content.ContextCompat
import br.com.alertasilencioso.data.Repository

/**
 * Com a tela apagada o Android não entrega os botões ao serviço de acessibilidade. O caminho que sobra é
 * se apresentar como um "player remoto" (como o de um Chromecast): aí o Android manda para o app cada
 * aperto do volume em vez de mudar o volume do celular. Isso só fica ligado enquanto a tela está apagada.
 *
 * Funciona na maioria dos aparelhos, mas alguns fabricantes bloqueiam. Por isso existe o modo teste.
 */
class ScreenOffVolumeListener(
    private val context: Context,
    private val onVolume: (direction: Int) -> Unit,
) {
    private val power = context.getSystemService(PowerManager::class.java)
    private val session = MediaSession(context, "AlertaSilencioso")
    private var registered = false

    private val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
        override fun onAdjustVolume(direction: Int) = onVolume(direction)
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> activate()
                Intent.ACTION_SCREEN_ON -> deactivate()
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        // Só o próprio Android envia estes avisos de tela; "exportado" não abre nada para outros apps.
        ContextCompat.registerReceiver(context, screenReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        registered = true
        if (!power.isInteractive) activate()
    }

    fun stop() {
        if (registered) {
            context.unregisterReceiver(screenReceiver)
            registered = false
        }
        deactivate()
        session.release()
    }

    private fun activate() {
        if (!Repository.state.value.settings.screenOffDetection) return
        session.setPlaybackToRemote(volumeProvider)
        session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0L, 1f).build())
        session.isActive = true
    }

    private fun deactivate() {
        session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED, 0L, 0f).build())
        session.isActive = false
    }
}

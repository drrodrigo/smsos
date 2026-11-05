package br.com.alertasilencioso.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

/**
 * Toca um alarme ALTO em loop e vibra — como uma chamada — até o guardião ver o alerta. Usa o volume de
 * ALARME, então é ouvido mesmo com o celular no silencioso. Para sozinho depois de um tempo (segurança).
 */
object Alarme {
    private const val TAG = "Alarme"
    private const val LIMITE_MILLIS = 120_000L
    private val handler = Handler(Looper.getMainLooper())
    private var mp: MediaPlayer? = null

    fun tocar(context: Context) {
        parar(context)
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        runCatching {
            mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { Log.w(TAG, "Não consegui tocar o alarme", it) }
        vibrar(context)
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ parar(context) }, LIMITE_MILLIS)
    }

    fun parar(context: Context) {
        handler.removeCallbacksAndMessages(null)
        runCatching {
            mp?.let { if (it.isPlaying) it.stop(); it.release() }
        }
        mp = null
        runCatching { vibrador(context)?.cancel() }
    }

    private fun vibrar(context: Context) {
        val v = vibrador(context) ?: return
        val padrao = longArrayOf(0, 700, 400, 700, 400)
        runCatching { v.vibrate(VibrationEffect.createWaveform(padrao, 0)) }
    }

    private fun vibrador(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}

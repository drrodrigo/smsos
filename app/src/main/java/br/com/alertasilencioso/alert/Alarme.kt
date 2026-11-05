package br.com.alertasilencioso.alert

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import br.com.alertasilencioso.R

/**
 * Alarme de emergência: toca a SIRENE embutida (dois tons, alta e distinta) em loop, com o volume de
 * ALARME no MÁXIMO — mesmo no silencioso. VIBRA e PISCA A LANTERNA, para o guardião perceber mesmo com
 * o celular no bolso ou em reunião. Só para quando a pessoa silencia, ou sozinho depois de 2 minutos.
 */
object Alarme {
    private const val TAG = "Alarme"
    private const val LIMITE_MILLIS = 120_000L
    private val handler = Handler(Looper.getMainLooper())
    private var mp: MediaPlayer? = null
    private var volumeAnterior: Int? = null

    private var cameraManager: CameraManager? = null
    private var flashId: String? = null
    private var flashLigado = false
    private val piscaLanterna = object : Runnable {
        override fun run() {
            val cm = cameraManager
            val id = flashId
            if (cm == null || id == null) return
            flashLigado = !flashLigado
            runCatching { cm.setTorchMode(id, flashLigado) }
            handler.postDelayed(this, 450)
        }
    }

    fun tocar(context: Context) {
        parar(context)
        maximizarVolume(context)
        runCatching {
            context.resources.openRawResourceFd(R.raw.sirene).use { afd ->
                mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    isLooping = true
                    setVolume(1f, 1f)
                    prepare()
                    start()
                }
            }
        }.onFailure { Log.w(TAG, "Não consegui tocar a sirene", it) }
        vibrar(context)
        iniciarLanterna(context)
        handler.postDelayed({ parar(context) }, LIMITE_MILLIS)
    }

    fun parar(context: Context) {
        handler.removeCallbacksAndMessages(null)
        runCatching {
            mp?.let { if (it.isPlaying) it.stop(); it.release() }
        }
        mp = null
        runCatching { vibrador(context)?.cancel() }
        pararLanterna()
        restaurarVolume(context)
    }

    /** Põe o volume de ALARME no máximo, guardando o anterior para devolver depois. */
    private fun maximizarVolume(context: Context) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            volumeAnterior = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }
    }

    private fun restaurarVolume(context: Context) {
        val anterior = volumeAnterior ?: return
        volumeAnterior = null
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, anterior, 0)
        }
    }

    private fun iniciarLanterna(context: Context) {
        runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cameraManager = cm
            flashId = id
            handler.post(piscaLanterna)
        }
    }

    private fun pararLanterna() {
        handler.removeCallbacks(piscaLanterna)
        runCatching { flashId?.let { cameraManager?.setTorchMode(it, false) } }
        flashLigado = false
        cameraManager = null
        flashId = null
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

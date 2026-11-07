package br.com.alertasilencioso.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/** O que a captura juntou ao acionar o SOS. */
data class CapturaResultado(
    val videoUri: String? = null,
    val segundos: Int = 0,
    val comAudio: Boolean = false,
    val erros: List<String> = emptyList(),
) {
    val temVideo get() = videoUri != null
}

/**
 * Ao acionar o SOS, grava um VÍDEO curto (câmera frontal, COM áudio do ambiente), sem abrir a tela —
 * prova do que estava acontecendo pra mandar aos guardiões. A localização já vem pelo LocationFetcher.
 *
 * Experimental e OPT-IN (desligado por padrão). Câmera/microfone são permissões sensíveis: entra como
 * ATUALIZAÇÃO depois da aprovação nas lojas, nunca na 1ª versão.
 */
object Captura {
    private const val TAG = "Captura"

    private var recording: Recording? = null

    fun temCamera(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun temAudio(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun capturar(context: Context, segundos: Int, onResult: (CapturaResultado) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        if (!temCamera(context)) {
            main.post { onResult(CapturaResultado(erros = listOf("Sem permissão de câmera"))) }
            return
        }
        var entregue = false
        val entregar = { r: CapturaResultado ->
            if (!entregue) { entregue = true; main.post { onResult(r) } }
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = try {
                future.get()
            } catch (e: Exception) {
                Log.w(TAG, "Câmera não abriu", e)
                entregar(CapturaResultado(erros = listOf("Câmera não abriu: ${e.message}")))
                return@addListener
            }
            main.post {
                val lifecycle = CameraLifecycle()
                try {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.fromOrderedList(listOf(Quality.SD, Quality.LOWEST)))
                        .build()
                    val videoCapture = VideoCapture.withOutput(recorder)
                    val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    lifecycle.resume()
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle, selector, videoCapture)

                    val valores = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "SOS_${System.currentTimeMillis()}.mp4")
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GuardioesSOS")
                        }
                    }
                    val output = MediaStoreOutputOptions.Builder(
                        context.contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    ).setContentValues(valores).build()

                    val comAudio = temAudio(context)
                    var pending = videoCapture.output.prepareRecording(context, output)
                    if (comAudio) pending = pending.withAudioEnabled()
                    recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            runCatching { provider.unbindAll() }
                            lifecycle.destroy()
                            recording = null
                            val uri = event.outputResults.outputUri
                            if (!event.hasError() && uri != Uri.EMPTY) {
                                entregar(CapturaResultado(uri.toString(), segundos, comAudio))
                            } else {
                                Log.w(TAG, "Vídeo falhou: erro ${event.error}")
                                entregar(CapturaResultado(erros = listOf("Vídeo falhou (erro ${event.error})")))
                            }
                        }
                    }
                    // Para a gravação depois de [segundos].
                    main.postDelayed({ runCatching { recording?.stop() } }, segundos * 1000L)
                } catch (e: Exception) {
                    Log.w(TAG, "Vídeo indisponível", e)
                    runCatching { provider.unbindAll() }
                    lifecycle.destroy()
                    entregar(CapturaResultado(erros = listOf("Câmera indisponível: ${e.message}")))
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Dono de ciclo de vida "de mentira" para usar a CameraX sem uma tela aberta. Só na thread principal. */
    private class CameraLifecycle : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun resume() { registry.currentState = Lifecycle.State.RESUMED }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}

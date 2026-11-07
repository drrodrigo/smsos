package br.com.alertasilencioso.capture

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.io.File
import java.util.concurrent.Executors

/** O que a captura conseguiu juntar quando o SOS foi acionado. */
data class CapturaResultado(
    val fotoUri: String? = null,
    val audioPath: String? = null,
    val audioSegundos: Int = 0,
    val erros: List<String> = emptyList(),
) {
    val temFoto get() = fotoUri != null
    val temAudio get() = audioPath != null
}

/**
 * Ao acionar o SOS, tira uma FOTO (câmera frontal, sem abrir tela) e grava um ÁUDIO curto do ambiente —
 * prova do que estava acontecendo, para mandar aos guardiões. A localização já vem pelo LocationFetcher.
 *
 * Experimental e OPT-IN (desligado por padrão). Câmera/microfone são permissões sensíveis: por isso
 * esse recurso entra como ATUALIZAÇÃO depois da aprovação nas lojas, nunca na 1ª versão.
 */
object Captura {
    private const val TAG = "Captura"

    fun temCamera(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun temAudio(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Faz foto + áudio em paralelo e chama [onResult] UMA vez, na thread principal, quando os dois terminam. */
    fun capturar(context: Context, audioMs: Long, onResult: (CapturaResultado) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        val erros = mutableListOf<String>()
        var fotoUri: String? = null
        var audioPath: String? = null
        var audioSeg = 0
        var pendentes = 2
        var entregue = false
        val terminarUm = {
            pendentes--
            if (pendentes <= 0 && !entregue) {
                entregue = true
                main.post { onResult(CapturaResultado(fotoUri, audioPath, audioSeg, erros.toList())) }
            }
        }

        if (temAudio(context)) {
            gravarAudio(context, audioMs) { path, seg, erro ->
                audioPath = path
                audioSeg = seg
                if (erro != null) erros.add(erro)
                terminarUm()
            }
        } else {
            erros.add("Sem permissão de microfone")
            terminarUm()
        }

        if (temCamera(context)) {
            tirarFoto(context, main) { uri, erro ->
                fotoUri = uri
                if (erro != null) erros.add(erro)
                terminarUm()
            }
        } else {
            erros.add("Sem permissão de câmera")
            terminarUm()
        }
    }

    private fun gravarAudio(context: Context, audioMs: Long, onDone: (String?, Int, String?) -> Unit) {
        val dir = File(context.getExternalFilesDir(null), "sos").apply { mkdirs() }
        val file = File(dir, "audio_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao iniciar áudio", e)
            runCatching { rec.release() }
            onDone(null, 0, "Áudio falhou: ${e.message}")
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            val ok = runCatching { rec.stop() }.isSuccess
            runCatching { rec.release() }
            if (ok && file.exists() && file.length() > 0) {
                onDone(file.absolutePath, (audioMs / 1000).toInt(), null)
            } else {
                onDone(null, 0, "Áudio não gravou (aparelho recusou)")
            }
        }, audioMs)
    }

    private fun tirarFoto(context: Context, main: Handler, onDone: (String?, String?) -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = try {
                future.get()
            } catch (e: Exception) {
                Log.w(TAG, "Câmera não abriu", e)
                onDone(null, "Câmera não abriu: ${e.message}")
                return@addListener
            }
            main.post {
                val lifecycle = CameraLifecycle()
                try {
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    lifecycle.resume()
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle, selector, imageCapture)

                    val valores = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "SOS_${System.currentTimeMillis()}.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GuardioesSOS")
                        }
                    }
                    val saida = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        valores,
                    ).build()

                    imageCapture.takePicture(
                        saida,
                        Executors.newSingleThreadExecutor(),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                main.post { runCatching { provider.unbindAll() }; lifecycle.destroy() }
                                onDone(results.savedUri?.toString(), null)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.w(TAG, "Falha na foto", exc)
                                main.post { runCatching { provider.unbindAll() }; lifecycle.destroy() }
                                onDone(null, "Foto falhou: ${exc.message}")
                            }
                        },
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Câmera indisponível", e)
                    runCatching { provider.unbindAll() }
                    lifecycle.destroy()
                    onDone(null, "Câmera indisponível: ${e.message}")
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

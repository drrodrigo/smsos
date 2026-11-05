package br.com.alertasilencioso.alert

import android.util.Log
import br.com.alertasilencioso.BuildConfig
import br.com.alertasilencioso.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Quando a pessoa aciona o SOS, avisa os GUARDIÕES dela pelo app: o servidor (guardioes-alertar) acha
 * quem aceitou protegê-la e dispara a SIRENE (push) em cada aparelho. Roda em background e NUNCA trava
 * o alerta — o SMS pelo gateway sai de qualquer jeito. Sem gateway configurado, não faz nada.
 */
object Alertar {
    private const val TAG = "Alertar"
    private const val TIMEOUT_MS = 15_000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun configurado() = BuildConfig.GATEWAY_URL.isNotBlank() && BuildConfig.GATEWAY_KEY.isNotBlank()
    private fun url() = BuildConfig.GATEWAY_URL.substringBeforeLast('/') + "/guardioes-alertar"

    /** Dispara a sirene nos guardiões desta pessoa, em background. */
    fun alertarGuardioes(titulo: String, corpo: String) {
        if (!configurado()) return
        val dispositivo = Repository.state.value.settings.dispositivo
        if (dispositivo.isBlank()) return
        scope.launch { withContext(Dispatchers.IO) { enviar(dispositivo, titulo, corpo) } }
    }

    private fun enviar(dispositivo: String, titulo: String, corpo: String) {
        val body = JSONObject().apply {
            put("dispositivo", dispositivo)
            put("titulo", titulo)
            put("corpo", corpo)
        }.toString()
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-smsos-app", BuildConfig.GATEWAY_KEY)
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            Log.i(TAG, "guardioes-alertar -> ${conn.responseCode}")
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao tocar nos guardiões (o SMS já saiu)", e)
        } finally {
            conn?.disconnect()
        }
    }
}

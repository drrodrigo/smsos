package br.com.alertasilencioso.alert

import android.util.Log
import br.com.alertasilencioso.BuildConfig
import br.com.alertasilencioso.core.PhoneNumbers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Confirmação de número por SMS (código de 6 dígitos), pela mesma estrutura e chave do gateway
 * (função guardioes-otp). O código vai por SMS da NOSSA estrutura — não usa a permissão de SMS do app.
 */
object Otp {
    private const val TAG = "Otp"
    private const val TIMEOUT_MS = 15_000

    data class Resultado(val ok: Boolean, val erro: String?)

    fun configurado(): Boolean = BuildConfig.GATEWAY_URL.isNotBlank() && BuildConfig.GATEWAY_KEY.isNotBlank()

    private fun url(): String = BuildConfig.GATEWAY_URL.substringBeforeLast('/') + "/guardioes-otp"

    private fun post(corpo: JSONObject): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url()).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-smsos-app", BuildConfig.GATEWAY_KEY)
            }
            conn.outputStream.use { it.write(corpo.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val txt = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code in 200..299) txt else { Log.w(TAG, "erro $code: $txt"); null }
        } catch (e: Exception) {
            Log.w(TAG, "falha", e); null
        } finally {
            conn?.disconnect()
        }
    }

    /** Manda um código de 6 dígitos por SMS para o número. */
    suspend fun enviar(telefone: String): Boolean = withContext(Dispatchers.IO) {
        if (!configurado()) return@withContext false
        val txt = post(JSONObject().put("acao", "enviar").put("telefone", PhoneNumbers.normalize(telefone)))
            ?: return@withContext false
        runCatching { JSONObject(txt).optBoolean("ok", false) }.getOrDefault(false)
    }

    /** Confere o código. Devolve ok + motivo do erro (expirado, codigo_incorreto, ...). */
    suspend fun confirmar(telefone: String, codigo: String): Resultado = withContext(Dispatchers.IO) {
        if (!configurado()) return@withContext Resultado(false, "sem_conexao")
        val txt = post(
            JSONObject().put("acao", "verificar")
                .put("telefone", PhoneNumbers.normalize(telefone))
                .put("codigo", codigo.trim()),
        ) ?: return@withContext Resultado(false, "sem_conexao")
        val o = runCatching { JSONObject(txt) }.getOrNull() ?: return@withContext Resultado(false, "falhou")
        Resultado(o.optBoolean("ok", false), o.optString("erro").ifBlank { null })
    }
}

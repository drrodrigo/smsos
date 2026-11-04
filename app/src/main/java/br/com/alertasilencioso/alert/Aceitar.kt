package br.com.alertasilencioso.alert

import android.util.Log
import br.com.alertasilencioso.BuildConfig
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * O lado do GUARDIÃO: ver convites pendentes pelo telefone, aceitar por código e listar quem protejo.
 * Mesma estrutura e chave do gateway (função guardioes-convite). Sem senha.
 */
object Aceitar {
    private const val TAG = "Aceitar"
    private const val TIMEOUT_MS = 15_000

    data class Pendente(val codigo: String, val usuarioNome: String)
    data class Protegido(val usuarioNome: String)
    data class Resultado(val ok: Boolean, val usuarioNome: String?, val jaAceito: Boolean)

    fun configurado(): Boolean = BuildConfig.GATEWAY_URL.isNotBlank() && BuildConfig.GATEWAY_KEY.isNotBlank()

    private fun url(): String = BuildConfig.GATEWAY_URL.substringBeforeLast('/') + "/guardioes-convite"

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

    private fun objetos(txt: String?): List<JSONObject> {
        val arr = txt?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    /** Convites pendentes para o telefone deste usuário (aparecem sem precisar do código). */
    suspend fun pendentes(): List<Pendente> = withContext(Dispatchers.IO) {
        val s = Repository.state.value.settings
        if (!configurado() || s.userPhone.isBlank()) return@withContext emptyList()
        val txt = post(JSONObject().put("acao", "do_telefone").put("telefone", PhoneNumbers.normalize(s.userPhone)))
        objetos(txt).map { Pendente(it.optString("codigo"), it.optString("usuario_nome")) }
    }

    /** Aceita um convite pelo código. */
    suspend fun aceitar(codigo: String): Resultado? = withContext(Dispatchers.IO) {
        val s = Repository.state.value.settings
        if (!configurado() || s.dispositivo.isBlank()) return@withContext null
        val txt = post(
            JSONObject().put("acao", "aceitar").put("codigo", codigo.trim())
                .put("guardiao_dispositivo", s.dispositivo),
        ) ?: return@withContext null
        val o = runCatching { JSONObject(txt) }.getOrNull() ?: return@withContext null
        Resultado(o.optBoolean("ok", false), o.optString("usuario_nome").ifBlank { null }, o.optBoolean("ja_aceito", false))
    }

    /** Quem eu protejo (convites que aceitei). */
    suspend fun quemProtejo(): List<Protegido> = withContext(Dispatchers.IO) {
        val s = Repository.state.value.settings
        if (!configurado() || s.dispositivo.isBlank()) return@withContext emptyList()
        val txt = post(JSONObject().put("acao", "quem_protejo").put("guardiao_dispositivo", s.dispositivo))
        objetos(txt).map { Protegido(it.optString("usuario_nome")) }
    }
}

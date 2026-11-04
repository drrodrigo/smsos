package br.com.alertasilencioso.alert

import android.util.Log
import br.com.alertasilencioso.BuildConfig
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.StateJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Backup da CONFIGURAÇÃO (guardiões, lugares, ajustes) na nuvem, pela conta = telefone.
 * O HISTÓRICO não entra (fica só no aparelho). Restaurar exige o código OTP (prova de posse do número).
 */
object Backup {
    private const val TAG = "Backup"
    private const val TIMEOUT_MS = 15_000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class Tem(val existe: Boolean, val resumo: String?)
    data class Restauracao(val ok: Boolean, val erro: String?, val dados: String?)

    fun configurado(): Boolean = BuildConfig.GATEWAY_URL.isNotBlank() && BuildConfig.GATEWAY_KEY.isNotBlank()

    private fun url(): String = BuildConfig.GATEWAY_URL.substringBeforeLast('/') + "/guardioes-backup"

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

    /** Salva o backup da config (sem o histórico) em background. Chame ao sair do app. */
    fun salvarEmBackground() {
        val s = Repository.state.value.settings
        if (!configurado() || s.userPhone.isBlank()) return
        scope.launch { salvar() }
    }

    private fun salvar(): Boolean {
        val state = Repository.state.value
        val s = state.settings
        if (s.userPhone.isBlank()) return false
        // Tira o histórico e o alerta ativo do backup — só vai a configuração.
        val dados = StateJson.encode(state.copy(history = emptyList(), activeAlert = null))
        val resumo = "${state.alwaysContacts.size} guardião(ões), ${state.places.size} lugar(es)"
        val corpo = JSONObject()
            .put("acao", "salvar")
            .put("telefone", PhoneNumbers.normalize(s.userPhone))
            .put("resumo", resumo)
            .put("dados", JSONObject(dados))
        val txt = post(corpo) ?: return false
        return runCatching { JSONObject(txt).optBoolean("ok", false) }.getOrDefault(false)
    }

    /** Existe backup para este número? (sem expor os dados) */
    suspend fun tem(telefone: String): Tem = withContext(Dispatchers.IO) {
        if (!configurado()) return@withContext Tem(false, null)
        val txt = post(JSONObject().put("acao", "tem").put("telefone", PhoneNumbers.normalize(telefone)))
            ?: return@withContext Tem(false, null)
        val o = runCatching { JSONObject(txt) }.getOrNull() ?: return@withContext Tem(false, null)
        Tem(o.optBoolean("existe", false), o.optString("resumo").ifBlank { null })
    }

    /** Restaura: devolve os dados (JSON) se o código OTP conferir. */
    suspend fun restaurar(telefone: String, codigo: String): Restauracao = withContext(Dispatchers.IO) {
        if (!configurado()) return@withContext Restauracao(false, "sem_conexao", null)
        val txt = post(
            JSONObject().put("acao", "restaurar")
                .put("telefone", PhoneNumbers.normalize(telefone))
                .put("codigo", codigo.trim()),
        ) ?: return@withContext Restauracao(false, "sem_conexao", null)
        val o = runCatching { JSONObject(txt) }.getOrNull() ?: return@withContext Restauracao(false, "falhou", null)
        val dados = if (o.isNull("dados")) null else o.optJSONObject("dados")?.toString()
        Restauracao(o.optBoolean("ok", false), o.optString("erro").ifBlank { null }, dados)
    }
}

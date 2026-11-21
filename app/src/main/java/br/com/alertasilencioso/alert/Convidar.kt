package br.com.alertasilencioso.alert

import android.util.Log
import br.com.alertasilencioso.BuildConfig
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cria o convite de guardião pela MESMA estrutura e chave do gateway (função guardioes-convite).
 * Devolve o código e o link que a pessoa compartilha (WhatsApp/SMS). Sem senha.
 */
object Convidar {
    private const val TAG = "Convidar"
    private const val TIMEOUT_MS = 15_000

    data class Resultado(val codigo: String, val link: String)

    fun configurado(): Boolean = BuildConfig.GATEWAY_URL.isNotBlank() && BuildConfig.GATEWAY_KEY.isNotBlank()

    private fun url(): String = BuildConfig.GATEWAY_URL.substringBeforeLast('/') + "/guardioes-convite"

    /**
     * Em que pé está cada guardião que este aparelho convidou (ação "situacao" da guardioes-convite): falta aceitar,
     * aceitou e o celular toca, ou aceitou sem aviso. Chave = [PhoneNumbers.key]. Nulo = sem resposta (sem internet,
     * servidor sem a ação): a tela usa o que o aparelho lembra. Faz rede: chame fora da main.
     */
    suspend fun situacao(): Map<String, SituacaoDoGuardiao>? = withContext(Dispatchers.IO) {
        if (!configurado()) return@withContext null
        val dispositivo = Repository.state.value.settings.dispositivo
        if (dispositivo.isBlank()) return@withContext null
        val corpo = JSONObject().put("acao", "situacao").put("usuario_dispositivo", dispositivo).toString()
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
            conn.outputStream.use { it.write(corpo.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return@withContext null
            SituacaoDoGuardiao.lerDoServidor(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            Log.w(TAG, "Sem a situação dos guardiões", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Cria (ou reaproveita) o convite para este telefone. Faz rede: chame fora da main. */
    suspend fun convidar(telefoneGuardiao: String): Resultado? = withContext(Dispatchers.IO) {
        if (!configurado()) return@withContext null
        val s = Repository.state.value.settings
        if (s.dispositivo.isBlank()) return@withContext null
        val corpo = JSONObject().apply {
            put("acao", "convidar")
            put("usuario_dispositivo", s.dispositivo)
            put("usuario_nome", s.userName)
            put("guardiao_telefone", PhoneNumbers.normalize(telefoneGuardiao))
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
            conn.outputStream.use { it.write(corpo.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val texto = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                Log.w(TAG, "convite falhou ($code): $texto")
                return@withContext null
            }
            val o = JSONObject(texto)
            val codigo = o.optString("codigo")
            val link = o.optString("link")
            if (codigo.isBlank()) null else Resultado(codigo, link)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao convidar", e)
            null
        } finally {
            conn?.disconnect()
        }
    }
}

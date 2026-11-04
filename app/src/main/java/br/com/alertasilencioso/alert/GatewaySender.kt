package br.com.alertasilencioso.alert

import android.util.Log
import br.com.alertasilencioso.BuildConfig
import br.com.alertasilencioso.core.PhoneNumbers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manda o alerta pela NOSSA estrutura (função smsos-alerta -> SMS Market), pela internet.
 *
 * O celular não usa o chip nem guarda a credencial do SMS Market: ele só conhece a chave do app,
 * que serve apenas para falar com a nossa função. Assim nada que dê para extrair do APK consegue
 * gastar o pacote direto -- a função é quem fala com o fornecedor, e os limites moram no servidor.
 *
 * É BLOQUEANTE (faz rede): chame sempre fora da thread principal.
 */
object GatewaySender {
    private const val TAG = "GatewaySender"
    private const val MAX_POR_CHAMADA = 15
    private const val TIMEOUT_MS = 20_000

    fun configurado(): Boolean =
        BuildConfig.GATEWAY_URL.isNotBlank() && BuildConfig.GATEWAY_KEY.isNotBlank()

    /**
     * Envia cada par (telefone, texto). Retorna, por telefone, se a nossa função aceitou e o
     * fornecedor enfileirou. Divide em lotes para respeitar o limite por chamada da função.
     */
    fun enviar(envios: List<Pair<String, String>>): Map<String, Boolean> {
        val resultado = HashMap<String, Boolean>()
        envios.forEach { resultado[it.first] = false }
        if (!configurado()) return resultado
        envios.chunked(MAX_POR_CHAMADA).forEach { lote -> resultado.putAll(enviarLote(lote)) }
        return resultado
    }

    private fun enviarLote(lote: List<Pair<String, String>>): Map<String, Boolean> {
        val resultado = HashMap<String, Boolean>()
        lote.forEach { resultado[it.first] = false }

        val corpo = JSONObject().apply {
            put("envios", JSONArray().apply {
                lote.forEach { (telefone, texto) ->
                    put(JSONObject().apply {
                        put("telefone", PhoneNumbers.clean(telefone))
                        put("texto", texto)
                    })
                }
            })
        }.toString()

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(BuildConfig.GATEWAY_URL).openConnection() as HttpURLConnection).apply {
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
            val arr = runCatching { JSONObject(texto) }.getOrNull()?.optJSONArray("resultados")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val tel = item.optString("telefone")
                    val ok = item.optBoolean("ok", false)
                    lote.firstOrNull { PhoneNumbers.clean(it.first) == tel }?.let { resultado[it.first] = ok }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao falar com o gateway", e)
        } finally {
            conn?.disconnect()
        }
        return resultado
    }
}

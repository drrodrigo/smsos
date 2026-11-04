package br.com.alertasilencioso.alert

import android.util.Log
import br.com.alertasilencioso.BuildConfig
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Cadastra/atualiza o usuário do app (o lead) na nossa conta, pela MESMA estrutura e chave do gateway
 * (função guardioes-registrar). Sem senha: a identidade é o aparelho + o telefone.
 *
 * NUNCA atrapalha o alerta nem trava a tela: roda em background e, se a rede falhar, tenta de novo no
 * próximo início. Sem gateway configurado, não faz nada.
 */
object Registrar {
    private const val TAG = "Registrar"
    private const val TIMEOUT_MS = 15_000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun configurado(): Boolean = BuildConfig.GATEWAY_URL.isNotBlank() && BuildConfig.GATEWAY_KEY.isNotBlank()

    // Mesma base do gateway, só troca o nome da função.
    private fun url(): String = BuildConfig.GATEWAY_URL.substringBeforeLast('/') + "/guardioes-registrar"

    /** Dispara o cadastro em background (não espera a rede). Marca "registrado" quando o servidor aceita. */
    fun registrar() {
        if (!configurado()) return
        val s = Repository.state.value.settings
        if (s.dispositivo.isBlank()) return
        scope.launch {
            val ok = withContext(Dispatchers.IO) { enviar(s) }
            if (ok) Repository.updateSettings { it.copy(registrado = true) }
        }
    }

    /** No início do app: se tem telefone mas o cadastro ainda não foi confirmado, tenta de novo. */
    fun registrarSePendente() {
        val s = Repository.state.value.settings
        if (s.userPhone.isNotBlank() && !s.registrado) registrar()
    }

    private fun enviar(s: Settings): Boolean {
        val corpo = JSONObject().apply {
            put("dispositivo", s.dispositivo)
            put("nome", s.userName)
            put("telefone", s.userPhone)
            put("idioma", Locale.getDefault().language.ifBlank { "pt" })
            put("plataforma", "android")
            put("versao", BuildConfig.VERSION_NAME)
            if (s.tokenPush.isNotBlank()) put("token_push", s.tokenPush)
        }.toString()
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
            conn.outputStream.use { it.write(corpo.toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao registrar (tenta de novo depois)", e)
            false
        } finally {
            conn?.disconnect()
        }
    }
}

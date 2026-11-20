package br.com.alertasilencioso.relogio

import org.json.JSONObject

/**
 * O CONTRATO entre o relógio e o celular (a ponte do Wear OS). Ele mora em DOIS lugares, com os MESMOS nomes e
 * valores: aqui e em `app/.../relogio/ContratoDoRelogio.kt`. Mudou um, mude o outro: o ContratoDoRelogioTest
 * (módulo app) lê este arquivo e reprova se os caminhos ou as chaves não baterem.
 *
 * Ida (relógio → celular), caminho [CAMINHO_SOS]: `{"perfil":"seguranca"|"saude","id":"<aleatório>"}`.
 * Volta (celular → relógio), caminho [CAMINHO_RESULTADO]: `{"id","ok":bool,"teste":bool,"contatos":N,"erro":?}`.
 */
object Contrato {
    const val CAMINHO_SOS = "/guardioes/sos"
    const val CAMINHO_RESULTADO = "/guardioes/sos/resultado"

    const val CHAVE_PERFIL = "perfil"
    const val CHAVE_ID = "id"
    const val CHAVE_OK = "ok"
    const val CHAVE_TESTE = "teste"
    const val CHAVE_CONTATOS = "contatos"
    const val CHAVE_ERRO = "erro"

    const val PERFIL_SEGURANCA = "seguranca"
    const val PERFIL_SAUDE = "saude"

    /** O pedido de SOS que vai para o celular. */
    fun pedido(perfil: Perfil, id: String): String = JSONObject()
        .put(CHAVE_PERFIL, if (perfil == Perfil.SAUDE) PERFIL_SAUDE else PERFIL_SEGURANCA)
        .put(CHAVE_ID, id)
        .toString()

    /** A resposta do celular. Texto que não é a resposta (ou sem id): null, e o relógio continua esperando. */
    fun lerResultado(texto: String): Resultado? {
        val json = runCatching { JSONObject(texto) }.getOrNull() ?: return null
        val id = json.optString(CHAVE_ID, "")
        if (id.isEmpty() || !json.has(CHAVE_OK)) return null
        return Resultado(
            id = id,
            ok = json.optBoolean(CHAVE_OK, false),
            teste = json.optBoolean(CHAVE_TESTE, false),
            contatos = if (json.has(CHAVE_CONTATOS) && !json.isNull(CHAVE_CONTATOS)) json.optInt(CHAVE_CONTATOS, -1).takeIf { it >= 0 } else null,
            erro = if (json.has(CHAVE_ERRO) && !json.isNull(CHAVE_ERRO)) json.optString(CHAVE_ERRO).ifBlank { null } else null,
        )
    }
}

/** O tipo do SOS, igual ao do celular (Profile.SEGURANCA / Profile.SAUDE). */
enum class Perfil { SEGURANCA, SAUDE }

/** O que o celular respondeu ao pedido [id]. [contatos] = quantos receberiam (null quando o celular não disse). */
data class Resultado(
    val id: String,
    val ok: Boolean,
    val teste: Boolean,
    val contatos: Int?,
    val erro: String?,
)

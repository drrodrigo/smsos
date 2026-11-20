package br.com.alertasilencioso.relogio

import br.com.alertasilencioso.data.Profile
import org.json.JSONObject

/**
 * O CONTRATO entre o celular e o app do RELÓGIO (a ponte do Wear OS). Ele mora em DOIS lugares, com os MESMOS
 * nomes e valores: aqui e em `relogio/src/main/java/br/com/alertasilencioso/relogio/Contrato.kt`. Mudou um, mude
 * o outro: o ContratoDoRelogioTest lê o arquivo do relógio e reprova se os caminhos ou as chaves não baterem.
 *
 * Ida (relógio → celular), caminho [CAMINHO_SOS]: `{"perfil":"seguranca"|"saude","id":"<aleatório>"}`.
 * Volta (celular → relógio), caminho [CAMINHO_RESULTADO]: `{"id","ok":bool,"teste":bool,"contatos":N,"erro":?}`.
 */
object ContratoDoRelogio {
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

    /** O que o relógio pediu. */
    data class Pedido(val perfil: Profile, val id: String)

    /**
     * Lê o pedido do relógio. Só o NOSSO app de relógio (mesmo pacote e mesma assinatura) fala neste caminho, então
     * qualquer mensagem aqui é um SOS: na dúvida (texto estranho, tipo desconhecido) dispara o de SEGURANÇA.
     */
    fun lerPedido(texto: String): Pedido {
        val json = runCatching { JSONObject(texto) }.getOrNull() ?: return Pedido(Profile.SEGURANCA, "")
        val perfil = if (json.optString(CHAVE_PERFIL) == PERFIL_SAUDE) Profile.SAUDE else Profile.SEGURANCA
        return Pedido(perfil, json.optString(CHAVE_ID, ""))
    }

    /** A resposta ao relógio. [contatos] = quantos receberiam (null: não diz). [erro] só quando não deu certo. */
    fun resultado(id: String, ok: Boolean, teste: Boolean, contatos: Int?, erro: String? = null): String =
        JSONObject().apply {
            put(CHAVE_ID, id)
            put(CHAVE_OK, ok)
            put(CHAVE_TESTE, teste)
            if (contatos != null) put(CHAVE_CONTATOS, contatos)
            if (!erro.isNullOrBlank()) put(CHAVE_ERRO, erro)
        }.toString()
}

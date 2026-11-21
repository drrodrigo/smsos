package br.com.alertasilencioso.alert

import br.com.alertasilencioso.core.PhoneNumbers
import org.json.JSONObject

/**
 * Os DOIS TIPOS de guardião, como o Rodrigo pediu (21/11): "quando põe o telefone, guardião SMS; o do convite,
 * SMS e toca no celular". Cada guardião da lista mostra em qual está.
 *
 *  - [SO_SMS]: só o número na lista. Recebe o SMS com a localização.
 *  - [CONVITE_ENVIADO]: o convite saiu, mas ainda não foi aceito no app dele. Até lá, só SMS.
 *  - [COM_APP]: aceitou no app e o app dele recebe aviso. Recebe o SMS E o celular TOCA a sirene.
 *  - [ACEITOU_SEM_AVISO]: aceitou, mas o app dele não tem como receber aviso (notificação bloqueada, app
 *    desinstalado...). Na prática, só SMS até ele abrir o app de novo.
 *
 * Quem sabe a verdade é o servidor (ação "situacao" da guardioes-convite). Sem resposta dele (sem internet,
 * servidor antigo), vale o que este aparelho lembra: convite mandado daqui = [CONVITE_ENVIADO].
 */
enum class SituacaoDoGuardiao {
    SO_SMS, CONVITE_ENVIADO, COM_APP, ACEITOU_SEM_AVISO;

    /** O celular dele toca a sirene no SOS? */
    val tocaNoCelular: Boolean get() = this == COM_APP

    companion object {
        /** A situação de um número: primeiro o servidor ([servidor] nulo = sem resposta), depois o que o aparelho lembra. */
        fun de(telefone: String, servidor: Map<String, SituacaoDoGuardiao>?, convitesEnviados: Set<String>): SituacaoDoGuardiao {
            val chave = PhoneNumbers.key(telefone)
            servidor?.get(chave)?.let { return it }
            return if (chave in convitesEnviados) CONVITE_ENVIADO else SO_SMS
        }

        /**
         * Lê a resposta do servidor: {"guardioes":[{"telefone":"31984491586","status":"aceito","tem_app":true}, ...]}.
         * Devolve nulo se não for essa resposta (ex.: servidor antigo, "acao_desconhecida").
         */
        fun lerDoServidor(texto: String): Map<String, SituacaoDoGuardiao>? = runCatching {
            val lista = JSONObject(texto).optJSONArray("guardioes") ?: return null
            buildMap {
                for (i in 0 until lista.length()) {
                    val g = lista.optJSONObject(i) ?: continue
                    val chave = PhoneNumbers.key(g.optString("telefone"))
                    if (chave.isBlank()) continue
                    val situacao = when {
                        g.optString("status") != "aceito" -> CONVITE_ENVIADO
                        g.optBoolean("tem_app", false) -> COM_APP
                        else -> ACEITOU_SEM_AVISO
                    }
                    put(chave, situacao)
                }
            }
        }.getOrNull()
    }
}

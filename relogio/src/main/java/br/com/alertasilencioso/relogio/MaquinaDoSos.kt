package br.com.alertasilencioso.relogio

/**
 * A MÁQUINA DO SOS NO RELÓGIO, em Kotlin puro (nada do Android aqui dentro: o MaquinaDoSosTest roda no
 * computador). A tela e o relógio de verdade (segundos, ponte, vibração) moram no [SosViewModel]; aqui fica só
 * a regra:
 *
 *   abrir ──► Contando(3, 2, 1) ──► Enviando ──► Pronto(ENVIADO | TESTE | SEM_CONTATOS | NAO_SAIU)
 *                 │ cancelar                          └─ NAO_SAIU: "Tentar de novo" ──► Enviando
 *                 ▼
 *              Escolha (SEGURANÇA / SAÚDE / Ajustes)
 *
 * Regras que os testes travam: contagem 0 dispara NA HORA; o padrão é 3 s; cancelar antes do fim NÃO dispara;
 * cada abertura dispara UMA vez só (segundo a mais, toque a mais ou abrir de novo no meio não disparam outra).
 */
class MaquinaDoSos(private val novoId: () -> String) {

    var estado: Estado = Estado.Escolha
        private set

    /** Quantos pedidos de SOS esta máquina já mandou (os testes conferem "uma vez só"). */
    var disparos: Int = 0
        private set

    /** Abre o SOS de [perfil] contando [segundos] (0 = dispara na hora). No meio de uma contagem ou de um envio, nada muda. */
    fun abrir(perfil: Perfil, segundos: Int, discreto: Boolean): List<Efeito> {
        if (estado is Estado.Contando || estado is Estado.Enviando) return emptyList()
        if (segundos <= 0) return disparar(perfil, discreto)
        estado = Estado.Contando(perfil, segundos, discreto)
        return listOf(Efeito.Vibrar(Vibracao.ABRIU))
    }

    /** Passou um segundo. Chegou a 0: dispara. */
    fun tique(): List<Efeito> {
        val c = estado as? Estado.Contando ?: return emptyList()
        val faltam = c.faltam - 1
        if (faltam <= 0) return disparar(c.perfil, c.discreto)
        estado = c.copy(faltam = faltam)
        // No modo discreto NÃO vibra a cada segundo (só ao abrir e ao disparar): ninguém percebe.
        return if (c.discreto) emptyList() else listOf(Efeito.Vibrar(Vibracao.SEGUNDO))
    }

    /** Cancelar (toque na tela, botão de voltar, CANCELAR). Só vale durante a contagem: o que já saiu, saiu. */
    fun cancelar(): List<Efeito> {
        if (estado !is Estado.Contando) return emptyList()
        estado = Estado.Escolha
        return emptyList()
    }

    /** "Trocar para Saúde" / "Trocar para Segurança", durante a contagem visível. A contagem segue. */
    fun trocarPerfil(): List<Efeito> {
        val c = estado as? Estado.Contando ?: return emptyList()
        estado = c.copy(perfil = if (c.perfil == Perfil.SAUDE) Perfil.SEGURANCA else Perfil.SAUDE)
        return emptyList()
    }

    /** A resposta do celular. Só vale a do pedido que está esperando (o mesmo id). */
    fun resposta(r: Resultado): List<Efeito> {
        val e = estado as? Estado.Enviando ?: return emptyList()
        if (r.id != e.id) return emptyList()
        val desfecho = desfechoDe(r)
        estado = Estado.Pronto(e.perfil, desfecho, e.discreto, r.erro)
        return listOf(Efeito.Vibrar(vibracaoDo(desfecho)))
    }

    /** Sem celular conectado, envio recusado ou ~8 s sem resposta: o alerta NÃO saiu. */
    fun semResposta(id: String): List<Efeito> {
        val e = estado as? Estado.Enviando ?: return emptyList()
        if (id != e.id) return emptyList()
        estado = Estado.Pronto(e.perfil, Desfecho.NAO_SAIU, e.discreto)
        return listOf(Efeito.Vibrar(Vibracao.NAO_SAIU))
    }

    /** "Tentar de novo" depois do "não saiu": manda outro pedido NA HORA (a pessoa já decidiu). */
    fun tentarDeNovo(): List<Efeito> {
        val p = estado as? Estado.Pronto ?: return emptyList()
        if (p.desfecho != Desfecho.NAO_SAIU) return emptyList()
        return disparar(p.perfil, p.discreto)
    }

    private fun disparar(perfil: Perfil, discreto: Boolean): List<Efeito> {
        val id = novoId()
        estado = Estado.Enviando(perfil, id, discreto)
        disparos++
        return listOf(Efeito.Vibrar(Vibracao.DISPAROU), Efeito.Enviar(perfil, id))
    }

    companion object {
        /** Quanto o relógio espera a resposta do celular antes de dizer que NÃO saiu. */
        const val ESPERA_DA_RESPOSTA_MS = 8_000L

        fun desfechoDe(r: Resultado): Desfecho = when {
            !r.ok -> Desfecho.NAO_SAIU
            r.teste -> Desfecho.TESTE
            r.contatos == 0 -> Desfecho.SEM_CONTATOS
            else -> Desfecho.ENVIADO
        }

        /** Dupla curta = chegou ao celular; longa = não adiantou (não saiu, ou não há guardião cadastrado). */
        fun vibracaoDo(d: Desfecho): Vibracao = when (d) {
            Desfecho.ENVIADO, Desfecho.TESTE -> Vibracao.CHEGOU
            Desfecho.SEM_CONTATOS, Desfecho.NAO_SAIU -> Vibracao.NAO_SAIU
        }

        /**
         * A BATERIA: a tela fica acesa (e o relógio acordado) SÓ na contagem e no envio. No resultado, no menu e
         * nos ajustes o relógio apaga a tela quando quiser.
         */
        fun telaAcesa(e: Estado): Boolean = e is Estado.Contando || e is Estado.Enviando

        /**
         * Modo discreto: o brilho vai ao mínimo (a tela preta parece apagada). Volta ao normal quando há algo que a
         * pessoa PRECISA ler (não saiu / sem guardiões) ou fora do modo discreto.
         */
        fun telaEscura(e: Estado): Boolean = when (e) {
            is Estado.Contando -> e.discreto
            is Estado.Enviando -> e.discreto
            is Estado.Pronto -> e.discreto && (e.desfecho == Desfecho.ENVIADO || e.desfecho == Desfecho.TESTE)
            Estado.Escolha -> false
        }

        /** Os desenhos de vibração (tempos em ms: espera, vibra, espera, vibra...). */
        fun padrao(v: Vibracao): LongArray = when (v) {
            Vibracao.ABRIU -> longArrayOf(0, 70)
            Vibracao.SEGUNDO -> longArrayOf(0, 40)
            Vibracao.DISPAROU -> longArrayOf(0, 350)
            Vibracao.CHEGOU -> longArrayOf(0, 70, 140, 70)
            Vibracao.NAO_SAIU -> longArrayOf(0, 1_200)
        }
    }
}

sealed interface Estado {
    /** Depois de cancelar: os dois botões grandes (SEGURANÇA / SAÚDE) e os ajustes. */
    data object Escolha : Estado

    data class Contando(val perfil: Perfil, val faltam: Int, val discreto: Boolean) : Estado

    data class Enviando(val perfil: Perfil, val id: String, val discreto: Boolean) : Estado

    data class Pronto(val perfil: Perfil, val desfecho: Desfecho, val discreto: Boolean, val erro: String? = null) : Estado
}

enum class Desfecho {
    /** O celular recebeu e acionou: os guardiões estão sendo avisados. */
    ENVIADO,

    /** O celular está em MODO TESTE: nada foi enviado. */
    TESTE,

    /** O celular acionou, mas não há guardião cadastrado lá. */
    SEM_CONTATOS,

    /** Sem celular conectado, sem resposta a tempo, ou o celular não conseguiu. */
    NAO_SAIU,
}

enum class Vibracao { ABRIU, SEGUNDO, DISPAROU, CHEGOU, NAO_SAIU }

sealed interface Efeito {
    data class Vibrar(val tipo: Vibracao) : Efeito

    /** Mandar o pedido de SOS ao celular pela ponte. */
    data class Enviar(val perfil: Perfil, val id: String) : Efeito
}

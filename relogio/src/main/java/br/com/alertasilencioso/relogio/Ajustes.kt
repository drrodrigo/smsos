package br.com.alertasilencioso.relogio

import android.content.Context

/**
 * Os ajustes do relógio (guardados NO relógio). De fábrica: contagem de 3 s, modo DISCRETO ligado (tela preta,
 * parece apagada) e o SOS rápido de Segurança. Pedido do Rodrigo (20/11): disparo rápido e sem ninguém perceber.
 */
data class Ajustes(
    /** Segundos até disparar sozinho: 0 (na hora), 3 ou 5. */
    val contagem: Int = CONTAGEM_PADRAO,
    /** Ligado: tela preta, sem número, sem "SOS", sem vermelho. Desligado: a contagem visível com CANCELAR. */
    val discreto: Boolean = true,
    /** O tipo do SOS que abre direto (botão, ícone, complicação). */
    val perfilRapido: Perfil = Perfil.SEGURANCA,
) {
    companion object {
        const val CONTAGEM_PADRAO = 3
        val CONTAGENS = listOf(0, 3, 5)

        /** Valor guardado estranho (versão antiga, arquivo mexido) volta para o padrão. */
        fun contagemValida(segundos: Int): Int = if (segundos in CONTAGENS) segundos else CONTAGEM_PADRAO
    }
}

/** Guarda os [Ajustes] nas SharedPreferences do relógio. */
class GuardaDosAjustes(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ajustes_do_relogio", Context.MODE_PRIVATE)

    fun ler(): Ajustes = Ajustes(
        contagem = Ajustes.contagemValida(prefs.getInt(CONTAGEM, Ajustes.CONTAGEM_PADRAO)),
        discreto = prefs.getBoolean(DISCRETO, true),
        perfilRapido = if (prefs.getString(PERFIL, null) == Perfil.SAUDE.name) Perfil.SAUDE else Perfil.SEGURANCA,
    )

    fun gravar(a: Ajustes) {
        prefs.edit()
            .putInt(CONTAGEM, Ajustes.contagemValida(a.contagem))
            .putBoolean(DISCRETO, a.discreto)
            .putString(PERFIL, a.perfilRapido.name)
            .apply()
    }

    private companion object {
        const val CONTAGEM = "contagem"
        const val DISCRETO = "discreto"
        const val PERFIL = "perfil_rapido"
    }
}

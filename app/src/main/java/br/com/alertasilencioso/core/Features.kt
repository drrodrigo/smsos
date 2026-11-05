package br.com.alertasilencioso.core

/**
 * Liga/desliga recursos sensíveis de aprovação na loja, num lugar só.
 *
 * ACESSIBILIDADE = false: a PRIMEIRA versão da Play sai SEM o gesto do botão de volume (que usa o
 * serviço de Acessibilidade, o maior motivo de recusa do Google). O app funciona pelo botão na tela.
 * Pra tentar o gesto de volume numa versão futura: volte para true E descomente o `<service>` do
 * GuardianService no AndroidManifest.xml (está comentado com a marca ACESSIBILIDADE).
 */
object Features {
    const val ACESSIBILIDADE = false
}

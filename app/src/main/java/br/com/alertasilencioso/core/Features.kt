package br.com.alertasilencioso.core

/**
 * Liga/desliga recursos sensíveis de aprovação na loja, num lugar só.
 *
 * ACESSIBILIDADE = false: a PRIMEIRA versão da Play sai SEM o gesto do botão de volume (que usa o
 * serviço de Acessibilidade, o maior motivo de recusa do Google). O app funciona pelo botão na tela.
 * Pra tentar o gesto de volume numa versão futura: volte para true E descomente o `<service>` do
 * GuardianService no AndroidManifest.xml (está comentado com a marca ACESSIBILIDADE) E recoloque a
 * permissão ACCESS_BACKGROUND_LOCATION (também removida junto, por depender do app fechado).
 *
 * Com ACESSIBILIDADE = false o SOS só dispara com o app aberto, então a localização de primeiro
 * plano basta — a de 2º plano (que o Google revisa com vídeo e é motivo comum de recusa) fica fora.
 */
object Features {
    const val ACESSIBILIDADE = false
}

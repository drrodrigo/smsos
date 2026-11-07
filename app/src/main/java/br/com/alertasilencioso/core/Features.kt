package br.com.alertasilencioso.core

import br.com.alertasilencioso.BuildConfig

/**
 * Liga/desliga recursos sensíveis de aprovação na loja, num lugar só.
 *
 * REGRA: no build da LOJA (release) a Acessibilidade fica DESLIGADA (maior motivo de recusa do
 * Google) e o gesto de tela apagada usa o MonitorService (sem Acessibilidade). No APK de TESTE
 * (debug, fora da loja) a gente LIGA a Acessibilidade — ela entrega o botão lateral com a tela
 * LIGADA e APAGADA — e desliga o MonitorService pra não rodar os dois motores juntos. Por isso os
 * dois flags saem de BuildConfig.DEBUG: o release continua EXATAMENTE como está na Play.
 *
 * O <service> da Acessibilidade e a permissão ACCESS_BACKGROUND_LOCATION moram só no manifesto de
 * debug (app/src/debug/AndroidManifest.xml), então NÃO entram no APK/AAB da loja. Pra levar a
 * Acessibilidade pra LOJA numa versão futura, ela precisa de declaração + vídeo de demonstração pro
 * Google (e aí o <service> e a permissão migram pro manifesto principal).
 */
object Features {
    /** true só no APK de teste (debug). No release (loja) é false: sem serviço de Acessibilidade. */
    val ACESSIBILIDADE = BuildConfig.DEBUG

    /**
     * Gesto do botão de volume SEM Acessibilidade (motor de TELA APAGADA via sessão de mídia, no
     * MonitorService). Vale no RELEASE (loja), onde a Acessibilidade está desligada. No debug fica
     * desligado porque a Acessibilidade já cobre tela ligada E apagada (não duplicar os motores).
     */
    val GESTO_SEM_ACESSIBILIDADE = !BuildConfig.DEBUG
}

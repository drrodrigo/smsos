package br.com.alertasilencioso.core

import br.com.alertasilencioso.BuildConfig

/**
 * Liga/desliga recursos sensíveis de aprovação na loja, num lugar só.
 *
 * REGRA (24/11): NENHUM build tem o serviço de Acessibilidade (o APK de teste também não, ver
 * ACESSIBILIDADE). O gesto de tela apagada usa o MonitorService. A permissão ACCESS_BACKGROUND_LOCATION
 * mora só no manifesto de debug (app/src/debug/AndroidManifest.xml), então NÃO entra no APK/AAB da loja.
 * O <service> antigo (GuardianService) ficou guardado em docs/acessibilidade-service.xml.txt.
 */
object Features {
    /**
     * Serviço de Acessibilidade: DESLIGADO em TODOS os builds desde 24/11 (0.35.0). No APK de teste ele
     * fazia o NUBANK bloquear o acesso à conta do Rodrigo ("o aplicativo GuardiõesSOS teste possui
     * características suspeitas"): app instalado por fora + Acessibilidade é o perfil do golpe da "mão
     * fantasma", e o banco olha a lista de apps que TÊM o serviço, ligado ou não. E no Samsung ele nem
     * resolvia o volume com a tela apagada. Quem cobre a tela apagada: o SOS rápido (botão lateral) e o
     * relógio. Recomendação dele/nossa: NUNCA levar a Acessibilidade para a loja (o banco do cliente
     * bloquearia o GuardiõesSOS e a pessoa desinstalaria o nosso app).
     */
    const val ACESSIBILIDADE = false

    /**
     * Localização o tempo todo (ACCESS_BACKGROUND_LOCATION): a permissão mora só no manifesto de debug, então
     * a tela de configuração só pede no APK de teste. (Antes andava junto com a Acessibilidade.)
     */
    val LOCALIZACAO_O_TEMPO_TODO = BuildConfig.DEBUG

    /**
     * Gesto do botão de volume SEM Acessibilidade (motor de TELA APAGADA via sessão de mídia, no
     * MonitorService). Vale em todos os builds, agora que nenhum tem a Acessibilidade.
     */
    const val GESTO_SEM_ACESSIBILIDADE = true
}

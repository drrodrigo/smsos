package br.com.alertasilencioso.ui

import br.com.alertasilencioso.data.FollowUpAudience
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.data.TriggerKey
import br.com.alertasilencioso.data.TriggerMode

val TriggerKey.label: String
    get() = when (this) {
        TriggerKey.VOLUME_DOWN -> "Volume −"
        TriggerKey.VOLUME_UP -> "Volume +"
        TriggerKey.ANY -> "Qualquer um"
    }

private val TriggerKey.buttonName: String
    get() = when (this) {
        TriggerKey.VOLUME_DOWN -> "de diminuir o volume (−)"
        TriggerKey.VOLUME_UP -> "de aumentar o volume (+)"
        TriggerKey.ANY -> "de volume (+ ou −)"
    }

val TriggerMode.label: String
    get() = when (this) {
        TriggerMode.HOLD -> "Segurar"
        TriggerMode.TAPS -> "Apertar várias vezes"
    }

val Profile.label: String
    get() = when (this) {
        Profile.SEGURANCA -> "🛡️ Segurança"
        Profile.SAUDE -> "❤️ Saúde"
    }

val Profile.descricao: String
    get() = when (this) {
        Profile.SEGURANCA -> "Para assalto e situações de perigo. A mensagem diz que você pode estar em perigo."
        Profile.SAUDE -> "Para idoso ou quem tem problema de saúde. A mensagem diz que você pode estar passando mal."
    }

val FollowUpAudience.label: String
    get() = when (this) {
        FollowUpAudience.ALWAYS_ONLY -> "Só os guardiões"
        FollowUpAudience.EVERYONE -> "Todos que receberam"
    }

fun Settings.triggerDescription(): String {
    if (dualSos) {
        return when (triggerMode) {
            TriggerMode.HOLD -> "Segure Volume − (Segurança) ou Volume + (Saúde) por $holdSeconds segundos."
            TriggerMode.TAPS -> "Aperte Volume − (Segurança) ou Volume + (Saúde) $tapCount vezes, rápido."
        }
    }
    return when (triggerMode) {
        TriggerMode.HOLD -> "Segure o botão ${triggerKey.buttonName} por $holdSeconds segundos."
        TriggerMode.TAPS -> "Aperte o botão ${triggerKey.buttonName} $tapCount vezes seguidas, rápido."
    }
}

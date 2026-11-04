package br.com.alertasilencioso.data

import br.com.alertasilencioso.core.MessageBuilder
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

data class Contact(
    val id: String = newId(),
    val name: String,
    val phone: String,
)

/** Um lugar cadastrado (casa, escritório...) com as pessoas que devem ser avisadas quando o alerta sai de lá. */
data class Place(
    val id: String = newId(),
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 200,
    val contacts: List<Contact> = emptyList(),
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
}

enum class TriggerKey { VOLUME_DOWN, VOLUME_UP, ANY }

enum class TriggerMode { HOLD, TAPS }

enum class FollowUpAudience { ALWAYS_ONLY, EVERYONE }

/** Para quem é o alerta: muda o texto que chega ("em perigo" x "passando mal"). */
enum class Profile { SEGURANCA, SAUDE }

object Limits {
    /** Teto de contatos por lista. Acima disso o alerta tumultua e o custo de SMS dispara. */
    const val MAX_CONTACTS = 10
}

data class Settings(
    val userName: String = "",
    /** O WhatsApp/celular da própria pessoa (a conta). Guardado já padronizado. */
    val userPhone: String = "",
    /** Se o número já foi confirmado por código (OTP) — prova que é da pessoa. */
    val numeroVerificado: Boolean = false,
    val profile: Profile = Profile.SEGURANCA,
    /**
     * Dois SOS ativos ao mesmo tempo: Volume − = Segurança, Volume + = Saúde (e botão dividido na tela).
     * Ligado por padrão — é melhor a pessoa ver as duas opções e desligar uma do que achar que só tem uma.
     */
    val dualSos: Boolean = true,
    val triggerKey: TriggerKey = TriggerKey.ANY,
    val triggerMode: TriggerMode = TriggerMode.HOLD,
    val holdSeconds: Int = 5,
    val tapCount: Int = 5,
    val messageTemplate: String = MessageBuilder.DEFAULT_TEMPLATE,
    /** Texto do alerta de Saúde (o de Segurança é o messageTemplate acima). */
    val messageTemplateSaude: String = MessageBuilder.HEALTH_TEMPLATE,
    val vibrateOnSend: Boolean = true,
    val followUpCount: Int = 3,
    val followUpIntervalMinutes: Int = 3,
    val followUpAudience: FollowUpAudience = FollowUpAudience.ALWAYS_ONLY,
    val screenOffDetection: Boolean = true,
    val testMode: Boolean = false,
    /** Depois de quantos dias o histórico some sozinho deste aparelho. 0 = nunca apagar. */
    val historyRetentionDays: Int = 7,
    /** Se a pessoa já passou pelo passo a passo inicial (ele abre sozinho até isso acontecer). */
    val onboardingDone: Boolean = false,
    /** Id deste aparelho (gerado uma vez). É a identidade da conta, junto do telefone — sem senha. */
    val dispositivo: String = "",
    /** Se a tela de cadastro do primeiro uso já foi mostrada (não insiste de novo). */
    val registroVisto: Boolean = false,
    /** Se o cadastro (lead) já foi aceito pelo servidor. Se não, tenta de novo no próximo início. */
    val registrado: Boolean = false,
    /** Token do FCM deste aparelho (push). Vai no cadastro para o servidor saber onde avisar. */
    val tokenPush: String = "",
)

enum class DeliveryStatus { PENDING, SENT, FAILED }

data class Delivery(val name: String, val phone: String, val status: DeliveryStatus)

data class AlertRecord(
    val id: String = newId(),
    val timestamp: Long,
    val test: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val placeNames: List<String>,
    val deliveries: List<Delivery>,
    val message: String,
    val notes: List<String> = emptyList(),
)

/** Alerta que acabou de sair e ainda pode mandar atualizações de localização ou o aviso de "tudo bem". */
data class ActiveAlert(
    val alertId: String,
    val startedAt: Long,
    val followUpRecipients: List<Contact>,
    val allRecipients: List<Contact>,
    val followUpsSent: Int,
    val followUpsTotal: Int,
    val followUpIntervalMillis: Long,
)

/** O texto certo para o tipo do alerta: Saúde usa o seu template; o resto usa o de Segurança. */
fun Settings.templateFor(profile: Profile): String =
    if (profile == Profile.SAUDE) messageTemplateSaude else messageTemplate

data class AppState(
    val settings: Settings = Settings(),
    val alwaysContacts: List<Contact> = emptyList(),
    val places: List<Place> = emptyList(),
    val history: List<AlertRecord> = emptyList(),
    val activeAlert: ActiveAlert? = null,
    /** Se este aparelho avisa quando o botão é solto com a tela apagada (descoberto no primeiro uso). */
    val screenOffReleaseSupported: Boolean = false,
)

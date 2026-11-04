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
    val profile: Profile = Profile.SEGURANCA,
    val triggerKey: TriggerKey = TriggerKey.ANY,
    val triggerMode: TriggerMode = TriggerMode.HOLD,
    val holdSeconds: Int = 5,
    val tapCount: Int = 5,
    val messageTemplate: String = MessageBuilder.DEFAULT_TEMPLATE,
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

data class AppState(
    val settings: Settings = Settings(),
    val alwaysContacts: List<Contact> = emptyList(),
    val places: List<Place> = emptyList(),
    val history: List<AlertRecord> = emptyList(),
    val activeAlert: ActiveAlert? = null,
    /** Se este aparelho avisa quando o botão é solto com a tela apagada (descoberto no primeiro uso). */
    val screenOffReleaseSupported: Boolean = false,
)

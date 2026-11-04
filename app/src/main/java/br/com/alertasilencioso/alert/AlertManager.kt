package br.com.alertasilencioso.alert

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import br.com.alertasilencioso.core.MessageBuilder
import br.com.alertasilencioso.core.RecipientResolver
import br.com.alertasilencioso.core.SmsLength
import br.com.alertasilencioso.data.ActiveAlert
import br.com.alertasilencioso.data.AlertRecord
import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Delivery
import br.com.alertasilencioso.data.DeliveryStatus
import br.com.alertasilencioso.data.FollowUpAudience
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TriggerSource(val description: String) {
    SCREEN_ON("botão de volume, com a tela ligada"),
    SCREEN_OFF("botão de volume, com a tela apagada"),
    APP_BUTTON("botão dentro do app"),
}

/**
 * O que acontece depois que o gesto é reconhecido: pega a localização, escolhe os contatos, manda os SMS
 * e depois as atualizações de localização. Tudo em silêncio, sem abrir nenhuma tela.
 */
object AlertManager {
    private const val TAG = "AlertManager"
    private const val COOLDOWN_MILLIS = 30_000L
    private const val TEST_COOLDOWN_MILLIS = 5_000L
    private const val LOCATION_TIMEOUT_MILLIS = 20_000L
    private const val WAKE_MARGIN_MILLIS = 2 * 60_000L

    // O Android mostra uma confirmação na tela quando um app envia 30 SMS em 30 minutos.
    private const val SMS_LIMIT = 28
    private const val SMS_LIMIT_WINDOW_MILLIS = 30 * 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val sentSmsTimes = ArrayDeque<Long>()
    private var lastTriggerAt: Long? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var appContext: Context
    private val followUpTick = Runnable { sendFollowUp() }

    fun trigger(context: Context, source: TriggerSource) {
        bind(context)
        val now = SystemClock.elapsedRealtime()
        val testMode = Repository.state.value.settings.testMode
        val cooldown = if (testMode) TEST_COOLDOWN_MILLIS else COOLDOWN_MILLIS
        lastTriggerAt?.let {
            if (now - it < cooldown) {
                Log.i(TAG, "Ignorado: um alerta acabou de ser acionado")
                return
            }
        }
        lastTriggerAt = now
        // Vibra NA HORA em que o gesto é reconhecido (ao completar os segundos), sem esperar o GPS — que pode
        // levar até 20 s. É o aviso que a pessoa sente para saber que acionou. No teste vibra sempre; no modo
        // real respeita o ajuste de vibração (ligado por padrão).
        if (testMode || Repository.state.value.settings.vibrateOnSend) Feedback.vibrateFired(appContext)
        acquireWakeLock(LOCATION_TIMEOUT_MILLIS + WAKE_MARGIN_MILLIS)
        val startedAt = System.currentTimeMillis()
        LocationFetcher(appContext).fetch(LOCATION_TIMEOUT_MILLIS) { location ->
            deliverAlert(startedAt, location, source)
        }
    }

    /** Avisa todos que receberam o alerta que está tudo bem e encerra as atualizações de localização. */
    fun sendAllClear(context: Context) {
        bind(context)
        val active = Repository.state.value.activeAlert ?: return
        handler.removeCallbacks(followUpTick)
        val now = System.currentTimeMillis()
        val text = MessageBuilder.allClear(Repository.state.value.settings.userName, now)
        enviarSimples(active.allRecipients.map { it.phone to text })
        Repository.updateAlert(active.alertId) {
            it.copy(notes = it.notes + "Aviso de \"está tudo bem\" enviado às ${MessageBuilder.formatTime(now)}")
        }
        Repository.update { it.copy(activeAlert = null) }
        releaseWakeLock()
    }

    /** Para as atualizações sem mandar mensagem nenhuma. */
    fun dismiss(context: Context) {
        bind(context)
        handler.removeCallbacks(followUpTick)
        Repository.update { it.copy(activeAlert = null) }
        releaseWakeLock()
    }

    private fun deliverAlert(startedAt: Long, location: Location?, source: TriggerSource) {
        val state = Repository.state.value
        val settings = state.settings
        val position = location?.toGeoPoint()
        val resolution = RecipientResolver.resolve(position, location?.accuracy, state.alwaysContacts, state.places)
        val placeNames = if (resolution.locationKnown) resolution.places.map { it.name } else emptyList()
        val text = MessageBuilder.alert(settings.messageTemplate, settings.userName, position, placeNames, startedAt)
        val notes = buildList {
            add("Acionado pelo ${source.description}")
            if (!resolution.locationKnown) add("Localização não encontrada: todos os contatos foram avisados")
            if (resolution.recipients.isEmpty()) add("Nenhum contato cadastrado")
        }
        val record = AlertRecord(
            timestamp = startedAt,
            test = settings.testMode,
            latitude = position?.latitude,
            longitude = position?.longitude,
            accuracyMeters = location?.accuracy,
            placeNames = placeNames,
            deliveries = resolution.recipients.map { Delivery(it.name, it.phone, DeliveryStatus.PENDING) },
            message = text,
            notes = notes,
        )
        Repository.addAlert(record)

        if (settings.testMode) {
            Feedback.showTestResult(appContext, record)
            releaseWakeLock()
            return
        }

        enviarAlerta(record, resolution.recipients, text, settings)
    }

    /**
     * Manda o alerta pelo gateway (internet). Se o gateway não for alcançado (sem internet) e o chip
     * estiver disponível, cai para o chip como reserva. Sem gateway configurado, usa o chip direto.
     */
    private fun enviarAlerta(record: AlertRecord, recipients: List<Contact>, text: String, settings: Settings) {
        val concluir = {
            if (settings.vibrateOnSend) Feedback.vibrateSent(appContext)
            startFollowUps(record, recipients, settings)
        }
        if (recipients.isEmpty()) {
            concluir()
            return
        }
        if (!GatewaySender.configurado()) {
            recipients.forEach { c ->
                if (!sendSms(c.phone, text, record.id)) Repository.setDeliveryStatus(record.id, c.phone, DeliveryStatus.FAILED)
            }
            concluir()
            return
        }
        scope.launch {
            val resultados = withContext(Dispatchers.IO) { GatewaySender.enviar(recipients.map { it.phone to text }) }
            val algumOk = resultados.values.any { it }
            if (!algumOk && SmsSender.hasPermission(appContext) && SmsSender.deviceCanSend(appContext)) {
                Repository.updateAlert(record.id) {
                    it.copy(notes = it.notes + "Gateway indisponível (sem internet?): enviado pelo chip como reserva")
                }
                recipients.forEach { c ->
                    if (!sendSms(c.phone, text, record.id)) Repository.setDeliveryStatus(record.id, c.phone, DeliveryStatus.FAILED)
                }
            } else {
                recipients.forEach { c ->
                    val status = if (resultados[c.phone] == true) DeliveryStatus.SENT else DeliveryStatus.FAILED
                    Repository.setDeliveryStatus(record.id, c.phone, status)
                }
            }
            concluir()
        }
    }

    /** Envio simples (sem rastrear status por contato): aviso de "está tudo bem" e atualizações. */
    private fun enviarSimples(envios: List<Pair<String, String>>) {
        if (envios.isEmpty()) return
        if (GatewaySender.configurado()) {
            scope.launch { withContext(Dispatchers.IO) { GatewaySender.enviar(envios) } }
        } else {
            envios.forEach { (phone, text) -> sendSms(phone, text, null) }
        }
    }

    private fun startFollowUps(record: AlertRecord, recipients: List<Contact>, settings: Settings) {
        handler.removeCallbacks(followUpTick)
        val audience = when (settings.followUpAudience) {
            FollowUpAudience.ALWAYS_ONLY -> RecipientResolver.distinctByPhone(Repository.state.value.alwaysContacts)
            FollowUpAudience.EVERYONE -> recipients
        }
        val total = if (audience.isEmpty()) 0 else settings.followUpCount
        val interval = settings.followUpIntervalMinutes * 60_000L
        Repository.update {
            it.copy(activeAlert = ActiveAlert(record.id, record.timestamp, audience, recipients, 0, total, interval))
        }
        if (total > 0) {
            acquireWakeLock(total * interval + WAKE_MARGIN_MILLIS)
            handler.postDelayed(followUpTick, interval)
        } else {
            releaseWakeLock()
        }
    }

    private fun sendFollowUp() {
        val active = Repository.state.value.activeAlert
        if (active == null || active.followUpsSent >= active.followUpsTotal) {
            releaseWakeLock()
            return
        }
        LocationFetcher(appContext).fetch(LOCATION_TIMEOUT_MILLIS) { location ->
            // Pode ter sido cancelado enquanto o GPS procurava.
            val current = Repository.state.value.activeAlert?.takeIf { it.alertId == active.alertId } ?: return@fetch
            val index = current.followUpsSent + 1
            val text = MessageBuilder.followUp(
                Repository.state.value.settings.userName,
                location?.toGeoPoint(),
                index,
                current.followUpsTotal,
                System.currentTimeMillis(),
            )
            if (GatewaySender.configurado()) {
                enviarSimples(current.followUpRecipients.map { it.phone to text })
            } else if (smsBudget() >= current.followUpRecipients.size * SmsLength.parts(text)) {
                current.followUpRecipients.forEach { sendSms(it.phone, text, null) }
            } else {
                Repository.updateAlert(current.alertId) {
                    it.copy(notes = it.notes + "Atualização $index não enviada para não passar do limite de SMS do Android")
                }
            }
            Repository.update { s ->
                s.copy(activeAlert = s.activeAlert?.takeIf { it.alertId == current.alertId }?.copy(followUpsSent = index))
            }
            if (index < current.followUpsTotal) {
                handler.postDelayed(followUpTick, current.followUpIntervalMillis)
            } else {
                releaseWakeLock()
            }
        }
    }

    private fun sendSms(phone: String, text: String, alertId: String?): Boolean {
        val sent = SmsSender.send(appContext, phone, text, alertId)
        if (sent) repeat(SmsLength.parts(text)) { sentSmsTimes.addLast(SystemClock.elapsedRealtime()) }
        return sent
    }

    private fun smsBudget(): Int {
        val cutoff = SystemClock.elapsedRealtime() - SMS_LIMIT_WINDOW_MILLIS
        while (sentSmsTimes.isNotEmpty() && sentSmsTimes.first() < cutoff) sentSmsTimes.removeFirst()
        return SMS_LIMIT - sentSmsTimes.size
    }

    private fun bind(context: Context) {
        appContext = context.applicationContext
        Repository.init(appContext)
    }

    private fun acquireWakeLock(timeoutMillis: Long) {
        val lock = wakeLock ?: appContext.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlertaSilencioso:alerta")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(timeoutMillis)
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }
}

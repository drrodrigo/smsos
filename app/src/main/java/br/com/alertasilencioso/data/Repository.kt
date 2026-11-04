package br.com.alertasilencioso.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda tudo o que o usuário cadastrou. As telas e o serviço que escuta o botão rodam no mesmo
 * processo, então todos leem o mesmo [state].
 */
object Repository {
    private const val TAG = "Repository"
    private const val PREFS = "alerta_silencioso"
    private const val KEY_STATE = "state"
    private const val MAX_HISTORY = 50
    private const val ACTIVE_ALERT_MAX_AGE_MILLIS = 12 * 60 * 60_000L

    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        p.getString(KEY_STATE, null)?.let { json ->
            runCatching { StateJson.decode(json) }
                .onSuccess { _state.value = it }
                .onFailure { Log.e(TAG, "Não foi possível ler os dados salvos", it) }
        }
        // Id do aparelho (identidade da conta, sem senha). Gerado uma vez e guardado.
        if (_state.value.settings.dispositivo.isBlank()) {
            update { it.copy(settings = it.settings.copy(dispositivo = newId())) }
        }
    }

    @Synchronized
    fun update(transform: (AppState) -> AppState) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        prefs?.edit()?.putString(KEY_STATE, StateJson.encode(next))?.apply()
    }

    fun updateSettings(transform: (Settings) -> Settings) = update { it.copy(settings = transform(it.settings)) }

    fun addAlwaysContact(contact: Contact) = update { it.copy(alwaysContacts = it.alwaysContacts + contact) }

    fun updateAlwaysContact(contact: Contact) = update { s ->
        s.copy(alwaysContacts = s.alwaysContacts.map { if (it.id == contact.id) contact else it })
    }

    fun removeAlwaysContact(id: String) = update { s ->
        s.copy(alwaysContacts = s.alwaysContacts.filterNot { it.id == id })
    }

    fun addPlace(place: Place) = update { it.copy(places = it.places + place) }

    fun updatePlace(id: String, transform: (Place) -> Place) = update { s ->
        s.copy(places = s.places.map { if (it.id == id) transform(it) else it })
    }

    fun removePlace(id: String) = update { s -> s.copy(places = s.places.filterNot { it.id == id }) }

    fun addAlert(record: AlertRecord) = update {
        it.copy(history = (listOf(record) + it.history).take(MAX_HISTORY))
    }

    fun updateAlert(id: String, transform: (AlertRecord) -> AlertRecord) = update { s ->
        s.copy(history = s.history.map { if (it.id == id) transform(it) else it })
    }

    /** Apaga o histórico deste aparelho (proteção contra coação). O histórico completo fica no site. */
    fun clearHistory() = update { it.copy(history = emptyList()) }

    /** Apaga UM registro do histórico (a pessoa escolhe qual tirar e qual deixar). */
    fun removeAlert(id: String) = update { s -> s.copy(history = s.history.filterNot { it.id == id }) }

    /**
     * Some sozinho com os alertas mais velhos que o limite escolhido em Ajustes (padrão 7 dias;
     * 0 = nunca). Deixa o app leve e é proteção: histórico curto diz menos se o celular for tomado.
     */
    fun purgeExpiredHistory(now: Long) = update { s ->
        val days = s.settings.historyRetentionDays
        if (days <= 0) return@update s
        val cutoff = now - days * 24L * 60L * 60L * 1000L
        val kept = s.history.filter { it.timestamp >= cutoff }
        if (kept.size == s.history.size) s else s.copy(history = kept)
    }

    /** Uma falha sempre vale; "enviado" só substitui "aguardando" (se uma parte falhou, fica como falha). */
    fun setDeliveryStatus(alertId: String, phone: String, status: DeliveryStatus) = updateAlert(alertId) { r ->
        r.copy(deliveries = r.deliveries.map { d ->
            val applies = d.phone == phone &&
                (status == DeliveryStatus.FAILED || d.status == DeliveryStatus.PENDING)
            if (applies) d.copy(status = status) else d
        })
    }

    fun expireOldActiveAlert(now: Long) = update { s ->
        val active = s.activeAlert
        if (active != null && now - active.startedAt > ACTIVE_ALERT_MAX_AGE_MILLIS) s.copy(activeAlert = null) else s
    }
}

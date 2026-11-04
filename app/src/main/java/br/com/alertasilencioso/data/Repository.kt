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

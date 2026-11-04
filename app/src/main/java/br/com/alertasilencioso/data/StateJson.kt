package br.com.alertasilencioso.data

import org.json.JSONArray
import org.json.JSONObject

/** Converte o estado do app para JSON e de volta. É assim que tudo fica salvo no celular. */
internal object StateJson {

    fun encode(state: AppState): String = JSONObject().apply {
        put("settings", encodeSettings(state.settings))
        put("alwaysContacts", encodeContacts(state.alwaysContacts))
        put("places", JSONArray(state.places.map(::encodePlace)))
        put("history", JSONArray(state.history.map(::encodeAlert)))
        state.activeAlert?.let { put("activeAlert", encodeActive(it)) }
        put("screenOffReleaseSupported", state.screenOffReleaseSupported)
    }.toString()

    fun decode(json: String): AppState {
        val root = JSONObject(json)
        return AppState(
            settings = root.optJSONObject("settings")?.let(::decodeSettings) ?: Settings(),
            alwaysContacts = decodeContacts(root.optJSONArray("alwaysContacts")),
            places = root.optJSONArray("places").objects().map(::decodePlace),
            history = root.optJSONArray("history").objects().map(::decodeAlert),
            activeAlert = root.optJSONObject("activeAlert")?.let(::decodeActive),
            screenOffReleaseSupported = root.optBoolean("screenOffReleaseSupported", false),
        )
    }

    private fun encodeSettings(s: Settings) = JSONObject().apply {
        put("userName", s.userName)
        put("userPhone", s.userPhone)
        put("numeroVerificado", s.numeroVerificado)
        put("profile", s.profile.name)
        put("dualSos", s.dualSos)
        put("triggerKey", s.triggerKey.name)
        put("triggerMode", s.triggerMode.name)
        put("holdSeconds", s.holdSeconds)
        put("tapCount", s.tapCount)
        put("messageTemplate", s.messageTemplate)
        put("messageTemplateSaude", s.messageTemplateSaude)
        put("vibrateOnSend", s.vibrateOnSend)
        put("followUpCount", s.followUpCount)
        put("followUpIntervalMinutes", s.followUpIntervalMinutes)
        put("followUpAudience", s.followUpAudience.name)
        put("screenOffDetection", s.screenOffDetection)
        put("testMode", s.testMode)
        put("historyRetentionDays", s.historyRetentionDays)
        put("onboardingDone", s.onboardingDone)
        put("dispositivo", s.dispositivo)
        put("registroVisto", s.registroVisto)
        put("registrado", s.registrado)
        put("tokenPush", s.tokenPush)
    }

    private fun decodeSettings(o: JSONObject): Settings {
        val d = Settings()
        return Settings(
            userName = o.optString("userName", d.userName),
            userPhone = o.optString("userPhone", d.userPhone),
            numeroVerificado = o.optBoolean("numeroVerificado", d.numeroVerificado),
            profile = enumOr(o.optString("profile"), d.profile),
            dualSos = o.optBoolean("dualSos", d.dualSos),
            triggerKey = enumOr(o.optString("triggerKey"), d.triggerKey),
            triggerMode = enumOr(o.optString("triggerMode"), d.triggerMode),
            holdSeconds = o.optInt("holdSeconds", d.holdSeconds),
            tapCount = o.optInt("tapCount", d.tapCount),
            messageTemplate = o.optString("messageTemplate", d.messageTemplate),
            messageTemplateSaude = o.optString("messageTemplateSaude", d.messageTemplateSaude),
            vibrateOnSend = o.optBoolean("vibrateOnSend", d.vibrateOnSend),
            followUpCount = o.optInt("followUpCount", d.followUpCount),
            followUpIntervalMinutes = o.optInt("followUpIntervalMinutes", d.followUpIntervalMinutes),
            followUpAudience = enumOr(o.optString("followUpAudience"), d.followUpAudience),
            screenOffDetection = o.optBoolean("screenOffDetection", d.screenOffDetection),
            testMode = o.optBoolean("testMode", d.testMode),
            historyRetentionDays = o.optInt("historyRetentionDays", d.historyRetentionDays),
            onboardingDone = o.optBoolean("onboardingDone", d.onboardingDone),
            dispositivo = o.optString("dispositivo", d.dispositivo),
            registroVisto = o.optBoolean("registroVisto", d.registroVisto),
            registrado = o.optBoolean("registrado", d.registrado),
            tokenPush = o.optString("tokenPush", d.tokenPush),
        )
    }

    private fun encodeContact(c: Contact) = JSONObject().put("id", c.id).put("name", c.name).put("phone", c.phone)

    private fun decodeContact(o: JSONObject) = Contact(
        id = o.optString("id").ifEmpty { newId() },
        name = o.optString("name"),
        phone = o.optString("phone"),
    )

    private fun encodeContacts(list: List<Contact>) = JSONArray(list.map(::encodeContact))

    private fun decodeContacts(a: JSONArray?) = a.objects().map(::decodeContact)

    private fun encodePlace(p: Place) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        p.latitude?.let { put("latitude", it) }
        p.longitude?.let { put("longitude", it) }
        put("radiusMeters", p.radiusMeters)
        put("contacts", encodeContacts(p.contacts))
    }

    private fun decodePlace(o: JSONObject) = Place(
        id = o.optString("id").ifEmpty { newId() },
        name = o.optString("name"),
        latitude = o.optDoubleOrNull("latitude"),
        longitude = o.optDoubleOrNull("longitude"),
        radiusMeters = o.optInt("radiusMeters", 200),
        contacts = decodeContacts(o.optJSONArray("contacts")),
    )

    private fun encodeAlert(a: AlertRecord) = JSONObject().apply {
        put("id", a.id)
        put("timestamp", a.timestamp)
        put("test", a.test)
        a.latitude?.let { put("latitude", it) }
        a.longitude?.let { put("longitude", it) }
        a.accuracyMeters?.let { put("accuracyMeters", it.toDouble()) }
        put("placeNames", JSONArray(a.placeNames))
        put("deliveries", JSONArray(a.deliveries.map {
            JSONObject().put("name", it.name).put("phone", it.phone).put("status", it.status.name)
        }))
        put("message", a.message)
        put("notes", JSONArray(a.notes))
    }

    private fun decodeAlert(o: JSONObject) = AlertRecord(
        id = o.optString("id").ifEmpty { newId() },
        timestamp = o.optLong("timestamp"),
        test = o.optBoolean("test"),
        latitude = o.optDoubleOrNull("latitude"),
        longitude = o.optDoubleOrNull("longitude"),
        accuracyMeters = o.optDoubleOrNull("accuracyMeters")?.toFloat(),
        placeNames = o.optJSONArray("placeNames").strings(),
        deliveries = o.optJSONArray("deliveries").objects().map {
            Delivery(
                name = it.optString("name"),
                phone = it.optString("phone"),
                status = enumOr(it.optString("status"), DeliveryStatus.PENDING),
            )
        },
        message = o.optString("message"),
        notes = o.optJSONArray("notes").strings(),
    )

    private fun encodeActive(a: ActiveAlert) = JSONObject().apply {
        put("alertId", a.alertId)
        put("startedAt", a.startedAt)
        put("followUpRecipients", encodeContacts(a.followUpRecipients))
        put("allRecipients", encodeContacts(a.allRecipients))
        put("followUpsSent", a.followUpsSent)
        put("followUpsTotal", a.followUpsTotal)
        put("followUpIntervalMillis", a.followUpIntervalMillis)
    }

    private fun decodeActive(o: JSONObject) = ActiveAlert(
        alertId = o.optString("alertId"),
        startedAt = o.optLong("startedAt"),
        followUpRecipients = decodeContacts(o.optJSONArray("followUpRecipients")),
        allRecipients = decodeContacts(o.optJSONArray("allRecipients")),
        followUpsSent = o.optInt("followUpsSent"),
        followUpsTotal = o.optInt("followUpsTotal"),
        followUpIntervalMillis = o.optLong("followUpIntervalMillis"),
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key).takeUnless { it.isNaN() } else null
}

package br.com.alertasilencioso.core

import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Place
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

data class Resolution(
    /** Lugares onde a pessoa está (ou todos, se a localização não foi encontrada). */
    val places: List<Place>,
    val recipients: List<Contact>,
    val locationKnown: Boolean,
)

/** Decide quem recebe o alerta: os contatos de "sempre avisar" mais os contatos do lugar onde a pessoa está. */
object RecipientResolver {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun resolve(
        position: GeoPoint?,
        accuracyMeters: Float?,
        always: List<Contact>,
        places: List<Place>,
    ): Resolution {
        // Sem GPS não dá para saber onde a pessoa está: é melhor avisar gente demais do que ninguém.
        val matched = if (position == null) places else places.filter { contains(it, position, accuracyMeters) }
        return Resolution(
            places = matched,
            recipients = distinctByPhone(always + matched.flatMap { it.contacts }),
            locationKnown = position != null,
        )
    }

    /**
     * A pessoa está no lugar se estiver dentro do raio. A imprecisão do GPS (comum dentro de garagens e
     * prédios) é somada ao raio, limitada ao próprio tamanho do raio.
     */
    fun contains(place: Place, position: GeoPoint, accuracyMeters: Float?): Boolean {
        val lat = place.latitude ?: return false
        val lng = place.longitude ?: return false
        val slack = min((accuracyMeters ?: 0f).toDouble(), place.radiusMeters.toDouble())
        return distanceMeters(position, GeoPoint(lat, lng)) <= place.radiusMeters + slack
    }

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }

    fun distinctByPhone(contacts: List<Contact>): List<Contact> =
        contacts.filter { PhoneNumbers.key(it.phone).isNotEmpty() }.distinctBy { PhoneNumbers.key(it.phone) }
}

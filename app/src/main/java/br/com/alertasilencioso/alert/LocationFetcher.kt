package br.com.alertasilencioso.alert

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import br.com.alertasilencioso.core.GeoPoint

fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

/** Busca a posição atual usando só o Android (sem depender dos serviços do Google). */
class LocationFetcher(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    fun lastKnown(): Location? {
        if (!hasPermission()) return null
        return providers()
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.elapsedRealtimeNanos }
    }

    /**
     * Procura a posição por até [timeoutMillis] e entrega a melhor encontrada (ou a última conhecida, se o
     * GPS não responder a tempo). Sempre chama [onResult] uma única vez, na thread principal.
     */
    fun fetch(timeoutMillis: Long, onResult: (Location?) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        val last = lastKnown()
        if (last != null && last.ageMillis() <= RECENT_MILLIS && last.accuracy <= GOOD_ACCURACY_METERS) {
            handler.post { onResult(last) }
            return
        }
        Attempt(handler, last, onResult).start(timeoutMillis)
    }

    private inner class Attempt(
        private val handler: Handler,
        private val fallback: Location?,
        private val onResult: (Location?) -> Unit,
    ) : LocationListener {
        private var best: Location? = null
        private var finished = false
        private val timeout = Runnable { finish() }

        @SuppressLint("MissingPermission")
        fun start(timeoutMillis: Long) {
            val enabled = if (hasPermission()) {
                providers().filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            } else {
                emptyList()
            }
            val requested = enabled.filter { provider ->
                runCatching { manager.requestLocationUpdates(provider, 1_000L, 0f, this, Looper.getMainLooper()) }.isSuccess
            }
            if (requested.isEmpty()) {
                handler.post { finish() }
                return
            }
            handler.postDelayed(timeout, timeoutMillis)
        }

        override fun onLocationChanged(location: Location) {
            val current = best
            if (current == null || location.accuracy < current.accuracy) best = location
            if (location.accuracy <= GOOD_ACCURACY_METERS) finish()
        }

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit

        // Em Android antigos este método não tem implementação padrão e precisa existir.
        @Deprecated("Exigido por versões antigas do Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        private fun finish() {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeout)
            runCatching { manager.removeUpdates(this) }
            onResult(best ?: fallback)
        }
    }

    private fun providers(): List<String> {
        val all = manager.allProviders
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { it in all }
    }

    private fun Location.ageMillis() = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private companion object {
        const val GOOD_ACCURACY_METERS = 30f
        const val RECENT_MILLIS = 60_000L
    }
}

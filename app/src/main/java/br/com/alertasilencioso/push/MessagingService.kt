package br.com.alertasilencioso.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.com.alertasilencioso.R
import br.com.alertasilencioso.alert.Alarme
import br.com.alertasilencioso.alert.Registrar
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.ui.AlertaRecebidoActivity
import br.com.alertasilencioso.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recebe o push do FCM. Dois tipos: "alerta" (SOS — toca alto e abre a tela por cima de tudo, como uma
 * chamada) e qualquer outro (aviso comum, só notifica). Também atualiza o token deste aparelho.
 */
class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Repository.init(applicationContext)
        Repository.updateSettings { it.copy(tokenPush = token) }
        Registrar.registrar()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val tipo = message.data["tipo"].orEmpty()
        val titulo = message.data["titulo"] ?: message.notification?.title ?: "GuardiõesSOS"
        val corpo = message.data["corpo"] ?: message.notification?.body
            ?: "Alguém que você protege precisa de ajuda."
        if (tipo == "alerta") mostrarAlerta(titulo, corpo) else mostrarAviso(titulo, corpo)
    }

    /** Aviso comum: só notifica, sem tocar. */
    private fun mostrarAviso(titulo: String, corpo: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_AVISO, "Avisos", NotificationManager.IMPORTANCE_HIGH),
        )
        val n = NotificationCompat.Builder(this, CANAL_AVISO)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(abrir(MainActivity::class.java))
            .build()
        nm.notify(System.currentTimeMillis().toInt(), n)
    }

    /** ALERTA de SOS: toca alto (como uma chamada), vibra e abre a tela em cima de tudo. */
    private fun mostrarAlerta(titulo: String, corpo: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_SOS, "SOS (emergência)", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // o som é o alarme em loop, tocado pelo app (volume de alarme)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        Alarme.tocar(applicationContext)
        val cheia = Intent(this, AlertaRecebidoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("titulo", titulo)
            putExtra("corpo", corpo)
        }
        val pi = PendingIntent.getActivity(
            this, 1, cheia,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(this, CANAL_SOS)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        nm.notify(SOS_NOTIF_ID, n)
    }

    private fun abrir(cls: Class<*>): PendingIntent {
        val intent = Intent(this, cls).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CANAL_AVISO = "guardioes_alertas"
        const val CANAL_SOS = "guardioes_sos"
        const val SOS_NOTIF_ID = 7001
    }
}

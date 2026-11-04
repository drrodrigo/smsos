package br.com.alertasilencioso.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import br.com.alertasilencioso.R
import br.com.alertasilencioso.alert.Registrar
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recebe o push do FCM (quando um guardião precisa ser avisado) e mostra a notificação,
 * mesmo com o app fechado. Também atualiza o token deste aparelho no servidor.
 */
class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Repository.init(applicationContext)
        Repository.updateSettings { it.copy(tokenPush = token) }
        Registrar.registrar()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: message.data["titulo"] ?: "GuardiõesSOS"
        val corpo = message.notification?.body ?: message.data["corpo"] ?: "Alguém que você protege precisa de ajuda."
        mostrar(titulo, corpo)
    }

    private fun mostrar(titulo: String, corpo: String) {
        val canalId = "guardioes_alertas"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(canalId, "Alertas de guardião", NotificationManager.IMPORTANCE_HIGH),
        )
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, canalId)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), n)
    }
}

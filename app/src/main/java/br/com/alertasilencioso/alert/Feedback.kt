package br.com.alertasilencioso.alert

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.alertasilencioso.R
import br.com.alertasilencioso.data.AlertRecord
import br.com.alertasilencioso.ui.MainActivity

/** Retorno discreto para quem acionou: vibração curta e, só no modo teste, uma notificação com o resultado. */
object Feedback {
    private const val TEST_CHANNEL = "testes"
    private const val TEST_NOTIFICATION_ID = 1

    /** Duas vibrações bem curtas: só quem está segurando o celular percebe. */
    fun vibrateSent(context: Context) = vibrate(context, longArrayOf(0, 60, 120, 60))

    /** No modo teste a vibração é mais forte, para ficar claro que o gesto foi reconhecido. */
    fun vibrateTest(context: Context) = vibrate(context, longArrayOf(0, 200, 150, 200, 150, 200))

    @SuppressLint("MissingPermission")
    fun showTestResult(context: Context, record: AlertRecord) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(TEST_CHANNEL, "Resultados do modo teste", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val names = record.deliveries.joinToString(", ") { it.name.ifBlank { it.phone } }
        val place = record.placeNames.joinToString(", ").ifEmpty { "nenhum lugar cadastrado" }
        val summary = if (record.deliveries.isEmpty()) {
            "Nenhum contato receberia o alerta. Cadastre contatos no app."
        } else {
            "Seria enviado para ${record.deliveries.size} contato(s): $names.\nLugar: $place.\n\n${record.message}"
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle("Teste: o alerta funcionou (nenhum SMS enviado)")
            .setContentText(summary.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(pattern, -1)
        // Uso "alarme" para o Android não ignorar a vibração com a tela apagada ou o app em segundo plano.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}

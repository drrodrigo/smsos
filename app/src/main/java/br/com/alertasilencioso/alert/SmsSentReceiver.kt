package br.com.alertasilencioso.alert

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.com.alertasilencioso.data.DeliveryStatus
import br.com.alertasilencioso.data.Repository

/** Recebe do Android o resultado de cada SMS enviado e atualiza o histórico. */
class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Repository.init(context)
        val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: return
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: return
        val isLastPart = intent.getIntExtra(EXTRA_PART, 0) == intent.getIntExtra(EXTRA_PART_COUNT, 1) - 1
        when {
            resultCode != Activity.RESULT_OK -> Repository.setDeliveryStatus(alertId, phone, DeliveryStatus.FAILED)
            isLastPart -> Repository.setDeliveryStatus(alertId, phone, DeliveryStatus.SENT)
        }
    }

    companion object {
        private const val EXTRA_ALERT_ID = "alertId"
        private const val EXTRA_PHONE = "phone"
        private const val EXTRA_PART = "part"
        private const val EXTRA_PART_COUNT = "partCount"

        fun pendingIntent(context: Context, alertId: String, phone: String, part: Int, partCount: Int): PendingIntent {
            // O endereço (data) diferente para cada parte garante que um aviso não substitua o outro.
            val intent = Intent(context, SmsSentReceiver::class.java)
                .setData(Uri.parse("alerta://sms/$alertId/${Uri.encode(phone)}/$part"))
                .putExtra(EXTRA_ALERT_ID, alertId)
                .putExtra(EXTRA_PHONE, phone)
                .putExtra(EXTRA_PART, part)
                .putExtra(EXTRA_PART_COUNT, partCount)
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}

package br.com.alertasilencioso.alert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import br.com.alertasilencioso.core.PhoneNumbers

/** Envia SMS direto pelo chip do celular, sem abrir nenhuma tela e sem precisar de internet. */
object SmsSender {
    private const val TAG = "SmsSender"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun deviceCanSend(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    /**
     * Envia [text] para [phone]. Com [alertId], o resultado de cada envio é gravado no histórico daquele alerta.
     * Retorna false se não foi possível nem tentar (sem permissão, número inválido...).
     */
    fun send(context: Context, phone: String, text: String, alertId: String? = null): Boolean {
        val number = PhoneNumbers.clean(phone)
        if (number.isEmpty()) return false
        return try {
            val manager = smsManager(context)
            val parts = manager.divideMessage(text)
            val sentIntents = alertId?.let { id ->
                ArrayList(parts.indices.map { SmsSentReceiver.pendingIntent(context, id, phone, it, parts.size) })
            }
            manager.sendMultipartTextMessage(number, null, parts, sentIntents, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao enviar SMS", e)
            false
        }
    }

    /**
     * SMS de teste: envia e pede ao Android o resultado real de cada parte, que o [SmsTestReceiver] mostra
     * na tela. Assim dá para ver POR QUE um "disparado" não chega. Retorna false se nem deu para tentar.
     */
    fun sendTest(context: Context, phone: String, text: String): Boolean {
        val number = PhoneNumbers.clean(phone)
        if (number.isEmpty()) return false
        return try {
            val manager = smsManager(context)
            val parts = manager.divideMessage(text)
            val sentIntents = ArrayList(parts.indices.map { SmsTestReceiver.pendingIntent(context, it) })
            manager.sendMultipartTextMessage(number, null, parts, sentIntents, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao enviar SMS de teste", e)
            false
        }
    }

    // Em celulares com dois chips, usar um chip definido evita que o Android pergunte na tela qual usar.
    private fun smsManager(context: Context): SmsManager {
        val subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId()
            .takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
            ?: SubscriptionManager.getDefaultSubscriptionId()
        val hasSubscription = subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java)
            return if (hasSubscription) base.createForSubscriptionId(subscriptionId) else base
        }
        @Suppress("DEPRECATION")
        return if (hasSubscription) SmsManager.getSmsManagerForSubscriptionId(subscriptionId) else SmsManager.getDefault()
    }
}

package br.com.alertasilencioso.alert

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.widget.Toast

/**
 * Só para o SMS de TESTE: mostra na tela o resultado real que o Android devolve do envio.
 * Serve para descobrir por que um SMS "disparado" não chega (chip, operadora, bloqueio do fabricante...).
 */
class SmsTestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val msg = when (resultCode) {
            Activity.RESULT_OK ->
                "SMS de teste ACEITO pelo chip ✓ — veja se chegou no outro celular."
            SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                "Falhou: erro genérico da operadora/aparelho. Em Xiaomi/MIUI é comum o sistema bloquear SMS enviado por app — veja as permissões do SMSOS."
            SmsManager.RESULT_ERROR_NO_SERVICE ->
                "Falhou: sem serviço no chip usado. O chip padrão de SMS pode ser o que não envia."
            SmsManager.RESULT_ERROR_RADIO_OFF ->
                "Falhou: rádio do celular desligado (está no modo avião?)."
            SmsManager.RESULT_ERROR_NULL_PDU ->
                "Falhou: mensagem vazia (erro interno do app)."
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED ->
                "Falhou: muitos SMS em pouco tempo (limite do Android)."
            else ->
                "Falhou: o Android devolveu o código $resultCode."
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        /** Um PendingIntent por parte do SMS, para o Android devolver o resultado de cada uma. */
        fun pendingIntent(context: Context, part: Int): PendingIntent {
            val intent = Intent(context, SmsTestReceiver::class.java)
                .setData(android.net.Uri.parse("alerta://sms-teste/$part"))
            return PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}

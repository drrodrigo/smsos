package br.com.alertasilencioso.trigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import br.com.alertasilencioso.R
import br.com.alertasilencioso.alert.AlertManager
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.ui.MainActivity

/**
 * Percebe o gesto do botão de volume COM A TELA APAGADA, SEM o serviço de Acessibilidade.
 *
 * O motor é o mesmo [ScreenOffVolumeListener] (o celular trata o app como um controle de volume
 * remoto, tipo o de um Chromecast, e entrega cada aperto aqui em vez de mudar o volume). A única
 * diferença para o [GuardianService] é QUEM hospeda esse motor: lá era o serviço de Acessibilidade
 * (maior motivo de recusa da Play); aqui é um serviço de primeiro plano comum, com uma notificação
 * fixa discreta. Assim o pega-e-segurar funciona sem pedir Acessibilidade.
 *
 * Limites honestos: só vale com a tela apagada (tela ligada depende da Acessibilidade); alguns
 * fabricantes bloqueiam a sessão de mídia; e se houver música tocando, o volume controla a música.
 */
class MonitorService : Service() {
    private var controller: TriggerController? = null
    private var listener: ScreenOffVolumeListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        iniciarPrimeiroPlano(NOTIF_ID, buildNotification())
        val trigger = TriggerController(this) { source, profile -> AlertManager.trigger(this, source, profile) }
        controller = trigger
        listener = ScreenOffVolumeListener(this, trigger::onScreenOffVolume).also { it.start() }
        running = true
    }

    // Se o Android encerrar por falta de memória, recria o serviço quando puder.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        listener?.stop()
        listener = null
        controller?.shutdown()
        controller = null
        running = false
        super.onDestroy()
    }

    private fun iniciarPrimeiroPlano(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            @Suppress("DEPRECATION")
            startForeground(id, notification)
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Proteção ativa", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Mostra que o GuardiõesSOS está percebendo o botão de volume."
                    setShowBadge(false)
                },
            )
        }
        val abrir = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("GuardiõesSOS protegendo você")
            .setContentText("Segure o botão de volume para pedir socorro.")
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setOngoing(true)
            .setContentIntent(abrir)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL = "guardioes_protecao"
        const val NOTIF_ID = 42

        /** Se o motor de tela apagada está no ar neste momento. */
        @Volatile
        var running = false
            private set

        /** Liga o motor (chamado ao abrir o app, ao ligar a chave e depois de reiniciar o celular). */
        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}

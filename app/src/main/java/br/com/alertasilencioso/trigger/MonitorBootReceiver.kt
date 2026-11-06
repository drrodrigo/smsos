package br.com.alertasilencioso.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.alertasilencioso.core.Features
import br.com.alertasilencioso.data.Repository

/**
 * Depois de reiniciar o celular, religa o motor de tela apagada se a pessoa deixou o gesto ligado.
 * Sem isso, o gesto pararia de funcionar até a pessoa abrir o app de novo.
 */
class MonitorBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Features.GESTO_SEM_ACESSIBILIDADE) return
        Repository.init(context)
        if (Repository.state.value.settings.screenOffDetection) {
            MonitorService.start(context)
        }
    }
}

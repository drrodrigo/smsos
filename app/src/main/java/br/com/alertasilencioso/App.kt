package br.com.alertasilencioso

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import br.com.alertasilencioso.alert.Registrar
import br.com.alertasilencioso.data.Repository
import com.google.firebase.messaging.FirebaseMessaging

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        Repository.expireOldActiveAlert(System.currentTimeMillis())
        // Cria o canal do push JÁ na abertura. Sem isso, uma notificação que chega com o app FECHADO
        // cai num canal inexistente e o Android não mostra. (minSdk 26: canal sempre disponível.)
        runCatching {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel("guardioes_alertas", "Alertas de guardião", NotificationManager.IMPORTANCE_HIGH),
            )
        }.onFailure { Log.w("App", "Não criou o canal de push", it) }
        // Se o cadastro (lead) ficou pendente por falta de internet, tenta de novo agora.
        Registrar.registrarSePendente()
        // Pega o token do push (FCM) deste aparelho e manda pro servidor junto do cadastro.
        // Em try/catch: no ambiente de teste (Robolectric) o Firebase não existe e tudo bem.
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    Repository.updateSettings { it.copy(tokenPush = token) }
                    Registrar.registrar()
                }
                .addOnFailureListener { Log.w("App", "Sem token de push", it) }
        }.onFailure { Log.w("App", "Firebase indisponível", it) }
    }
}

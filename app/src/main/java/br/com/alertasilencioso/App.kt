package br.com.alertasilencioso

import android.app.Application
import br.com.alertasilencioso.alert.Registrar
import br.com.alertasilencioso.data.Repository

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        Repository.expireOldActiveAlert(System.currentTimeMillis())
        // Se o cadastro (lead) ficou pendente por falta de internet, tenta de novo agora.
        Registrar.registrarSePendente()
    }
}

package br.com.alertasilencioso

import android.app.Application
import br.com.alertasilencioso.data.Repository

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        Repository.expireOldActiveAlert(System.currentTimeMillis())
    }
}

package br.com.alertasilencioso.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import br.com.alertasilencioso.trigger.GuardianService

// Atalhos para as telas do Android que a pessoa precisa visitar durante a configuração.

fun Context.openAppDetails() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
}

/**
 * Tenta abrir direto a página do serviço do app (onde fica a chave). Nem todo aparelho deixa;
 * nesse caso abre a lista de Acessibilidade, já pedindo para destacar o item do app.
 */
fun Context.openAccessibilitySettings() {
    val component = ComponentName(this, GuardianService::class.java).flattenToString()
    val details = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
        .putExtra(Intent.EXTRA_COMPONENT_NAME, component)
    val list = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .putExtra(":settings:fragment_args_key", component)
        .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", component) })
    for (intent in listOf(details, list)) {
        try {
            startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            // Tenta o próximo.
        } catch (e: SecurityException) {
            // Alguns Android só deixam o próprio sistema abrir a página de detalhes.
        }
    }
}

@SuppressLint("BatteryLife")
fun Context.requestIgnoreBatteryOptimizations() {
    try {
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

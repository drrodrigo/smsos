package br.com.alertasilencioso.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import br.com.alertasilencioso.alert.SmsSender
import br.com.alertasilencioso.trigger.GuardianService

/** O que já foi liberado no celular. Lido de novo sempre que a pessoa volta para o app. */
data class SetupStatus(
    val sms: Boolean,
    val location: Boolean,
    val backgroundLocation: Boolean,
    val notifications: Boolean,
    val accessibility: Boolean,
    val batteryUnrestricted: Boolean,
    val deviceCanSendSms: Boolean,
) {
    val essentialsOk: Boolean get() = sms && location && accessibility

    companion object {
        fun read(context: Context): SetupStatus = SetupStatus(
            sms = granted(context, Manifest.permission.SEND_SMS),
            location = granted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                granted(context, Manifest.permission.ACCESS_COARSE_LOCATION),
            backgroundLocation = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                granted(context, Manifest.permission.POST_NOTIFICATIONS),
            accessibility = isAccessibilityEnabled(context),
            batteryUnrestricted = context.getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(context.packageName),
            deviceCanSendSms = SmsSender.deviceCanSend(context),
        )

        private fun granted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        private fun isAccessibilityEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val service = ComponentName(context, GuardianService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == service }
        }
    }
}

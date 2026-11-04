package br.com.alertasilencioso.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.alertasilencioso.alert.AlertManager
import br.com.alertasilencioso.alert.TriggerSource
import br.com.alertasilencioso.core.GeoPoint
import br.com.alertasilencioso.core.MessageBuilder
import br.com.alertasilencioso.core.RecipientResolver
import br.com.alertasilencioso.data.ActiveAlert
import br.com.alertasilencioso.data.AlertRecord
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.DeliveryStatus
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.data.TriggerMode
import br.com.alertasilencioso.trigger.Diagnostics
import br.com.alertasilencioso.trigger.GuardianService

@Composable
fun HomeScreen(
    state: AppState,
    status: SetupStatus,
    onRefreshStatus: () -> Unit,
    onOpenTab: (Tab) -> Unit,
    onOpenWizard: () -> Unit,
) {
    val context = LocalContext.current
    val running by GuardianService.running.collectAsStateWithLifecycle()
    val diagnostics by Diagnostics.state.collectAsStateWithLifecycle()
    val recipients = remember(state.alwaysContacts, state.places) {
        RecipientResolver.distinctByPhone(state.alwaysContacts + state.places.flatMap { it.contacts }).size
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onRefreshStatus()
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { StatusCard(state.settings, status, running, recipients) }
        if (!status.essentialsOk || !running || recipients == 0) {
            item {
                Button(onClick = onOpenWizard, Modifier.fillMaxWidth()) {
                    Text("Configurar passo a passo", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        state.activeAlert?.let { active -> item { ActiveAlertCard(active) } }
        item { PanicButton(state.settings.holdSeconds * 1_000L) { AlertManager.trigger(context, TriggerSource.APP_BUTTON) } }
        item { SetupCard(state.settings, status, recipients, onRefreshStatus, onOpenTab) }
        item {
            TestCard(
                settings = state.settings,
                diagnostics = diagnostics,
                releaseSupported = state.screenOffReleaseSupported,
                onTestModeEnabled = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notifications) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
        item { HistoryCard(state.history) }
    }
}

@Composable
private fun StatusCard(settings: Settings, status: SetupStatus, running: Boolean, recipients: Int) {
    val ok = StatusColors.ok
    val warning = StatusColors.warning
    val danger = StatusColors.danger
    val (title, body, color) = when {
        !status.deviceCanSendSms -> Triple("Este aparelho não envia SMS", "O alerta precisa de um celular com chip.", danger)
        !status.essentialsOk || !running -> Triple("Proteção DESLIGADA", "Termine a configuração logo abaixo.", danger)
        recipients == 0 -> Triple("Falta cadastrar contatos", "Cadastre quem deve ser avisado.", warning)
        settings.testMode -> Triple("MODO TESTE", "O botão funciona, mas nenhum SMS é enviado.", warning)
        else -> Triple("Proteção ATIVA", settings.triggerDescription(), ok)
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (color == ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ActiveAlertCard(active: ActiveAlert) {
    val context = LocalContext.current
    var confirming by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Alerta enviado às ${MessageBuilder.formatTime(active.startedAt)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("${active.allRecipients.size} contato(s) avisado(s).")
            if (active.followUpsTotal > 0) {
                val minutes = active.followUpIntervalMillis / 60_000
                Text(
                    if (active.followUpsSent < active.followUpsTotal) {
                        "Atualizações de localização: ${active.followUpsSent} de ${active.followUpsTotal} (a cada $minutes min)."
                    } else {
                        "Todas as atualizações de localização foram enviadas."
                    }
                )
            }
            Button(onClick = { confirming = true }, Modifier.fillMaxWidth()) { Text("Estou bem — avisar todos") }
            TextButton(onClick = { AlertManager.dismiss(context) }, Modifier.fillMaxWidth()) {
                Text("Encerrar sem mandar mensagem")
            }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Avisar que está tudo bem?") },
            text = { Text("Os ${active.allRecipients.size} contato(s) que receberam o alerta vão receber um SMS dizendo que está tudo bem.") },
            confirmButton = {
                TextButton(onClick = {
                    AlertManager.sendAllClear(context)
                    confirming = false
                }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancelar") } },
        )
    }
}

/** Botão dentro do app: fica pressionado pelo mesmo tempo do botão de volume, para não disparar sem querer. */
@Composable
private fun PanicButton(holdMillis: Long, onTrigger: () -> Unit) {
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    var holding by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = tween(if (holding) holdMillis.toInt() else 150, easing = LinearEasing),
        label = "panicProgress",
    )
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(holdMillis) {
                awaitEachGesture {
                    awaitFirstDown()
                    holding = true
                    val releasedEarly = withTimeoutOrNull(holdMillis) {
                        waitForUpOrCancellation()
                        true
                    } ?: false
                    if (!releasedEarly) {
                        currentOnTrigger()
                        waitForUpOrCancellation()
                    }
                    holding = false
                }
            },
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SEGURE AQUI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("por ${holdMillis / 1_000} segundos para enviar o alerta agora", style = MaterialTheme.typography.bodyMedium)
            if (holding) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                )
            }
        }
    }
}

private enum class SetupDialog { ACCESSIBILITY, BACKGROUND_LOCATION }

@Composable
private fun SetupCard(
    settings: Settings,
    status: SetupStatus,
    recipients: Int,
    onRefreshStatus: () -> Unit,
    onOpenTab: (Tab) -> Unit,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SetupDialog?>(null) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onRefreshStatus()
    }
    val backgroundLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onRefreshStatus()
        if (!granted) {
            context.toast("Abra \"Permissões\" > \"Localização\" e escolha \"Permitir o tempo todo\".")
            context.openAppDetails()
        }
    }

    SectionCard("Configuração") {
        SetupRow(
            done = settings.userName.isNotBlank(),
            title = "Seu nome",
            description = "Aparece na mensagem, para as pessoas saberem quem pediu ajuda.",
            action = "Preencher",
        ) { onOpenTab(Tab.SETTINGS) }
        SetupRow(
            done = recipients > 0,
            title = "Contatos",
            description = "Quem recebe o alerta: \"Sempre avisar\" e as pessoas de cada lugar.",
            action = "Cadastrar",
        ) { onOpenTab(Tab.CONTACTS) }
        SetupRow(
            done = status.sms,
            title = "Enviar SMS",
            description = "Para mandar o alerta pelo seu chip, sem precisar de internet.",
            action = "Permitir",
        ) { permissions.launch(arrayOf(Manifest.permission.SEND_SMS)) }
        SetupRow(
            done = status.location,
            title = "Localização",
            description = "Para mandar o link do mapa e saber em qual lugar você está.",
            action = "Permitir",
        ) {
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SetupRow(
                done = status.backgroundLocation,
                title = "Localização o tempo todo",
                description = if (status.location) {
                    "Para pegar o GPS mesmo com o app fechado."
                } else {
                    "Libere primeiro a \"Localização\" acima."
                },
                action = "Permitir",
                enabled = status.location,
            ) { dialog = SetupDialog.BACKGROUND_LOCATION }
        }
        SetupRow(
            done = status.accessibility,
            title = "Perceber o botão de volume",
            description = "Liga o serviço que observa o botão, mesmo com o app fechado.",
            action = "Ativar",
        ) { dialog = SetupDialog.ACCESSIBILITY }
        SetupRow(
            done = status.batteryUnrestricted,
            title = "Sem economia de bateria",
            description = "Evita que o celular desligue o app sozinho.",
            action = "Liberar",
        ) { context.requestIgnoreBatteryOptimizations() }
    }

    when (dialog) {
        SetupDialog.ACCESSIBILITY -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Perceber o botão de volume") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Para perceber quando você segura o botão de volume, mesmo com outro app aberto, o SMSOS usa o serviço de Acessibilidade do Android.")
                    Text("Ele NÃO lê a tela, NÃO vê o que você digita e NÃO coleta dados. Só observa os botões de volume.", fontWeight = FontWeight.Bold)
                    Text("Na próxima tela, toque em \"SMSOS\" (às vezes fica dentro de \"Apps instalados\" ou \"Serviços baixados\") e ative.")
                    HelpText("Se aparecer \"Configuração restrita\": toque em \"Informações do app\" aqui embaixo, depois nos 3 pontinhos ⋮ no canto de cima e em \"Permitir configurações restritas\". Depois volte e ative.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    context.openAccessibilitySettings()
                }) { Text("Abrir Acessibilidade") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialog = null
                    context.openAppDetails()
                }) { Text("Informações do app") }
            },
        )
        SetupDialog.BACKGROUND_LOCATION -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Localização o tempo todo") },
            text = {
                Text("Na próxima tela, escolha \"Permitir o tempo todo\". Assim o alerta consegue mandar sua localização mesmo com o app fechado e a tela apagada.")
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    backgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Agora não") } },
        )
        null -> Unit
    }
}

@Composable
private fun SetupRow(
    done: Boolean,
    title: String,
    description: String,
    action: String,
    enabled: Boolean = true,
    onAction: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (done) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = if (done) "Pronto" else "Pendente",
            tint = if (done) StatusColors.ok else StatusColors.warning,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            HelpText(description)
        }
        if (!done) {
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onAction, enabled = enabled) { Text(action) }
        }
    }
}

@Composable
private fun TestCard(
    settings: Settings,
    diagnostics: Diagnostics.Snapshot,
    releaseSupported: Boolean,
    onTestModeEnabled: () -> Unit,
) {
    SectionCard("Testar sem enviar SMS") {
        SwitchRow("Modo teste", settings.testMode) { on ->
            Repository.updateSettings { it.copy(testMode = on) }
            if (on) onTestModeEnabled()
        }
        HelpText("No modo teste o app faz tudo (reconhece o botão, pega a localização e escolhe os contatos), mas NÃO manda SMS. Ao completar os segundos ele VIBRA na hora e mostra uma notificação dizendo quem receberia.")
        HelpText("Teste agora: com o modo teste ligado, ${settings.triggerDescription().replaceFirstChar { it.lowercase() }} O número abaixo tem que subir a cada aperto. Se ele NÃO subir, o serviço de acessibilidade não está pegando a tecla — reative-o no passo a passo (no Samsung, desligue o Bloqueador automático antes).")
        HelpText("Para testar com a tela apagada: apague a tela, faça o mesmo gesto, acenda a tela e veja o resultado aqui.")
        Text("Tela ligada: ${pressesText(diagnostics.screenOnPresses)}", style = MaterialTheme.typography.bodyMedium)
        Text("Tela apagada: ${pressesText(diagnostics.screenOffPresses)}", style = MaterialTheme.typography.bodyMedium)
        if (diagnostics.screenOffPresses > 0 && !releaseSupported && settings.triggerMode == TriggerMode.HOLD) {
            HelpText("Seu celular ainda não avisou quando o botão é solto com a tela apagada. Se \"segurar\" não funcionar com a tela apagada, escolha \"Apertar várias vezes\" em Ajustes.")
        }
    }
}

private fun pressesText(count: Int) =
    if (count > 0) "✓ $count aperto(s) percebido(s)" else "nenhum aperto percebido ainda"

@Composable
private fun HistoryCard(history: List<AlertRecord>) {
    SectionCard("Histórico") {
        if (history.isEmpty()) HelpText("Nenhum alerta ainda.")
        history.take(10).forEachIndexed { index, record ->
            if (index > 0) HorizontalDivider()
            HistoryItem(record)
        }
    }
}

@Composable
private fun HistoryItem(record: AlertRecord) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "${MessageBuilder.formatDateTime(record.timestamp)} · ${if (record.test) "TESTE" else "ALERTA"}",
            style = MaterialTheme.typography.titleSmall,
            color = if (record.test) StatusColors.warning else MaterialTheme.colorScheme.primary,
        )
        if (record.placeNames.isNotEmpty()) HelpText("Lugar: ${record.placeNames.joinToString(", ")}")
        val total = record.deliveries.size
        if (record.test) {
            HelpText("$total contato(s) receberiam o alerta")
        } else {
            val sent = record.deliveries.count { it.status == DeliveryStatus.SENT }
            val failed = record.deliveries.count { it.status == DeliveryStatus.FAILED }
            val pending = record.deliveries.count { it.status == DeliveryStatus.PENDING }
            HelpText(buildString {
                append("Enviado: $sent de $total")
                if (failed > 0) append(" · falhou: $failed")
                if (pending > 0) append(" · aguardando: $pending")
            })
        }
        record.notes.forEach { HelpText(it) }
        val lat = record.latitude
        val lng = record.longitude
        if (lat != null && lng != null) {
            TextButton(onClick = { context.openMap(GeoPoint(lat, lng), "Alerta") }) { Text("Ver no mapa") }
        }
    }
}


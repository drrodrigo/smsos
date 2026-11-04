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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.data.Profile
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
        item { PanicArea(state.settings, context) }
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
        item { HistoryCard(state.history, state.settings.historyRetentionDays) }
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

/** Área do botão: um botão só (modo simples) ou dois lados — Segurança e Saúde (modo duplo). */
@Composable
private fun PanicArea(settings: Settings, context: android.content.Context) {
    val hold = settings.holdSeconds * 1_000L
    val seg = settings.holdSeconds
    if (!settings.dualSos) {
        HoldButton(
            holdMillis = hold,
            title = "SEGURE AQUI",
            subtitle = "por $seg segundos para enviar o alerta agora",
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth(),
        ) { AlertManager.trigger(context, TriggerSource.APP_BUTTON, settings.profile) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HoldButton(
                holdMillis = hold,
                title = "🛡️ SEGURANÇA",
                subtitle = "segure $seg s",
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
            ) { AlertManager.trigger(context, TriggerSource.APP_BUTTON, Profile.SEGURANCA) }
            HoldButton(
                holdMillis = hold,
                title = "❤️ SAÚDE",
                subtitle = "segure $seg s",
                color = Color(0xFF1E66D0),
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
            ) { AlertManager.trigger(context, TriggerSource.APP_BUTTON, Profile.SAUDE) }
        }
    }
}

/** Botão que fica pressionado pelo mesmo tempo do botão de volume, para não disparar sem querer. */
@Composable
private fun HoldButton(
    holdMillis: Long,
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onTrigger: () -> Unit,
) {
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    var holding by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = tween(if (holding) holdMillis.toInt() else 150, easing = LinearEasing),
        label = "holdProgress",
    )
    Surface(
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.pointerInput(holdMillis) {
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
        Column(
            Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            if (holding) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.3f),
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

    val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val feitos = listOf(
        settings.userName.isNotBlank(),
        recipients > 0,
        status.sms,
        status.location,
        if (isQ) status.backgroundLocation else true,
        status.accessibility,
        status.batteryUnrestricted,
    )
    val pendentes = feitos.count { !it }
    CollapsibleSectionCard(
        "Configuração",
        subtitle = if (pendentes == 0) "Tudo pronto ✓" else "Falta $pendentes ajuste(s) — toque para abrir",
        initiallyExpanded = pendentes > 0,
    ) {
        SetupRow(
            done = settings.userName.isNotBlank(),
            title = "Seu nome",
            description = "Aparece na mensagem, para as pessoas saberem quem pediu ajuda.",
            action = "Preencher",
        ) { onOpenTab(Tab.SETTINGS) }
        SetupRow(
            done = recipients > 0,
            title = "Guardiões",
            description = "Quem recebe o alerta: seus guardiões e as pessoas de cada lugar.",
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
                    Text("Para perceber quando você segura o botão de volume, mesmo com outro app aberto, o GuardiõesSOS usa o serviço de Acessibilidade do Android.")
                    Text("Ele NÃO lê a tela, NÃO vê o que você digita e NÃO coleta dados. Só observa os botões de volume.", fontWeight = FontWeight.Bold)
                    Text("Na próxima tela, toque em \"GuardiõesSOS\" (às vezes fica dentro de \"Apps instalados\" ou \"Serviços baixados\") e ative.")
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
    CollapsibleSectionCard(
        "Testar sem enviar SMS",
        subtitle = "Treinar o gatilho sem avisar ninguém — toque para abrir",
        initiallyExpanded = false,
    ) {
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
private fun HistoryCard(history: List<AlertRecord>, retentionDays: Int) {
    var confirmarApagar by remember { mutableStateOf(false) }
    CollapsibleSectionCard(
        "Histórico",
        subtitle = if (history.isEmpty()) "Nenhum alerta ainda" else "${history.size} registro(s) — toque para abrir",
        initiallyExpanded = false,
    ) {
        if (history.isEmpty()) {
            HelpText("Nenhum alerta ainda.")
        } else {
            history.take(10).forEachIndexed { index, record ->
                if (index > 0) HorizontalDivider()
                HistoryItem(record, onDelete = { Repository.removeAlert(record.id) })
            }
            HorizontalDivider()
            TextButton(onClick = { confirmarApagar = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Apagar o histórico deste aparelho")
            }
            HelpText(
                buildString {
                    append("Toque na lixeira ao lado de um alerta para apagar só ele. ")
                    append(
                        if (retentionDays > 0) {
                            "O histórico some sozinho depois de $retentionDays dias (você muda em Ajustes)."
                        } else {
                            "O histórico fica guardado até você apagar (você liga o sumiço automático em Ajustes)."
                        },
                    )
                    append(" Apagar aqui não desfaz — útil se tomarem seu telefone.")
                },
            )
        }
    }
    if (confirmarApagar) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            title = { Text("Apagar o histórico?") },
            text = { Text("Isso apaga a lista de alertas deste celular e não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = { Repository.clearHistory(); confirmarApagar = false }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HistoryItem(record: AlertRecord, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Apagar este alerta", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


package br.com.alertasilencioso.ui

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.alertasilencioso.alert.GatewaySender
import br.com.alertasilencioso.alert.Otp
import br.com.alertasilencioso.billing.Billing
import br.com.alertasilencioso.core.GeoPoint
import br.com.alertasilencioso.core.MessageBuilder
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.core.SmsLength
import br.com.alertasilencioso.data.FollowUpAudience
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.data.TriggerKey
import br.com.alertasilencioso.data.TriggerMode
import br.com.alertasilencioso.trigger.Diagnostics
import br.com.alertasilencioso.trigger.MonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// Só para mostrar o exemplo da mensagem (Praça da Sé, São Paulo).
private val PREVIEW_POSITION = GeoPoint(-23.55052, -46.63331)

/** Confirma que o número é da pessoa: manda um código por SMS e a pessoa digita de volta (OTP). */
@Composable
private fun ConfirmarNumeroCard(settings: Settings) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var numeroDigitado by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    // Usa o número do cadastro; se não tiver (pulou o cadastro), deixa digitar aqui mesmo.
    val numero = settings.userPhone.ifBlank { PhoneNumbers.normalize(numeroDigitado) }
    val numeroOk = PhoneNumbers.isPlausible(numero)
    SectionCard("Confirmar seu número") {
        when {
            settings.numeroVerificado ->
                Text("✓ Número confirmado: ${PhoneNumbers.format(settings.userPhone)}", color = StatusColors.ok)
            !enviado -> {
                if (settings.userPhone.isBlank()) {
                    HelpText("Qual é o seu WhatsApp? Mandamos um código por SMS para confirmar que é seu.")
                    OutlinedTextField(
                        value = numeroDigitado,
                        onValueChange = { numeroDigitado = it },
                        label = { Text("Celular com DDD") },
                        placeholder = { Text("(31) 99999-8888") },
                        singleLine = true,
                        isError = numeroDigitado.isNotBlank() && !numeroOk,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    HelpText("Confirme que o ${PhoneNumbers.format(settings.userPhone)} é seu. Mandamos um código por SMS.")
                }
                Button(
                    enabled = numeroOk && !ocupado,
                    onClick = {
                        if (settings.userPhone.isBlank()) Repository.updateSettings { it.copy(userPhone = numero) }
                        ocupado = true
                        escopo.launch {
                            val ok = Otp.enviar(numero)
                            ocupado = false
                            if (ok) { enviado = true; context.toast("Código enviado por SMS.") }
                            else context.toast("Não consegui enviar o código agora. Confira a internet.")
                        }
                    },
                ) { Text(if (ocupado) "Enviando..." else "Enviar código por SMS") }
            }
            else -> {
                HelpText("Digite o código de 6 dígitos que chegou por SMS no ${PhoneNumbers.format(numero)}.")
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Código de 6 dígitos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = codigo.length == 6 && !ocupado,
                    onClick = {
                        ocupado = true
                        escopo.launch {
                            val r = Otp.confirmar(numero, codigo)
                            ocupado = false
                            when {
                                r.ok -> {
                                    Repository.updateSettings { it.copy(numeroVerificado = true) }
                                    context.toast("Número confirmado ✓")
                                }
                                r.erro == "expirado" -> context.toast("O código expirou. Envie outro.")
                                r.erro == "codigo_incorreto" -> context.toast("Código incorreto. Confira e tente de novo.")
                                r.erro == "muitas_tentativas" -> context.toast("Muitas tentativas. Envie um novo código.")
                                else -> context.toast("Não consegui confirmar agora. Tente de novo.")
                            }
                        }
                    },
                ) { Text(if (ocupado) "Confirmando..." else "Confirmar") }
                TextButton(onClick = { enviado = false; codigo = "" }) { Text("Enviar outro código") }
            }
        }
    }
}

/** Um editor de texto de alerta (com exemplo de como chega e contagem de SMS). Serve para Segurança e Saúde. */
@Composable
private fun MensagemEditor(titulo: String?, valor: String, padrao: String, nome: String, onChange: (String) -> Unit) {
    if (titulo != null) Text(titulo, style = MaterialTheme.typography.labelLarge)
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    val preview = MessageBuilder.alert(valor, nome, PREVIEW_POSITION, listOf("Casa"), System.currentTimeMillis())
    Text("Exemplo de como chega:", style = MaterialTheme.typography.labelLarge)
    Text(preview, style = MaterialTheme.typography.bodySmall)
    val parts = SmsLength.parts(preview)
    Text(
        "${preview.length} letras · $parts SMS por contato",
        style = MaterialTheme.typography.bodySmall,
        color = if (parts > 1) StatusColors.warning else StatusColors.ok,
    )
    TextButton(onClick = { onChange(padrao) }) { Text("Voltar para o texto padrão") }
}

@Composable
fun SettingsScreen(settings: Settings) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var name by remember { mutableStateOf(settings.userName) }
    var templateSeg by remember { mutableStateOf(settings.messageTemplate) }
    var templateSau by remember { mutableStateOf(settings.messageTemplateSaude) }
    var testPhone by rememberSaveable { mutableStateOf("") }
    val save: ((Settings) -> Settings) -> Unit = Repository::updateSettings
    val diag by Diagnostics.state.collectAsStateWithLifecycle()
    val permissaoCaptura = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* o estado das permissões é lido na hora do SOS; nada a fazer aqui */ }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) }
        item {
            val plano by Billing.estado.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { Billing.iniciar(context) }
            SectionCard("Planos") {
                val ativo = plano.ativo
                if (ativo == Billing.COMPLETO) {
                    Text("✓ Você é Guardião+ Completo 💙", color = StatusColors.ok, style = MaterialTheme.typography.titleMedium)
                    HelpText("Todos os recursos liberados. Obrigado!")
                } else {
                    HelpText("O pânico pra sua FAMÍLIA (guardiões que te avisam em qualquer lugar) é sempre GRÁTIS. Os planos liberam mais:")
                    if (ativo == Billing.PROTECAO) {
                        Text("✓ Você tem o Proteção+ 💙", color = StatusColors.ok)
                    } else {
                        Text("📍 Proteção+ — localização AO VIVO no mapa (GPS) durante o SOS.")
                        if (plano.disponivel(Billing.PROTECAO)) {
                            Button(
                                onClick = { (context as? Activity)?.let { Billing.assinar(it, Billing.PROTECAO) } },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Assinar Proteção+ — ${plano.precos[Billing.PROTECAO]}/mês") }
                        }
                    }
                    Text("🎥 Completo — tudo do Proteção+ e ainda VÍDEO e ÁUDIO no SOS.")
                    if (plano.disponivel(Billing.COMPLETO)) {
                        Button(
                            onClick = { (context as? Activity)?.let { Billing.assinar(it, Billing.COMPLETO) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Assinar Completo — ${plano.precos[Billing.COMPLETO]}/mês") }
                    }
                    if (plano.precos.isEmpty()) {
                        HelpText("Planos em configuração — ficam disponíveis quando os produtos forem criados na loja.")
                    }
                }
            }
        }
        item {
            SectionCard("Seu nome") {
                // Mostra o nome salvo com um "Editar"; só grava quando toca "Salvar". Assim a pessoa
                // sabe que ficou salvo e não muda sem querer ao tocar no campo.
                var editandoNome by remember { mutableStateOf(settings.userName.isBlank()) }
                if (editandoNome) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome que aparece na mensagem") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            save { it.copy(userName = name.trim()) }
                            editandoNome = false
                            context.toast("Nome salvo ✓")
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Salvar") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            settings.userName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { name = settings.userName; editandoNome = true }) {
                            Text("Editar")
                        }
                    }
                }
            }
        }
        item { ConfirmarNumeroCard(settings) }
        item {
            SectionCard("Para que você usa") {
                SwitchRow("Ativar os dois SOS (Segurança e Saúde)", settings.dualSos) { on ->
                    save { it.copy(dualSos = on) }
                }
                if (settings.dualSos) {
                    HelpText("Os dois ficam ativos. No botão de volume: segure Volume − para Segurança e Volume + para Saúde. Na tela Início aparece o botão dividido, um lado de cada.")
                } else {
                    ChipRow(Profile.entries, settings.profile, { it.label }) { p -> save { it.copy(profile = p) } }
                    HelpText(settings.profile.descricao)
                }
            }
        }
        item {
            SectionCard("Botão de acionamento") {
                Text("Qual botão", style = MaterialTheme.typography.labelLarge)
                ChipRow(TriggerKey.entries, settings.triggerKey, { it.label }) { key -> save { it.copy(triggerKey = key) } }
                Text("Como acionar", style = MaterialTheme.typography.labelLarge)
                ChipRow(TriggerMode.entries, settings.triggerMode, { it.label }) { mode -> save { it.copy(triggerMode = mode) } }
                when (settings.triggerMode) {
                    TriggerMode.HOLD -> {
                        Text("Segurar por ${settings.holdSeconds} segundos")
                        Slider(
                            value = settings.holdSeconds.toFloat(),
                            onValueChange = { v -> save { it.copy(holdSeconds = v.roundToInt()) } },
                            valueRange = 3f..30f,
                            steps = 26,
                        )
                    }
                    TriggerMode.TAPS -> {
                        Text("Apertar ${settings.tapCount} vezes (em até 4 segundos)")
                        Slider(
                            value = settings.tapCount.toFloat(),
                            onValueChange = { v -> save { it.copy(tapCount = v.roundToInt()) } },
                            valueRange = 3f..10f,
                            steps = 6,
                        )
                    }
                }
                HelpText("Toques rápidos no botão continuam mudando o volume normalmente.")
            }
        }
        item {
            SectionCard("Mensagem") {
                HelpText(MessageBuilder.PLACEHOLDER_HELP)
                val mostrarSaude = settings.dualSos || settings.profile == Profile.SAUDE
                MensagemEditor(
                    titulo = if (mostrarSaude) "🛡️ Mensagem de Segurança" else null,
                    valor = templateSeg,
                    padrao = MessageBuilder.DEFAULT_TEMPLATE,
                    nome = name,
                ) { v -> templateSeg = v; save { it.copy(messageTemplate = v) } }
                if (mostrarSaude) {
                    MensagemEditor(
                        titulo = "❤️ Mensagem de Saúde",
                        valor = templateSau,
                        padrao = MessageBuilder.HEALTH_TEMPLATE,
                        nome = name,
                    ) { v -> templateSau = v; save { it.copy(messageTemplateSaude = v) } }
                }
            }
        }
        item {
            SectionCard("Depois do alerta") {
                SwitchRow("Vibrar discretamente quando o alerta for enviado", settings.vibrateOnSend) { on ->
                    save { it.copy(vibrateOnSend = on) }
                }
                Text(
                    if (settings.followUpCount == 0) {
                        "Não mandar atualizações de localização"
                    } else {
                        "Mandar a localização atualizada ${settings.followUpCount} vez(es), a cada ${settings.followUpIntervalMinutes} min"
                    }
                )
                Slider(
                    value = settings.followUpCount.toFloat(),
                    onValueChange = { v -> save { it.copy(followUpCount = v.roundToInt()) } },
                    valueRange = 0f..10f,
                    steps = 9,
                )
                if (settings.followUpCount > 0) {
                    Text("Intervalo: ${settings.followUpIntervalMinutes} min", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = settings.followUpIntervalMinutes.toFloat(),
                        onValueChange = { v -> save { it.copy(followUpIntervalMinutes = v.roundToInt()) } },
                        valueRange = 1f..15f,
                        steps = 13,
                    )
                    Text("As atualizações vão para:", style = MaterialTheme.typography.labelLarge)
                    ChipRow(FollowUpAudience.entries, settings.followUpAudience, { it.label }) { audience ->
                        save { it.copy(followUpAudience = audience) }
                    }
                }
                HelpText("Atenção: o Android pede confirmação na tela quando um app manda mais de 30 SMS em meia hora. Por isso o padrão é mandar as atualizações só para os seus guardiões.")
            }
        }
        item {
            SectionCard("Botão com a tela apagada (experimental)") {
                SwitchRow("Perceber o botão com a tela apagada", settings.screenOffDetection) { on ->
                    save { it.copy(screenOffDetection = on) }
                    if (on) MonitorService.start(context) else MonitorService.stop(context)
                }
                HelpText("Sem precisar de Acessibilidade. Funciona na maioria dos celulares, mas não em todos — confira no seu, abaixo.")
                if (settings.screenOffDetection) {
                    Text(
                        if (MonitorService.running) "Motor do gesto: ligado ✓" else "Motor do gesto: desligado (abra o app de novo)",
                        color = if (MonitorService.running) StatusColors.ok else StatusColors.warning,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "Apertos percebidos com a tela apagada: ${diag.screenOffPresses}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    HelpText(
                        "Teste no seu celular: bloqueie a tela e segure o botão de volume por ${settings.holdSeconds}s — " +
                            "o celular deve vibrar (ligue o Modo teste na tela Início para não enviar nada). " +
                            "Depois volte aqui: se o contador acima subiu, seu aparelho aceita o recurso. Se ficou em 0, ele bloqueia.",
                    )
                }
            }
        }
        item {
            SectionCard("Gravar vídeo no SOS (experimental)") {
                SwitchRow("Ao acionar, gravar um vídeo curto da cena", settings.capturarMidia) { on ->
                    if (on) {
                        permissaoCaptura.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    }
                    save { it.copy(capturarMidia = on) }
                }
                if (settings.capturarMidia) {
                    Text(
                        "Duração do vídeo: ${settings.capturaSegundos} segundos",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = settings.capturaSegundos.toFloat(),
                        onValueChange = { v -> save { it.copy(capturaSegundos = v.roundToInt()) } },
                        valueRange = 5f..30f,
                        steps = 24,
                    )
                }
                HelpText(
                    "Ao acionar o SOS, o app grava um VÍDEO curto (câmera frontal, COM áudio do ambiente), " +
                        "sem abrir a tela — uma prova pra mandar aos guardiões. Precisa liberar câmera e " +
                        "microfone. Teste com o Modo teste ligado (tela Início): o vídeo é salvo na sua " +
                        "galeria, em \"Movies/GuardiõesSOS\", e chega um aviso quando terminar.",
                )
            }
        }
        item {
            SectionCard("Testar o envio de SMS") {
                HelpText("Manda uma mensagem de teste pela nossa estrutura (internet). Precisa ser celular com os 11 dígitos: DDD + 9 + número.")
                val testWarning = PhoneNumbers.formatWarning(testPhone)
                OutlinedTextField(
                    value = testPhone,
                    onValueChange = { testPhone = it },
                    label = { Text("Celular com DDD") },
                    placeholder = { Text("(31) 99999-8888") },
                    singleLine = true,
                    isError = testWarning != null,
                    supportingText = testWarning?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (!GatewaySender.configurado()) {
                            context.toast("Envio não configurado neste app.")
                        } else {
                            context.toast("Enviando pela nossa estrutura...")
                            escopo.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    GatewaySender.enviar(listOf(testPhone to MessageBuilder.testSms()))[testPhone] == true
                                }
                                context.toast(
                                    if (ok) "Enviado ✓ Confira se chegou no celular."
                                    else "Não enviado. Confira o número (11 dígitos com o 9) e a internet.",
                                )
                            }
                        }
                    },
                    enabled = PhoneNumbers.isPlausible(testPhone),
                ) { Text("Enviar SMS de teste") }
            }
        }
        item {
            SectionCard("Histórico") {
                Text("Apagar o histórico sozinho depois de:", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    listOf(0, 2, 7, 30),
                    settings.historyRetentionDays,
                    { if (it == 0) "Nunca" else "$it dias" },
                ) { dias -> save { it.copy(historyRetentionDays = dias) } }
                HelpText(
                    "Histórico curto deixa o app leve e é mais seguro se tomarem seu celular. " +
                        "\"Nunca\" mantém tudo até você apagar. Você também pode apagar um alerta por vez na tela Início.",
                )
            }
        }
        item {
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            }
            HelpText("GuardiõesSOS · versão ${version ?: "?"}")
        }
    }
}

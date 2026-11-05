package br.com.alertasilencioso.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.alertasilencioso.alert.LocationFetcher
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Place
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.trigger.Diagnostics

private const val HOME_LOCATION_TIMEOUT_MILLIS = 30_000L

private enum class WizardStep { WELCOME, NAME, CONTACTS, LOCATION, BACKGROUND_LOCATION, ACCESSIBILITY, BATTERY, PLACE, TEST }

private fun WizardStep.applies(): Boolean =
    this != WizardStep.BACKGROUND_LOCATION || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

private fun WizardStep.isDone(state: AppState, status: SetupStatus): Boolean = when (this) {
    WizardStep.WELCOME, WizardStep.TEST -> false
    WizardStep.NAME -> state.settings.userName.isNotBlank()
    WizardStep.CONTACTS -> state.alwaysContacts.isNotEmpty()
    WizardStep.LOCATION -> status.location
    WizardStep.BACKGROUND_LOCATION -> status.backgroundLocation
    WizardStep.ACCESSIBILITY -> status.accessibility
    WizardStep.BATTERY -> status.batteryUnrestricted
    WizardStep.PLACE -> state.places.any { it.hasLocation }
}

/**
 * Configuração guiada: uma coisa por tela, com o que tocar escrito em ordem. Passos que já estão
 * prontos são pulados, então quem volta aqui cai direto no que falta.
 */
@Composable
fun SetupWizard(
    state: AppState,
    status: SetupStatus,
    onRefreshStatus: () -> Unit,
    onClose: () -> Unit,
) {
    val steps = remember { WizardStep.entries.filter { it.applies() } }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]

    val goNext: () -> Unit = {
        var next = index + 1
        while (next < steps.lastIndex && steps[next].isDone(state, status)) next++
        index = next.coerceAtMost(steps.lastIndex)
    }
    val goBack: () -> Unit = { if (index > 0) index -= 1 else onClose() }
    BackHandler(onBack = goBack)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (index > 0) {
                IconButton(onClick = goBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
            Text(
                if (index == 0) "Configuração" else "Passo $index de ${steps.lastIndex}",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = onClose) { Text("Fazer depois") }
        }
        LinearProgressIndicator(
            progress = { index / steps.lastIndex.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                WizardStep.WELCOME -> WelcomeStep(stepCount = steps.lastIndex, onStart = goNext)
                WizardStep.NAME -> NameStep(state, onNext = goNext)
                WizardStep.CONTACTS -> ContactsStep(state, onNext = goNext)
                WizardStep.LOCATION, WizardStep.BACKGROUND_LOCATION, WizardStep.BATTERY ->
                    PermissionStep(step, state, status, onRefreshStatus, onNext = goNext)
                WizardStep.ACCESSIBILITY -> AccessibilityStep(done = status.accessibility, onNext = goNext)
                WizardStep.PLACE -> PlaceStep(state, status, onNext = goNext)
                WizardStep.TEST -> TestStep(state, status, onRefreshStatus, onFinish = onClose)
            }
        }
    }
}

@Composable
private fun WelcomeStep(stepCount: Int, onStart: () -> Unit) {
    StepHeader(
        "👋",
        "Vamos deixar seu botão de pânico pronto",
        "São $stepCount passos rápidos. Em cada um, é só tocar no botão grande e seguir o que aparece na tela. Leva uns 3 minutos.",
    )
    BigButton("Começar", onClick = onStart)
}

@Composable
private fun NameStep(state: AppState, onNext: () -> Unit) {
    var name by remember { mutableStateOf(state.settings.userName) }
    StepHeader("✍️", "Qual é o seu nome?", "Ele aparece na mensagem, para as pessoas saberem quem está pedindo ajuda.")
    OutlinedTextField(
        value = name,
        onValueChange = { value ->
            name = value
            Repository.updateSettings { it.copy(userName = value) }
        },
        label = { Text("Seu nome") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Para que você vai usar?", style = MaterialTheme.typography.titleSmall)
    ChipRow(Profile.entries, state.settings.profile, { it.label }) { p ->
        Repository.updateSettings { it.copy(profile = p) }
    }
    HelpText(state.settings.profile.descricao)
    BigButton("Continuar", enabled = name.isNotBlank(), onClick = onNext)
}

@Composable
private fun ContactsStep(state: AppState, onNext: () -> Unit) {
    StepHeader(
        "👨‍👩‍👦",
        "Quem deve ser avisado sempre?",
        "Essas pessoas recebem o alerta onde você estiver. Por exemplo, seu filho, mesmo que ele esteja longe. Os vizinhos você cadastra daqui a pouco.",
    )
    ContactListEditor(
        contacts = state.alwaysContacts,
        emptyText = "Toque em \"Da agenda\" para escolher alguém dos seus contatos, ou em \"Digitar\" para escrever o número.",
        onAdd = Repository::addAlwaysContact,
        onRemove = { Repository.removeAlwaysContact(it.id) },
        onUpdate = Repository::updateAlwaysContact,
    )
    BigButton("Continuar", enabled = state.alwaysContacts.isNotEmpty(), onClick = onNext)
    SkipButton(onNext)
}

/** Passos que são só "liberar uma permissão": explicam, abrem a janela do Android e confirmam na volta. */
@Composable
private fun PermissionStep(
    step: WizardStep,
    state: AppState,
    status: SetupStatus,
    onRefreshStatus: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var tried by remember(step) { mutableStateOf(false) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onRefreshStatus()
    }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefreshStatus() }
    val done = step.isDone(state, status)

    when (step) {
        WizardStep.LOCATION -> {
            StepHeader("📍", "Permitir a localização", "Para mandar o link do mapa e saber se você está em casa, no escritório...")
            if (!done) {
                Instructions(
                    "Toque em \"Permitir localização\" aqui embaixo.",
                    "Escolha \"Durante o uso do app\".",
                    "Deixe \"Usar localização precisa\" ligado.",
                )
                BigButton("Permitir localização") {
                    tried = true
                    permissions.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
                if (tried) AppSettingsHelp("Toque em \"Permissões\" → \"Localização\" → \"Permitir durante o uso do app\".")
            }
        }
        WizardStep.BACKGROUND_LOCATION -> {
            StepHeader("🛰️", "Localização o tempo todo", "Assim o alerta pega o GPS mesmo com o app fechado e a tela apagada.")
            if (!done) {
                Instructions(
                    "Toque em \"Abrir\" aqui embaixo.",
                    "Na tela que abrir, marque \"Permitir o tempo todo\".",
                    "Toque na seta de voltar para voltar ao app.",
                )
                BigButton("Abrir") {
                    tried = true
                    if (status.location) {
                        single.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        permissions.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                }
                if (tried) AppSettingsHelp("Toque em \"Permissões\" → \"Localização\" → \"Permitir o tempo todo\".")
            }
        }
        WizardStep.BATTERY -> {
            StepHeader(
                "🔋",
                "Não deixar o celular desligar o app",
                "Para economizar bateria, o celular fecha apps sozinho. Aqui você pede para ele não fazer isso com o alerta.",
            )
            if (!done) {
                Instructions("Toque em \"Liberar\" aqui embaixo.", "Na janela que abrir, toque em \"Permitir\".")
                BigButton("Liberar") {
                    tried = true
                    context.requestIgnoreBatteryOptimizations()
                }
            }
            if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
                HelpCard("No Samsung, faça também") {
                    Instructions(
                        "Abra as Configurações do celular → \"Bateria\".",
                        "Toque em \"Limites de uso em segundo plano\".",
                        "Toque em \"Apps que nunca entram em suspensão\".",
                        "Toque no \"+\" e marque \"GuardiõesSOS\".",
                    )
                }
            }
        }
        else -> Unit
    }
    if (done) {
        DoneBanner()
        BigButton("Continuar", onClick = onNext)
    } else {
        SkipButton(onNext)
    }
}

@Composable
private fun AccessibilityStep(done: Boolean, onNext: () -> Unit) {
    val context = LocalContext.current
    StepHeader(
        "🔘",
        "Perceber o botão de volume",
        "Este é o passo mais importante: é ele que percebe quando você segura o botão, mesmo com o app fechado. " +
            "O app só observa os botões de volume. Ele não lê a tela nem o que você digita.",
    )
    if (done) {
        DoneBanner()
        BigButton("Continuar", onClick = onNext)
        return
    }
    Instructions(
        "Toque em \"Abrir Acessibilidade\" aqui embaixo.",
        "Toque em \"GuardiõesSOS – botão de pânico\". No Samsung ele fica dentro de \"Apps instalados\".",
        "Ligue a chave e toque em \"Permitir\".",
        "Toque na seta de voltar para voltar ao app.",
    )
    BigButton("Abrir Acessibilidade") { context.openAccessibilitySettings() }
    HelpText("Se o celular perguntar sobre um \"atalho\" pelo botão lateral, pode recusar — o GuardiõesSOS não precisa disso.")
    HelpCard("Travou em \"Acesso negado\" ou \"Configuração restrita\"?") {
        HelpText("Isso acontece porque o app ainda não veio da Play Store. No Samsung novo, faça nesta ordem:")
        Instructions(
            "SAMSUNG: Configurações → Segurança e privacidade → Bloqueador automático → DESLIGUE. (Sem isso, o passo abaixo nem aparece.)",
            "Toque em \"Abrir informações do app\" aqui embaixo.",
            "Toque nos 3 pontinhos ⋮, no canto de cima à direita.",
            "Toque em \"Permitir configurações restritas\" e confirme com a digital ou o PIN.",
            "Volte aqui e toque de novo em \"Abrir Acessibilidade\".",
        )
        HelpText("Quando o app estiver na Play Store, nada disso é necessário.")
        OutlinedButton(onClick = { context.openAppDetails() }, Modifier.fillMaxWidth()) {
            Text("Abrir informações do app")
        }
    }
    SkipButton(onNext)
}

@Composable
private fun PlaceStep(state: AppState, status: SetupStatus, onNext: () -> Unit) {
    val context = LocalContext.current
    var locating by remember { mutableStateOf(false) }
    val home = state.places.firstOrNull { it.hasLocation }
    StepHeader(
        "🏠",
        "Cadastre sua casa",
        "Quando o alerta sair de casa, seus vizinhos também recebem. Faça isto estando em casa. Se preferir, pule e faça depois na aba \"Lugares\".",
    )
    if (home == null) {
        BigButton(if (locating) "Procurando o GPS..." else "Marcar minha casa aqui", enabled = !locating && status.location) {
            locating = true
            LocationFetcher(context).fetch(HOME_LOCATION_TIMEOUT_MILLIS) { location ->
                locating = false
                if (location == null) {
                    context.toast("Não consegui pegar a localização. Confira se o GPS (Localização) está ligado.")
                } else {
                    Repository.addPlace(Place(name = "Casa", latitude = location.latitude, longitude = location.longitude))
                }
            }
        }
        if (!status.location) HelpText("Para marcar a casa, libere a localização no passo anterior.")
        SkipButton(onNext)
    } else {
        DoneBanner("${home.name} marcada! Agora adicione os vizinhos que devem ser avisados quando você estiver aqui.")
        ContactListEditor(
            contacts = home.contacts,
            emptyText = "Nenhum vizinho ainda.",
            onAdd = { contact -> Repository.updatePlace(home.id) { it.copy(contacts = it.contacts + contact) } },
            onRemove = { contact ->
                Repository.updatePlace(home.id) { it.copy(contacts = it.contacts.filterNot { c -> c.id == contact.id }) }
            },
            onUpdate = { contact ->
                Repository.updatePlace(home.id) { it.copy(contacts = it.contacts.map { c -> if (c.id == contact.id) contact else c }) }
            },
        )
        BigButton("Continuar", onClick = onNext)
    }
}

@Composable
private fun TestStep(state: AppState, status: SetupStatus, onRefreshStatus: () -> Unit, onFinish: () -> Unit) {
    val diagnostics by Diagnostics.state.collectAsStateWithLifecycle()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onRefreshStatus()
    }
    val settings = state.settings
    StepHeader("🧪", "Tudo pronto! Vamos testar?", "No modo teste o app faz tudo, mas NÃO manda SMS. Ele só vibra 3 vezes e mostra uma notificação.")
    SwitchRow("Modo teste", settings.testMode) { on ->
        Repository.updateSettings { it.copy(testMode = on) }
        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notifications) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Instructions(
        "Ligue o \"Modo teste\" aqui em cima.",
        settings.triggerDescription(),
        "O celular vibra 3 vezes: deu certo!",
        "Agora apague a tela e faça de novo. Depois acenda a tela e veja o resultado aqui embaixo.",
    )
    Text("Tela ligada: ${pressesText(diagnostics.screenOnPresses)}", style = MaterialTheme.typography.bodyLarge)
    Text("Tela apagada: ${pressesText(diagnostics.screenOffPresses)}", style = MaterialTheme.typography.bodyLarge)
    HelpText("Depois de testar, desligue o modo teste na tela Início. Com ele ligado, o alerta de verdade não sai.")
    BigButton("Concluir", onClick = onFinish)
}

private fun pressesText(count: Int) = if (count > 0) "✓ funcionou ($count aperto(s))" else "ainda não testado"

@Composable
private fun StepHeader(emoji: String, title: String, body: String) {
    Text(emoji, fontSize = 48.sp)
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun Instructions(vararg lines: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEachIndexed { i, line ->
            Row {
                Text("${i + 1}.", Modifier.width(28.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(line, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun BigButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SkipButton(onSkip: () -> Unit) {
    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Pular por enquanto") }
}

@Composable
private fun DoneBanner(text: String = "Pronto! Já está liberado.") {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StatusColors.ok.copy(alpha = 0.15f)),
    ) {
        Text("✅ $text", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HelpCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StatusColors.warning.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** Quando a janela do Android não aparece (a pessoa já negou antes), o caminho é pelas configurações do app. */
@Composable
private fun AppSettingsHelp(howTo: String) {
    val context = LocalContext.current
    HelpCard("Não apareceu a janela?") {
        HelpText("Libere pelas configurações do app. $howTo Depois volte aqui.")
        OutlinedButton(onClick = { context.openAppDetails() }, Modifier.fillMaxWidth()) {
            Text("Abrir configurações do app")
        }
    }
    Spacer(Modifier.height(4.dp))
}

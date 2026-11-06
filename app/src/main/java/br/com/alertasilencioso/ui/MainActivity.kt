package br.com.alertasilencioso.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.alertasilencioso.alert.Backup
import br.com.alertasilencioso.core.Features
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.trigger.MonitorService

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Filled.Home),
    CONTACTS("Guardiões", Icons.Filled.Person),
    PLACES("Lugares", Icons.Filled.Place),
    PROTEGE("Protejo", Icons.Filled.Favorite),
    SETTINGS("Ajustes", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Liga o motor do gesto de TELA APAGADA (sem Acessibilidade) se a pessoa deixou ligado em Ajustes.
        if (Features.GESTO_SEM_ACESSIBILIDADE && Repository.state.value.settings.screenOffDetection) {
            MonitorService.start(this)
        }
        enableEdgeToEdge()
        setContent {
            AlertaTheme { AppRoot() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val state by Repository.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    // O passo a passo abre sozinho na primeira vez; depois, pelo botão da tela Início.
    var wizardOpen by rememberSaveable { mutableStateOf(!state.settings.onboardingDone) }
    var status by remember { mutableStateOf(SetupStatus.read(context)) }
    val refreshStatus = { status = SetupStatus.read(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshStatus()
        // O histórico some sozinho conforme o limite escolhido em Ajustes (0 = nunca).
        Repository.purgeExpiredHistory(System.currentTimeMillis())
    }
    // Ao sair do app, salva o backup da config na nuvem (guardiões/lugares/ajustes; sem histórico).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { Backup.salvarEmBackground() }

    // Cadastro do primeiro uso (a conta, sem senha) aparece antes do passo a passo, até a pessoa passar.
    if (!state.settings.registroVisto) {
        Scaffold { padding ->
            Box(Modifier.padding(padding).imePadding()) {
                RegistroScreen(state.settings, onDone = {
                    Repository.updateSettings { it.copy(registroVisto = true) }
                })
            }
        }
        return
    }

    if (wizardOpen) {
        Scaffold { padding ->
            Box(Modifier.padding(padding).imePadding()) {
                SetupWizard(state, status, onRefreshStatus = refreshStatus, onClose = {
                    Repository.updateSettings { it.copy(onboardingDone = true) }
                    wizardOpen = false
                    tab = Tab.HOME
                })
            }
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(R.drawable.logo_escudo),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("GuardiõesSOS", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    val site = "https://guardioessos.com.br"
                    IconButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Baixe o GuardiõesSOS — um toque avisa seus guardiões com a sua localização. $site",
                            )
                        }
                        context.startActivity(Intent.createChooser(share, "Compartilhar"))
                    }) { Icon(Icons.Filled.Share, contentDescription = "Compartilhar o app") }
                    IconButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site))) }
                    }) { Icon(painterResource(R.drawable.ic_site), contentDescription = "Abrir o site") }
                    IconButton(onClick = {
                        Toast.makeText(context, "Em breve: avisos e novidades aqui.", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Filled.Notifications, contentDescription = "Avisos") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).imePadding()) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    state,
                    status,
                    onRefreshStatus = refreshStatus,
                    onOpenTab = { tab = it },
                    onOpenWizard = { wizardOpen = true },
                )
                Tab.CONTACTS -> ContactsScreen(state)
                Tab.PLACES -> PlacesScreen(state)
                Tab.PROTEGE -> GuardiaoScreen(state)
                Tab.SETTINGS -> SettingsScreen(state.settings)
            }
        }
    }
}

package br.com.alertasilencioso.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.alertasilencioso.data.Repository

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Filled.Home),
    CONTACTS("Sempre avisar", Icons.Filled.Person),
    PLACES("Lugares", Icons.Filled.Place),
    SETTINGS("Ajustes", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlertaTheme { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val state by Repository.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    // O passo a passo abre sozinho na primeira vez; depois, pelo botão da tela Início.
    var wizardOpen by rememberSaveable { mutableStateOf(!state.settings.onboardingDone) }
    var status by remember { mutableStateOf(SetupStatus.read(context)) }
    val refreshStatus = { status = SetupStatus.read(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshStatus() }

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
                Tab.SETTINGS -> SettingsScreen(state.settings)
            }
        }
    }
}

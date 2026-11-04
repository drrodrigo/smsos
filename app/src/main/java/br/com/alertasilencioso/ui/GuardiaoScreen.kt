package br.com.alertasilencioso.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.alert.Aceitar
import br.com.alertasilencioso.data.AppState
import kotlinx.coroutines.launch

@Composable
fun GuardiaoScreen(state: AppState) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var refresh by remember { mutableStateOf(0) }
    var pendentes by remember { mutableStateOf<List<Aceitar.Pendente>>(emptyList()) }
    var protejo by remember { mutableStateOf<List<Aceitar.Protegido>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var digitando by remember { mutableStateOf(false) }

    LaunchedEffect(refresh, state.settings.userPhone, state.settings.dispositivo) {
        carregando = true
        pendentes = Aceitar.pendentes()
        protejo = Aceitar.quemProtejo()
        carregando = false
    }

    val aceitar: (String) -> Unit = { codigo ->
        context.toast("Aceitando convite...")
        escopo.launch {
            val r = Aceitar.aceitar(codigo)
            when {
                r == null -> context.toast("Não consegui aceitar. Confira a internet e tente de novo.")
                !r.ok -> context.toast("Código inválido ou convite indisponível.")
                r.jaAceito -> context.toast("Você já protege ${r.usuarioNome ?: "essa pessoa"}.")
                else -> context.toast("Pronto! Agora você protege ${r.usuarioNome ?: "essa pessoa"}.")
            }
            if (r?.ok == true) refresh++
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Protejo", style = MaterialTheme.typography.headlineSmall)
            HelpText("As pessoas que você protege como guardião. Quando elas acionarem o SOS, você recebe o alerta com a localização.")
        }
        if (pendentes.isNotEmpty()) {
            item {
                SectionCard("Convites pra você") {
                    pendentes.forEach { p ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${p.usuarioNome.ifBlank { "Alguém" }} quer você como guardião",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Button(onClick = { aceitar(p.codigo) }) { Text("Aceitar") }
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Tenho um código") {
                HelpText("Recebeu um convite com um código? Digite aqui pra aceitar.")
                Button(onClick = { digitando = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Aceitar com código")
                }
            }
        }
        item {
            SectionCard("Quem eu protejo") {
                when {
                    carregando && protejo.isEmpty() -> HelpText("Carregando...")
                    protejo.isEmpty() -> HelpText("Você ainda não protege ninguém. Aceite um convite para começar.")
                    else -> protejo.forEach { Text("🛡️ ${it.usuarioNome.ifBlank { "Alguém" }}", style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
    }

    if (digitando) {
        var codigo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { digitando = false },
            title = { Text("Aceitar convite") },
            text = {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase() },
                    label = { Text("Código do convite") },
                    placeholder = { Text("Ex.: 215F67") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { digitando = false; aceitar(codigo) }, enabled = codigo.isNotBlank()) {
                    Text("Aceitar")
                }
            },
            dismissButton = { TextButton(onClick = { digitando = false }) { Text("Cancelar") } },
        )
    }
}

package br.com.alertasilencioso.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.alert.Convidar
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Repository
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(state: AppState) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var convidando by remember { mutableStateOf(false) }

    // Cria o convite pro app (código + link) e abre o WhatsApp DAQUELE número já com a mensagem pronta.
    // Usado tanto pelo botão "Convidar" de cada guardião que já está na lista (sem redigitar) quanto
    // pelo "Convidar um guardião" (pra quem ainda não está na lista).
    val convidar: (String, String) -> Unit = { nome, fone ->
        val numero = PhoneNumbers.normalize(fone)
        // Já entra como guardião (recebe SMS) e, ao instalar com o convite, ganha o app (push).
        if (state.alwaysContacts.none { PhoneNumbers.key(it.phone) == PhoneNumbers.key(numero) }) {
            Repository.addAlwaysContact(Contact(name = nome, phone = numero))
        }
        context.toast("Criando o convite...")
        escopo.launch {
            val r = Convidar.convidar(numero)
            if (r == null) {
                context.toast("Não consegui criar o convite agora. Confira a internet e tente de novo.")
            } else {
                val nomeUsuario = state.settings.userName.ifBlank { "Alguém" }
                val msg = "$nomeUsuario te escolheu como guardião no GuardiõesSOS. " +
                    "Se algo acontecer, você recebe o alerta com a localização. " +
                    "Baixe o app e use o código ${r.codigo}: ${r.link}"
                // Abre direto a conversa do WhatsApp DAQUELE número (sai da conta do próprio
                // usuário, sem API). Se não tiver WhatsApp, cai no menu de compartilhar.
                val soDigitos = numero.filter { it.isDigit() }
                val internacional = if (numero.startsWith("+")) soDigitos else "55$soDigitos"
                val whatsapp = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/$internacional?text=${Uri.encode(msg)}"),
                )
                try {
                    context.startActivity(whatsapp)
                } catch (e: ActivityNotFoundException) {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, msg)
                    }
                    context.startActivity(Intent.createChooser(share, "Convidar guardião"))
                }
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Guardiões", style = MaterialTheme.typography.headlineSmall)
            HelpText("Seus guardiões recebem o alerta em qualquer lugar onde você estiver. Por exemplo, seu filho, mesmo que ele esteja longe.")
        }
        item {
            SectionCard("Meus guardiões") {
                ContactListEditor(
                    contacts = state.alwaysContacts,
                    emptyText = "Nenhum guardião ainda.",
                    onAdd = Repository::addAlwaysContact,
                    onRemove = { Repository.removeAlwaysContact(it.id) },
                    onUpdate = Repository::updateAlwaysContact,
                    // Um toque em "Convidar" ao lado de quem já está aqui — sem digitar nome e número de novo.
                    onInvite = { convidar(it.name, it.phone) },
                )
                HelpText("Toque em \"Convidar\" ao lado de um guardião pra mandar o convite do app pelo WhatsApp — sem redigitar nada.")
            }
        }
        item {
            SectionCard("Convidar alguém novo") {
                HelpText("Pra convidar quem ainda NÃO está na lista acima. Quem instala com o seu convite recebe o alerta na hora (aviso no celular) e vai poder te acompanhar.")
                Button(onClick = { convidando = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Convidar um guardião novo")
                }
            }
        }
        item {
            HelpText("Os vizinhos e as pessoas de cada lugar (casa, escritório...) ficam na aba \"Lugares\".")
        }
    }

    if (convidando) {
        ContactDialog(
            title = "Convidar guardião",
            confirmLabel = "Convidar",
            onDismiss = { convidando = false },
            onConfirm = { nome, fone ->
                convidando = false
                convidar(nome, fone)
            },
        )
    }
}

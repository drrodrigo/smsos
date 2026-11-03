package br.com.alertasilencioso.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Repository

@Composable
fun ContactsScreen(state: AppState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Sempre avisar", style = MaterialTheme.typography.headlineSmall)
            HelpText("Estas pessoas recebem o alerta em qualquer lugar onde você estiver. Por exemplo, seu filho, mesmo que ele esteja longe.")
        }
        item {
            SectionCard("Contatos") {
                ContactListEditor(
                    contacts = state.alwaysContacts,
                    emptyText = "Nenhum contato ainda.",
                    onAdd = Repository::addAlwaysContact,
                    onRemove = { Repository.removeAlwaysContact(it.id) },
                )
            }
        }
        item {
            HelpText("Os vizinhos e as pessoas de cada lugar (casa, escritório...) ficam na aba \"Lugares\".")
        }
    }
}

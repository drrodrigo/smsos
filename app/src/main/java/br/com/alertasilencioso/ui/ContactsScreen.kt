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
                )
            }
        }
        item {
            HelpText("Os vizinhos e as pessoas de cada lugar (casa, escritório...) ficam na aba \"Lugares\".")
        }
    }
}

package br.com.alertasilencioso.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.alert.LocationFetcher
import br.com.alertasilencioso.core.Coordinates
import br.com.alertasilencioso.core.GeoPoint
import br.com.alertasilencioso.data.AppState
import br.com.alertasilencioso.data.Place
import br.com.alertasilencioso.data.Repository
import java.util.Locale

private val RADIUS_OPTIONS = listOf(100, 200, 300, 500, 1000)
private const val PLACE_LOCATION_TIMEOUT_MILLIS = 30_000L

@Composable
fun PlacesScreen(state: AppState) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = state.places.firstOrNull { it.id == editingId }
    if (editing != null) {
        BackHandler { editingId = null }
        PlaceEditor(editing, onClose = { editingId = null })
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Lugares", style = MaterialTheme.typography.headlineSmall)
            HelpText("Cadastre os lugares onde você costuma ficar: casa, escritório, garagem... Quando o alerta sair de dentro de um lugar, as pessoas dele (por exemplo, os vizinhos) também recebem a mensagem.")
        }
        // Sugestões prontas: a pessoa toca e já vai configurar. Some quando o lugar já foi criado.
        val sugestoes = listOf("🏠 Casa" to "Casa", "💼 Trabalho" to "Trabalho")
            .filter { (_, nome) -> state.places.none { it.name.equals(nome, ignoreCase = true) } }
        if (sugestoes.isNotEmpty()) {
            item {
                HelpText("Comece pelos mais comuns:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sugestoes.forEach { (rotulo, nome) ->
                        OutlinedButton(
                            onClick = {
                                val place = Place(name = nome)
                                Repository.addPlace(place)
                                editingId = place.id
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(rotulo) }
                    }
                }
            }
        }
        items(state.places, key = { it.id }) { place -> PlaceCard(place) { editingId = place.id } }
        item {
            Button(onClick = {
                val place = Place(name = "Novo lugar")
                Repository.addPlace(place)
                editingId = place.id
            }) {
                Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Adicionar lugar")
            }
        }
    }
}

@Composable
private fun PlaceCard(place: Place, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(place.name.ifBlank { "Sem nome" }, style = MaterialTheme.typography.titleMedium)
                HelpText("Área de ${place.radiusMeters} m · ${place.contacts.size} contato(s)")
                if (!place.hasLocation) {
                    Text("Falta marcar a localização", color = StatusColors.warning, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PlaceEditor(place: Place, onClose: () -> Unit) {
    val context = LocalContext.current
    var name by remember(place.id) { mutableStateOf(place.name) }
    var coordinatesText by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val update: ((Place) -> Place) -> Unit = { Repository.updatePlace(place.id, it) }

    val locateHere = {
        locating = true
        LocationFetcher(context).fetch(PLACE_LOCATION_TIMEOUT_MILLIS) { location ->
            locating = false
            if (location == null) {
                context.toast("Não consegui pegar a localização. Confira se o GPS (Localização) está ligado.")
            } else {
                update { it.copy(latitude = location.latitude, longitude = location.longitude) }
                context.toast("Localização salva (precisão de ${location.accuracy.toInt()} m)")
            }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { granted -> granted }) locateHere() else context.toast("Sem a permissão de localização não dá para marcar o lugar.")
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                Text("Editar lugar", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    name = value
                    update { it.copy(name = value) }
                },
                label = { Text("Nome do lugar (ex.: Casa, Escritório)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SectionCard("Onde fica") {
                val lat = place.latitude
                val lng = place.longitude
                if (lat != null && lng != null) {
                    Text(String.format(Locale.US, "%.5f, %.5f", lat, lng), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { context.openMap(GeoPoint(lat, lng), place.name) }) { Text("Ver no mapa") }
                } else {
                    Text("Localização ainda não marcada", color = StatusColors.warning)
                }
                Button(
                    onClick = {
                        if (LocationFetcher(context).hasPermission()) {
                            locateHere()
                        } else {
                            locationPermission.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    },
                    enabled = !locating,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (locating) "Procurando o GPS..." else "Usar minha localização atual") }
                HelpText("Faça isso estando no lugar (por exemplo, dentro de casa).")
                OutlinedTextField(
                    value = coordinatesText,
                    onValueChange = { coordinatesText = it },
                    label = { Text("Ou cole as coordenadas") },
                    placeholder = { Text("-19.46830, -42.53670") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                HelpText("No Google Maps, segure o dedo em cima do lugar e copie os números que aparecem.")
                OutlinedButton(onClick = {
                    val point = Coordinates.parse(coordinatesText)
                    if (point == null) {
                        context.toast("Não entendi essas coordenadas")
                    } else {
                        update { it.copy(latitude = point.latitude, longitude = point.longitude) }
                        coordinatesText = ""
                        context.toast("Localização salva")
                    }
                }, enabled = coordinatesText.isNotBlank()) { Text("Usar estas coordenadas") }
            }
        }
        item {
            SectionCard("Tamanho da área") {
                HelpText("Se o alerta sair a até esta distância do ponto marcado, as pessoas deste lugar são avisadas.")
                ChipRow(RADIUS_OPTIONS, place.radiusMeters, { if (it >= 1000) "${it / 1000} km" else "$it m" }) { radius ->
                    update { it.copy(radiusMeters = radius) }
                }
            }
        }
        item {
            SectionCard("Quem avisar neste lugar") {
                ContactListEditor(
                    contacts = place.contacts,
                    emptyText = "Nenhum contato. Adicione seus vizinhos ou quem estiver perto deste lugar.",
                    onAdd = { contact -> update { it.copy(contacts = it.contacts + contact) } },
                    onRemove = { contact -> update { it.copy(contacts = it.contacts.filterNot { c -> c.id == contact.id }) } },
                    onUpdate = { contact -> update { it.copy(contacts = it.contacts.map { c -> if (c.id == contact.id) contact else c }) } },
                )
            }
        }
        item {
            OutlinedButton(onClick = { confirmingDelete = true }, Modifier.fillMaxWidth()) { Text("Excluir este lugar") }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Excluir \"${place.name}\"?") },
            text = { Text("Os contatos deste lugar também serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    Repository.removePlace(place.id)
                    onClose()
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") } },
        )
    }
}

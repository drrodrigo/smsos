package br.com.alertasilencioso.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.core.GeoPoint
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.Contact
import java.util.Locale

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun Context.openMap(point: GeoPoint, label: String = "") {
    val coords = String.format(Locale.US, "%.6f,%.6f", point.latitude, point.longitude)
    val uri = Uri.parse("geo:$coords?q=$coords(${Uri.encode(label)})")
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$coords")))
    }
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun HelpText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

/** Lista de contatos com os botões para adicionar da agenda ou digitando. */
@Composable
fun ContactListEditor(
    contacts: List<Contact>,
    emptyText: String,
    onAdd: (Contact) -> Unit,
    onRemove: (Contact) -> Unit,
) {
    val context = LocalContext.current
    var typing by remember { mutableStateOf(false) }
    val add: (String, String) -> Unit = { name, phone ->
        if (contacts.any { PhoneNumbers.key(it.phone) == PhoneNumbers.key(phone) }) {
            context.toast("Esse número já está na lista")
        } else {
            onAdd(Contact(name = name, phone = phone))
        }
    }
    val pickContact = rememberContactPicker(add)

    if (contacts.isEmpty()) HelpText(emptyText)
    contacts.forEach { contact ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(contact.name.ifBlank { "Sem nome" }, style = MaterialTheme.typography.bodyLarge)
                HelpText(contact.phone)
            }
            IconButton(onClick = { onRemove(contact) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remover ${contact.name}")
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = pickContact) {
            Icon(Icons.Filled.Person, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Da agenda")
        }
        OutlinedButton(onClick = { typing = true }) {
            Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Digitar")
        }
    }
    if (typing) {
        AddContactDialog(
            onDismiss = { typing = false },
            onConfirm = { name, phone ->
                add(name, phone)
                typing = false
            },
        )
    }
}

/** Abre a agenda do celular para escolher um número. Não precisa de permissão para ler os contatos. */
@Composable
fun rememberContactPicker(onPicked: (name: String, phone: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use {
                if (it.moveToFirst()) onPicked(it.getString(0).orEmpty(), it.getString(1).orEmpty())
            }
        }.onFailure { context.toast("Não foi possível ler esse contato") }
    }
    return {
        try {
            launcher.launch(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI))
        } catch (e: ActivityNotFoundException) {
            context.toast("Nenhum app de contatos encontrado. Use \"Digitar\".")
        }
    }
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onConfirm: (name: String, phone: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo contato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                )
                val warning = PhoneNumbers.formatWarning(phone)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Celular com DDD") },
                    placeholder = { Text("(31) 99999-8888") },
                    singleLine = true,
                    isError = warning != null,
                    supportingText = warning?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), phone.trim()) }, enabled = PhoneNumbers.isPlausible(phone)) {
                Text("Adicionar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

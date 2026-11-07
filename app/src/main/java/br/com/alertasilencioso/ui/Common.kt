package br.com.alertasilencioso.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.alert.GatewaySender
import br.com.alertasilencioso.core.GeoPoint
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.Contact
import br.com.alertasilencioso.data.Limits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

/**
 * Igual ao [SectionCard], mas a pessoa clica no título para ABRIR ou FECHAR o conteúdo —
 * deixa a tela leve. Quando está fechado, mostra um resumo ([subtitle]) pra não precisar abrir.
 */
@Composable
fun CollapsibleSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!expanded && subtitle != null) HelpText(subtitle)
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Fechar $title" else "Abrir $title",
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
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
    onUpdate: (Contact) -> Unit,
    max: Int = Limits.MAX_CONTACTS,
    // Quando passado, cada contato ganha um botão "Convidar" (um toque, sem redigitar nada): o app
    // já tem o nome e o número. Só a tela de guardiões usa; as outras listas passam nulo.
    onInvite: ((Contact) -> Unit)? = null,
) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var typing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Contact?>(null) }
    val add: (String, String) -> Unit = { name, phone ->
        val numero = PhoneNumbers.normalize(phone)
        when {
            contacts.size >= max -> context.toast("Limite de $max contatos nesta lista.")
            contacts.any { PhoneNumbers.key(it.phone) == PhoneNumbers.key(numero) } ->
                context.toast("Esse número já está na lista")
            else -> onAdd(Contact(name = name, phone = numero))
        }
    }
    val pickContact = rememberContactPicker(add)
    // Manda um SMS de teste pro próprio número do guardião, pra confirmar que ele recebe.
    val testar: (String) -> Unit = { phone ->
        if (!GatewaySender.configurado()) {
            context.toast("Teste indisponível nesta versão.")
        } else {
            context.toast("Enviando um teste pra esse número...")
            escopo.launch {
                val texto = "Teste do GuardioesSOS: voce foi adicionado como guardiao. " +
                    "Se recebeu, seu numero esta certo para os alertas. Pode ignorar."
                val ok = withContext(Dispatchers.IO) { GatewaySender.enviar(listOf(phone to texto))[phone] == true }
                context.toast(if (ok) "Teste enviado ✓ Confira se ele recebeu." else "Não enviou. Confira o número.")
            }
        }
    }

    if (contacts.isEmpty()) HelpText(emptyText)
    contacts.forEach { contact ->
        val aviso = PhoneNumbers.formatWarning(contact.phone)
            ?: "Número incompleto. Toque para corrigir.".takeIf { !PhoneNumbers.isPlausible(contact.phone) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { editing = contact }.padding(vertical = 4.dp)) {
                Text(contact.name.ifBlank { "Sem nome" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (contact.phone.isBlank()) "Sem número" else PhoneNumbers.format(contact.phone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (aviso != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (aviso != null) {
                    Text("⚠ $aviso", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            if (onInvite != null) {
                TextButton(onClick = { onInvite(contact) }) { Text("Convidar") }
            }
            IconButton(onClick = { editing = contact }) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar ${contact.name}")
            }
            IconButton(onClick = { onRemove(contact) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remover ${contact.name}")
            }
        }
    }
    if (contacts.size >= max) {
        HelpText("Você chegou ao limite de $max contatos nesta lista.")
    } else {
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
    }
    if (typing) {
        ContactDialog(
            title = "Novo guardião",
            confirmLabel = "Adicionar",
            onTest = testar,
            onDismiss = { typing = false },
            onConfirm = { name, phone ->
                add(name, phone)
                typing = false
            },
        )
    }
    editing?.let { alvo ->
        ContactDialog(
            title = "Editar guardião",
            confirmLabel = "Salvar",
            initialName = alvo.name,
            initialPhone = alvo.phone,
            onTest = testar,
            onDismiss = { editing = null },
            onConfirm = { name, phone ->
                onUpdate(alvo.copy(name = name, phone = PhoneNumbers.normalize(phone)))
                editing = null
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
fun ContactDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit,
    onTest: ((String) -> Unit)? = null,
    initialName: String = "",
    initialPhone: String = "",
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    // A padronização (tira traço/+/55, acrescenta o 9) é feita aqui, pra validar e avisar já no número arrumado.
    val canonico = PhoneNumbers.normalize(phone)
    val warning = PhoneNumbers.formatWarning(canonico)
    val valido = PhoneNumbers.isPlausible(canonico)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
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
                if (valido && canonico != phone.trim()) {
                    Text(
                        "Vai salvar como: ${PhoneNumbers.format(canonico)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (onTest != null) {
                    TextButton(onClick = { onTest(canonico) }, enabled = valido) {
                        Text("Enviar um teste pra esse número")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), phone.trim()) }, enabled = valido) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

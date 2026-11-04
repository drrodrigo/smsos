package br.com.alertasilencioso.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.R
import br.com.alertasilencioso.alert.Backup
import br.com.alertasilencioso.alert.Otp
import br.com.alertasilencioso.alert.Registrar
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings
import kotlinx.coroutines.launch

/**
 * Primeira tela do app: cria a conta do usuário (nome + WhatsApp), SEM senha — a identidade é o
 * aparelho + o telefone. O cadastro é enviado em background; a pessoa pode pular e usar o app mesmo assim.
 */
@Composable
fun RegistroScreen(settings: Settings, onDone: () -> Unit) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var nome by remember { mutableStateOf(settings.userName) }
    var fone by remember { mutableStateOf(settings.userPhone) }
    val foneCanon = PhoneNumbers.normalize(fone)
    val foneOk = PhoneNumbers.isPlausible(foneCanon)
    val aviso = PhoneNumbers.formatWarning(foneCanon)
    var checando by remember { mutableStateOf(false) }
    var mostrarRestore by remember { mutableStateOf(false) }
    var resumoRestore by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painterResource(R.drawable.logo_escudo),
            contentDescription = null,
            modifier = Modifier.size(84.dp).padding(top = 16.dp),
        )
        Text(
            "Bem-vindo ao GuardiõesSOS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Pra começar, me diga seu nome e seu WhatsApp. É a sua conta — sem senha, sem complicação.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Seu nome") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = fone,
            onValueChange = { fone = it },
            label = { Text("Seu WhatsApp (com DDD)") },
            placeholder = { Text("(31) 99999-8888") },
            singleLine = true,
            isError = aviso != null,
            supportingText = aviso?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        if (foneOk && foneCanon != fone.trim()) {
            Text(
                "Vai salvar como: ${PhoneNumbers.format(foneCanon)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Button(
            onClick = {
                Repository.updateSettings { it.copy(userName = nome.trim(), userPhone = foneCanon) }
                Registrar.registrar()
                onDone()
            },
            enabled = nome.isNotBlank() && foneOk,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continuar") }
        TextButton(onClick = {
            if (nome.isNotBlank()) Repository.updateSettings { it.copy(userName = nome.trim()) }
            onDone()
        }) { Text("Pular por enquanto") }
        TextButton(
            enabled = foneOk && !checando,
            onClick = {
                checando = true
                escopo.launch {
                    val t = Backup.tem(foneCanon)
                    checando = false
                    if (t.existe) { resumoRestore = t.resumo; mostrarRestore = true }
                    else context.toast("Não achamos uma conta com esse número.")
                }
            },
        ) { Text(if (checando) "Procurando..." else "Já usei antes? Restaurar meus dados") }
        HelpText("Usamos seus dados só pra criar sua conta e te avisar — nada de spam. Política de privacidade em guardioessos.com.br/privacidade.")
    }

    if (mostrarRestore) {
        RestaurarDialog(
            telefone = foneCanon,
            resumo = resumoRestore,
            onDismiss = { mostrarRestore = false },
            onRestaurado = { mostrarRestore = false; onDone() },
        )
    }
}

/** Confirma o número por OTP e, se der certo, restaura os guardiões/lugares/ajustes da nuvem. */
@Composable
private fun RestaurarDialog(telefone: String, resumo: String?, onDismiss: () -> Unit, onRestaurado: () -> Unit) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var enviado by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restaurar sua conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Achamos uma conta no ${PhoneNumbers.format(telefone)}${resumo?.let { " ($it)" } ?: ""}.")
                if (!enviado) {
                    HelpText("Para restaurar com segurança, confirme que o número é seu: mandamos um código por SMS.")
                } else {
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("Código de 6 dígitos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (!enviado) {
                TextButton(enabled = !ocupado, onClick = {
                    ocupado = true
                    escopo.launch {
                        val ok = Otp.enviar(telefone)
                        ocupado = false
                        if (ok) { enviado = true; context.toast("Código enviado por SMS.") }
                        else context.toast("Não consegui enviar o código agora.")
                    }
                }) { Text(if (ocupado) "Enviando..." else "Enviar código") }
            } else {
                TextButton(enabled = codigo.length == 6 && !ocupado, onClick = {
                    ocupado = true
                    escopo.launch {
                        val r = Backup.restaurar(telefone, codigo)
                        ocupado = false
                        when {
                            r.ok && r.dados != null -> {
                                Repository.restaurarBackup(r.dados)
                                context.toast("Conta restaurada ✓")
                                onRestaurado()
                            }
                            r.ok -> {
                                Repository.updateSettings {
                                    it.copy(userPhone = PhoneNumbers.normalize(telefone), registroVisto = true, numeroVerificado = true)
                                }
                                context.toast("Número confirmado.")
                                onRestaurado()
                            }
                            r.erro == "codigo_incorreto" -> context.toast("Código incorreto.")
                            r.erro == "expirado" -> context.toast("O código expirou. Envie outro.")
                            else -> context.toast("Não consegui restaurar agora.")
                        }
                    }
                }) { Text(if (ocupado) "Restaurando..." else "Restaurar") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.R
import br.com.alertasilencioso.alert.Registrar
import br.com.alertasilencioso.core.PhoneNumbers
import br.com.alertasilencioso.data.Repository
import br.com.alertasilencioso.data.Settings

/**
 * Primeira tela do app: cria a conta do usuário (nome + WhatsApp), SEM senha — a identidade é o
 * aparelho + o telefone. O cadastro é enviado em background; a pessoa pode pular e usar o app mesmo assim.
 */
@Composable
fun RegistroScreen(settings: Settings, onDone: () -> Unit) {
    var nome by remember { mutableStateOf(settings.userName) }
    var fone by remember { mutableStateOf(settings.userPhone) }
    val foneCanon = PhoneNumbers.normalize(fone)
    val foneOk = PhoneNumbers.isPlausible(foneCanon)
    val aviso = PhoneNumbers.formatWarning(foneCanon)

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
        HelpText("Usamos seus dados só pra criar sua conta e te avisar — nada de spam. Política de privacidade em guardioessos.com.br/privacidade.")
    }
}

package br.com.alertasilencioso.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.alertasilencioso.alert.Alarme
import br.com.alertasilencioso.core.GeoPoint
import br.com.alertasilencioso.core.MessageBuilder
import br.com.alertasilencioso.data.Profile
import br.com.alertasilencioso.data.Settings
import br.com.alertasilencioso.data.templateFor

/** Posição de exemplo (Praça Sete, centro de BH). NÃO é a localização real de ninguém — é só vitrine. */
private val DEMO_POS = GeoPoint(-19.9191, -43.9386)

/**
 * Monta os dois textos (Segurança e Saúde) EXATAMENTE como o guardião recebe, com dados de exemplo.
 * Não envia nada, não precisa de contato cadastrado, não toca no gateway. Função pura (dá pra testar).
 */
fun textosDemo(settings: Settings): Pair<String, String> {
    val nome = settings.userName.ifBlank { "Maria" }
    val agora = System.currentTimeMillis()
    val seguranca = MessageBuilder.alert(settings.templateFor(Profile.SEGURANCA), nome, DEMO_POS, listOf("Casa"), agora)
    val saude = MessageBuilder.alert(settings.templateFor(Profile.SAUDE), nome, DEMO_POS, listOf("Casa"), agora)
    return seguranca to saude
}

/**
 * Cartão de DEMONSTRAÇÃO (pra mostrar pra alguém) + AUTOTESTE (conferir que toca/acende neste aparelho).
 * Nada aqui envia SMS, nem avisa ninguém, nem gasta estoque — é tudo local.
 */
@Composable
fun DemonstracaoCard(settings: Settings) {
    val context = LocalContext.current
    var vendoMensagem by remember { mutableStateOf(false) }

    SectionCard("Demonstração e teste") {
        HelpText("Pra mostrar como funciona pra alguém, ou conferir no seu aparelho. Nada é enviado — ninguém é avisado.")
        OutlinedButton(onClick = { vendoMensagem = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver como a mensagem chega")
        }
        OutlinedButton(
            onClick = {
                // Toca a sirene e abre a tela cheia de alerta recebido (a experiência do guardião).
                // A própria tela tem "Silenciar/Fechar" que para tudo; e para sozinha em 2 min.
                val (seguranca, _) = textosDemo(settings)
                Alarme.tocar(context.applicationContext)
                context.startActivity(
                    Intent(context, AlertaRecebidoActivity::class.java)
                        .putExtra("titulo", "TESTE — ⚠️ Alerta de emergência")
                        .putExtra("corpo", "Isto é um TESTE. $seguranca")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Testar a sirene e a tela do guardião")
        }
    }

    if (vendoMensagem) {
        val (seguranca, saude) = textosDemo(settings)
        AlertDialog(
            onDismissRequest = { vendoMensagem = false },
            confirmButton = { TextButton(onClick = { vendoMensagem = false }) { Text("Fechar") } },
            title = { Text("Como a mensagem chega") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("É assim que o seu guardião recebe (exemplo — nada foi enviado):")
                    Balao("🛡️ Segurança", seguranca)
                    Balao("❤️ Saúde", saude)
                }
            },
        )
    }
}

/** Um "balão" de mensagem, estilo SMS recebido, só pra demonstração. */
@Composable
private fun Balao(rotulo: String, texto: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(texto, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

package br.com.alertasilencioso.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import br.com.alertasilencioso.alert.Alarme
import br.com.alertasilencioso.push.MessagingService

/**
 * Tela que aparece POR CIMA DE TUDO (mesmo com o celular bloqueado) quando um alerta de SOS chega.
 * "Abrir no mapa" é a ação principal; "Silenciar" para o alarme mas MANTÉM a tela (pra ainda dar pra
 * ver o mapa). Só o "Fechar" sai da tela.
 */
class AlertaRecebidoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val titulo = intent.getStringExtra("titulo") ?: "Alerta de emergência"
        val corpo = intent.getStringExtra("corpo").orEmpty()
        val mapa = Regex("https?://\\S+").find(corpo)?.value

        setContent {
            AlertaTheme {
                var silenciado by remember { mutableStateOf(false) }
                Surface(color = Color(0xFFB00020)) {
                    Column(
                        Modifier.fillMaxSize().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("⚠️", fontSize = 72.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            titulo,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(corpo, color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(34.dp))
                        if (mapa != null) {
                            Button(
                                onClick = {
                                    pararAlarme()
                                    silenciado = true
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapa))) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFFB00020),
                                ),
                            ) { Text("Ver no mapa (silencia)", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                            Spacer(Modifier.height(12.dp))
                        }
                        if (!silenciado) {
                            OutlinedButton(
                                onClick = { pararAlarme(); silenciado = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            ) { Text("Silenciar", fontSize = 16.sp) }
                        } else {
                            OutlinedButton(
                                onClick = { finish() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            ) { Text("Fechar", fontSize = 16.sp) }
                        }
                    }
                }
            }
        }
    }

    private fun pararAlarme() {
        Alarme.parar(applicationContext)
        NotificationManagerCompat.from(this).cancel(MessagingService.SOS_NOTIF_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        Alarme.parar(applicationContext)
    }
}

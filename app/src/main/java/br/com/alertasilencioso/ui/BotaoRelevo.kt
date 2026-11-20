package br.com.alertasilencioso.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/**
 * Botão secundário com CARA DE BOTÃO: fundo claro, borda fina e sombra embaixo (relevo), que "afunda"
 * ao apertar. Pedido do Rodrigo (20/11): o botão só de contorno "não ficou parecendo um botão mesmo".
 * Substitui o OutlinedButton no app todo; o iPhone (Tema.swift, EstiloM3 .contorno) e a página
 * guardioessos.com.br/app (.btn.contorno) usam o MESMO visual.
 */
@Composable
fun BotaoRelevo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val escuro = isSystemInDarkTheme()
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (escuro) Color(0xFF3B3842) else Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 5.dp,
        ),
        border = BorderStroke(1.dp, if (escuro) Color(0xFF49454F) else Color(0xFFE2D8DF)),
    ) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontWeight = FontWeight.SemiBold))) {
            content()
        }
    }
}

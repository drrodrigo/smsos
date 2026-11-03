package br.com.alertasilencioso.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB71C1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E4EA),
    onSecondaryContainer = Color(0xFF101D23),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF1C2B32),
    secondaryContainer = Color(0xFF334148),
    onSecondaryContainer = Color(0xFFD9E4EA),
)

object StatusColors {
    val ok: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
    val warning: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFE65100)
    val danger: Color @Composable get() = MaterialTheme.colorScheme.error
}

@Composable
fun AlertaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

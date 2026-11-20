package br.com.alertasilencioso.relogio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/** O que a tela pode pedir ao [SosViewModel]. */
class Acoes(
    val cancelar: () -> Unit,
    val trocarPerfil: () -> Unit,
    val escolher: (Perfil) -> Unit,
    val tentarDeNovo: () -> Unit,
    val mostrarAjustes: (Boolean) -> Unit,
    val mudarAjustes: (Ajustes) -> Unit,
    val esconder: () -> Unit,
    val fechar: () -> Unit,
)

private val VERMELHO = Color(0xFFD32F2F)
private val AZUL = Color(0xFF1E66D0)
private val VERDE = Color(0xFF66BB6A)
private val CINZA_ESCURO = Color(0xFF2E2E2E)
private val CINZA = Color(0xFF8A8A8A)
private val FUNDO_BOTAO = Color(0xFF262626)

@Composable
fun ConteudoDoSos(estado: Estado, ajustes: Ajustes, mostrandoAjustes: Boolean, acoes: Acoes) {
    // O botão de baixo do Galaxy Watch (voltar): na contagem, CANCELA; nos ajustes, volta ao menu; no envio,
    // sai da frente sem fechar (a resposta ainda vibra); no resto, fecha como sempre.
    BackHandler(enabled = estado is Estado.Contando) { acoes.cancelar() }
    BackHandler(enabled = estado is Estado.Enviando) { acoes.esconder() }
    BackHandler(enabled = estado is Estado.Escolha && mostrandoAjustes) { acoes.mostrarAjustes(false) }

    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (estado) {
                is Estado.Contando -> if (estado.discreto) ContandoDiscreto(acoes) else ContandoVisivel(estado, acoes)
                // Discreto: preto, sem nada (tocar não cancela: o pedido já saiu).
                is Estado.Enviando -> if (!estado.discreto) Centro { Texto("Enviando…", 18.sp, Color.White, negrito = true) }
                is Estado.Pronto -> if (estado.discreto) ProntoDiscreto(estado, acoes) else ProntoVisivel(estado, acoes)
                Estado.Escolha -> if (mostrandoAjustes) TelaDeAjustes(ajustes, acoes) else Escolha(acoes)
            }
        }
    }
}

/** MODO DISCRETO: tudo preto (parece apagada). Tocar em QUALQUER lugar cancela. */
@Composable
private fun ContandoDiscreto(acoes: Acoes) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { acoes.cancelar() },
    ) {
        Text(
            "toque para cancelar",
            color = CINZA_ESCURO,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}

/** Modo visível: número grande, o tipo, CANCELAR enorme e "Trocar para ...". */
@Composable
private fun ContandoVisivel(c: Estado.Contando, acoes: Acoes) {
    val saude = c.perfil == Perfil.SAUDE
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Texto("${c.faltam}", 46.sp, Color.White, negrito = true)
        Texto(if (saude) "SOS de Saúde" else "SOS de Segurança", 13.sp, if (saude) Color(0xFF90B8F8) else Color(0xFFFF8A80))
        Spacer(Modifier.height(6.dp))
        Botao("CANCELAR", VERMELHO, altura = 52.dp, tamanho = 18.sp, aoTocar = acoes.cancelar)
        Spacer(Modifier.height(6.dp))
        Botao(
            if (saude) "Trocar para Segurança" else "Trocar para Saúde",
            FUNDO_BOTAO,
            altura = 30.dp,
            tamanho = 11.sp,
            largura = 0.8f,
            aoTocar = acoes.trocarPerfil,
        )
    }
}

/** MODO DISCRETO depois do disparo: preto. Só aparece letra quando a pessoa PRECISA saber. */
@Composable
private fun ProntoDiscreto(p: Estado.Pronto, acoes: Acoes) {
    when (p.desfecho) {
        // Chegou ao celular (vibração dupla): continua preto; tocar fecha.
        Desfecho.ENVIADO -> Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { acoes.fechar() },
        )
        Desfecho.TESTE -> Centro(aoTocar = acoes.fechar) { Texto("modo teste: nada foi enviado", 11.sp, CINZA) }
        Desfecho.SEM_CONTATOS -> Centro(aoTocar = acoes.fechar) { Texto("Cadastre guardiões no celular", 12.sp, CINZA) }
        Desfecho.NAO_SAIU -> Centro {
            Texto("Não saiu: celular longe", 12.sp, CINZA)
            Spacer(Modifier.height(8.dp))
            Botao("Tentar de novo", FUNDO_BOTAO, altura = 34.dp, tamanho = 12.sp, largura = 0.7f, aoTocar = acoes.tentarDeNovo)
        }
    }
}

@Composable
private fun ProntoVisivel(p: Estado.Pronto, acoes: Acoes) {
    Centro {
        when (p.desfecho) {
            Desfecho.ENVIADO -> {
                Texto("Pedido enviado ✓", 17.sp, VERDE, negrito = true)
                Texto("seus guardiões estão sendo avisados", 12.sp, Color.White)
            }
            Desfecho.TESTE -> {
                Texto("MODO TESTE", 16.sp, Color(0xFFFFD54F), negrito = true)
                Texto("nada foi enviado", 12.sp, Color.White)
            }
            Desfecho.SEM_CONTATOS -> {
                Texto("Cadastre guardiões no celular", 14.sp, Color(0xFFFFD54F), negrito = true)
                Texto("ninguém foi avisado", 12.sp, Color.White)
            }
            Desfecho.NAO_SAIU -> {
                Texto("O alerta NÃO saiu", 16.sp, Color(0xFFFF8A80), negrito = true)
                Texto("celular longe ou desligado", 12.sp, Color.White)
                Spacer(Modifier.height(8.dp))
                Botao("Tentar de novo", VERMELHO, altura = 44.dp, tamanho = 15.sp, aoTocar = acoes.tentarDeNovo)
            }
        }
        Spacer(Modifier.height(6.dp))
        Botao("OK", FUNDO_BOTAO, altura = 30.dp, tamanho = 12.sp, largura = 0.5f, aoTocar = acoes.fechar)
    }
}

/** Depois de cancelar: os dois SOS em botões grandes e os ajustes. */
@Composable
private fun Escolha(acoes: Acoes) {
    Lista {
        item { Texto("SOS cancelado", 12.sp, CINZA) }
        item { Botao("🛡️ SEGURANÇA", VERMELHO, altura = 54.dp, tamanho = 16.sp, aoTocar = { acoes.escolher(Perfil.SEGURANCA) }) }
        item { Botao("❤️ SAÚDE", AZUL, altura = 54.dp, tamanho = 16.sp, aoTocar = { acoes.escolher(Perfil.SAUDE) }) }
        item { Botao("Ajustes", FUNDO_BOTAO, altura = 36.dp, tamanho = 13.sp, largura = 0.7f, aoTocar = { acoes.mostrarAjustes(true) }) }
    }
}

@Composable
private fun TelaDeAjustes(a: Ajustes, acoes: Acoes) {
    Lista {
        item { Texto("Ajustes", 15.sp, Color.White, negrito = true) }
        item { Texto("Contagem", 12.sp, CINZA) }
        for (s in Ajustes.CONTAGENS) {
            item {
                Opcao(
                    texto = if (s == 0) "Na hora (0 s)" else "$s s",
                    marcada = a.contagem == s,
                    aoTocar = { acoes.mudarAjustes(a.copy(contagem = s)) },
                )
            }
        }
        item { Texto("Modo discreto", 12.sp, CINZA) }
        item {
            Opcao(
                texto = if (a.discreto) "Ligado: tela preta" else "Desligado: contagem na tela",
                marcada = a.discreto,
                aoTocar = { acoes.mudarAjustes(a.copy(discreto = !a.discreto)) },
            )
        }
        item { Texto("Tipo do SOS rápido", 12.sp, CINZA) }
        item {
            Opcao("🛡️ Segurança", a.perfilRapido == Perfil.SEGURANCA) { acoes.mudarAjustes(a.copy(perfilRapido = Perfil.SEGURANCA)) }
        }
        item {
            Opcao("❤️ Saúde", a.perfilRapido == Perfil.SAUDE) { acoes.mudarAjustes(a.copy(perfilRapido = Perfil.SAUDE)) }
        }
        item { Botao("Voltar", FUNDO_BOTAO, altura = 36.dp, tamanho = 13.sp, largura = 0.7f, aoTocar = { acoes.mostrarAjustes(false) }) }
    }
}

// ---------- peças ----------

@Composable
private fun Lista(conteudo: androidx.wear.compose.foundation.lazy.ScalingLazyListScope.() -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberScalingLazyListState(initialCenterItemIndex = 1),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = conteudo,
    )
}

@Composable
private fun Centro(aoTocar: (() -> Unit)? = null, conteudo: @Composable () -> Unit) {
    val base = Modifier.fillMaxSize()
    val mod = if (aoTocar != null) {
        base.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { aoTocar() }
    } else {
        base
    }
    Column(
        mod.padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { conteudo() }
}

@Composable
private fun Texto(texto: String, tamanho: TextUnit, cor: Color, negrito: Boolean = false) {
    Text(
        texto,
        color = cor,
        fontSize = tamanho,
        fontWeight = if (negrito) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Botão largo e arredondado (o botão redondo do Wear OS é pequeno demais para um SOS). */
@Composable
private fun Botao(
    texto: String,
    cor: Color,
    altura: Dp,
    tamanho: TextUnit,
    largura: Float = 1f,
    aoTocar: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth(largura)
            .height(altura)
            .clip(RoundedCornerShape(altura / 2))
            .background(cor)
            .clickable(onClick = aoTocar),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, color = Color.White, fontSize = tamanho, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** Uma opção dos ajustes: a marcada fica clara, com ✓. */
@Composable
private fun Opcao(texto: String, marcada: Boolean, aoTocar: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (marcada) Color(0xFF3D5A80) else FUNDO_BOTAO)
            .clickable(onClick = aoTocar)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            (if (marcada) "✓ " else "   ") + texto,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (marcada) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

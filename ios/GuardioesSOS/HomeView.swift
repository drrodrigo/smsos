import SwiftUI
import UIKit

/// Aba Início, igual à do Android, de cima para baixo: o cartão de status, o aviso do modo teste,
/// "Configurar passo a passo", o alerta ativo, os botões de SOS (segurar), a Configuração, o
/// "Testar sem enviar SMS", a "Demonstração e teste" e o Histórico.
struct InicioView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var camada: Camada
    let irPara: (Aba) -> Void

    @State private var configAberta: Bool
    @State private var testarAberto: Bool
    @State private var historicoAberto: Bool

    /// configInicial: a Configuração começa aberta só se faltar algo (como no Android).
    init(irPara: @escaping (Aba) -> Void, configInicial: Bool) {
        self.irPara = irPara
        _configAberta = State(initialValue: configInicial || Demo.abrir("configuracao"))
        _testarAberto = State(initialValue: Demo.abrir("testar"))
        _historicoAberto = State(initialValue: Demo.abrir("historico"))
    }

    var body: some View {
        ScrollViewReader { rolagem in
            ScrollView {
                VStack(spacing: 12) {
                    cartaoStatus
                    if app.emTeste { avisoModoTeste }
                    if !app.localOk || app.totalContatos == 0 {
                        BotaoM3("Configurar passo a passo", tipo: .cheio, largo: true, alto: 48, fonte: 16) {
                            withAnimation(.easeInOut(duration: 0.2)) { configAberta = true }
                            Task { @MainActor in
                                try? await Task.sleep(nanoseconds: 250_000_000)
                                withAnimation { rolagem.scrollTo("configuracao", anchor: .top) }
                            }
                        }
                    }
                    if let ativo = app.ativo { cartaoAtivo(ativo) }
                    AreaSOS(acionar: acionar, semGuardioes: semGuardioes)
                    cartaoConfiguracao.id("configuracao")
                    cartaoTestar
                    cartaoDemonstracao
                    cartaoHistorico
                    Rodape().id("fim")
                }
                .padding(16)
            }
            .task {
                if !Demo.ativo && app.local.naoPerguntou { app.local.pedirPermissao() }
                if Demo.rolar {
                    try? await Task.sleep(nanoseconds: 500_000_000)
                    rolagem.scrollTo("fim", anchor: .bottom)
                }
            }
        }
    }

    // MARK: - Status

    private var estado: (cor: Color, titulo: String, texto: String, ok: Bool) {
        if !app.localOk {
            return (Cor.error, "Proteção DESLIGADA", "Termine a configuração logo abaixo.", false)
        }
        if app.totalContatos == 0 {
            return (Cor.warning, "Falta cadastrar contatos", "Cadastre quem deve ser avisado.", false)
        }
        if app.emTeste {
            return (Cor.warning, "MODO TESTE", "O botão funciona, mas nenhum SMS é enviado.", false)
        }
        let texto = app.dualSos
            ? "Toque e segure o botão Segurança ou Saúde por \(app.segundos)s para pedir ajuda."
            : "Toque e segure o botão por \(app.segundos)s para pedir ajuda."
        return (Cor.ok, "Proteção ATIVA", texto, true)
    }

    private var cartaoStatus: some View {
        let e = estado
        return HStack(spacing: 12) {
            Image(systemName: e.ok ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                .font(.system(size: 34))
                .foregroundStyle(e.cor)
                .frame(width: 40, height: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(e.titulo)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(e.cor)
                Text(e.texto)
                    .font(.system(size: 14))
                    .foregroundStyle(Cor.onSurface)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(e.cor.opacity(0.14), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    // MARK: - Modo teste

    private var avisoModoTeste: some View {
        VStack(alignment: .leading, spacing: 8) {
            Toggle(isOn: Binding(get: { true }, set: { novo in if !novo { desligarTeste() } })) {
                HStack(spacing: 10) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(Cor.warning)
                    Text("Modo teste LIGADO")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Cor.warning)
                }
            }
            .toggleStyle(ChaveM3())
            Text("Os botões funcionam, mas o alerta NÃO é enviado — ninguém é avisado. Desligue aqui para valer de verdade.")
                .font(.system(size: 14))
                .foregroundStyle(Cor.onSurface)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Cor.warning.opacity(0.22), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var chaveDoTeste: Binding<Bool> {
        Binding(
            get: { app.emTeste },
            set: { novo in
                if novo {
                    app.modoTeste = true
                    camada.toast("Modo teste ligado: nada é enviado")
                } else {
                    desligarTeste()
                }
            }
        )
    }

    private func desligarTeste() {
        if !Gateway.configurado {
            camada.toast("Esta versão do app não envia SMS: o modo teste fica sempre ligado.")
            return
        }
        app.modoTeste = false
        camada.toast("Modo teste desligado")
    }

    // MARK: - Alerta que acabou de sair

    private func cartaoAtivo(_ ativo: AlertaAtivo) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Alerta enviado às \(Mensagem.soHora(ativo.em))")
                .font(.system(size: 16, weight: .bold))
            Text("\(ativo.telefones.count) contato(s) avisado(s).")
                .font(.system(size: 14))
            BotaoM3("Estou bem — avisar todos", tipo: .cheio, largo: true) { confirmarTudoBem(ativo) }
            BotaoM3("Encerrar sem mandar mensagem", tipo: .texto, largo: true) { app.ativo = nil }
        }
        .foregroundStyle(Cor.onPrimaryContainer)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Cor.primaryContainer, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func confirmarTudoBem(_ ativo: AlertaAtivo) {
        camada.abrir {
            CaixaDialogo("Avisar que está tudo bem?", acoes: [
                AcaoDialogo("Cancelar"),
                AcaoDialogo("Enviar") {
                    Task { await Alerta.avisarTudoBem(app: app, camada: camada) }
                },
            ]) {
                Text("Os \(ativo.telefones.count) contato(s) que receberam o alerta vão receber um SMS dizendo que está tudo bem.")
                    .font(.system(size: 14))
                    .foregroundStyle(Cor.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    // MARK: - SOS

    private func acionar(_ perfil: Perfil) {
        Task { await Alerta.disparar(perfil, app: app, camada: camada, irPara: irPara) }
    }

    private func semGuardioes(_ perfil: Perfil) {
        Demonstracao.semGuardioes(perfil, app: app, camada: camada, irPara: irPara)
    }

    // MARK: - Configuração

    private var cartaoConfiguracao: some View {
        let pend = app.pendentes
        return CartaoRecolhivel(
            "Configuração",
            resumo: pend == 0 ? "Tudo pronto ✓" : "Falta \(pend) ajuste(s) — toque para abrir",
            aberto: $configAberta
        ) {
            ItemConfig(
                ok: app.nomeOk,
                titulo: "Seu nome",
                descricao: "Aparece na mensagem, para as pessoas saberem quem pediu ajuda.",
                botao: "Preencher",
                ajuda: "É o nome que aparece na mensagem de alerta, para quem recebe saber quem pediu ajuda. Toque em \"Preencher\" (ou vá em Ajustes) e escreva o seu nome."
            ) { irPara(.ajustes) }
            ItemConfig(
                ok: app.totalContatos > 0,
                titulo: "Guardiões",
                descricao: "Quem recebe o alerta: seus guardiões e as pessoas de cada lugar.",
                botao: "Cadastrar",
                ajuda: "São as pessoas que recebem o seu alerta — por exemplo seu filho ou seu cônjuge. Toque em \"Cadastrar\" e adicione pelo número, ou use \"Convidar um guardião novo\" na aba Guardiões para enviar o convite pelo WhatsApp."
            ) { irPara(.guardioes) }
            ItemConfig(
                ok: app.localOk,
                titulo: "Localização",
                descricao: "Para mandar o link do mapa e saber em qual lugar você está.",
                botao: "Permitir",
                ajuda: "Deixa o app mandar o link do mapa com onde você está no momento do alerta. Toque em \"Permitir\" e escolha \"Permitir Durante o Uso do App\". Se você já negou: Ajustes do iPhone > GuardiõesSOS > Localização."
            ) { pedirLocalizacao() }
            ItemConfig(
                ok: app.telefoneOk,
                titulo: "Seu WhatsApp",
                descricao: "Sua conta na rede de guardiões: é por ele que os convites chegam até você.",
                botao: "Preencher",
                ajuda: "É a sua conta no GuardiõesSOS, sem senha. É por ele que chegam os convites de quem quer você como guardião. Toque em \"Preencher\" (ou vá em Ajustes) e escreva seu celular com DDD."
            ) { irPara(.ajustes) }
        }
    }

    private func pedirLocalizacao() {
        if app.local.naoPerguntou {
            app.local.pedirPermissao()
        } else if app.local.negado {
            camada.dialogo(
                "Localização",
                "A localização do GuardiõesSOS está desligada. Toque em \"Abrir Ajustes\", depois em \"Localização\", e escolha \"Durante o Uso do App\".",
                acoes: [
                    AcaoDialogo("Agora não"),
                    AcaoDialogo("Abrir Ajustes") { Tela.abrirAjustesDoIPhone() },
                ]
            )
        } else {
            app.atualizarLocal()
        }
    }

    // MARK: - Testar sem enviar SMS

    private var cartaoTestar: some View {
        CartaoRecolhivel(
            "Testar sem enviar SMS",
            resumo: "Treinar o gatilho sem avisar ninguém — toque para abrir",
            aberto: $testarAberto
        ) {
            LinhaComChave("Modo teste", ligado: chaveDoTeste)
            TextoAjuda("No modo teste o app faz tudo (pega a localização e escolhe os contatos), mas NÃO manda SMS. Ao completar os segundos ele VIBRA e mostra quem receberia.")
            TextoAjuda("Teste agora: com o modo teste ligado, segure o botão \(app.dualSos ? "Segurança ou Saúde" : "da tela Início") por \(app.segundos) segundos.")
        }
    }

    // MARK: - Demonstração e teste

    private var cartaoDemonstracao: some View {
        Cartao("Demonstração e teste") {
            TextoAjuda("Pra mostrar como funciona pra alguém, ou conferir no seu aparelho. Nada é enviado — ninguém é avisado.")
            BotaoM3("Ver como a mensagem chega", tipo: .contorno, largo: true) {
                Demonstracao.comoChega(app: app, camada: camada)
            }
            BotaoM3("Testar a sirene e a tela do guardião", tipo: .contorno, largo: true) {
                Demonstracao.telaDoGuardiao(app: app, camada: camada)
            }
        }
    }

    // MARK: - Histórico

    private var cartaoHistorico: some View {
        CartaoRecolhivel(
            "Histórico",
            resumo: app.historico.isEmpty ? "Nenhum alerta ainda" : "\(app.historico.count) registro(s) — toque para abrir",
            aberto: $historicoAberto
        ) {
            if app.historico.isEmpty {
                TextoAjuda("Nenhum alerta ainda.")
            } else {
                ForEach(Array(app.historico.prefix(10).enumerated()), id: \.element.id) { item in
                    if item.offset > 0 { Divisor() }
                    ItemHistorico(registro: item.element) {
                        app.historico.removeAll { $0.id == item.element.id }
                    }
                }
                Divisor()
                BotaoM3("Apagar o histórico deste aparelho", tipo: .texto, largo: true) { confirmarApagarHistorico() }
                TextoAjuda(textoDaRetencao)
            }
        }
    }

    private var textoDaRetencao: String {
        let quando = app.retencao > 0
            ? "O histórico some sozinho depois de \(app.retencao) dias (você muda em Ajustes)."
            : "O histórico fica guardado até você apagar (você liga o sumiço automático em Ajustes)."
        return "Toque na lixeira ao lado de um alerta para apagar só ele. \(quando) Apagar aqui não desfaz — útil se tomarem seu telefone."
    }

    private func confirmarApagarHistorico() {
        camada.dialogo(
            "Apagar o histórico?",
            "Isso apaga a lista de alertas deste celular e não dá para desfazer.",
            acoes: [
                AcaoDialogo("Cancelar"),
                AcaoDialogo("Apagar") { app.historico = [] },
            ]
        )
    }
}

// MARK: - Um item da Configuração

/// Título com ✓ verde ou ⚠ laranja e o "?" de ajuda; embaixo a explicação e o botão (SetupRow).
struct ItemConfig: View {
    @EnvironmentObject var camada: Camada
    let ok: Bool
    let titulo: String
    let descricao: String
    let botao: String
    let ajuda: String
    let acao: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 12) {
                Image(systemName: ok ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                    .font(.system(size: 19))
                    .foregroundStyle(ok ? Cor.ok : Cor.warning)
                    .frame(width: 24, height: 24)
                    .accessibilityLabel(ok ? "Pronto" : "Pendente")
                Text(titulo)
                    .font(.system(size: 16))
                    .foregroundStyle(Cor.onSurface)
                Spacer(minLength: 0)
                Button {
                    camada.dialogo(titulo, ajuda)
                } label: {
                    Text("?")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(Cor.primary)
                        .frame(width: 32, height: 32)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("O que é: \(titulo)")
            }
            Text(descricao)
                .font(.system(size: 14))
                .foregroundStyle(Cor.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.leading, 36)
            if !ok {
                BotaoM3(botao, tipo: .tonal, acao: acao)
                    .padding(.leading, 36)
            }
        }
    }
}

// MARK: - Um item do Histórico

struct ItemHistorico: View {
    let registro: RegistroAlerta
    let apagar: () -> Void
    @Environment(\.openURL) private var openURL

    private var resumo: String {
        if registro.teste { return "\(registro.total) contato(s) receberiam o alerta" }
        var s = "Enviado: \(registro.enviados) de \(registro.total)"
        let falhou = registro.total - registro.enviados
        if falhou > 0 { s += " · falhou: \(falhou)" }
        return s
    }

    private var mapa: URL? {
        guard let lat = registro.latitude, let lng = registro.longitude else { return nil }
        return URL(string: Mensagem.linkMapa(lat, lng))
    }

    var body: some View {
        HStack(alignment: .top, spacing: 4) {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(Mensagem.dataHora(registro.em)) · \(registro.teste ? "TESTE" : "ALERTA")")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(registro.teste ? Cor.warning : Cor.primary)
                TextoAjuda("SOS de \(registro.perfil.titulo) · pelo botão da tela")
                if let lugar = registro.lugar {
                    TextoAjuda("Lugar: \(lugar)")
                }
                TextoAjuda(resumo)
                if let mapa {
                    BotaoM3("Ver no mapa", tipo: .texto) { openURL(mapa) }
                        .padding(.leading, -12)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            BotaoIcone("trash.fill", rotulo: "Apagar este alerta", acao: apagar)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Os botões de SOS

/// Os dois botões lado a lado (Segurança e Saúde) ou um só (SEGURE AQUI), como no Android.
struct AreaSOS: View {
    @EnvironmentObject var app: AppState
    let acionar: (Perfil) -> Void
    let semGuardioes: (Perfil) -> Void

    private var pronto: Bool { app.totalContatos > 0 }

    private func subtitulo(curto: Bool) -> String {
        if !pronto { return "toque para configurar" }
        if app.emTeste { return "modo teste · não envia" }
        return curto ? "segure \(app.segundos) s" : "por \(app.segundos) segundos para enviar o alerta agora"
    }

    private var corSeguranca: Color { app.emTeste ? Cor.segurancaTeste : Cor.seguranca }
    private var corSaude: Color { app.emTeste ? Cor.saudeTeste : Cor.saude }

    var body: some View {
        if app.dualSos {
            HStack(spacing: 12) {
                BotaoSegurar(
                    titulo: "🛡️ SEGURANÇA",
                    subtitulo: subtitulo(curto: true),
                    cor: corSeguranca,
                    segundos: app.segundos,
                    armado: pronto,
                    aoCompletar: { acionar(.seguranca) },
                    aoTocar: { semGuardioes(.seguranca) }
                )
                BotaoSegurar(
                    titulo: "❤️ SAÚDE",
                    subtitulo: subtitulo(curto: true),
                    cor: corSaude,
                    segundos: app.segundos,
                    armado: pronto,
                    aoCompletar: { acionar(.saude) },
                    aoTocar: { semGuardioes(.saude) }
                )
            }
            .fixedSize(horizontal: false, vertical: true)
        } else {
            BotaoSegurar(
                titulo: "SEGURE AQUI",
                subtitulo: subtitulo(curto: false),
                cor: app.perfil == .saude ? corSaude : corSeguranca,
                segundos: app.segundos,
                armado: pronto,
                aoCompletar: { acionar(app.perfil) },
                aoTocar: { semGuardioes(app.perfil) }
            )
        }
    }
}

/// Botão de emergência. Armado: segure pelo tempo escolhido — a barra branca enche; soltar antes
/// zera; completou, dispara uma vez e vibra. Sem guardiões: um toque leva à configuração.
struct BotaoSegurar: View {
    let titulo: String
    let subtitulo: String
    let cor: Color
    let segundos: Int
    let armado: Bool
    let aoCompletar: () -> Void
    let aoTocar: () -> Void

    @State private var progresso: CGFloat = Demo.segurando ? 0.6 : 0
    @State private var segurando = Demo.segurando

    var body: some View {
        conteudo
            .modifier(GestoDoSOS(
                armado: armado,
                segundos: segundos,
                aoMudar: mudou,
                aoCompletar: completou,
                aoTocar: aoTocar
            ))
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isButton)
            .accessibilityHint(armado ? "Segure por \(segundos) segundos para pedir ajuda." : "Toque para configurar.")
            .accessibilityAction { armado ? aoCompletar() : aoTocar() }
    }

    private var conteudo: some View {
        VStack(spacing: 4) {
            Text(titulo)
                .font(.system(size: 22, weight: .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(subtitulo)
                .font(.system(size: 14))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(.white)
        .padding(.vertical, 20)
        .padding(.horizontal, 12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(cor, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(alignment: .bottom) {
            // A barra branca que enche enquanto segura (o LinearProgressIndicator do Android).
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.3))
                    Capsule().fill(Color.white).frame(width: g.size.width * progresso)
                }
            }
            .frame(height: 4)
            .padding(.horizontal, 12)
            .padding(.bottom, 9)
            .opacity(segurando ? 1 : 0)
        }
        .brightness(segurando ? 0.04 : 0)
        .scaleEffect(segurando ? 0.985 : 1)
        .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func mudou(_ apertando: Bool) {
        if apertando {
            withAnimation(.easeOut(duration: 0.1)) { segurando = true }
            progresso = 0
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            withAnimation(.linear(duration: Double(segundos))) { progresso = 1 }
        } else {
            withAnimation(.easeOut(duration: 0.15)) {
                segurando = false
                progresso = 0
            }
        }
    }

    private func completou() {
        segurando = false
        progresso = 0
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
        aoCompletar()
    }
}

/// O gesto do botão: segurar (armado) ou tocar (sem guardiões).
struct GestoDoSOS: ViewModifier {
    let armado: Bool
    let segundos: Int
    let aoMudar: (Bool) -> Void
    let aoCompletar: () -> Void
    let aoTocar: () -> Void

    func body(content: Content) -> some View {
        if armado {
            content.onLongPressGesture(
                minimumDuration: Double(segundos),
                maximumDistance: 80,
                perform: aoCompletar,
                onPressingChanged: aoMudar
            )
        } else {
            content.onTapGesture(perform: aoTocar)
        }
    }
}

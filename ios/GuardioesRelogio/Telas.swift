import SwiftUI

/// As cores do app (as mesmas dos botões do iPhone e do Android).
enum CorDoRelogio {
    static let seguranca = Color(red: 0xD3 / 255, green: 0x2F / 255, blue: 0x2F / 255)   // #D32F2F
    static let saude = Color(red: 0x1E / 255, green: 0x66 / 255, blue: 0xD0 / 255)       // #1E66D0
    static let ok = Color(red: 0x81 / 255, green: 0xC7 / 255, blue: 0x84 / 255)          // verde do tema escuro
    static let aviso = Color(red: 0xFF / 255, green: 0xB7 / 255, blue: 0x4D / 255)       // laranja do tema escuro
    /// O "toque para cancelar" do modo discreto: cinza-escuro, quase sumindo no preto.
    static let discreto = Color(white: 0.30)
    /// O "Não saiu" do modo discreto: um pouco mais claro, para dar para ler de perto.
    static let discretoLegivel = Color(white: 0.50)

    static func de(_ perfil: Perfil) -> Color { perfil == .saude ? saude : seguranca }
}

// MARK: - Raiz

/// A tela do SOS (contando, enviando, resultado) ou o menu. Abrir o app = começar a contar:
/// ao voltar de FORA do app (ícone, complicação, Botão de Ação). Baixar e levantar o braço não conta.
struct RaizView: View {
    @EnvironmentObject var estado: EstadoDoRelogio
    @Environment(\.scenePhase) private var faseDaCena
    /// true até a primeira abertura e sempre que o app vai para o segundo plano (saiu do app).
    @State private var veioDeFora = true

    var body: some View {
        Group {
            switch estado.fase {
            case .menu:
                NavigationStack { MenuView() }
            default:
                TelaDoSOS()
            }
        }
        .onAppear {
            // Aberto pela Siri sem tela, o app nasce em segundo plano: aí NÃO começa a contar.
            if faseDaCena == .active { abriu() }
        }
        .onChange(of: faseDaCena) { nova in
            switch nova {
            case .active:
                if veioDeFora { abriu() }
            case .background:
                veioDeFora = true
            default:
                break
            }
        }
        .onOpenURL { _ in abriu() }
    }

    private func abriu() {
        veioDeFora = false
        estado.aoAbrir()
    }
}

// MARK: - A tela do SOS

/// Contando, enviando e o resultado. No modo discreto é PRETA (parece apagada): sem número, sem
/// "SOS", sem vermelho; o resultado vem pela vibração. Tocar em qualquer lugar cancela a contagem.
struct TelaDoSOS: View {
    @EnvironmentObject var estado: EstadoDoRelogio
    /// Braço baixado (tela "sempre ligada" esmaecida): nem o texto pequeno aparece.
    @Environment(\.isLuminanceReduced) private var telaEsmaecida

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if estado.discreto {
                discreta
            } else {
                visivel
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { tocou() }
        .persistentSystemOverlays(.hidden)
        .accessibilityAction(named: "Cancelar") { tocou() }
    }

    private func tocou() {
        switch estado.fase {
        case .contando:
            estado.cancelar()
        case .saiu:
            estado.irParaOMenu()
        default:
            break
        }
    }

    // MARK: Discreta

    @ViewBuilder
    private var discreta: some View {
        switch estado.fase {
        case .contando:
            if !telaEsmaecida { rodape("toque para cancelar") }
        case let .saiu(_, r):
            if r.teste && !telaEsmaecida { rodape("teste · nada foi enviado") }
        case let .naoSaiu(_, r):
            naoSaiuDiscreto(r)
        case .enviando, .menu:
            EmptyView()
        }
    }

    private func rodape(_ texto: String) -> some View {
        VStack {
            Spacer()
            Text(texto)
                .font(.system(size: 12))
                .foregroundColor(CorDoRelogio.discreto)
                .padding(.bottom, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func naoSaiuDiscreto(_ r: Disparo.Resultado) -> some View {
        VStack(spacing: 6) {
            Spacer(minLength: 0)
            Text("Não saiu")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(CorDoRelogio.discretoLegivel)
            if let motivo = r.motivo {
                Text(motivo)
                    .font(.system(size: 11))
                    .foregroundColor(CorDoRelogio.discreto)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Button {
                estado.tentarDeNovo()
            } label: {
                Text("Tentar de novo")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(white: 0.62))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .background(Capsule().fill(Color(white: 0.13)))
            }
            .buttonStyle(.plain)
            Button {
                estado.irParaOMenu()
            } label: {
                Text("Voltar")
                    .font(.system(size: 12))
                    .foregroundColor(CorDoRelogio.discreto)
                    .padding(.vertical, 4)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 10)
    }

    // MARK: Visível (modo discreto desligado)

    @ViewBuilder
    private var visivel: some View {
        switch estado.fase {
        case let .contando(perfil, restante):
            contandoVisivel(perfil, restante)
        case let .enviando(perfil):
            VStack(spacing: 6) {
                ProgressView()
                    .tint(.white)
                    .frame(height: 44)
                Text("Enviando o SOS…")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                Text(perfil.rotulo)
                    .font(.system(size: 13))
                    .foregroundColor(CorDoRelogio.de(perfil))
            }
        case let .saiu(_, r):
            ScrollView { saiuVisivel(r) }
        case let .naoSaiu(_, r):
            ScrollView { naoSaiuVisivel(r) }
        case .menu:
            EmptyView()
        }
    }

    private func contandoVisivel(_ perfil: Perfil, _ restante: Int) -> some View {
        VStack(spacing: 0) {
            Text(perfil == .saude ? "SOS SAÚDE em" : "SOS SEGURANÇA em")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(.white.opacity(0.85))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text("\(restante)")
                .font(.system(size: 68, weight: .heavy, design: .rounded))
                .foregroundColor(.white)
                .monospacedDigit()
                .frame(maxHeight: .infinity)
            Button {
                estado.cancelar()
            } label: {
                Text("CANCELAR")
                    .font(.system(size: 17, weight: .heavy))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
                    .background(Capsule().fill(CorDoRelogio.seguranca))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Cancelar o SOS")
        }
        .padding(.horizontal, 6)
    }

    private func saiuVisivel(_ r: Disparo.Resultado) -> some View {
        VStack(spacing: 5) {
            Image(systemName: r.teste ? "checkmark.circle" : "checkmark.circle.fill")
                .font(.system(size: 30))
                .foregroundColor(r.teste ? CorDoRelogio.aviso : CorDoRelogio.ok)
            Text(r.teste ? "Teste: funcionou" : "Alerta enviado")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)
            Text(r.teste
                 ? "Nada foi enviado. \(r.contatos) contato(s) receberiam."
                 : "\(r.contatos) contato(s) avisado(s) com a sua localização.")
                .font(.system(size: 13))
                .foregroundColor(.white.opacity(0.85))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Text(r.caminho == .iphone ? "pelo iPhone" : "pela internet do relógio")
                .font(.system(size: 11))
                .foregroundColor(.gray)
            BotaoPequeno(titulo: "OK", cor: Color(white: 0.22)) { estado.irParaOMenu() }
                .padding(.top, 2)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 6)
    }

    private func naoSaiuVisivel(_ r: Disparo.Resultado) -> some View {
        VStack(spacing: 6) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 24))
                .foregroundColor(CorDoRelogio.aviso)
            Text("Não saiu")
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(.white)
            if let motivo = r.motivo {
                Text(motivo)
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.85))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            BotaoPequeno(titulo: "Tentar de novo", cor: CorDoRelogio.seguranca) { estado.tentarDeNovo() }
            Button {
                estado.irParaOMenu()
            } label: {
                Text("Voltar")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
                    .padding(.vertical, 4)
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 6)
    }
}

// MARK: - Menu (depois de cancelar)

/// Os dois SOS grandes, a linha de status e os ajustes. Tocar num SOS começa a contagem dele.
struct MenuView: View {
    @EnvironmentObject var estado: EstadoDoRelogio
    @State private var abrirAjustes = Demo.ativo && Demo.tela == "ajustes"

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                BotaoGrande(titulo: "🛡️ SEGURANÇA", cor: CorDoRelogio.seguranca) {
                    estado.comecar(.seguranca)
                }
                .accessibilityLabel("SOS de Segurança")
                BotaoGrande(titulo: "❤️ SAÚDE", cor: CorDoRelogio.saude) {
                    estado.comecar(.saude)
                }
                .accessibilityLabel("SOS de Saúde")
                LinhaDeStatus()
                NavigationLink(destination: AjustesDoRelogioView()) {
                    Label("Ajustes", systemImage: "gearshape.fill")
                        .font(.system(size: 15))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .navigationTitle("Guardiões")
        .navigationDestination(isPresented: $abrirAjustes) { AjustesDoRelogioView() }
        .task { estado.pedirLocalizacaoSeNuncaPediu() }
    }
}

struct BotaoGrande: View {
    let titulo: String
    let cor: Color
    let acao: () -> Void

    var body: some View {
        Button(action: acao) {
            Text(titulo)
                .font(.system(size: 17, weight: .heavy))
                .foregroundColor(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .padding(.horizontal, 6)
                .frame(maxWidth: .infinity, minHeight: 52)
                .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(cor))
        }
        .buttonStyle(.plain)
    }
}

/// Um botão em pílula, largo e baixo (cabe junto do texto na tela pequena).
struct BotaoPequeno: View {
    let titulo: String
    let cor: Color
    let acao: () -> Void

    var body: some View {
        Button(action: acao) {
            Text(titulo)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 9)
                .background(Capsule().fill(cor))
        }
        .buttonStyle(.plain)
    }
}

/// O que a pessoa precisa saber em uma linha: pronto, modo teste, falta guardião ou o último SOS.
struct LinhaDeStatus: View {
    @EnvironmentObject var estado: EstadoDoRelogio

    var body: some View {
        let s = status
        Text(s.texto)
            .font(.system(size: 12))
            .foregroundColor(s.cor)
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 2)
    }

    private var status: (texto: String, cor: Color) {
        guard let ctx = estado.contexto else {
            return ("Abra o GuardiõesSOS no iPhone para trazer seus guardiões.", CorDoRelogio.aviso)
        }
        if ctx.emTeste {
            return ("Modo teste no iPhone: nada é enviado.", CorDoRelogio.aviso)
        }
        if ctx.totalContatos == 0 {
            return ("Sem guardiões: cadastre no iPhone.", CorDoRelogio.aviso)
        }
        if let u = estado.ultimo, u.em > Date().addingTimeInterval(-12 * 3600) {
            let hora = Mensagem.soHora(u.em)
            if u.ok && !u.teste {
                return ("Último SOS às \(hora): \(u.contatos) avisado(s) ✓", CorDoRelogio.ok)
            }
            if !u.ok {
                return ("O SOS das \(hora) não saiu.", CorDoRelogio.aviso)
            }
        }
        return ("Pronto: \(ctx.totalContatos) contato(s) recebem o SOS.", CorDoRelogio.ok)
    }
}

// MARK: - Ajustes

/// Contagem (0, 3 ou 5 s), modo discreto e o tipo do SOS rápido. Fica guardado no relógio.
struct AjustesDoRelogioView: View {
    @EnvironmentObject var estado: EstadoDoRelogio

    var body: some View {
        ScrollViewReader { rolagem in
            List {
                Section(header: Text("Contagem"),
                        footer: Text("Depois da contagem o SOS sai sozinho. Tocar na tela cancela.")) {
                    ForEach(Guardado.opcoesDeContagem, id: \.self) { s in
                        LinhaDeEscolha(texto: s == 0 ? "Na hora (0 s)" : "\(s) s", escolhida: estado.contagem == s) {
                            estado.contagem = s
                        }
                    }
                }
                .id("contagem")
                Section(header: Text("Modo discreto"),
                        footer: Text(estado.discreto
                                     ? "Tela preta, sem número e sem “SOS”: parece apagada. Você sente pela vibração."
                                     : "Tela acesa, com o número grande e o CANCELAR vermelho.")) {
                    Toggle(estado.discreto ? "Ligado" : "Desligado", isOn: $estado.discreto)
                }
                .id("discreto")
                Section(header: Text("Tipo do SOS rápido"),
                        footer: Text("O que dispara ao abrir o app, pela complicação, pela Siri e pelo Botão de Ação.")) {
                    LinhaDeEscolha(texto: "🛡️ Segurança", escolhida: estado.tipoRapido == .seguranca) {
                        estado.tipoRapido = .seguranca
                    }
                    LinhaDeEscolha(texto: "❤️ Saúde", escolhida: estado.tipoRapido == .saude) {
                        estado.tipoRapido = .saude
                    }
                }
                .id("tipo")
                Section(header: Text("Do iPhone"),
                        footer: Text("Para mudar o nome e os guardiões, use o app do iPhone.")) {
                    if let ctx = estado.contexto {
                        LinhaDeInfo(rotulo: "Nome", valor: Mensagem.nomeOuPadrao(ctx.nome))
                        LinhaDeInfo(rotulo: "Contatos", valor: "\(ctx.totalContatos)")
                        LinhaDeInfo(rotulo: "Modo teste", valor: ctx.emTeste ? "ligado" : "desligado")
                        if let em = estado.contextoEm {
                            LinhaDeInfo(rotulo: "Atualizado", valor: Mensagem.dataHora(em))
                        }
                    } else {
                        Text("Ainda não veio nada. Abra o GuardiõesSOS no iPhone.")
                            .font(.system(size: 13))
                            .foregroundColor(CorDoRelogio.aviso)
                    }
                }
                .id("fim")
            }
            .task {
                if let alvo = Demo.ate ?? (Demo.rolar ? "fim" : nil) {
                    try? await Task.sleep(nanoseconds: 600_000_000)
                    rolagem.scrollTo(alvo, anchor: alvo == "fim" ? .bottom : .top)
                }
            }
        }
        .navigationTitle("Ajustes")
    }
}

struct LinhaDeEscolha: View {
    let texto: String
    let escolhida: Bool
    let acao: () -> Void

    var body: some View {
        Button(action: acao) {
            HStack {
                Text(texto)
                    .font(.system(size: 15))
                Spacer(minLength: 4)
                if escolhida {
                    Image(systemName: "checkmark")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(CorDoRelogio.ok)
                }
            }
        }
        .accessibilityAddTraits(escolhida ? .isSelected : [])
    }
}

struct LinhaDeInfo: View {
    let rotulo: String
    let valor: String

    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(rotulo)
                .font(.system(size: 11))
                .foregroundColor(.gray)
            Text(valor)
                .font(.system(size: 14))
                .lineLimit(2)
        }
    }
}

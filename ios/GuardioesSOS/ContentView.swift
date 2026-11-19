import SwiftUI

/// As 5 abas do app, iguais às do Android.
enum Aba: Hashable, CaseIterable {
    case inicio, guardioes, lugares, protejo, ajustes

    var titulo: String {
        switch self {
        case .inicio: return "Início"
        case .guardioes: return "Guardiões"
        case .lugares: return "Lugares"
        case .protejo: return "Protejo"
        case .ajustes: return "Ajustes"
        }
    }

    var icone: String {
        switch self {
        case .inicio: return "house.fill"
        case .guardioes: return "person.fill"
        case .lugares: return "mappin.and.ellipse"
        case .protejo: return "heart.fill"
        case .ajustes: return "gearshape.fill"
        }
    }
}

/// Primeira vez → cadastro. Depois → a barra de cima (com a setinha de voltar), a tela da aba e a
/// barra de baixo com as 5 abas. Por cima de tudo, a Camada (diálogos, avisos, tela do guardião).
struct ContentView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var camada: Camada
    @Environment(\.scenePhase) private var fase
    @State private var aba: Aba = Demo.abaInicial
    /// O lugar aberto no editor (aba Lugares). nil = a lista.
    @State private var lugarAberto: String? = nil
    @State private var demoMostrado = false

    var body: some View {
        ZStack {
            Cor.fundo.ignoresSafeArea()
            if app.configurado {
                principal
            } else {
                CadastroView()
            }
            CamadaView()
        }
        .tecladoFechavel()
        .onChange(of: fase) { nova in
            if nova == .active {
                app.atualizarLocal()
                app.limparHistoricoVelho()
            }
        }
        .task {
            if app.configurado { app.registrarLead() }
            mostrarDemo()
        }
    }

    private var principal: some View {
        VStack(spacing: 0) {
            BarraDeCima(podeVoltar: podeVoltar, voltar: voltar)
            Group {
                switch aba {
                case .inicio:
                    InicioView(irPara: irPara, configInicial: app.pendentes > 0)
                case .guardioes:
                    GuardioesView()
                case .lugares:
                    LugaresView(aberto: $lugarAberto)
                case .protejo:
                    ProtejoView()
                case .ajustes:
                    AjustesView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            BarraDeBaixo(aba: Binding(get: { aba }, set: { irPara($0) }))
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
    }

    private var podeVoltar: Bool {
        lugarAberto != nil || aba != .inicio
    }

    /// A setinha de voltar: fecha o editor do lugar; numa aba, volta para o Início.
    private func voltar() {
        esconderTeclado()
        if camada.folha != nil {
            camada.fechar()
        } else if lugarAberto != nil {
            lugarAberto = nil
        } else if aba != .inicio {
            aba = .inicio
        }
    }

    private func irPara(_ nova: Aba) {
        esconderTeclado()
        if nova != .lugares { lugarAberto = nil }
        aba = nova
    }

    /// No modo demonstração, abre o diálogo pedido por -mostrar (para os prints).
    private func mostrarDemo() {
        guard Demo.ativo, !demoMostrado, let pedido = Demo.mostrar else { return }
        demoMostrado = true
        switch pedido {
        case "como-chega":
            Demonstracao.comoChega(app: app, camada: camada)
        case "sirene":
            Demonstracao.telaDoGuardiao(app: app, camada: camada)
        case "sem-guardioes":
            Demonstracao.semGuardioes(.saude, app: app, camada: camada, irPara: irPara)
        case "novo-guardiao":
            aba = .guardioes
            camada.abrir {
                DialogoContato(titulo: "Novo guardião", confirmar: "Adicionar", nome: "Pedro", telefone: "31 8888-7777") { _, _ in true }
            }
        case "lugar":
            aba = .lugares
            lugarAberto = app.lugares.first?.id
        default:
            break
        }
    }
}

// MARK: - Barra de cima

/// A barra de cima do Android: a setinha de voltar à esquerda (regra de todo app), o escudo e o nome
/// no centro, e à direita compartilhar e o site.
struct BarraDeCima: View {
    let podeVoltar: Bool
    let voltar: () -> Void
    @Environment(\.openURL) private var openURL

    private let site = URL(string: "https://guardioessos.com.br")
    private let convite = "Baixe o GuardiõesSOS — um toque avisa seus guardiões com a sua localização. https://guardioessos.com.br"

    var body: some View {
        ZStack {
            HStack(spacing: 8) {
                Image("Escudo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 30, height: 30)
                Text("GuardiõesSOS")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Cor.onSurface)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .padding(.horizontal, 96)
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)

            HStack(spacing: 0) {
                Button(action: voltar) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(Cor.onSurface)
                        .frame(width: 48, height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .disabled(!podeVoltar)
                .opacity(podeVoltar ? 1 : 0.38)
                .accessibilityLabel("Voltar")

                Spacer()

                ShareLink(item: convite) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 19))
                        .foregroundStyle(Cor.onSurfaceVariant)
                        .frame(width: 44, height: 48)
                        .contentShape(Rectangle())
                }
                .accessibilityLabel("Compartilhar o app")

                Button {
                    if let site { openURL(site) }
                } label: {
                    Image(systemName: "globe")
                        .font(.system(size: 19))
                        .foregroundStyle(Cor.onSurfaceVariant)
                        .frame(width: 44, height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Abrir o site")
            }
        }
        .padding(.horizontal, 4)
        .frame(height: 64)
        .background(Cor.fundo)
    }
}

// MARK: - Barra de baixo

/// A barra de abas do Material 3: o ícone dentro de uma pílula quando a aba está escolhida.
struct BarraDeBaixo: View {
    @Binding var aba: Aba

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Aba.allCases, id: \.self) { a in
                let escolhida = a == aba
                Button {
                    aba = a
                } label: {
                    VStack(spacing: 4) {
                        ZStack {
                            Capsule()
                                .fill(escolhida ? Cor.secondaryContainer : Color.clear)
                                .frame(width: 64, height: 32)
                            Image(systemName: a.icone)
                                .font(.system(size: 19))
                                .foregroundStyle(escolhida ? Cor.onSecondaryContainer : Cor.onSurfaceVariant)
                        }
                        Text(a.titulo)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(escolhida ? Cor.onSurface : Cor.onSurfaceVariant)
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(a.titulo)
                .accessibilityAddTraits(escolhida ? .isSelected : [])
            }
        }
        .frame(height: 80)
        .background(Cor.barra.ignoresSafeArea(edges: .bottom))
    }
}

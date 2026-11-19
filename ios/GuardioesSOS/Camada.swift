import SwiftUI

/// Uma ação de diálogo (os botões de texto embaixo, à direita, como no AlertDialog do Android).
struct AcaoDialogo {
    let rotulo: String
    var ativo = true
    /// true: o diálogo fecha sozinho antes de rodar a ação.
    var fecha = true
    var acao: (() -> Void)? = nil

    init(_ rotulo: String, ativo: Bool = true, fecha: Bool = true, acao: (() -> Void)? = nil) {
        self.rotulo = rotulo
        self.ativo = ativo
        self.fecha = fecha
        self.acao = acao
    }
}

/// O que aparece POR CIMA das telas: o diálogo, o aviso rápido (toast) e a tela do guardião (teste).
/// Fica na raiz do app, para cobrir também a barra de cima e a de baixo — igual ao Android.
@MainActor
final class Camada: ObservableObject {
    @Published var folha: AnyView? = nil
    @Published var fechaTocandoFora = true
    @Published var aviso: String? = nil
    /// Texto da tela vermelha do guardião (a demonstração da sirene). nil = fechada.
    @Published var guardiao: String? = nil

    private var tarefaDoAviso: Task<Void, Never>?

    func abrir<V: View>(fechaTocandoFora: Bool = true, @ViewBuilder _ conteudo: () -> V) {
        self.fechaTocandoFora = fechaTocandoFora
        folha = AnyView(conteudo())
    }

    func fechar() {
        folha = nil
    }

    /// Diálogo simples: título, texto e botões (por padrão, um "Entendi").
    func dialogo(_ titulo: String, _ texto: String, acoes: [AcaoDialogo] = [AcaoDialogo("Entendi")]) {
        abrir {
            CaixaDialogo(titulo, rolavel: true, acoes: acoes) {
                Text(texto)
                    .font(.system(size: 14))
                    .foregroundStyle(Cor.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    /// Diálogo de espera ("Enviando o alerta…"), que não fecha tocando fora.
    func esperando(_ titulo: String) {
        abrir(fechaTocandoFora: false) {
            CaixaDialogo(titulo, acoes: []) {
                HStack(spacing: 12) {
                    ProgressView()
                    Text("Não feche o app.")
                        .font(.system(size: 14))
                        .foregroundStyle(Cor.onSurfaceVariant)
                }
            }
        }
    }

    /// Aviso rápido embaixo da tela (o Toast do Android). Some sozinho.
    func toast(_ texto: String) {
        aviso = texto
        tarefaDoAviso?.cancel()
        tarefaDoAviso = Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: 3_200_000_000)
            if Task.isCancelled { return }
            self?.aviso = nil
        }
    }
}

/// A caixa do diálogo no estilo Material 3: cantos de 28, título grande, botões de texto à direita.
struct CaixaDialogo<Conteudo: View>: View {
    @EnvironmentObject var camada: Camada
    let titulo: String
    let rolavel: Bool
    let acoes: [AcaoDialogo]
    let conteudo: Conteudo

    init(_ titulo: String, rolavel: Bool = false, acoes: [AcaoDialogo], @ViewBuilder conteudo: () -> Conteudo) {
        self.titulo = titulo
        self.rolavel = rolavel
        self.acoes = acoes
        self.conteudo = conteudo()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if !titulo.isEmpty {
                Text(titulo)
                    .font(.system(size: 24))
                    .foregroundStyle(Cor.onSurface)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if rolavel {
                ViewThatFits(in: .vertical) {
                    VStack(alignment: .leading, spacing: 12) { conteudo }
                    ScrollView {
                        VStack(alignment: .leading, spacing: 12) { conteudo }
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: 12) { conteudo }
            }
            if !acoes.isEmpty {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 8) {
                        Spacer(minLength: 0)
                        botoes
                    }
                    VStack(alignment: .trailing, spacing: 4) {
                        botoes
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                }
            }
        }
        .padding(24)
        .frame(maxWidth: 420, alignment: .leading)
        .background(Cor.dialogo, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    @ViewBuilder private var botoes: some View {
        ForEach(Array(acoes.enumerated()), id: \.offset) { _, a in
            Button {
                if a.fecha { camada.fechar() }
                a.acao?()
            } label: {
                Text(a.rotulo)
            }
            .buttonStyle(EstiloM3(tipo: .texto))
            .disabled(!a.ativo)
        }
    }
}

/// Desenha o que está na Camada, por cima de tudo.
struct CamadaView: View {
    @EnvironmentObject var camada: Camada

    var body: some View {
        ZStack {
            if let folha = camada.folha {
                Color.black.opacity(0.42)
                    .ignoresSafeArea()
                    .onTapGesture {
                        if camada.fechaTocandoFora { camada.fechar() }
                    }
                    .transition(.opacity)
                folha
                    .padding(24)
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
            }

            if let aviso = camada.aviso {
                VStack {
                    Spacer()
                    Text(aviso)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: 0xF5EFF7))
                        .multilineTextAlignment(.leading)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 12)
                        .background(Color(hex: 0x322F35), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .shadow(color: .black.opacity(0.25), radius: 6, y: 3)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 96)
                }
                .allowsHitTesting(false)
                .transition(.opacity)
            }

            if let corpo = camada.guardiao {
                TelaDoGuardiao(corpo: corpo) { camada.guardiao = nil }
                    .transition(.opacity)
                    .zIndex(3)
            }
        }
        .animation(.easeOut(duration: 0.15), value: camada.folha != nil)
        .animation(.easeOut(duration: 0.2), value: camada.aviso)
        .animation(.easeOut(duration: 0.2), value: camada.guardiao)
    }
}

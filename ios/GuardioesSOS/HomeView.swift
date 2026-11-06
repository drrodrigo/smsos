import SwiftUI
import CoreLocation

/// Aba Início: o botão de pânico. Segure para disparar (igual ao Android). Manda para TODOS os contatos.
struct InicioView: View {
    @EnvironmentObject var app: AppState
    @State private var loc = Localizacao()
    @State private var resultado: ResultadoAlerta?

    private var ehTeste: Bool { app.modoTeste || !Gateway.configurado }

    private var avisa: String {
        if app.contatos.isEmpty { return "Nenhum contato ainda — adicione na aba Contatos." }
        if app.contatos.count == 1 { return "Vai avisar \(app.contatos[0].exibicao)." }
        return "Vai avisar \(app.contatos.count) contatos."
    }

    var body: some View {
        ZStack {
            Fundo()
            VStack(spacing: 18) {
                HStack {
                    Text("GuardiõesSOS").font(.system(size: 22, weight: .heavy)).foregroundStyle(.white)
                    Spacer()
                    if ehTeste {
                        Text("TESTE").font(.caption.bold())
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(Color.yellow.opacity(0.25)).foregroundStyle(.yellow).clipShape(Capsule())
                    }
                }

                Spacer()

                if app.perfil == .saude {
                    Text("Modo Saúde").font(.subheadline.bold()).foregroundStyle(Color(red: 1, green: 0.6, blue: 0.6))
                }

                Text("Segure o botão para pedir socorro")
                    .font(.headline).foregroundStyle(Color(white: 0.85)).multilineTextAlignment(.center)

                BotaoPanico(disparar: executar)

                Text(avisa)
                    .font(.footnote).foregroundStyle(Color(white: 0.6)).multilineTextAlignment(.center)

                Spacer()

                if Gateway.configurado {
                    Toggle(isOn: $app.modoTeste) {
                        Text("Modo teste (não envia de verdade)").font(.footnote).foregroundStyle(.white)
                    }
                    .tint(.yellow)
                    .padding(.horizontal)
                } else {
                    Text("Envio real ainda não configurado nesta versão.")
                        .font(.caption).foregroundStyle(Color(white: 0.5)).multilineTextAlignment(.center)
                }
            }
            .padding(24)
        }
        .onAppear { loc.pedirPermissao() }
        .sheet(item: $resultado) { r in ResultadoAlertaView(resultado: r) }
    }

    private func executar() async {
        let telefones = app.telefonesDosContatos
        let posicao = await loc.pegar()
        let msg = Mensagem.alerta(
            nome: app.nome,
            perfil: app.perfil,
            latitude: posicao?.coordinate.latitude,
            longitude: posicao?.coordinate.longitude
        )
        let temLocal = posicao != nil
        let destino = telefones.isEmpty ? "ninguém" : "\(app.contatos.count) contato(s)"

        let status: StatusEnvio
        if telefones.isEmpty {
            status = .semContato
        } else if ehTeste {
            status = .teste
        } else {
            status = await Gateway.enviar(telefones: telefones, texto: msg) ? .enviado : .falhou
        }

        await MainActor.run {
            resultado = ResultadoAlerta(mensagem: msg, destino: destino, temLocal: temLocal, status: status)
        }
    }
}

/// Botão redondo de SOS: segure ~1,4s (um anel vai enchendo) para disparar. Soltar antes cancela.
struct BotaoPanico: View {
    let disparar: () async -> Void

    @State private var progresso: CGFloat = 0
    @State private var segurando = false
    @State private var enviando = false
    @State private var tarefa: Task<Void, Never>?

    private let duracao: TimeInterval = 1.4

    var body: some View {
        ZStack {
            Circle().fill(Color(red: 0.85, green: 0.12, blue: 0.14))
            Circle()
                .trim(from: 0, to: progresso)
                .stroke(Color.white, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .padding(7)
            if enviando {
                ProgressView().tint(.white).scaleEffect(1.6)
            } else {
                VStack(spacing: 6) {
                    Image(systemName: "exclamationmark.triangle.fill").font(.system(size: 50))
                    Text(segurando ? "Segure…" : "SOS").font(.system(size: segurando ? 24 : 40, weight: .black))
                }
                .foregroundStyle(.white)
            }
        }
        .frame(width: 220, height: 220)
        .shadow(color: .red.opacity(0.4), radius: 20)
        .contentShape(Circle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in if !segurando && !enviando { iniciar() } }
                .onEnded { _ in soltar() }
        )
        .accessibilityElement()
        .accessibilityLabel("Botão de pânico. Segure para pedir socorro.")
        .accessibilityAddTraits(.isButton)
        .accessibilityAction { Task { await fazerEnvio() } }
    }

    private func iniciar() {
        segurando = true
        withAnimation(.linear(duration: duracao)) { progresso = 1 }
        tarefa = Task {
            try? await Task.sleep(nanoseconds: UInt64(duracao * 1_000_000_000))
            if Task.isCancelled { return }
            await fazerEnvio()
        }
    }

    private func soltar() {
        if enviando { return }
        tarefa?.cancel()
        tarefa = nil
        segurando = false
        withAnimation(.easeOut(duration: 0.2)) { progresso = 0 }
    }

    private func fazerEnvio() async {
        await MainActor.run {
            segurando = false
            enviando = true
            progresso = 0
        }
        await disparar()
        await MainActor.run { enviando = false }
    }
}

enum StatusEnvio { case teste, enviado, falhou, semContato }

struct ResultadoAlerta: Identifiable {
    let id = UUID()
    let mensagem: String
    let destino: String
    let temLocal: Bool
    let status: StatusEnvio
}

struct ResultadoAlertaView: View {
    let resultado: ResultadoAlerta
    @Environment(\.dismiss) private var dismiss

    private var titulo: String {
        switch resultado.status {
        case .teste: return "Modo teste — nada foi enviado"
        case .enviado: return "Alerta enviado!"
        case .falhou: return "Não consegui enviar"
        case .semContato: return "Nenhum contato cadastrado"
        }
    }
    private var cor: Color {
        switch resultado.status {
        case .teste: return .orange
        case .enviado: return .green
        case .falhou: return .red
        case .semContato: return .orange
        }
    }
    private var icone: String {
        switch resultado.status {
        case .teste: return "flask.fill"
        case .enviado: return "checkmark.circle.fill"
        case .falhou: return "xmark.octagon.fill"
        case .semContato: return "person.crop.circle.badge.exclamationmark"
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Label(titulo, systemImage: icone).foregroundStyle(cor).font(.title3.bold())

                if resultado.status == .semContato {
                    Text("Adicione pelo menos um contato na aba \"Contatos\" para o SOS ter para quem avisar.")
                        .foregroundStyle(.secondary)
                } else {
                    Label(
                        resultado.temLocal ? "Localização capturada" : "Sem localização (segue sem o mapa)",
                        systemImage: resultado.temLocal ? "location.fill" : "location.slash"
                    )
                    .foregroundStyle(resultado.temLocal ? .green : .orange)
                    .font(.subheadline)

                    Text("Para: \(resultado.destino)").font(.subheadline).foregroundStyle(.secondary)

                    Text("Mensagem:").font(.headline)
                    Text(resultado.mensagem)
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    if resultado.status == .teste {
                        Text("Para enviar de verdade, desligue o \"Modo teste\" na tela principal.")
                            .font(.footnote).foregroundStyle(.secondary)
                    } else if resultado.status == .falhou {
                        Text("Verifique a internet e tente de novo. Se continuar, me avise.")
                            .font(.footnote).foregroundStyle(.secondary)
                    }
                }

                Button { dismiss() } label: {
                    Text("Fechar").font(.headline).frame(maxWidth: .infinity).padding()
                        .background(Color.accentColor).foregroundStyle(.white).clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(20)
        }
    }
}

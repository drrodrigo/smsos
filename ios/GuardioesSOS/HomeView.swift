import SwiftUI
import CoreLocation
import UIKit

/// Aba Início: banner de status + botão de pânico + checklist de Configuração (com a cara do Android).
struct InicioView: View {
    @EnvironmentObject var app: AppState
    let irPara: (Aba) -> Void

    @State private var loc = Localizacao()
    @State private var resultado: ResultadoAlerta?
    @State private var localizacaoOk = Demo.ativo || Localizacao.autorizado
    @Environment(\.openURL) private var openURL

    private var ehTeste: Bool { app.modoTeste || !Gateway.configurado }

    private var nomeOk: Bool { !app.nome.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    private var telefoneOk: Bool { Telefone.limpar(app.meuTelefone).count >= 10 }
    private var contatosOk: Bool { !app.contatos.isEmpty }
    private var pendentes: Int { [nomeOk, telefoneOk, contatosOk, localizacaoOk].filter { !$0 }.count }

    private var banner: (titulo: String, sub: String, cor: Color) {
        if !contatosOk { return ("Falta cadastrar contato", "Adicione quem deve ser avisado.", .orange) }
        if ehTeste { return ("MODO TESTE", "O botão funciona, mas nada é enviado.", .yellow) }
        return ("Proteção ATIVA", "Segure o botão para pedir socorro.", .green)
    }

    private var avisa: String {
        if app.contatos.isEmpty { return "Ninguém cadastrado ainda." }
        if app.contatos.count == 1 { return "Vai avisar \(app.contatos[0].exibicao)." }
        return "Vai avisar \(app.contatos.count) contatos."
    }

    var body: some View {
        ZStack {
            Fundo()
            ScrollView {
                VStack(spacing: 16) {
                    HStack {
                        Text("GuardiõesSOS").font(.system(size: 22, weight: .heavy)).foregroundStyle(.white)
                        Spacer()
                        if ehTeste {
                            Text("TESTE").font(.caption.bold())
                                .padding(.horizontal, 8).padding(.vertical, 4)
                                .background(Color.yellow.opacity(0.25)).foregroundStyle(.yellow).clipShape(Capsule())
                        }
                    }

                    VStack(spacing: 4) {
                        Text(banner.titulo).font(.headline).foregroundStyle(banner.cor)
                        Text(banner.sub).font(.caption).foregroundStyle(Color(white: 0.75))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(banner.cor.opacity(0.14))
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                    BotaoPanico(disparar: executar).padding(.top, 6)

                    Text(avisa).font(.footnote).foregroundStyle(Color(white: 0.6)).multilineTextAlignment(.center)

                    VStack(alignment: .leading, spacing: 0) {
                        HStack {
                            Text("Configuração").font(.subheadline.bold()).foregroundStyle(.white)
                            Spacer()
                            Text(pendentes == 0 ? "Tudo pronto ✓" : "Falta \(pendentes) ajuste(s)")
                                .font(.caption).foregroundStyle(pendentes == 0 ? .green : .orange)
                        }
                        .padding(.bottom, 12)

                        item("Seu nome", "Aparece na mensagem, pra saberem quem pediu ajuda.", nomeOk) { irPara(.ajustes) }
                        divisor
                        item("Seu telefone", "Sua identidade na rede de guardiões.", telefoneOk) { irPara(.ajustes) }
                        divisor
                        item("Contatos", "Quem recebe o seu SOS.", contatosOk) { irPara(.contatos) }
                        divisor
                        item("Localização", "Para mandar o link do mapa no alerta.", localizacaoOk) { pedirLocalizacao() }
                    }
                    .padding(16)
                    .background(Color.white.opacity(0.07))
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    if Gateway.configurado {
                        Toggle(isOn: $app.modoTeste) {
                            Text("Modo teste (não envia de verdade)").font(.footnote).foregroundStyle(.white)
                        }
                        .tint(.yellow)
                    }
                }
                .padding(20)
            }
        }
        .onAppear {
            if !Demo.ativo { loc.pedirPermissao() }
            localizacaoOk = Demo.ativo || Localizacao.autorizado
        }
        .sheet(item: $resultado) { r in ResultadoAlertaView(resultado: r) }
    }

    private var divisor: some View {
        Divider().overlay(Color.white.opacity(0.12)).padding(.vertical, 10)
    }

    private func item(_ titulo: String, _ desc: String, _ ok: Bool, _ acao: @escaping () -> Void) -> some View {
        Button(action: acao) {
            HStack(spacing: 12) {
                Image(systemName: ok ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                    .foregroundStyle(ok ? .green : .orange).font(.title3)
                VStack(alignment: .leading, spacing: 2) {
                    Text(titulo).font(.subheadline.bold()).foregroundStyle(.white)
                    Text(desc).font(.caption).foregroundStyle(Color(white: 0.6))
                }
                Spacer()
                if !ok {
                    Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(Color(white: 0.5))
                }
            }
        }
        .buttonStyle(.plain)
    }

    private func pedirLocalizacao() {
        if Localizacao.statusAtual() == .notDetermined {
            loc.pedirPermissao()
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { localizacaoOk = Localizacao.autorizado }
        } else if let url = URL(string: UIApplication.openSettingsURLString) {
            openURL(url)
        }
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
                    Image(systemName: "exclamationmark.triangle.fill").font(.system(size: 48))
                    Text(segurando ? "Segure…" : "SOS").font(.system(size: segurando ? 22 : 38, weight: .black))
                }
                .foregroundStyle(.white)
            }
        }
        .frame(width: 200, height: 200)
        .shadow(color: .red.opacity(0.4), radius: 18)
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

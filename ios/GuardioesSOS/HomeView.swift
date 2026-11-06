import SwiftUI
import CoreLocation

/// Tela principal: o botão de pânico.
/// - Modo teste (padrão): captura a localização e MOSTRA a mensagem, sem enviar.
/// - Envio real (modo teste desligado, com a chave configurada): manda o SMS pelo gateway (SMS Market).
struct HomeView: View {
    @EnvironmentObject var app: AppState
    @State private var loc = Localizacao()
    @State private var enviando = false
    @State private var confirmar = false
    @State private var resultado: ResultadoAlerta?

    /// É teste quando o usuário deixou o modo teste ligado, OU quando a chave não está configurada.
    private var ehTeste: Bool { app.modoTeste || !Gateway.configurado }

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

                Text(enviando ? "Enviando o alerta..." : "Toque para pedir socorro")
                    .font(.headline).foregroundStyle(Color(white: 0.85))

                Button { confirmar = true } label: {
                    ZStack {
                        Circle().fill(Color(red: 0.85, green: 0.12, blue: 0.14))
                        if enviando {
                            ProgressView().tint(.white).scaleEffect(1.6)
                        } else {
                            VStack(spacing: 6) {
                                Image(systemName: "exclamationmark.triangle.fill").font(.system(size: 52))
                                Text("SOS").font(.system(size: 40, weight: .black))
                            }
                            .foregroundStyle(.white)
                        }
                    }
                    .frame(width: 230, height: 230)
                    .shadow(color: .red.opacity(0.4), radius: 20)
                }
                .disabled(enviando)

                Text("Vai avisar \(app.contatoExibicao).")
                    .font(.footnote).foregroundStyle(Color(white: 0.6))

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
        .alert(ehTeste ? "Testar alerta?" : "Pedir socorro agora?", isPresented: $confirmar) {
            Button("Cancelar", role: .cancel) {}
            Button(ehTeste ? "Testar" : "Pedir socorro", role: .destructive) { Task { await disparar() } }
        } message: {
            Text(ehTeste
                 ? "Vamos pegar sua localização e montar o alerta (sem enviar)."
                 : "Vamos pegar sua localização e ENVIAR o alerta para \(app.contatoExibicao).")
        }
        .sheet(item: $resultado) { r in ResultadoAlertaView(resultado: r) }
    }

    private func disparar() async {
        enviando = true
        let posicao = await loc.pegar()
        let msg = Mensagem.alerta(
            nome: app.nome,
            latitude: posicao?.coordinate.latitude,
            longitude: posicao?.coordinate.longitude
        )
        let destino = "\(app.contatoExibicao) (\(app.contatoTelefone))"
        let temLocal = posicao != nil

        let status: StatusEnvio
        if ehTeste {
            status = .teste
        } else {
            let ok = await Gateway.enviar(telefone: app.contatoTelefone, texto: msg)
            status = ok ? .enviado : .falhou
        }

        enviando = false
        resultado = ResultadoAlerta(mensagem: msg, destino: destino, temLocal: temLocal, status: status)
    }
}

enum StatusEnvio { case teste, enviado, falhou }

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
        }
    }
    private var cor: Color {
        switch resultado.status {
        case .teste: return .orange
        case .enviado: return .green
        case .falhou: return .red
        }
    }
    private var icone: String {
        switch resultado.status {
        case .teste: return "flask.fill"
        case .enviado: return "checkmark.circle.fill"
        case .falhou: return "xmark.octagon.fill"
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Label(titulo, systemImage: icone).foregroundStyle(cor).font(.title3.bold())

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

                Button { dismiss() } label: {
                    Text("Fechar")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(20)
        }
    }
}

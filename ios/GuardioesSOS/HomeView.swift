import SwiftUI
import CoreLocation

/// Tela principal: o botão de pânico. Milestone 1 (modo teste): captura a localização e mostra a
/// mensagem que SERIA enviada. O envio real (SMS Market) entra na próxima versão.
struct HomeView: View {
    @EnvironmentObject var app: AppState
    @State private var loc = Localizacao()
    @State private var enviando = false
    @State private var confirmar = false
    @State private var resultado: ResultadoAlerta?

    var body: some View {
        ZStack {
            Fundo()
            VStack(spacing: 20) {
                HStack {
                    Text("GuardiõesSOS")
                        .font(.system(size: 22, weight: .heavy))
                        .foregroundStyle(.white)
                    Spacer()
                    Text("TESTE")
                        .font(.caption.bold())
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(Color.yellow.opacity(0.25))
                        .foregroundStyle(.yellow)
                        .clipShape(Capsule())
                }

                Spacer()

                Text(enviando ? "Preparando o alerta..." : "Toque para pedir socorro")
                    .font(.headline)
                    .foregroundStyle(Color(white: 0.85))

                Button {
                    confirmar = true
                } label: {
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

                Text("Vai avisar \(app.contatoNome.isEmpty ? "seu contato" : app.contatoNome).")
                    .font(.footnote)
                    .foregroundStyle(Color(white: 0.6))

                Spacer()

                Text("Versão de teste — ainda não envia de verdade. Mostramos a mensagem que será enviada.")
                    .font(.caption)
                    .foregroundStyle(Color(white: 0.5))
                    .multilineTextAlignment(.center)
            }
            .padding(24)
        }
        .onAppear { loc.pedirPermissao() }
        .alert("Pedir socorro agora?", isPresented: $confirmar) {
            Button("Cancelar", role: .cancel) {}
            Button("Pedir socorro", role: .destructive) { Task { await disparar() } }
        } message: {
            Text("Vamos pegar sua localização e montar o alerta.")
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
        let nomeContato = app.contatoNome.isEmpty ? "contato" : app.contatoNome
        enviando = false
        resultado = ResultadoAlerta(
            mensagem: msg,
            destino: "\(nomeContato) (\(app.contatoTelefone))",
            temLocal: posicao != nil
        )
    }
}

struct ResultadoAlerta: Identifiable {
    let id = UUID()
    let mensagem: String
    let destino: String
    let temLocal: Bool
}

struct ResultadoAlertaView: View {
    let resultado: ResultadoAlerta
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Label(
                    resultado.temLocal ? "Localização capturada" : "Sem localização (seguiria sem o mapa)",
                    systemImage: resultado.temLocal ? "location.fill" : "location.slash"
                )
                .foregroundStyle(resultado.temLocal ? .green : .orange)
                .font(.headline)

                Text("Para: \(resultado.destino)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Text("Mensagem que seria enviada:")
                    .font(.headline)
                Text(resultado.mensagem)
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                Text("Nesta versão de teste nada é enviado de verdade. O envio real pelo SMS Market é o próximo passo.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                Button {
                    dismiss()
                } label: {
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

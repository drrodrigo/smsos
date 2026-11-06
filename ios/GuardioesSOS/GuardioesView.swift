import SwiftUI

/// Aba Guardiões: convide alguém para ser seu guardião (recebe seus alertas).
struct GuardioesView: View {
    @EnvironmentObject var app: AppState
    @State private var telefone = ""
    @State private var carregando = false
    @State private var resultado: GuardioesConvite.ResultadoConvite?
    @State private var erro: String?

    private var telefoneValido: Bool { Telefone.limpar(telefone).count >= 10 }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("O guardião recebe seus alertas de SOS. Convide pelo número de WhatsApp dele.")
                        .font(.footnote).foregroundStyle(.secondary)
                    TextField("Telefone do guardião (com DDD)", text: $telefone).keyboardType(.phonePad)
                    Button {
                        Task { await convidar() }
                    } label: {
                        if carregando { ProgressView() } else { Text("Gerar convite") }
                    }
                    .disabled(carregando || !telefoneValido)
                } header: {
                    Text("Convidar um guardião")
                }

                if let r = resultado {
                    Section("Convite criado") {
                        LabeledContent("Código", value: r.codigo)
                        ShareLink(item: textoConvite(r)) {
                            Label("Compartilhar convite", systemImage: "square.and.arrow.up")
                        }
                        if !r.link.isEmpty {
                            Text(r.link).font(.caption).foregroundStyle(.secondary).textSelection(.enabled)
                        }
                    }
                }

                if let e = erro {
                    Section { Text(e).foregroundStyle(.red) }
                }
            }
            .navigationTitle("Guardiões")
        }
    }

    private func textoConvite(_ r: GuardioesConvite.ResultadoConvite) -> String {
        let quem = app.nome.trimmingCharacters(in: .whitespaces)
        let intro = quem.isEmpty ? "Quero te adicionar como meu guardião no GuardiõesSOS."
                                 : "\(quem) quer te adicionar como guardião no GuardiõesSOS."
        if r.link.isEmpty { return "\(intro) Use o código \(r.codigo) no app." }
        return "\(intro) Aceite aqui: \(r.link) (ou use o código \(r.codigo))."
    }

    private func convidar() async {
        carregando = true
        erro = nil
        let r = await GuardioesConvite.convidar(
            usuarioDispositivo: app.dispositivo,
            usuarioNome: app.nome,
            guardiaoTelefone: telefone
        )
        await MainActor.run {
            carregando = false
            if let r = r {
                resultado = r
            } else {
                erro = "Não consegui criar o convite. Verifique a internet e tente de novo."
            }
        }
    }
}

import SwiftUI

/// Aba Protejo: aceite convites (por código ou da lista) e veja quem você protege.
struct ProtejoView: View {
    @EnvironmentObject var app: AppState
    @State private var codigo = ""
    @State private var carregando = false
    @State private var mensagem: String?
    @State private var pendentes: [GuardioesConvite.Pendente] = []
    @State private var protegidos: [GuardioesConvite.Protegido] = []

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Recebeu um convite para ser guardião? Digite o código.")
                        .font(.footnote).foregroundStyle(.secondary)
                    TextField("Código do convite", text: $codigo)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.characters)
                    Button {
                        Task { await aceitar(codigo) }
                    } label: {
                        if carregando { ProgressView() } else { Text("Aceitar") }
                    }
                    .disabled(carregando || codigo.trimmingCharacters(in: .whitespaces).isEmpty)
                } header: {
                    Text("Tenho um código")
                }

                if !pendentes.isEmpty {
                    Section("Convites para você") {
                        ForEach(pendentes) { p in
                            HStack {
                                Text(p.usuarioNome.isEmpty ? "Alguém" : p.usuarioNome)
                                Spacer()
                                Button("Aceitar") { Task { await aceitar(p.codigo) } }
                                    .buttonStyle(.borderless)
                            }
                        }
                    }
                }

                Section("Quem eu protejo") {
                    if protegidos.isEmpty {
                        Text("Ninguém ainda.").foregroundStyle(.secondary)
                    }
                    ForEach(protegidos) { p in
                        Label(p.usuarioNome.isEmpty ? "Alguém" : p.usuarioNome, systemImage: "shield.lefthalf.filled")
                    }
                }

                if let m = mensagem {
                    Section { Text(m) }
                }
            }
            .navigationTitle("Protejo")
            .refreshable { await carregar() }
            .task { await carregar() }
        }
    }

    private func carregar() async {
        let pend = await GuardioesConvite.pendentes(telefone: app.meuTelefone)
        let prot = await GuardioesConvite.quemProtejo(guardiaoDispositivo: app.dispositivo)
        await MainActor.run {
            pendentes = pend
            protegidos = prot
        }
    }

    private func aceitar(_ cod: String) async {
        carregando = true
        mensagem = nil
        let r = await GuardioesConvite.aceitar(codigo: cod, guardiaoDispositivo: app.dispositivo)
        await MainActor.run {
            carregando = false
            if let r = r, r.ok || r.jaAceito {
                let quem = r.usuarioNome?.isEmpty == false ? r.usuarioNome! : "essa pessoa"
                mensagem = r.jaAceito ? "Você já protegia \(quem)." : "Pronto! Agora você protege \(quem)."
                codigo = ""
            } else {
                mensagem = "Código inválido ou já usado."
            }
        }
        await carregar()
    }
}

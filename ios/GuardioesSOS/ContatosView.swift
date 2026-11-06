import SwiftUI

/// Aba Contatos: quem recebe o SOS. Até 10.
struct ContatosView: View {
    @EnvironmentObject var app: AppState
    @State private var nome = ""
    @State private var telefone = ""

    private var telefoneValido: Bool { Telefone.limpar(telefone).count >= 10 }

    var body: some View {
        NavigationStack {
            List {
                Section("Quem recebe o SOS") {
                    if app.contatos.isEmpty {
                        Text("Nenhum contato ainda.").foregroundStyle(.secondary)
                    }
                    ForEach(app.contatos) { c in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(c.exibicao)
                            Text(c.telefone).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    .onDelete { app.contatos.remove(atOffsets: $0) }
                }

                if app.contatos.count < 10 {
                    Section("Adicionar contato") {
                        TextField("Nome (ex.: Maria)", text: $nome)
                        TextField("Telefone com DDD", text: $telefone).keyboardType(.phonePad)
                        Button("Adicionar") {
                            app.adicionarContato(Contato(nome: nome, telefone: telefone))
                            nome = ""
                            telefone = ""
                        }
                        .disabled(!telefoneValido)
                    }
                } else {
                    Section { Text("Máximo de 10 contatos.").foregroundStyle(.secondary) }
                }
            }
            .navigationTitle("Contatos")
        }
    }
}

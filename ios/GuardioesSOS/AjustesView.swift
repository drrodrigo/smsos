import SwiftUI

/// Aba Ajustes: nome, telefone, tipo de alerta e modo teste.
struct AjustesView: View {
    @EnvironmentObject var app: AppState

    private var versaoApp: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
        return "GuardiõesSOS \(v) (build \(b)) • iPhone"
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Você") {
                    TextField("Seu nome", text: $app.nome)
                    TextField("Seu telefone (WhatsApp)", text: $app.meuTelefone).keyboardType(.phonePad)
                }

                Section {
                    Picker("Tipo de alerta", selection: $app.perfil) {
                        Text("Segurança").tag(Perfil.seguranca)
                        Text("Saúde").tag(Perfil.saude)
                    }
                    .pickerStyle(.segmented)
                    Text(app.perfil == .saude
                         ? "O alerta diz que você pode estar passando mal."
                         : "O alerta diz que você pode estar em perigo.")
                        .font(.footnote).foregroundStyle(.secondary)
                } header: {
                    Text("Tipo de alerta")
                }

                if Gateway.configurado {
                    Section("Envio") {
                        Toggle("Modo teste (não envia de verdade)", isOn: $app.modoTeste)
                    }
                }

                Section {
                    LabeledContent("ID do aparelho", value: String(app.dispositivo.prefix(8)))
                } footer: {
                    Text(versaoApp)
                }
            }
            .navigationTitle("Ajustes")
            .tecladoFechavel()
            .onDisappear { app.registrarLead() }
        }
    }
}

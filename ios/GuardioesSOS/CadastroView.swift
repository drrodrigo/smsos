import SwiftUI

/// Cadastro mínimo: nome da pessoa e um contato de emergência.
struct CadastroView: View {
    @EnvironmentObject var app: AppState
    @FocusState private var foco: Campo?
    enum Campo { case nome, contatoNome, contatoTel }

    var body: some View {
        ZStack {
            Fundo()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Image(systemName: "shield.lefthalf.filled")
                        .font(.system(size: 54))
                        .foregroundStyle(.white)
                    Text("Vamos configurar")
                        .font(.system(size: 28, weight: .heavy))
                        .foregroundStyle(.white)
                    Text("Leva 1 minuto. Seu nome vai no alerta, e o contato é quem recebe o pedido de socorro.")
                        .foregroundStyle(Color(white: 0.8))

                    campo("Seu nome", $app.nome, .nome, .default)

                    Text("Contato de emergência")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .padding(.top, 6)
                    campo("Nome (ex.: Fernanda)", $app.contatoNome, .contatoNome, .default)
                    campo("Telefone com DDD", $app.contatoTelefone, .contatoTel, .phonePad)

                    Button {
                        foco = nil
                        app.configurado = true
                    } label: {
                        Text("Continuar")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(app.cadastroValido ? Color.white : Color.white.opacity(0.2))
                            .foregroundStyle(app.cadastroValido ? Color(red: 0.04, green: 0.11, blue: 0.21) : Color(white: 0.6))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .disabled(!app.cadastroValido)
                    .padding(.top, 8)
                }
                .padding(24)
            }
        }
    }

    private func campo(_ titulo: String, _ texto: Binding<String>, _ qual: Campo, _ teclado: UIKeyboardType) -> some View {
        TextField("", text: texto, prompt: Text(titulo).foregroundColor(Color(white: 0.5)))
            .focused($foco, equals: qual)
            .keyboardType(teclado)
            .foregroundStyle(.white)
            .padding()
            .background(Color.white.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

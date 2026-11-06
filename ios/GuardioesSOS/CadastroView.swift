import SwiftUI

/// Primeira abertura: nome e telefone da pessoa + o primeiro contato de emergência.
struct CadastroView: View {
    @EnvironmentObject var app: AppState
    @State private var contatoNome = ""
    @State private var contatoTelefone = ""
    @FocusState private var foco: Campo?
    enum Campo { case nome, tel, cNome, cTel }

    private var valido: Bool {
        app.cadastroValido && Telefone.limpar(contatoTelefone).count >= 10
    }

    var body: some View {
        ZStack {
            Fundo()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Image(systemName: "shield.lefthalf.filled").font(.system(size: 54)).foregroundStyle(.white)
                    Text("Vamos configurar").font(.system(size: 28, weight: .heavy)).foregroundStyle(.white)
                    Text("Leva 1 minuto. Seu nome vai no alerta; o contato é quem recebe o pedido de socorro.")
                        .foregroundStyle(Color(white: 0.8))

                    campo("Seu nome", $app.nome, .nome, .default)
                    campo("Seu telefone (WhatsApp)", $app.meuTelefone, .tel, .phonePad)

                    Text("Contato de emergência")
                        .font(.headline).foregroundStyle(.white).padding(.top, 6)
                    campo("Nome do contato (ex.: Maria)", $contatoNome, .cNome, .default)
                    campo("Telefone com DDD", $contatoTelefone, .cTel, .phonePad)

                    Button {
                        foco = nil
                        app.adicionarContato(Contato(nome: contatoNome, telefone: contatoTelefone))
                        app.configurado = true
                        app.registrarLead()
                    } label: {
                        Text("Continuar")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(valido ? Color.white : Color.white.opacity(0.2))
                            .foregroundStyle(valido ? Color(red: 0.04, green: 0.11, blue: 0.21) : Color(white: 0.6))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .disabled(!valido)
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

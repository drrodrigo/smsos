import SwiftUI

/// Primeira abertura: o nome e o WhatsApp da pessoa (a conta, sem senha) — igual ao RegistroScreen
/// do Android. Os guardiões ela cadastra depois, guiada pela tela Início.
struct CadastroView: View {
    @EnvironmentObject var app: AppState
    @State private var nome = ""
    @State private var fone = ""

    private var valido: Bool {
        !nome.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && Telefone.celularValido(fone)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Spacer()
                    Image("Escudo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 96, height: 96)
                    Spacer()
                }
                .padding(.top, 24)

                Text("Bem-vindo ao GuardiõesSOS")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(Cor.onSurface)
                    .fixedSize(horizontal: false, vertical: true)
                Text("Pra começar, me diga seu nome e seu WhatsApp. É a sua conta — sem senha, sem complicação.")
                    .font(.system(size: 16))
                    .foregroundStyle(Cor.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)

                CampoM3("Seu nome", texto: $nome, palavras: true)
                CampoM3("Seu WhatsApp (com DDD)", texto: $fone, placeholder: "(31) 99999-8888", teclado: .phonePad,
                        erro: Telefone.aviso(fone))
                if Telefone.celularValido(fone) && Telefone.formatar(fone) != fone.trimmingCharacters(in: .whitespaces) {
                    Text("Vai salvar como: \(Telefone.formatar(fone))")
                        .font(.system(size: 12))
                        .foregroundStyle(Cor.primary)
                }

                BotaoM3("Continuar", tipo: .cheio, largo: true, alto: 48, fonte: 16) {
                    esconderTeclado()
                    app.nome = String(nome.trimmingCharacters(in: .whitespacesAndNewlines).prefix(30))
                    app.meuTelefone = Telefone.nacional(fone)
                    app.configurado = true
                    app.registrarLead()
                }
                .disabled(!valido)
                .padding(.top, 8)

                BotaoM3("Pular por enquanto", tipo: .texto, largo: true) {
                    esconderTeclado()
                    app.nome = String(nome.trimmingCharacters(in: .whitespacesAndNewlines).prefix(30))
                    if Telefone.celularValido(fone) { app.meuTelefone = Telefone.nacional(fone) }
                    app.configurado = true
                    app.registrarLead()
                }

                TextoAjuda("Usamos seus dados só pra criar sua conta e te avisar — nada de spam. Política de privacidade em guardioessos.com.br/privacidade.")
                Rodape()
            }
            .padding(24)
        }
        .scrollDismissesKeyboard(.immediately)
        .background(Cor.fundo.ignoresSafeArea())
    }
}

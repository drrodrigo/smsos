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
        VStack(spacing: 0) {
            // A setinha de voltar no topo (regra de toda tela). Aqui é a primeira tela: não há para onde voltar.
            HStack {
                Button {} label: {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(Cor.onSurface)
                        .frame(width: 48, height: 48)
                }
                .buttonStyle(.plain)
                .disabled(true)
                .opacity(0.38)
                .accessibilityLabel("Voltar")
                Spacer()
            }
            .padding(.horizontal, 4)
            .frame(height: 64)
            formulario
        }
        .background(Cor.fundo.ignoresSafeArea())
    }

    private var formulario: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Image("Escudo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 84, height: 84)
                    .frame(maxWidth: .infinity)

                Text("Bem-vindo ao GuardiõesSOS")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(Cor.onSurface)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .fixedSize(horizontal: false, vertical: true)
                Text("Pra começar, me diga seu nome e seu WhatsApp. É a sua conta — sem senha, sem complicação.")
                    .font(.system(size: 14))
                    .foregroundStyle(Cor.onSurface)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
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
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .scrollDismissesKeyboard(.immediately)
    }
}

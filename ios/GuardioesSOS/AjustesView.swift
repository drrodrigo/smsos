import SwiftUI

/// Aba Ajustes, igual à do Android: seu nome, seu WhatsApp, para que você usa (os dois SOS ou um
/// só), quantos segundos segurar, como a mensagem chega e o histórico.
struct AjustesView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var camada: Camada
    @State private var editandoNome = false
    @State private var nome = ""
    @State private var editandoFone = false
    @State private var fone = ""

    var body: some View {
        ScrollViewReader { rolagem in
            ScrollView {
                VStack(spacing: 12) {
                    TituloTela("Ajustes")
                    cartaoNome
                    cartaoWhatsApp
                    cartaoUso
                    cartaoBotao
                    cartaoMensagem
                    cartaoHistorico
                    Rodape(versao: true).id("fim")
                }
                .padding(16)
            }
            .task {
                if Demo.rolar {
                    try? await Task.sleep(nanoseconds: 500_000_000)
                    rolagem.scrollTo("fim", anchor: .bottom)
                }
            }
        }
        .onAppear {
            nome = app.nome
            fone = Telefone.formatar(app.meuTelefone)
            editandoNome = !app.nomeOk
            editandoFone = app.meuTelefone.isEmpty
        }
        .onDisappear { app.registrarLead() }
    }

    // MARK: - Seu nome

    private var cartaoNome: some View {
        Cartao("Seu nome") {
            if editandoNome {
                CampoM3("Nome que aparece na mensagem", texto: $nome, palavras: true)
                BotaoM3("Salvar", tipo: .cheio, largo: true) {
                    app.nome = String(nome.trimmingCharacters(in: .whitespacesAndNewlines).prefix(30))
                    editandoNome = false
                    esconderTeclado()
                    app.registrarLead()
                    camada.toast("Nome salvo ✓")
                }
                .disabled(nome.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            } else {
                HStack {
                    Text(app.nome)
                        .font(.system(size: 16))
                        .foregroundStyle(Cor.onSurface)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    BotaoM3("Editar", tipo: .texto) {
                        nome = app.nome
                        editandoNome = true
                    }
                }
            }
        }
    }

    // MARK: - Seu WhatsApp

    private var cartaoWhatsApp: some View {
        Cartao("Seu WhatsApp") {
            TextoAjuda("É a sua conta no GuardiõesSOS, sem senha. É por ele que chegam os convites de quem quer você como guardião.")
            if editandoFone {
                CampoM3("Celular com DDD", texto: $fone, placeholder: "(31) 99999-8888", teclado: .phonePad,
                        erro: Telefone.aviso(fone))
                if Telefone.celularValido(fone) && Telefone.formatar(fone) != fone.trimmingCharacters(in: .whitespaces) {
                    Text("Vai salvar como: \(Telefone.formatar(fone))")
                        .font(.system(size: 12))
                        .foregroundStyle(Cor.primary)
                }
                BotaoM3("Salvar", tipo: .cheio, largo: true) {
                    app.meuTelefone = Telefone.nacional(fone)
                    fone = Telefone.formatar(app.meuTelefone)
                    editandoFone = false
                    esconderTeclado()
                    app.registrarLead()
                    camada.toast("WhatsApp salvo ✓")
                }
                .disabled(!Telefone.celularValido(fone))
            } else {
                HStack {
                    Text(Telefone.formatar(app.meuTelefone))
                        .font(.system(size: 16))
                        .foregroundStyle(app.telefoneOk ? Cor.onSurface : Cor.error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    BotaoM3("Editar", tipo: .texto) {
                        fone = Telefone.formatar(app.meuTelefone)
                        editandoFone = true
                    }
                }
                if let aviso = Telefone.aviso(app.meuTelefone) {
                    Text("⚠ " + aviso)
                        .font(.system(size: 12))
                        .foregroundStyle(Cor.error)
                }
            }
        }
    }

    // MARK: - Para que você usa

    private var cartaoUso: some View {
        Cartao("Para que você usa") {
            LinhaComChave("Ativar os dois SOS (Segurança e Saúde)", ligado: $app.dualSos)
            if app.dualSos {
                TextoAjuda("Os dois ficam ativos. Na tela Início aparece o botão dividido, um lado de cada.")
            } else {
                HStack(spacing: 8) {
                    ChipM3(Perfil.seguranca.rotulo, selecionado: app.perfil == .seguranca) { app.perfil = .seguranca }
                    ChipM3(Perfil.saude.rotulo, selecionado: app.perfil == .saude) { app.perfil = .saude }
                }
                TextoAjuda(app.perfil.descricao)
            }
        }
    }

    // MARK: - Botão de acionamento

    private var cartaoBotao: some View {
        Cartao("Botão de acionamento") {
            Text("Segurar por \(app.segundos) segundos")
                .font(.system(size: 16))
                .foregroundStyle(Cor.onSurface)
            Slider(
                value: Binding(get: { Double(app.segundos) }, set: { app.segundos = Int($0.rounded()) }),
                in: 3...30,
                step: 1
            )
            TextoAjuda("No iPhone, o SOS é o botão da tela Início: segure até a barra branca encher. Soltar antes cancela. O botão de volume do iPhone não aciona o SOS.")
        }
    }

    // MARK: - Mensagem

    private var cartaoMensagem: some View {
        let t = Demonstracao.textos(nome: app.nome)
        let unico = app.perfil == .saude ? t.saude : t.seguranca
        return Cartao("Mensagem") {
            Text(app.dualSos ? "Exemplo de como chega (🛡️ Segurança):" : "Exemplo de como chega:")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Cor.onSurface)
            BalaoMensagem(nil, app.dualSos ? t.seguranca : unico)
            if app.dualSos {
                Text("❤️ Saúde:")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Cor.onSurface)
                BalaoMensagem(nil, t.saude)
            }
            TextoAjuda("No iPhone vai sempre o texto padrão, sem acentos, para caber em um SMS só. O link é o da sua localização na hora do alerta.")
        }
    }

    // MARK: - Histórico

    private var cartaoHistorico: some View {
        Cartao("Histórico") {
            Text("Apagar o histórico sozinho depois de:")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Cor.onSurface)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach([0, 2, 7, 30], id: \.self) { dias in
                        ChipM3(dias == 0 ? "Nunca" : "\(dias) dias", selecionado: app.retencao == dias) {
                            app.retencao = dias
                            app.limparHistoricoVelho()
                        }
                    }
                }
            }
            TextoAjuda("Histórico curto deixa o app leve e é mais seguro se tomarem seu celular. \"Nunca\" mantém tudo até você apagar. Você também pode apagar um alerta por vez na tela Início.")
        }
    }
}

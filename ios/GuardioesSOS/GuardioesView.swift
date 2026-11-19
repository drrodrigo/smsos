import SwiftUI

/// Aba Guardiões, igual à do Android: quem recebe o alerta em qualquer lugar, com "Convidar"
/// (pelo WhatsApp, já com o código do convite), editar e apagar; e "Convidar alguém novo".
/// Junta o que antes eram as abas "Contatos" (a lista que recebe o SMS) e "Guardiões" (o convite).
struct GuardioesView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var camada: Camada
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollViewReader { rolagem in
            ScrollView {
                VStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        TituloTela("Guardiões")
                        TextoAjuda("Seus guardiões recebem o alerta em qualquer lugar onde você estiver. Por exemplo, seu filho, mesmo que ele esteja longe.")
                    }
                    Cartao("Meus guardiões") {
                        ListaDeContatos(
                            contatos: $app.guardioes,
                            vazio: "Nenhum guardião ainda.",
                            deGuardioes: true,
                            aoConvidar: { c in convidar(nome: c.nome, telefone: c.telefone) }
                        )
                        TextoAjuda("Toque em \"Convidar\" ao lado de um guardião pra mandar o convite do app pelo WhatsApp — sem redigitar nada.")
                    }
                    Cartao("Convidar alguém novo") {
                        TextoAjuda("Pra convidar quem ainda NÃO está na lista acima. A pessoa entra na sua lista de guardiões e recebe pelo WhatsApp o convite do app, com o código.")
                        BotaoM3("Convidar um guardião novo", tipo: .cheio, largo: true) { convidarNovo() }
                    }
                    TextoAjuda("Os vizinhos e as pessoas de cada lugar (casa, escritório...) ficam na aba \"Lugares\".")
                    Rodape().id("fim")
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
    }

    private func convidarNovo() {
        camada.abrir {
            DialogoContato(titulo: "Convidar guardião", confirmar: "Convidar", nome: "", telefone: "") { nome, tel in
                convidar(nome: nome, telefone: tel)
                return true
            }
        }
    }

    /// Cria o convite (código + link) e abre o WhatsApp DAQUELE número já com a mensagem pronta.
    /// Quem não está na lista já entra como guardião (recebe o SMS do alerta).
    private func convidar(nome: String, telefone: String) {
        let numero = Telefone.nacional(telefone)
        guard Telefone.celularValido(numero) else {
            camada.toast("Confira o número: precisa do DDD e do 9.")
            return
        }
        if !app.guardioes.contains(where: { Telefone.nacional($0.telefone) == numero }) {
            if app.guardioes.count < AppState.maximoDeContatos {
                app.guardioes.append(Contato(nome: nome, telefone: numero))
            }
        }
        camada.toast("Criando o convite...")
        Task {
            let r = await GuardioesConvite.convidar(
                usuarioDispositivo: app.dispositivo,
                usuarioNome: app.nome,
                guardiaoTelefone: numero
            )
            guard let r else {
                camada.toast("Não consegui criar o convite agora. Confira a internet e tente de novo.")
                return
            }
            let quem = app.nomeOk ? app.nome.trimmingCharacters(in: .whitespacesAndNewlines) : "Alguém"
            let baixe = r.link.isEmpty
                ? "Baixe o app (guardioessos.com.br) e use o código \(r.codigo)."
                : "Baixe o app e use o código \(r.codigo): \(r.link)"
            let msg = "\(quem) te escolheu como guardião no GuardiõesSOS. Se algo acontecer, você recebe o alerta com a localização. \(baixe)"
            if let url = URL(string: "https://wa.me/55\(numero)?text=\(Tela.codificar(msg))") {
                openURL(url)
            }
        }
    }
}

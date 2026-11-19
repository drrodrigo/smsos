import SwiftUI

/// Aba Protejo, igual à do Android: os convites para você ser guardião de alguém (da lista ou por
/// código) e as pessoas que você protege.
struct ProtejoView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var camada: Camada
    @State private var carregando = true
    @State private var pendentes: [GuardioesConvite.Pendente] = []
    @State private var protegidos: [GuardioesConvite.Protegido] = []

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    TituloTela("Protejo")
                    TextoAjuda("As pessoas que você protege como guardião. Quando elas acionarem o SOS, você recebe o alerta com a localização.")
                }

                if !pendentes.isEmpty {
                    Cartao("Convites pra você") {
                        ForEach(pendentes) { p in
                            HStack(spacing: 12) {
                                Text("\(p.usuarioNome.isEmpty ? "Alguém" : p.usuarioNome) quer você como guardião")
                                    .font(.system(size: 16))
                                    .foregroundStyle(Cor.onSurface)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                BotaoM3("Aceitar", tipo: .cheio) { aceitar(p.codigo) }
                            }
                        }
                    }
                }

                Cartao("Tenho um código") {
                    TextoAjuda("Recebeu um convite com um código? Digite aqui pra aceitar.")
                    BotaoM3("Aceitar com código", tipo: .cheio, largo: true) { digitarCodigo() }
                }

                Cartao("Quem eu protejo") {
                    if carregando && protegidos.isEmpty {
                        TextoAjuda("Carregando...")
                    } else if protegidos.isEmpty {
                        TextoAjuda("Você ainda não protege ninguém. Aceite um convite para começar.")
                    } else {
                        ForEach(protegidos) { p in
                            Text("🛡️ \(p.usuarioNome.isEmpty ? "Alguém" : p.usuarioNome)")
                                .font(.system(size: 16))
                                .foregroundStyle(Cor.onSurface)
                        }
                    }
                }

                Rodape()
            }
            .padding(16)
        }
        .refreshable { await carregar() }
        .task { await carregar() }
    }

    private func carregar() async {
        carregando = true
        if Demo.ativo {
            pendentes = Demo.vazio ? [] : [GuardioesConvite.Pendente(codigo: "215F67", usuarioNome: "Dona Lúcia")]
            protegidos = Demo.vazio ? [] : [GuardioesConvite.Protegido(usuarioNome: "Pedro Souza")]
            carregando = false
            return
        }
        var pend: [GuardioesConvite.Pendente] = []
        if app.telefoneOk {
            pend = await GuardioesConvite.pendentes(telefone: app.meuTelefone)
        }
        let prot = await GuardioesConvite.quemProtejo(guardiaoDispositivo: app.dispositivo)
        pendentes = pend
        protegidos = prot
        carregando = false
    }

    private func digitarCodigo() {
        camada.abrir {
            DialogoCodigo { codigo in aceitar(codigo) }
        }
    }

    private func aceitar(_ codigo: String) {
        camada.toast("Aceitando convite...")
        Task {
            let r = await GuardioesConvite.aceitar(codigo: codigo, guardiaoDispositivo: app.dispositivo)
            let quem = (r?.usuarioNome).flatMap { $0.isEmpty ? nil : $0 } ?? "essa pessoa"
            if let r {
                if !r.ok && !r.jaAceito {
                    camada.toast("Código inválido ou convite indisponível.")
                } else if r.jaAceito {
                    camada.toast("Você já protege \(quem).")
                } else {
                    camada.toast("Pronto! Agora você protege \(quem).")
                }
            } else {
                camada.toast("Não consegui aceitar. Confira a internet e tente de novo.")
            }
            await carregar()
        }
    }
}

/// O diálogo "Aceitar convite": o código que veio no convite.
struct DialogoCodigo: View {
    @EnvironmentObject var camada: Camada
    let aoAceitar: (String) -> Void
    @State private var codigo = ""

    init(aoAceitar: @escaping (String) -> Void) {
        self.aoAceitar = aoAceitar
    }

    var body: some View {
        let limpo = codigo.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        return CaixaDialogo("Aceitar convite", acoes: [
            AcaoDialogo("Cancelar"),
            AcaoDialogo("Aceitar", ativo: !limpo.isEmpty) { aoAceitar(limpo) },
        ]) {
            CampoM3("Código do convite", texto: $codigo, placeholder: "Ex.: 215F67", maiusculas: true, autoFoco: true)
        }
    }
}

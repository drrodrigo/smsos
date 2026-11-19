import SwiftUI

/// Lista de pessoas com os botões "Da agenda" e "Digitar" (ContactListEditor do Android).
/// Serve para os guardiões e para as pessoas de cada lugar. Até 10 por lista.
struct ListaDeContatos: View {
    @EnvironmentObject var camada: Camada
    @Binding var contatos: [Contato]
    let vazio: String
    let deGuardioes: Bool
    let aoConvidar: ((Contato) -> Void)?

    init(contatos: Binding<[Contato]>, vazio: String, deGuardioes: Bool = false, aoConvidar: ((Contato) -> Void)? = nil) {
        self._contatos = contatos
        self.vazio = vazio
        self.deGuardioes = deGuardioes
        self.aoConvidar = aoConvidar
    }

    private var limite: Int { AppState.maximoDeContatos }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if contatos.isEmpty {
                TextoAjuda(vazio)
            }
            ForEach(Array(contatos.enumerated()), id: \.element.id) { item in
                if item.offset > 0 { Divisor() }
                linha(item.element)
            }
            if contatos.count >= limite {
                TextoAjuda("Você chegou ao limite de \(limite) contatos nesta lista.")
            } else {
                HStack(spacing: 8) {
                    BotaoM3("Da agenda", icone: "person.fill", tipo: .contorno) { daAgenda() }
                    BotaoM3("Digitar", icone: "plus", tipo: .contorno) { novo() }
                }
            }
        }
    }

    private func linha(_ c: Contato) -> some View {
        let aviso = Telefone.aviso(c.telefone)
        let numero = Telefone.formatar(c.telefone)
        return HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                Text(c.exibicao)
                    .font(.system(size: 16))
                    .foregroundStyle(Cor.onSurface)
                    .lineLimit(1)
                Text(numero.isEmpty ? "Sem número" : numero)
                    .font(.system(size: 14))
                    .foregroundStyle(aviso == nil ? Cor.onSurfaceVariant : Cor.error)
                if let aviso {
                    Text("⚠ " + aviso)
                        .font(.system(size: 12))
                        .foregroundStyle(Cor.error)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
            .onTapGesture { editar(c) }
            if let aoConvidar {
                BotaoM3("Convidar", tipo: .texto) { aoConvidar(c) }
            }
            BotaoIcone("pencil", rotulo: "Editar \(c.exibicao)") { editar(c) }
            BotaoIcone("trash.fill", rotulo: "Apagar \(c.exibicao)") { apagar(c) }
        }
        .padding(.vertical, 2)
    }

    // MARK: - Ações

    private func novo(nome: String = "", telefone: String = "") {
        guard contatos.count < limite else {
            camada.toast("Limite de \(limite) contatos nesta lista.")
            return
        }
        camada.abrir {
            DialogoContato(
                titulo: deGuardioes ? "Novo guardião" : "Novo contato",
                confirmar: "Adicionar",
                nome: nome,
                telefone: telefone
            ) { n, t in salvar(id: nil, nome: n, telefone: t) }
        }
    }

    private func editar(_ c: Contato) {
        camada.abrir {
            DialogoContato(
                titulo: deGuardioes ? "Editar guardião" : "Editar contato",
                confirmar: "Salvar",
                nome: c.nome,
                telefone: Telefone.formatar(c.telefone)
            ) { n, t in salvar(id: c.id, nome: n, telefone: t) }
        }
    }

    /// Guarda (novo ou editado). false = não fecha o diálogo (número repetido).
    private func salvar(id: String?, nome: String, telefone: String) -> Bool {
        if contatos.contains(where: { $0.id != id && Telefone.nacional($0.telefone) == telefone }) {
            camada.toast("Esse número já está na lista")
            return false
        }
        if let id, let i = contatos.firstIndex(where: { $0.id == id }) {
            contatos[i].nome = nome
            contatos[i].telefone = telefone
        } else if contatos.count < limite {
            contatos.append(Contato(nome: nome, telefone: telefone))
        }
        return true
    }

    private func apagar(_ c: Contato) {
        camada.dialogo(
            "Apagar contato?",
            "\(c.exibicao) — \(Telefone.formatar(c.telefone)) deixa de receber o alerta.",
            acoes: [
                AcaoDialogo("Cancelar"),
                AcaoDialogo("Apagar") { contatos.removeAll { $0.id == c.id } },
            ]
        )
    }

    private func daAgenda() {
        Agenda.abrir { nome, numero in
            let tel = Telefone.nacional(numero)
            if contatos.contains(where: { Telefone.nacional($0.telefone) == tel }) {
                camada.toast("Esse número já está na lista")
            } else if contatos.count >= limite {
                camada.toast("Limite de \(limite) contatos nesta lista.")
            } else if Telefone.celularValido(tel) {
                contatos.append(Contato(nome: nome, telefone: tel))
                camada.toast("Contato adicionado ✓")
            } else {
                // Número da agenda sem DDD ou sem o 9: abre o diálogo para a pessoa corrigir.
                novo(nome: nome, telefone: numero)
            }
        }
    }
}

/// O diálogo de um contato: "Nome", "Celular com DDD", o aviso em vermelho quando o número parece
/// errado e o "Vai salvar como: (31) 99999-8888" (ContactDialog do Android).
struct DialogoContato: View {
    @EnvironmentObject var camada: Camada
    let titulo: String
    let confirmar: String
    let aoConfirmar: (String, String) -> Bool
    @State private var nome: String
    @State private var tel: String
    private let editando: Bool

    /// aoConfirmar recebe o nome e o número já padronizado (11 dígitos); devolve true para fechar.
    init(titulo: String, confirmar: String, nome: String, telefone: String,
         aoConfirmar: @escaping (String, String) -> Bool) {
        self.titulo = titulo
        self.confirmar = confirmar
        self.aoConfirmar = aoConfirmar
        self.editando = !nome.isEmpty
        _nome = State(initialValue: nome)
        _tel = State(initialValue: telefone)
    }

    private var valido: Bool { Telefone.celularValido(tel) }

    private var salvaraComo: String? {
        guard valido else { return nil }
        let f = Telefone.formatar(tel)
        return f == tel.trimmingCharacters(in: .whitespaces) ? nil : f
    }

    var body: some View {
        CaixaDialogo(titulo, acoes: [
            AcaoDialogo("Cancelar"),
            AcaoDialogo(confirmar, ativo: valido, fecha: false) {
                let n = nome.trimmingCharacters(in: .whitespacesAndNewlines)
                if aoConfirmar(n, Telefone.nacional(tel)) { camada.fechar() }
            },
        ]) {
            CampoM3("Nome", texto: $nome, palavras: true, autoFoco: !editando)
            CampoM3("Celular com DDD", texto: $tel, placeholder: "(31) 99999-8888", teclado: .phonePad,
                    erro: Telefone.aviso(tel), autoFoco: editando)
            if let salvaraComo {
                Text("Vai salvar como: \(salvaraComo)")
                    .font(.system(size: 12))
                    .foregroundStyle(Cor.primary)
            }
        }
    }
}

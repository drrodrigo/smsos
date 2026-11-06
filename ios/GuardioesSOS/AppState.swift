import Foundation
import Combine

/// Estado do app, guardado no aparelho (UserDefaults). Milestone 1: nome, um contato e se já configurou.
final class AppState: ObservableObject {
    @Published var nome: String { didSet { d.set(nome, forKey: K.nome) } }
    @Published var contatoNome: String { didSet { d.set(contatoNome, forKey: K.contatoNome) } }
    @Published var contatoTelefone: String { didSet { d.set(contatoTelefone, forKey: K.contatoTelefone) } }
    @Published var configurado: Bool { didSet { d.set(configurado, forKey: K.configurado) } }

    private let d = UserDefaults.standard
    private enum K {
        static let nome = "nome"
        static let contatoNome = "contatoNome"
        static let contatoTelefone = "contatoTelefone"
        static let configurado = "configurado"
    }

    init() {
        nome = d.string(forKey: K.nome) ?? ""
        contatoNome = d.string(forKey: K.contatoNome) ?? ""
        contatoTelefone = d.string(forKey: K.contatoTelefone) ?? ""
        configurado = d.bool(forKey: K.configurado)
    }

    /// Cadastro mínimo válido: nome preenchido e um telefone com pelo menos 10 dígitos (DDD + número).
    var cadastroValido: Bool {
        !nome.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && Telefone.limpar(contatoTelefone).count >= 10
    }
}

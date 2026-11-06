import Foundation
import Combine

/// Estado do app, guardado no aparelho (UserDefaults). Identidade = dispositivo (UUID) + telefone.
final class AppState: ObservableObject {
    @Published var nome: String { didSet { d.set(nome, forKey: K.nome) } }
    @Published var meuTelefone: String { didSet { d.set(meuTelefone, forKey: K.meuTelefone) } }
    @Published var perfil: Perfil { didSet { d.set(perfil.rawValue, forKey: K.perfil) } }
    @Published var contatos: [Contato] { didSet { salvarContatos() } }
    @Published var configurado: Bool { didSet { d.set(configurado, forKey: K.configurado) } }
    @Published var modoTeste: Bool { didSet { d.set(modoTeste, forKey: K.modoTeste) } }

    /// Id deste aparelho, gerado uma vez. É a identidade da conta na rede de guardiões.
    let dispositivo: String

    private let d = UserDefaults.standard
    private enum K {
        static let nome = "nome"
        static let meuTelefone = "meuTelefone"
        static let perfil = "perfil"
        static let contatos = "contatos"
        static let configurado = "configurado"
        static let modoTeste = "modoTeste"
        static let dispositivo = "dispositivo"
    }

    init() {
        let def = UserDefaults.standard
        nome = def.string(forKey: K.nome) ?? ""
        meuTelefone = def.string(forKey: K.meuTelefone) ?? ""
        perfil = Perfil(rawValue: def.string(forKey: K.perfil) ?? "") ?? .seguranca
        configurado = def.bool(forKey: K.configurado)
        modoTeste = def.object(forKey: K.modoTeste) as? Bool ?? true

        if let existente = def.string(forKey: K.dispositivo), !existente.isEmpty {
            dispositivo = existente
        } else {
            let novo = UUID().uuidString
            def.set(novo, forKey: K.dispositivo)
            dispositivo = novo
        }

        if let data = def.data(forKey: K.contatos),
           let lista = try? JSONDecoder().decode([Contato].self, from: data) {
            contatos = lista
        } else {
            contatos = []
        }
    }

    private func salvarContatos() {
        if let data = try? JSONEncoder().encode(contatos) { d.set(data, forKey: K.contatos) }
    }

    /// Cadastro mínimo válido: nome preenchido e um telefone próprio com pelo menos 10 dígitos.
    var cadastroValido: Bool {
        !nome.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && Telefone.limpar(meuTelefone).count >= 10
    }

    var telefonesDosContatos: [String] { contatos.map { $0.telefone } }

    func adicionarContato(_ c: Contato) {
        guard contatos.count < 10 else { return }
        contatos.append(c)
    }

    func removerContato(_ c: Contato) {
        contatos.removeAll { $0.id == c.id }
    }

    /// Registra o lead no servidor (em segundo plano).
    func registrarLead() {
        Registrar.registrar(dispositivo: dispositivo, nome: nome, telefone: meuTelefone)
    }
}

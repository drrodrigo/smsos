import Foundation
import Combine

/// Estado do app, guardado no aparelho (UserDefaults). Identidade = dispositivo (UUID) + telefone.
/// O que guarda é o mesmo do Android: nome, guardiões, lugares, ajustes e o histórico (só aqui).
final class AppState: ObservableObject {
    @Published var nome: String { didSet { gravar(nome, K.nome) } }
    @Published var meuTelefone: String { didSet { gravar(meuTelefone, K.meuTelefone) } }
    @Published var perfil: Perfil { didSet { gravar(perfil.rawValue, K.perfil) } }
    /// Os dois SOS (Segurança e Saúde) na tela Início. Desligado: um botão só, do perfil escolhido.
    @Published var dualSos: Bool { didSet { gravar(dualSos, K.dualSos) } }
    /// Quantos segundos segurar o botão (3 a 30; padrão 5).
    @Published var segundos: Int { didSet { gravar(segundos, K.segundos) } }
    /// Apagar o histórico sozinho depois de N dias (0 = nunca).
    @Published var retencao: Int { didSet { gravar(retencao, K.retencao) } }
    @Published var modoTeste: Bool { didSet { gravar(modoTeste, K.modoTeste) } }
    @Published var configurado: Bool { didSet { gravar(configurado, K.configurado) } }
    /// Os guardiões: recebem o alerta em qualquer lugar. (A chave "contatos" vem da versão anterior.)
    @Published var guardioes: [Contato] { didSet { gravarJSON(guardioes, K.contatos) } }
    @Published var lugares: [Lugar] { didSet { gravarJSON(lugares, K.lugares) } }
    @Published var historico: [RegistroAlerta] { didSet { gravarJSON(historico, K.historico) } }
    @Published var ativo: AlertaAtivo? { didSet { gravarJSON(ativo, K.ativo) } }
    /// A localização está liberada (o checklist e o cartão de status dependem disso).
    @Published var localOk: Bool

    /// Id deste aparelho, gerado uma vez. É a identidade da conta na rede de guardiões.
    let dispositivo: String
    let local: Localizacao

    static let maximoDeContatos = 10

    private let d = UserDefaults.standard
    private enum K {
        static let nome = "nome"
        static let meuTelefone = "meuTelefone"
        static let perfil = "perfil"
        static let dualSos = "dualSos"
        static let segundos = "segundos"
        static let retencao = "retencao"
        static let contatos = "contatos"
        static let lugares = "lugares"
        static let historico = "historico"
        static let ativo = "ativo"
        static let configurado = "configurado"
        static let modoTeste = "modoTeste"
        static let dispositivo = "dispositivo"
    }

    init() {
        let l = Localizacao()
        local = l

        if Demo.ativo {
            let demo = Demo.dados()
            nome = demo.nome
            meuTelefone = demo.telefone
            perfil = .seguranca
            dualSos = true
            segundos = 5
            retencao = 7
            configurado = true
            modoTeste = Demo.teste
            dispositivo = "demo"
            guardioes = demo.guardioes
            lugares = demo.lugares
            historico = demo.historico
            ativo = nil
            localOk = !Demo.vazio
            return
        }

        let def = UserDefaults.standard
        nome = def.string(forKey: K.nome) ?? ""
        meuTelefone = def.string(forKey: K.meuTelefone) ?? ""
        perfil = Perfil(rawValue: def.string(forKey: K.perfil) ?? "") ?? .seguranca
        dualSos = def.object(forKey: K.dualSos) as? Bool ?? true
        let s = def.object(forKey: K.segundos) as? Int ?? 5
        segundos = min(30, max(3, s))
        retencao = def.object(forKey: K.retencao) as? Int ?? 7
        configurado = def.bool(forKey: K.configurado)
        modoTeste = def.object(forKey: K.modoTeste) as? Bool ?? true
        guardioes = AppState.lerJSON([Contato].self, K.contatos) ?? []
        lugares = AppState.lerJSON([Lugar].self, K.lugares) ?? []
        historico = AppState.lerJSON([RegistroAlerta].self, K.historico) ?? []
        ativo = AppState.lerJSON(AlertaAtivo.self, K.ativo)
        localOk = l.autorizado

        if let existente = def.string(forKey: K.dispositivo), !existente.isEmpty {
            dispositivo = existente
        } else {
            let novo = UUID().uuidString
            def.set(novo, forKey: K.dispositivo)
            dispositivo = novo
        }

        l.aoMudarPermissao = { [weak self] in self?.atualizarLocal() }
        limparHistoricoVelho()
    }

    // MARK: - Guardar

    private func gravar(_ valor: Any, _ chave: String) {
        guard !Demo.ativo else { return }
        d.set(valor, forKey: chave)
    }

    private func gravarJSON<T: Encodable>(_ valor: T?, _ chave: String) {
        guard !Demo.ativo else { return }
        guard let valor else {
            d.removeObject(forKey: chave)
            return
        }
        if let data = try? JSONEncoder().encode(valor) { d.set(data, forKey: chave) }
    }

    private static func lerJSON<T: Decodable>(_ tipo: T.Type, _ chave: String) -> T? {
        guard let data = UserDefaults.standard.data(forKey: chave) else { return nil }
        return try? JSONDecoder().decode(tipo, from: data)
    }

    // MARK: - O que as telas perguntam

    /// No teste quando o modo teste está ligado ou quando esta versão não tem o envio configurado.
    var emTeste: Bool { modoTeste || !Gateway.configurado }

    var nomeOk: Bool { !nome.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    var telefoneOk: Bool { Telefone.celularValido(meuTelefone) }

    /// Os guardiões + as pessoas de todos os lugares.
    var totalContatos: Int { guardioes.count + lugares.reduce(0) { $0 + $1.contatos.count } }

    /// Quantos itens da Configuração ainda faltam.
    var pendentes: Int { [nomeOk, totalContatos > 0, localOk, telefoneOk].filter { !$0 }.count }

    /// Cadastro mínimo válido: nome preenchido e um celular próprio válido.
    var cadastroValido: Bool { nomeOk && telefoneOk }

    func atualizarLocal() {
        localOk = Demo.ativo ? !Demo.vazio : local.autorizado
    }

    // MARK: - Histórico

    func registrar(_ r: RegistroAlerta) {
        historico.insert(r, at: 0)
        if historico.count > 50 { historico = Array(historico.prefix(50)) }
    }

    /// O histórico some sozinho depois dos dias escolhidos em Ajustes (0 = nunca).
    func limparHistoricoVelho() {
        guard retencao > 0 else { return }
        let limite = Date().addingTimeInterval(-Double(retencao) * 86_400)
        if historico.contains(where: { $0.em < limite }) {
            historico.removeAll { $0.em < limite }
        }
    }

    /// Registra o lead no servidor (em segundo plano).
    func registrarLead() {
        Registrar.registrar(dispositivo: dispositivo, nome: nome, telefone: meuTelefone)
    }
}

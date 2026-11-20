import Foundation

/// O combinado entre o app do iPhone e o do Apple Watch (WatchConnectivity). Este arquivo entra
/// nos DOIS apps, para os dois lados falarem exatamente a mesma língua.
///
/// 1. O relógio pede o SOS com `sendMessage` (o iPhone acorda em segundo plano para atender):
///        {"acao": "sos", "perfil": "seguranca"|"saude", "id": "<uuid>", "lat": .., "lng": .., "precisao": ..}
///    (a posição é a do relógio, quando ele conseguiu pegar) e o iPhone responde:
///        {"id": "<uuid>", "ok": true|false, "teste": true|false, "contatos": N, "erro": "..."}
///
/// 2. O iPhone manda para o relógio (`updateApplicationContext`) o que ele precisa para disparar
///    SOZINHO, pela internet, quando o iPhone não está por perto:
///        {"contexto": <JSON de ContextoDoRelogio>}
///
/// 3. Quando o relógio dispara sozinho, ele conta ao iPhone depois (`transferUserInfo`, que chega
///    mesmo que demore), para o histórico e o "Estou bem — avisar todos" do iPhone:
///        {"acao": "registro", "registro": <JSON de RegistroDoRelogio>}
enum ProtocoloRelogio {
    enum Chave {
        static let acao = "acao"
        static let perfil = "perfil"
        static let id = "id"
        static let lat = "lat"
        static let lng = "lng"
        static let precisao = "precisao"
        static let ok = "ok"
        static let teste = "teste"
        static let contatos = "contatos"
        static let erro = "erro"
        static let contexto = "contexto"
        static let registro = "registro"
    }

    enum Acao {
        static let sos = "sos"
        static let registro = "registro"
    }

    enum Erro {
        /// Ninguém para avisar: falta cadastrar guardiões no iPhone.
        static let semContatos = "sem-contatos"
        /// O iPhone tentou, mas a nossa estrutura não aceitou nenhum número (sem internet?).
        static let naoSaiu = "nao-saiu"
        /// O pedido veio sem a ação certa.
        static let pedidoInvalido = "pedido-invalido"
    }
}

/// O que o iPhone sincroniza para o relógio: o nome que vai na mensagem, quem recebe e o modo teste.
/// Com isso o relógio monta o MESMO texto e escolhe as MESMAS pessoas que o iPhone escolheria.
struct ContextoDoRelogio: Codable, Equatable {
    var nome: String
    /// O tipo escolhido no iPhone (o relógio usa como "SOS rápido" até a pessoa trocar no relógio).
    var perfil: Perfil
    var guardioes: [Contato]
    var lugares: [Lugar]
    /// Modo teste ligado no iPhone (ou versão sem a chave de envio): o relógio também não envia.
    var emTeste: Bool
    /// O cadastro do iPhone está feito.
    var configurado: Bool

    var totalContatos: Int { guardioes.count + lugares.reduce(0) { $0 + $1.contatos.count } }

    func codificado() -> Data? { try? JSONEncoder().encode(self) }

    static func decodificar(_ data: Data?) -> ContextoDoRelogio? {
        guard let data else { return nil }
        return try? JSONDecoder().decode(ContextoDoRelogio.self, from: data)
    }
}

/// A resposta do iPhone ao pedido de SOS do relógio.
struct RespostaSOS: Equatable {
    var id: String
    /// true: o alerta saiu (ou, no modo teste, o teste funcionou).
    var ok: Bool
    var teste: Bool
    /// Quantos foram avisados (no teste: quantos seriam).
    var contatos: Int
    var erro: String?

    init(id: String, ok: Bool, teste: Bool, contatos: Int, erro: String? = nil) {
        self.id = id
        self.ok = ok
        self.teste = teste
        self.contatos = contatos
        self.erro = erro
    }

    init(dicionario d: [String: Any]) {
        typealias K = ProtocoloRelogio.Chave
        id = d[K.id] as? String ?? ""
        ok = d[K.ok] as? Bool ?? false
        teste = d[K.teste] as? Bool ?? false
        contatos = (d[K.contatos] as? Int) ?? Int((d[K.contatos] as? Double) ?? 0)
        erro = d[K.erro] as? String
    }

    var dicionario: [String: Any] {
        typealias K = ProtocoloRelogio.Chave
        var d: [String: Any] = [K.id: id, K.ok: ok, K.teste: teste, K.contatos: contatos]
        if let erro { d[K.erro] = erro }
        return d
    }
}

/// O alerta que o relógio disparou sozinho (pela internet dele), contado depois ao iPhone.
struct RegistroDoRelogio: Codable, Equatable {
    var id: String
    var em: Date
    var perfil: Perfil
    var teste: Bool
    var total: Int
    var enviados: Int
    /// Os telefones (11 dígitos) que receberam: o iPhone usa no "Estou bem — avisar todos".
    var telefones: [String]
    var lugar: String?
    var latitude: Double?
    var longitude: Double?
}

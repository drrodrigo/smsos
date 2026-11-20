import Foundation

/// O que o relógio guarda nele mesmo (UserDefaults do relógio): os ajustes da pessoa, o que o iPhone
/// sincronizou (para disparar sozinho) e o último alerta. No modo demonstração (-demo) nada é gravado.
enum Guardado {
    private static let d = UserDefaults.standard

    private enum K {
        static let contagem = "contagem"
        static let discreto = "discreto"
        static let tipoRapido = "tipoRapido"
        static let contexto = "contexto"
        static let contextoEm = "contextoEm"
        static let ultimo = "ultimo"
    }

    /// Os segundos da contagem antes de disparar: 0 (na hora), 3 ou 5. Padrão 3.
    static let opcoesDeContagem = [0, 3, 5]

    static var contagem: Int {
        get {
            let s = d.object(forKey: K.contagem) as? Int ?? 3
            return opcoesDeContagem.contains(s) ? s : 3
        }
        set { if !Demo.ativo { d.set(newValue, forKey: K.contagem) } }
    }

    /// Modo discreto: tela preta, sem número e sem "SOS". Ligado de fábrica.
    static var discreto: Bool {
        get { d.object(forKey: K.discreto) as? Bool ?? true }
        set { if !Demo.ativo { d.set(newValue, forKey: K.discreto) } }
    }

    /// O tipo do SOS rápido (o que dispara ao abrir o app). nil = ainda não escolheu no relógio
    /// (vale o perfil escolhido no iPhone).
    static var tipoRapido: Perfil? {
        get { Perfil(rawValue: d.string(forKey: K.tipoRapido) ?? "") }
        set { if !Demo.ativo { d.set(newValue?.rawValue, forKey: K.tipoRapido) } }
    }

    static var contexto: ContextoDoRelogio? {
        ContextoDoRelogio.decodificar(d.data(forKey: K.contexto))
    }

    static var contextoEm: Date? {
        d.object(forKey: K.contextoEm) as? Date
    }

    static func salvar(contexto: ContextoDoRelogio) {
        guard !Demo.ativo, let dados = contexto.codificado() else { return }
        d.set(dados, forKey: K.contexto)
        d.set(Date(), forKey: K.contextoEm)
    }

    static var ultimo: UltimoAlerta? {
        get {
            guard let dados = d.data(forKey: K.ultimo) else { return nil }
            return try? JSONDecoder().decode(UltimoAlerta.self, from: dados)
        }
        set {
            guard !Demo.ativo else { return }
            if let newValue, let dados = try? JSONEncoder().encode(newValue) {
                d.set(dados, forKey: K.ultimo)
            } else {
                d.removeObject(forKey: K.ultimo)
            }
        }
    }
}

/// O último SOS disparado pelo relógio (a linha de status do menu).
struct UltimoAlerta: Codable, Equatable {
    var em: Date
    var perfil: Perfil
    var ok: Bool
    var teste: Bool
    var contatos: Int
    var caminho: Disparo.Caminho
}

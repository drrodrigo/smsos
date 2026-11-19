import Foundation

/// Modo demonstração: ligado só quando o app é aberto com o argumento -demo (nos prints).
/// Nunca liga para um usuário de verdade. Serve para as telas aparecerem cheias e sem pedir permissão.
///
/// Argumentos (todos só valem junto com -demo):
///   -tab <inicio|guardioes|lugares|protejo|ajustes>   abre direto nessa aba
///   -abrir <configuracao,testar,historico>            abre esses cartões da tela Início
///   -mostrar <como-chega|sirene|novo-guardiao|lugar|sem-guardioes>  abre um diálogo/tela por cima
///   -rolar                                            rola a tela até o fim
///   -teste                                            liga o modo teste
///   -vazio                                            sem guardiões, sem lugares e sem localização
///   -segurando                                        os botões de SOS com a barra enchendo
///   -ativo                                            com um alerta que acabou de sair
///   -cadastro                                         a tela do primeiro uso
enum Demo {
    private static let args = ProcessInfo.processInfo.arguments

    static let ativo = args.contains("-demo")
    static let teste = ativo && args.contains("-teste")
    static let vazio = ativo && args.contains("-vazio")
    static let rolar = ativo && args.contains("-rolar")
    static let segurando = ativo && args.contains("-segurando")
    static let alertaAtivo = ativo && args.contains("-ativo")
    static let cadastro = ativo && args.contains("-cadastro")

    private static func valor(_ chave: String) -> String? {
        guard ativo, let i = args.firstIndex(of: chave), i + 1 < args.count else { return nil }
        return args[i + 1]
    }

    /// Aba inicial pedida por -tab <nome> (para os prints abrirem direto na tela certa).
    static var abaInicial: Aba {
        let pedida = ProcessInfo.processInfo.arguments
        guard let i = pedida.firstIndex(of: "-tab"), i + 1 < pedida.count else { return .inicio }
        switch pedida[i + 1] {
        case "guardioes", "contatos": return .guardioes
        case "lugares": return .lugares
        case "protejo": return .protejo
        case "ajustes": return .ajustes
        default: return .inicio
        }
    }

    static func abrir(_ cartao: String) -> Bool {
        (valor("-abrir") ?? "").split(separator: ",").contains { $0 == cartao }
    }

    static var mostrar: String? { valor("-mostrar") }

    struct Dados {
        let nome: String
        let telefone: String
        let guardioes: [Contato]
        let lugares: [Lugar]
        let historico: [RegistroAlerta]
    }

    /// Os dados de exemplo (nomes e números fictícios).
    static func dados() -> Dados {
        if vazio {
            return Dados(nome: "Ana Souza", telefone: "11988887777", guardioes: [], lugares: [], historico: [])
        }
        let agora = Date()
        let casa = Lugar(
            nome: "Casa",
            latitude: -23.55052,
            longitude: -46.63331,
            precisao: 12,
            raio: 200,
            contatos: [
                Contato(nome: "Vizinha do 12", telefone: "11988880003"),
                Contato(nome: "Portaria", telefone: "11988880004"),
            ]
        )
        let trabalho = Lugar(nome: "Trabalho", raio: 300, contatos: [
            Contato(nome: "Recepção", telefone: "11988880005"),
        ])
        return Dados(
            nome: "Ana Souza",
            telefone: "11988887777",
            guardioes: [
                Contato(nome: "Carlos (filho)", telefone: "11988880001"),
                Contato(nome: "Marina (irmã)", telefone: "11988880002"),
            ],
            lugares: [casa, trabalho],
            historico: [
                RegistroAlerta(
                    em: agora.addingTimeInterval(-26 * 3600),
                    teste: true, perfil: .seguranca, total: 4, enviados: 0, lugar: "Casa",
                    latitude: -23.55052, longitude: -46.63331
                ),
                RegistroAlerta(
                    em: agora.addingTimeInterval(-3 * 86_400),
                    teste: false, perfil: .saude, total: 2, enviados: 2, lugar: nil,
                    latitude: -23.56120, longitude: -46.65590
                ),
            ]
        )
    }
}

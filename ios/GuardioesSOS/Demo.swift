import Foundation

/// Modo demonstração: ligado só quando o app é aberto com o argumento -demo (nos prints da loja).
/// Nunca liga para um usuário de verdade. Serve para as telas aparecerem "ativas" e com exemplos.
enum Demo {
    static let ativo = ProcessInfo.processInfo.arguments.contains("-demo")

    /// Aba inicial pedida por -tab <nome> (para os prints abrirem direto na tela certa).
    static var abaInicial: Aba {
        let args = ProcessInfo.processInfo.arguments
        guard let i = args.firstIndex(of: "-tab"), i + 1 < args.count else { return .inicio }
        switch args[i + 1] {
        case "contatos": return .contatos
        case "guardioes": return .guardioes
        case "protejo": return .protejo
        case "ajustes": return .ajustes
        default: return .inicio
        }
    }
}

import Foundation

/// Modo demonstração do relógio: ligado só quando o app abre com o argumento -demo (nos prints do
/// simulador). Nunca liga para um usuário de verdade: não conta, não vibra, não envia nada e não
/// grava nada; só mostra a tela pedida, com dados de exemplo.
///
/// Argumentos (todos só valem junto com -demo):
///   -tela <contando|enviando|saiu|nao-saiu|menu|ajustes>   a tela que aparece
///   -visivel                                               modo discreto DESLIGADO
///   -teste                                                 o iPhone está no modo teste
///   -vazio                                                 sem guardiões (o iPhone ainda não mandou)
///   -rolar                                                 rola a tela até o fim
///   -contagem <0|3|5>                                      a contagem escolhida
///   -saude                                                 o SOS rápido é o de Saúde
enum Demo {
    private static let args = ProcessInfo.processInfo.arguments

    static let ativo = args.contains("-demo")
    static let visivel = ativo && args.contains("-visivel")
    static let teste = ativo && args.contains("-teste")
    static let vazio = ativo && args.contains("-vazio")
    static let rolar = ativo && args.contains("-rolar")
    static let saude = ativo && args.contains("-saude")

    private static func valor(_ chave: String) -> String? {
        guard ativo, let i = args.firstIndex(of: chave), i + 1 < args.count else { return nil }
        return args[i + 1]
    }

    static var tela: String { valor("-tela") ?? "menu" }

    static var contagem: Int {
        let n = Int(valor("-contagem") ?? "") ?? 3
        return Guardado.opcoesDeContagem.contains(n) ? n : 3
    }

    /// O que o iPhone teria sincronizado (nomes e números fictícios, os mesmos do -demo do iPhone).
    static var contexto: ContextoDoRelogio? {
        if vazio { return nil }
        return ContextoDoRelogio(
            nome: "Ana Souza",
            perfil: .seguranca,
            guardioes: [
                Contato(nome: "Carlos (filho)", telefone: "11988880001"),
                Contato(nome: "Marina (irmã)", telefone: "11988880002"),
            ],
            lugares: [
                Lugar(nome: "Casa", latitude: -23.55052, longitude: -46.63331, precisao: 12, raio: 200, contatos: [
                    Contato(nome: "Vizinha do 12", telefone: "11988880003"),
                    Contato(nome: "Portaria", telefone: "11988880004"),
                ]),
            ],
            emTeste: teste,
            configurado: true
        )
    }
}

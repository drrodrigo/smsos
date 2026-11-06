import Foundation

/// A chave do gateway é preenchida NO BUILD, a partir do segredo GUARDIOES_GATEWAY_KEY do GitHub.
/// Fica vazia aqui de propósito — a chave NUNCA é versionada. Vazia = o app roda em modo teste.
enum Secrets {
    static let gatewayKey = ""
}

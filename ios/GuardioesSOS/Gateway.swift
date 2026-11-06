import Foundation

/// Envia o alerta pela NOSSA estrutura (função smsos-alerta -> SMS Market), pela internet.
/// Igual ao GatewaySender do Android.
enum Gateway {
    static var configurado: Bool { Demo.ativo || Backend.configurado }

    /// Manda o mesmo texto para vários telefones (até 15 por chamada). true se a função aceitou.
    static func enviar(telefones: [String], texto: String) async -> Bool {
        let envios: [[String: Any]] = telefones
            .map { Telefone.limpar($0) }
            .filter { !$0.isEmpty }
            .map { ["telefone": $0, "texto": texto] }
        guard !envios.isEmpty else { return false }
        guard let obj = await Backend.postObj("smsos-alerta", ["envios": envios]) else { return false }
        return (obj["ok"] as? Bool) ?? false
    }
}

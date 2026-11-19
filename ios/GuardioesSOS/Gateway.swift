import Foundation

/// Envia o alerta pela NOSSA estrutura (função smsos-alerta -> SMS Market), pela internet.
/// Igual ao GatewaySender do Android: manda os 11 dígitos de cada número, em lotes de até 15.
enum Gateway {
    static var configurado: Bool { Demo.ativo || Backend.configurado }

    private static let maximoPorChamada = 15

    /// Manda o mesmo texto para vários telefones. Devolve os números (11 dígitos) que a função aceitou.
    static func enviar(telefones: [String], texto: String) async -> Set<String> {
        var numeros: [String] = []
        for t in telefones {
            let n = Telefone.nacional(t)
            if !n.isEmpty && !numeros.contains(n) { numeros.append(n) }
        }
        var aceitos = Set<String>()
        var inicio = 0
        while inicio < numeros.count {
            let fim = min(inicio + maximoPorChamada, numeros.count)
            let lote = Array(numeros[inicio..<fim])
            inicio = fim
            let envios: [[String: Any]] = lote.map { ["telefone": $0, "texto": texto] }
            guard let obj = await Backend.postObj("smsos-alerta", ["envios": envios]) else { continue }
            if let resultados = obj["resultados"] as? [[String: Any]], !resultados.isEmpty {
                for r in resultados where (r["ok"] as? Bool) == true {
                    if let t = r["telefone"] as? String {
                        let n = Telefone.nacional(t)
                        if lote.contains(n) { aceitos.insert(n) }
                    }
                }
            } else if (obj["ok"] as? Bool) == true {
                lote.forEach { aceitos.insert($0) }
            }
        }
        return aceitos
    }
}

import Foundation

enum Telefone {
    /// Só os dígitos (igual ao PhoneNumbers.clean do Android).
    static func limpar(_ s: String) -> String { s.filter { $0.isNumber } }

    /// Acrescenta o DDI 55 quando é número nacional (10 ou 11 dígitos). Igual ao PhoneNumbers.normalize.
    static func normalizar(_ s: String) -> String {
        let d = limpar(s)
        if d.count == 10 || d.count == 11 { return "55" + d }
        return d
    }
}

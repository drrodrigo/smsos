import Foundation

enum Telefone {
    /// Só os dígitos (igual ao PhoneNumbers.clean do Android; a função do servidor acrescenta o DDI 55 quando falta).
    static func limpar(_ s: String) -> String {
        s.filter { $0.isNumber }
    }
}

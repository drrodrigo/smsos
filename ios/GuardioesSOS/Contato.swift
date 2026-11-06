import Foundation

/// Um contato de emergência (quem recebe o SOS).
struct Contato: Identifiable, Codable, Equatable {
    var id: String = UUID().uuidString
    var nome: String
    var telefone: String

    var exibicao: String {
        let n = nome.trimmingCharacters(in: .whitespacesAndNewlines)
        return n.isEmpty ? telefone : n
    }
}

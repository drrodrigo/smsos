import Foundation
import CoreLocation

/// Uma pessoa que recebe o SOS (um guardião, ou alguém de um lugar).
struct Contato: Identifiable, Codable, Equatable {
    var id: String = UUID().uuidString
    var nome: String
    var telefone: String

    var exibicao: String {
        let n = nome.trimmingCharacters(in: .whitespacesAndNewlines)
        return n.isEmpty ? "Sem nome" : n
    }
}

/// Um lugar onde a pessoa costuma estar (casa, trabalho...). Quando o SOS sai de dentro da área,
/// as pessoas do lugar também são avisadas (igual ao Place do Android).
struct Lugar: Identifiable, Codable, Equatable {
    var id: String = UUID().uuidString
    var nome: String
    var latitude: Double? = nil
    var longitude: Double? = nil
    /// Precisão do GPS quando o lugar foi marcado, em metros.
    var precisao: Int? = nil
    /// Tamanho da área, em metros (50 a 1000; padrão 200).
    var raio: Int = 200
    var contatos: [Contato] = []

    var temLocal: Bool { latitude != nil && longitude != nil }
}

/// Um registro do histórico (fica só neste aparelho).
struct RegistroAlerta: Identifiable, Codable, Equatable {
    var id: String = UUID().uuidString
    var em: Date
    var teste: Bool
    var perfil: Perfil
    /// Quantas pessoas receberiam / deveriam receber.
    var total: Int
    /// Quantas a nossa estrutura aceitou (no teste, 0).
    var enviados: Int
    var lugar: String?
    var latitude: Double?
    var longitude: Double?
}

/// O alerta que acabou de sair: a tela Início mostra "Estou bem — avisar todos".
struct AlertaAtivo: Codable, Equatable {
    var em: Date
    /// Os telefones (11 dígitos) que receberam o alerta.
    var telefones: [String]
}

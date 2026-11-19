import Foundation

/// Para quem é o alerta: muda o texto que chega.
enum Perfil: String, Codable {
    case seguranca
    case saude

    var titulo: String { self == .saude ? "Saúde" : "Segurança" }
    var rotulo: String { self == .saude ? "❤️ Saúde" : "🛡️ Segurança" }
    var descricao: String {
        self == .saude
            ? "Para idoso ou quem tem problema de saúde. A mensagem diz que você pode estar passando mal."
            : "Para assalto e situações de perigo. A mensagem diz que você pode estar em perigo."
    }
}

/// Monta os textos do SMS, iguais ao MessageBuilder do Android (sem acento, cabe num SMS de 160 letras).
enum Mensagem {
    /// Posição de exemplo (Praça Sete, centro de BH) — só para mostrar como a mensagem chega.
    static let exemploLatitude = -19.9191
    static let exemploLongitude = -43.9386

    static func alerta(nome: String, perfil: Perfil, latitude: Double?, longitude: Double?, data: Date = Date()) -> String {
        let mapa: String
        if let lat = latitude, let lon = longitude {
            mapa = linkMapa(lat, lon)
        } else {
            mapa = "localizacao indisponivel"
        }

        let corpo: String
        if perfil == .saude {
            corpo = "ALERTA DE SAUDE! \(nomeOuPadrao(nome)) apertou o botao e pode estar passando mal. Local: \(mapa) - \(dataHora(data))"
        } else {
            corpo = "ALERTA! \(nomeOuPadrao(nome)) acionou o botao de emergencia e pode estar em perigo. Local: \(mapa) - \(dataHora(data))"
        }
        return semAcento(corpo)
    }

    /// O "está tudo bem" depois do alerta (MessageBuilder.allClear).
    static func tudoBem(nome: String, data: Date = Date()) -> String {
        semAcento("\(nomeOuPadrao(nome)) avisou que esta tudo bem. Pode desconsiderar o alerta de emergencia. - \(soHora(data))")
    }

    static func linkMapa(_ lat: Double, _ lon: Double) -> String {
        String(format: "https://maps.google.com/?q=%.6f,%.6f", locale: Locale(identifier: "en_US_POSIX"), lat, lon)
    }

    static func nomeOuPadrao(_ nome: String) -> String {
        let t = nome.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? "Um contato seu" : t
    }

    /// "dd/MM HH:mm"
    static func dataHora(_ data: Date) -> String {
        formatar(data, "dd/MM HH:mm")
    }

    /// "HH:mm"
    static func soHora(_ data: Date) -> String {
        formatar(data, "HH:mm")
    }

    private static func formatar(_ data: Date, _ padrao: String) -> String {
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "pt_BR")
        fmt.dateFormat = padrao
        return fmt.string(from: data)
    }

    static func semAcento(_ s: String) -> String {
        s.folding(options: .diacriticInsensitive, locale: Locale(identifier: "pt_BR"))
    }
}

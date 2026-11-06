import Foundation

/// Monta o texto do alerta, igual ao MessageBuilder do Android (sem acento, para caber num SMS de 160 letras).
enum Mensagem {
    static func alerta(nome: String, latitude: Double?, longitude: Double?, data: Date = Date()) -> String {
        let t = nome.trimmingCharacters(in: .whitespacesAndNewlines)
        let nomeFinal = t.isEmpty ? "Um contato seu" : t

        let mapa: String
        if let lat = latitude, let lon = longitude {
            mapa = String(format: "https://maps.google.com/?q=%.6f,%.6f", lat, lon)
        } else {
            mapa = "localizacao indisponivel"
        }

        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "pt_BR")
        fmt.dateFormat = "dd/MM HH:mm"
        let hora = fmt.string(from: data)

        let texto = "ALERTA! \(nomeFinal) acionou o botao de emergencia e pode estar em perigo. Local: \(mapa) - \(hora)"
        return semAcento(texto)
    }

    static func semAcento(_ s: String) -> String {
        s.folding(options: .diacriticInsensitive, locale: Locale(identifier: "pt_BR"))
    }
}

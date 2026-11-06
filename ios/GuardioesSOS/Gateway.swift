import Foundation

/// Envia o alerta pela NOSSA estrutura (função smsos-alerta -> SMS Market), pela internet.
/// O app não usa o chip nem guarda a credencial do SMS Market: só a chave do app, que serve
/// apenas para falar com a nossa função. Igual ao GatewaySender do Android.
enum Gateway {
    static let url = URL(string: "https://ymszgqpgxtprkqxjhtwb.supabase.co/functions/v1/smsos-alerta")!

    static var chave: String { Secrets.gatewayKey }
    static var configurado: Bool { !chave.isEmpty }

    /// Manda um texto para um telefone. Retorna true se a função aceitou e o fornecedor enfileirou.
    static func enviar(telefone: String, texto: String) async -> Bool {
        guard configurado else { return false }

        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.timeoutInterval = 20
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue(chave, forHTTPHeaderField: "x-smsos-app")

        let corpo: [String: Any] = [
            "envios": [["telefone": Telefone.limpar(telefone), "texto": texto]]
        ]
        req.httpBody = try? JSONSerialization.data(withJSONObject: corpo)

        do {
            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                return false
            }
            if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let ok = obj["ok"] as? Bool {
                return ok
            }
            return true
        } catch {
            return false
        }
    }
}

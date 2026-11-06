import Foundation

/// A nossa estrutura no Supabase. Todas as funções usam a MESMA chave do app (x-smsos-app),
/// que vem do Secrets.swift (preenchido no build pelo segredo GUARDIOES_GATEWAY_KEY).
enum Backend {
    static let base = "https://ymszgqpgxtprkqxjhtwb.supabase.co/functions/v1"
    static var chave: String { Secrets.gatewayKey }
    static var configurado: Bool { !chave.isEmpty }

    /// POST JSON cru. Devolve o corpo da resposta (Data) em 2xx, senão nil.
    static func post(_ funcao: String, _ corpo: [String: Any]) async -> Data? {
        guard configurado, let url = URL(string: "\(base)/\(funcao)") else { return nil }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.timeoutInterval = 20
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue(chave, forHTTPHeaderField: "x-smsos-app")
        req.httpBody = try? JSONSerialization.data(withJSONObject: corpo)
        do {
            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse, (200...299).contains(http.statusCode) else { return nil }
            return data
        } catch {
            return nil
        }
    }

    static func postObj(_ funcao: String, _ corpo: [String: Any]) async -> [String: Any]? {
        guard let data = await post(funcao, corpo) else { return nil }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
    }

    static func postArray(_ funcao: String, _ corpo: [String: Any]) async -> [[String: Any]]? {
        guard let data = await post(funcao, corpo) else { return nil }
        return (try? JSONSerialization.jsonObject(with: data)) as? [[String: Any]]
    }
}

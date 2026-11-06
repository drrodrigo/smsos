import Foundation

/// Cadastra/atualiza o usuário do app (o lead) na nossa conta — função guardioes-registrar.
/// Roda em segundo plano e nunca atrapalha nada. Identidade = dispositivo + telefone, sem senha.
enum Registrar {
    static func registrar(dispositivo: String, nome: String, telefone: String) {
        guard Backend.configurado, !dispositivo.isEmpty, !Telefone.limpar(telefone).isEmpty else { return }
        let versao = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? ""
        Task {
            _ = await Backend.post("guardioes-registrar", [
                "dispositivo": dispositivo,
                "nome": nome,
                "telefone": Telefone.normalizar(telefone),
                "idioma": "pt",
                "plataforma": "ios",
                "versao": versao,
            ])
        }
    }
}

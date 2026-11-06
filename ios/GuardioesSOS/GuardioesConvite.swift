import Foundation

/// O lado da REDE DE GUARDIÕES: convidar (gerar código/link), ver convites pendentes pelo telefone,
/// aceitar por código e listar quem eu protejo. Função guardioes-convite, mesma chave do app.
enum GuardioesConvite {
    static var configurado: Bool { Backend.configurado }

    struct Pendente: Identifiable { var id: String { codigo }; let codigo: String; let usuarioNome: String }
    struct Protegido: Identifiable { let id = UUID(); let usuarioNome: String }
    struct ResultadoConvite { let codigo: String; let link: String }
    struct ResultadoAceite { let ok: Bool; let usuarioNome: String?; let jaAceito: Bool }

    /// Cria o convite para um guardião (por telefone). Devolve código + link para compartilhar.
    static func convidar(usuarioDispositivo: String, usuarioNome: String, guardiaoTelefone: String) async -> ResultadoConvite? {
        let obj = await Backend.postObj("guardioes-convite", [
            "acao": "convidar",
            "usuario_dispositivo": usuarioDispositivo,
            "usuario_nome": usuarioNome,
            "guardiao_telefone": Telefone.normalizar(guardiaoTelefone),
        ])
        guard let o = obj, let codigo = o["codigo"] as? String, !codigo.isEmpty else { return nil }
        return ResultadoConvite(codigo: codigo, link: (o["link"] as? String) ?? "")
    }

    /// Convites pendentes para o telefone deste usuário.
    static func pendentes(telefone: String) async -> [Pendente] {
        guard let arr = await Backend.postArray("guardioes-convite", [
            "acao": "do_telefone",
            "telefone": Telefone.normalizar(telefone),
        ]) else { return [] }
        return arr.compactMap { o in
            guard let codigo = o["codigo"] as? String else { return nil }
            return Pendente(codigo: codigo, usuarioNome: (o["usuario_nome"] as? String) ?? "")
        }
    }

    /// Aceita um convite pelo código.
    static func aceitar(codigo: String, guardiaoDispositivo: String) async -> ResultadoAceite? {
        guard let o = await Backend.postObj("guardioes-convite", [
            "acao": "aceitar",
            "codigo": codigo.trimmingCharacters(in: .whitespacesAndNewlines),
            "guardiao_dispositivo": guardiaoDispositivo,
        ]) else { return nil }
        return ResultadoAceite(
            ok: (o["ok"] as? Bool) ?? false,
            usuarioNome: o["usuario_nome"] as? String,
            jaAceito: (o["ja_aceito"] as? Bool) ?? false
        )
    }

    /// Quem eu protejo (convites que aceitei).
    static func quemProtejo(guardiaoDispositivo: String) async -> [Protegido] {
        guard let arr = await Backend.postArray("guardioes-convite", [
            "acao": "quem_protejo",
            "guardiao_dispositivo": guardiaoDispositivo,
        ]) else { return [] }
        return arr.map { Protegido(usuarioNome: ($0["usuario_nome"] as? String) ?? "") }
    }
}

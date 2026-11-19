import Foundation

/// As regras de telefone, iguais às do Android (core/PhoneNumbers.kt) e às da versão no navegador:
/// tira +55, traço, parênteses e espaço; acrescenta o 9 do celular quando falta; guarda os 11 dígitos
/// e mostra "(31) 99999-8888". Número errado = alerta que não chega.
enum Telefone {
    /// Só os dígitos.
    static func limpar(_ s: String) -> String {
        s.filter { $0.isASCII && $0.isNumber }
    }

    /// Os dígitos NACIONAIS (DDD + número), já com o 9 do celular quando faltava.
    static func nacional(_ bruto: String) -> String {
        var d = limpar(bruto)
        if (d.count == 12 || d.count == 13) && d.hasPrefix("55") { d = String(d.dropFirst(2)) }
        while d.hasPrefix("0") { d.removeFirst() }
        if d.count == 10 {
            let i = d.index(d.startIndex, offsetBy: 2)
            if "6789".contains(d[i]) { d.insert("9", at: i) }
        }
        return d
    }

    /// Celular brasileiro válido: 11 dígitos, DDD sem zero e o 9 na frente.
    static func celularValido(_ bruto: String) -> Bool {
        let d = Array(nacional(bruto))
        return d.count == 11 && d[2] == "9" && d[0] != "0"
    }

    /// O aviso em vermelho quando o número parece errado (nil = está certo ou vazio).
    static func aviso(_ bruto: String) -> String? {
        let n = nacional(bruto).count
        if n == 0 || celularValido(bruto) { return nil }
        switch n {
        case 11: return "Esse número não parece de celular. Confira o DDD e o 9."
        case 10: return "Parece faltar o 9 do celular. Hoje o celular tem 11 dígitos (DDD + 9): (31) 99999-8888."
        case 8, 9: return "Parece faltar o DDD. Escreva com DDD, assim: (31) 99999-8888."
        case 1...7: return "Número curto demais. Confira o DDD e o número."
        default: return "Número com dígitos demais. Confira."
        }
    }

    /// Mostra sempre igual: "(31) 99999-8888". Se não tiver 11 dígitos, mostra como está.
    static func formatar(_ bruto: String) -> String {
        let d = Array(nacional(bruto))
        guard d.count == 11 else { return bruto }
        return "(\(String(d[0..<2]))) \(String(d[2..<7]))-\(String(d[7..<11]))"
    }

    /// Com o DDI 55 na frente (o que o servidor da rede de guardiões espera).
    static func normalizar(_ s: String) -> String {
        let d = nacional(s)
        if d.count == 10 || d.count == 11 { return "55" + d }
        return limpar(s)
    }
}

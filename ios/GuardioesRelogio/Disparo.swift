import Foundation
import CoreLocation

/// O SOS do relógio, sem tela (a tela, a Siri e o Botão de Ação usam este mesmo caminho).
///
/// 1. Com o iPhone por perto: pede ao app do iPhone, que dispara pelo MESMO caminho do botão SOS de
///    lá (modo teste incluído) e responde se saiu.
/// 2. Sem o iPhone (ou o iPhone não conseguiu): o relógio chama a nossa estrutura (smsos-alerta)
///    DIRETO, pela internet dele (Wi-Fi ou celular), com a mesma chave e o mesmo texto do SMS, os
///    guardiões que o iPhone sincronizou e a posição do próprio relógio. No modo teste, não envia.
@MainActor
enum Disparo {
    enum Caminho: String, Codable {
        /// O iPhone enviou.
        case iphone
        /// O relógio enviou pela internet dele.
        case internet
        /// Não chegou a sair de lugar nenhum.
        case nenhum
    }

    struct Resultado: Equatable {
        /// true: o alerta saiu (no modo teste: o teste funcionou).
        var ok: Bool
        var teste: Bool
        /// Quantos foram avisados (no teste: quantos seriam).
        var contatos: Int
        var caminho: Caminho
        /// Por que não saiu (frase curta, para a tela pequena).
        var motivo: String? = nil
    }

    static func executar(perfil: Perfil, posicao: CLLocation?) async -> Resultado {
        let id = UUID().uuidString
        let ponte = PonteComOIPhone.unica
        await ponte.esperarLigar()

        if ponte.iPhoneAlcancavel {
            do {
                let r = try await ponte.pedirSOS(id: id, perfil: perfil, posicao: posicao)
                if r.ok {
                    return Resultado(ok: true, teste: r.teste, contatos: r.contatos, caminho: .iphone)
                }
                if r.erro == ProtocoloRelogio.Erro.semContatos {
                    return Resultado(ok: false, teste: false, contatos: 0, caminho: .nenhum,
                                     motivo: "Sem guardiões. Cadastre no iPhone.")
                }
                // O iPhone tentou e nada saiu (sem internet lá?): tenta pela internet do relógio.
            } catch {
                // Não chegou ao iPhone ou ele não respondeu a tempo: vai pela internet do relógio.
                // (Se o iPhone recebeu e só a resposta se perdeu, o alerta pode sair duas vezes:
                // num botão de pânico, repetido é melhor do que nenhum.)
            }
        }
        return await direto(id: id, perfil: perfil, posicao: posicao)
    }

    /// Pela internet do próprio relógio, com o que o iPhone sincronizou.
    private static func direto(id: String, perfil: Perfil, posicao: CLLocation?) async -> Resultado {
        guard let contexto = Demo.ativo ? Demo.contexto : Guardado.contexto else {
            return Resultado(ok: false, teste: false, contatos: 0, caminho: .nenhum,
                             motivo: "Sem o iPhone. Abra o GuardiõesSOS no iPhone uma vez.")
        }
        let destino = QuemRecebe.calcular(guardioes: contexto.guardioes, lugares: contexto.lugares, posicao: posicao)
        let lat = posicao?.coordinate.latitude
        let lng = posicao?.coordinate.longitude
        let texto = Mensagem.alerta(nome: contexto.nome, perfil: perfil, latitude: lat, longitude: lng)
        let total = destino.contatos.count
        // Sem a chave de envio nesta versão (ou -demo), também é teste: nada sai.
        let teste = contexto.emTeste || !Backend.configurado

        func registro(_ telefones: [String], teste: Bool) -> RegistroDoRelogio {
            RegistroDoRelogio(id: id, em: Date(), perfil: perfil, teste: teste, total: total,
                              enviados: telefones.count, telefones: telefones, lugar: destino.lugar,
                              latitude: lat, longitude: lng)
        }

        if teste {
            PonteComOIPhone.unica.contarAoIPhone(registro([], teste: true))
            return Resultado(ok: true, teste: true, contatos: total, caminho: .internet)
        }
        if total == 0 {
            return Resultado(ok: false, teste: false, contatos: 0, caminho: .nenhum,
                             motivo: "Sem guardiões. Cadastre no iPhone.")
        }

        let aceitos = await Gateway.enviar(telefones: destino.contatos.map { $0.telefone }, texto: texto)
        PonteComOIPhone.unica.contarAoIPhone(registro(Array(aceitos), teste: false))
        if aceitos.isEmpty {
            return Resultado(ok: false, teste: false, contatos: 0, caminho: .nenhum,
                             motivo: "Sem internet no relógio e o iPhone longe.")
        }
        return Resultado(ok: true, teste: false, contatos: aceitos.count, caminho: .internet)
    }
}

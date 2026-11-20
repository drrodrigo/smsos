import Foundation
import CoreLocation
import WatchConnectivity

/// O lado do relógio da conversa com o iPhone (WatchConnectivity). O combinado está em
/// Comum/ProtocoloDoRelogio.swift.
///
/// - Recebe do iPhone o nome, os guardiões, os lugares e o modo teste (updateApplicationContext) e
///   guarda no relógio, para disparar sozinho quando o iPhone não está por perto.
/// - Pede o SOS ao iPhone (sendMessage, que acorda o app do iPhone em segundo plano) e espera a resposta.
/// - Conta ao iPhone o SOS que disparou sozinho (transferUserInfo, entregue mesmo que demore).
final class PonteComOIPhone: NSObject, WCSessionDelegate {
    static let unica = PonteComOIPhone()

    private typealias K = ProtocoloRelogio.Chave
    private typealias Acao = ProtocoloRelogio.Acao

    /// Chamado (na thread principal) quando chega um contexto novo do iPhone.
    var aoReceberContexto: (@MainActor (ContextoDoRelogio) -> Void)?

    enum Falha: Error {
        case semIPhone
        case semResposta
    }

    func ligar() {
        guard !Demo.ativo, WCSession.isSupported() else { return }
        let sessao = WCSession.default
        sessao.delegate = self
        sessao.activate()
    }

    /// O iPhone está por perto (Bluetooth ou a mesma rede) e a conversa está ligada.
    var iPhoneAlcancavel: Bool {
        guard !Demo.ativo, WCSession.isSupported() else { return false }
        let sessao = WCSession.default
        return sessao.activationState == .activated && sessao.isReachable
    }

    // MARK: - Pedir o SOS ao iPhone

    /// Manda o pedido e espera a resposta (até `limite` segundos). Erro = não chegou ou não respondeu.
    func pedirSOS(id: String, perfil: Perfil, posicao: CLLocation?, limite: TimeInterval = 35) async throws -> RespostaSOS {
        guard iPhoneAlcancavel else { throw Falha.semIPhone }
        var pedido: [String: Any] = [K.acao: Acao.sos, K.perfil: perfil.rawValue, K.id: id]
        if let p = posicao {
            pedido[K.lat] = p.coordinate.latitude
            pedido[K.lng] = p.coordinate.longitude
            pedido[K.precisao] = p.horizontalAccuracy
        }
        return try await withCheckedThrowingContinuation { (c: CheckedContinuation<RespostaSOS, Error>) in
            let uma = UmaVez(c)
            WCSession.default.sendMessage(pedido, replyHandler: { resposta in
                uma.resolver(.success(RespostaSOS(dicionario: resposta)))
            }, errorHandler: { erro in
                uma.resolver(.failure(erro))
            })
            DispatchQueue.global().asyncAfter(deadline: .now() + limite) {
                uma.resolver(.failure(Falha.semResposta))
            }
        }
    }

    /// Conta ao iPhone o SOS que o relógio disparou sozinho (para o histórico e o "Estou bem").
    func contarAoIPhone(_ registro: RegistroDoRelogio) {
        guard !Demo.ativo, WCSession.isSupported(), WCSession.default.activationState == .activated,
              let dados = try? JSONEncoder().encode(registro) else { return }
        WCSession.default.transferUserInfo([K.acao: Acao.registro, K.registro: dados])
    }

    // MARK: - WCSessionDelegate

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if activationState == .activated {
            receber(session.receivedApplicationContext)
        }
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        receber(applicationContext)
    }

    private func receber(_ dicionario: [String: Any]) {
        guard let dados = dicionario[K.contexto] as? Data,
              let contexto = ContextoDoRelogio.decodificar(dados) else { return }
        Guardado.salvar(contexto: contexto)
        Task { @MainActor in self.aoReceberContexto?(contexto) }
    }
}

/// Garante que a espera termina UMA vez só (a resposta, o erro ou o tempo limite: o que vier antes).
private final class UmaVez<T> {
    private var continuacao: CheckedContinuation<T, Error>?
    private let trava = NSLock()

    init(_ c: CheckedContinuation<T, Error>) { continuacao = c }

    func resolver(_ resultado: Result<T, Error>) {
        trava.lock()
        let c = continuacao
        continuacao = nil
        trava.unlock()
        c?.resume(with: resultado)
    }
}

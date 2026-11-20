import Foundation
import UIKit
import CoreLocation
import Combine
import WatchConnectivity

/// O lado do iPhone da conversa com o Apple Watch (WatchConnectivity). O combinado está em
/// Comum/ProtocoloDoRelogio.swift.
///
/// - O relógio pede o SOS ("acao": "sos"): o iPhone dispara pelo MESMO caminho do botão SOS
///   (Alerta.executar, respeitando o modo teste) e responde se saiu.
/// - Sempre que o nome, os guardiões, os lugares ou o modo teste mudam, o iPhone manda tudo para o
///   relógio (updateApplicationContext), para ele conseguir disparar sozinho pela internet dele.
/// - Quando o relógio disparou sozinho, ele conta depois ("acao": "registro"): entra no histórico e
///   no "Estou bem — avisar todos".
///
/// Tudo o que mexe no AppState roda na thread principal.
final class PonteComORelogio: NSObject, WCSessionDelegate {
    static let unica = PonteComORelogio()

    private typealias K = ProtocoloRelogio.Chave
    private typealias Acao = ProtocoloRelogio.Acao
    private typealias Erro = ProtocoloRelogio.Erro

    private var app: AppState?
    private var observador: AnyCancellable?
    /// O último contexto mandado (para não mandar de novo o que o relógio já tem).
    private var ultimoContexto: Data?
    /// Os pedidos já atendidos (id → resposta): o relógio que repete o pedido não dispara duas vezes.
    private var atendidos: [String: RespostaSOS] = [:]
    private var emAndamento: [String: Task<RespostaSOS, Never>] = [:]

    /// Liga a ponte (uma vez, na abertura do app). No modo demonstração (-demo) não liga.
    func ligar(app: AppState) {
        guard !Demo.ativo, WCSession.isSupported(), self.app == nil else { return }
        self.app = app
        let sessao = WCSession.default
        sessao.delegate = self
        sessao.activate()
        // O AppState avisa ANTES de mudar; a espera curta junta várias mudanças numa sincronização só.
        observador = app.objectWillChange
            .debounce(for: .milliseconds(800), scheduler: DispatchQueue.main)
            .sink { [weak self] _ in self?.sincronizar() }
    }

    // MARK: - O que o relógio precisa para disparar sozinho

    /// Manda nome, perfil, guardiões, lugares e modo teste para o relógio. Roda na thread principal.
    private func sincronizar() {
        guard let app else { return }
        let sessao = WCSession.default
        guard sessao.activationState == .activated, sessao.isPaired, sessao.isWatchAppInstalled else { return }
        let contexto = ContextoDoRelogio(
            nome: app.nome,
            perfil: app.perfil,
            guardioes: app.guardioes,
            lugares: app.lugares,
            emTeste: app.emTeste,
            configurado: app.configurado
        )
        guard let dados = contexto.codificado(), dados != ultimoContexto else { return }
        do {
            try sessao.updateApplicationContext([K.contexto: dados])
            ultimoContexto = dados
        } catch {
            // Sem relógio por perto agora: tenta de novo na próxima mudança ou abertura.
        }
    }

    // MARK: - WCSessionDelegate

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        DispatchQueue.main.async { self.sincronizar() }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    /// Trocou de relógio: liga de novo para o relógio novo.
    func sessionDidDeactivate(_ session: WCSession) {
        WCSession.default.activate()
    }

    /// Instalou (ou tirou) o app no relógio, ou emparelhou outro: manda o contexto de novo.
    func sessionWatchStateDidChange(_ session: WCSession) {
        DispatchQueue.main.async {
            self.ultimoContexto = nil
            self.sincronizar()
        }
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any],
                 replyHandler: @escaping ([String: Any]) -> Void) {
        Task { @MainActor in
            let resposta = await self.atender(message)
            replyHandler(resposta.dicionario)
        }
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        Task { @MainActor in _ = await self.atender(message) }
    }

    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        DispatchQueue.main.async { self.registrar(userInfo) }
    }

    // MARK: - O SOS pedido pelo relógio

    @MainActor
    private func atender(_ pedido: [String: Any]) async -> RespostaSOS {
        let id = (pedido[K.id] as? String) ?? UUID().uuidString
        guard (pedido[K.acao] as? String) == Acao.sos, let app else {
            return RespostaSOS(id: id, ok: false, teste: false, contatos: 0, erro: Erro.pedidoInvalido)
        }
        if let pronta = atendidos[id] { return pronta }
        if let andando = emAndamento[id] { return await andando.value }

        let perfil = Perfil(rawValue: pedido[K.perfil] as? String ?? "") ?? app.perfil
        let posicao = Self.posicao(do: pedido)
        let tarefa = Task { @MainActor () -> RespostaSOS in
            // Acordado em segundo plano, o iPhone tem pouco tempo: pede mais para terminar o envio.
            let fundo = UIApplication.shared.beginBackgroundTask(withName: "SOS do relógio")
            let r = await Alerta.executar(perfil, app: app, posicaoDada: posicao)
            if fundo != .invalid { UIApplication.shared.endBackgroundTask(fundo) }
            return Self.resposta(id: id, r)
        }
        emAndamento[id] = tarefa
        let resposta = await tarefa.value
        emAndamento[id] = nil
        atendidos[id] = resposta
        if atendidos.count > 30 { atendidos = [id: resposta] }
        return resposta
    }

    /// A posição que o relógio mandou (o iPhone em segundo plano não consegue pegar a dele).
    private static func posicao(do pedido: [String: Any]) -> CLLocation? {
        guard let lat = pedido[K.lat] as? Double, let lng = pedido[K.lng] as? Double else { return nil }
        let precisao = (pedido[K.precisao] as? Double) ?? 50
        return CLLocation(
            coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lng),
            altitude: 0,
            horizontalAccuracy: precisao,
            verticalAccuracy: -1,
            timestamp: Date()
        )
    }

    private static func resposta(id: String, _ r: Alerta.Resultado) -> RespostaSOS {
        if r.teste {
            return RespostaSOS(id: id, ok: true, teste: true, contatos: r.total)
        }
        if r.total == 0 {
            return RespostaSOS(id: id, ok: false, teste: false, contatos: 0, erro: Erro.semContatos)
        }
        if r.enviados > 0 {
            return RespostaSOS(id: id, ok: true, teste: false, contatos: r.enviados)
        }
        return RespostaSOS(id: id, ok: false, teste: false, contatos: 0, erro: Erro.naoSaiu)
    }

    // MARK: - O SOS que o relógio disparou sozinho

    /// Guarda no histórico (uma vez só) e, se saiu, liga o "Estou bem — avisar todos".
    private func registrar(_ info: [String: Any]) {
        guard let app,
              (info[K.acao] as? String) == Acao.registro,
              let dados = info[K.registro] as? Data,
              let r = try? JSONDecoder().decode(RegistroDoRelogio.self, from: dados),
              !app.historico.contains(where: { $0.id == r.id })
        else { return }
        app.registrar(RegistroAlerta(id: r.id, em: r.em, teste: r.teste, perfil: r.perfil, total: r.total,
                                     enviados: r.enviados, lugar: r.lugar, latitude: r.latitude, longitude: r.longitude))
        if !r.teste && !r.telefones.isEmpty && (app.ativo.map { $0.em < r.em } ?? true) {
            app.ativo = AlertaAtivo(em: r.em, telefones: r.telefones)
        }
    }
}

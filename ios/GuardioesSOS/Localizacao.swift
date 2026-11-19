import CoreLocation

/// A localização do aparelho: a permissão (para o checklist) e UMA leitura do GPS, com tempo limite.
/// Sem permissão ou sem sinal, devolve nil (o alerta vai sem o mapa).
/// Tudo o que mexe no CLLocationManager roda na thread principal (onde ele foi criado).
final class Localizacao: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var cont: CheckedContinuation<CLLocation?, Never>?
    private var pedido = 0

    /// Chamado quando a pessoa muda a permissão (para a tela Início se atualizar).
    var aoMudarPermissao: (() -> Void)?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    var status: CLAuthorizationStatus { manager.authorizationStatus }
    var autorizado: Bool { status == .authorizedWhenInUse || status == .authorizedAlways }
    var negado: Bool { status == .denied || status == .restricted }
    var naoPerguntou: Bool { status == .notDetermined }

    func pedirPermissao() {
        if manager.authorizationStatus == .notDetermined {
            manager.requestWhenInUseAuthorization()
        }
    }

    /// Pega UMA posição. Se ainda não perguntou a permissão, pergunta e espera a resposta.
    func pegar(timeout: TimeInterval = 12) async -> CLLocation? {
        if negado { return nil }
        return await withCheckedContinuation { (c: CheckedContinuation<CLLocation?, Never>) in
            DispatchQueue.main.async {
                self.resolver(nil)
                self.pedido += 1
                let meu = self.pedido
                self.cont = c
                if self.manager.authorizationStatus == .notDetermined {
                    self.manager.requestWhenInUseAuthorization()
                } else {
                    self.manager.requestLocation()
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + timeout) {
                    if self.pedido == meu { self.resolver(nil) }
                }
            }
        }
    }

    private func resolver(_ loc: CLLocation?) {
        let c = cont
        cont = nil
        c?.resume(returning: loc)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        resolver(locations.last)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // "Posição ainda desconhecida" é passageiro: espera até o tempo limite.
        if let e = error as? CLError, e.code == .locationUnknown { return }
        resolver(nil)
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        aoMudarPermissao?()
        guard cont != nil else { return }
        if autorizado {
            manager.requestLocation()
        } else if negado {
            resolver(nil)
        }
    }
}

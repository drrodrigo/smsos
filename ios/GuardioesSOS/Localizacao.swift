import CoreLocation

/// Pega UMA localização, com timeout. Sem permissão ou sem sinal, devolve nil (o alerta vai sem o mapa).
final class Localizacao: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var cont: CheckedContinuation<CLLocation?, Never>?
    private var resolvido = false

    func pedirPermissao() {
        if manager.authorizationStatus == .notDetermined {
            manager.requestWhenInUseAuthorization()
        }
    }

    func pegar(timeout: TimeInterval = 12) async -> CLLocation? {
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest

        switch manager.authorizationStatus {
        case .denied, .restricted:
            return nil
        case .notDetermined:
            manager.requestWhenInUseAuthorization()
        default:
            break
        }

        resolvido = false
        return await withCheckedContinuation { c in
            self.cont = c
            self.manager.requestLocation()
            DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { [weak self] in
                self?.resolver(nil)
            }
        }
    }

    private func resolver(_ loc: CLLocation?) {
        guard !resolvido else { return }
        resolvido = true
        let c = cont
        cont = nil
        c?.resume(returning: loc)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        resolver(locations.last)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        resolver(nil)
    }
}

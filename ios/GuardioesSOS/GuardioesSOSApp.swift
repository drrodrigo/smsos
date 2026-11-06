import SwiftUI

/// O GuardiõesSOS no iPhone. Milestone 1: cadastro + botão de pânico (modo teste).
/// As telas do lado guardião, o envio real e o alerta que toca como ligação vêm nas próximas versões.
@main
struct GuardioesSOSApp: App {
    @StateObject private var app = AppState()
    var body: some Scene {
        WindowGroup {
            ContentView().environmentObject(app)
        }
    }
}

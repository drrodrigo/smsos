import SwiftUI

/// O GuardiõesSOS no iPhone, com a mesma cara e o mesmo jeito do app Android.
@main
struct GuardioesSOSApp: App {
    @StateObject private var app = AppState()
    @StateObject private var camada = Camada()

    var body: some Scene {
        WindowGroup {
            NavigationStack {
                ContentView()
                    .toolbar(.hidden, for: .navigationBar)
            }
            .environmentObject(app)
            .environmentObject(camada)
            .tint(Cor.primary)
        }
    }
}

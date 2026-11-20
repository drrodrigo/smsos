import SwiftUI
import UIKit

/// O GuardiõesSOS no iPhone, com a mesma cara e o mesmo jeito do app Android.
@main
struct GuardioesSOSApp: App {
    @UIApplicationDelegateAdaptor(EntradaDoApp.self) private var entrada
    @StateObject private var app = AppState.unico
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

/// A entrada do app, ANTES de qualquer tela: liga a ponte com o Apple Watch. Precisa ser aqui porque
/// o relógio pode acordar o app em segundo plano (para pedir o SOS), e aí nenhuma tela é montada.
final class EntradaDoApp: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        PonteComORelogio.unica.ligar(app: AppState.unico)
        return true
    }
}

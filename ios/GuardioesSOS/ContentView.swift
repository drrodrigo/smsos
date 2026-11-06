import SwiftUI

/// Roteia entre o cadastro (primeira vez) e a tela do botão de pânico.
struct ContentView: View {
    @EnvironmentObject var app: AppState
    var body: some View {
        if app.configurado {
            HomeView()
        } else {
            CadastroView()
        }
    }
}

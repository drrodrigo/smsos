import SwiftUI

/// Primeira vez → cadastro. Depois → as abas do app (igual ao Android).
struct ContentView: View {
    @EnvironmentObject var app: AppState

    var body: some View {
        if app.configurado {
            TabView {
                InicioView()
                    .tabItem { Label("Início", systemImage: "house.fill") }
                ContatosView()
                    .tabItem { Label("Contatos", systemImage: "person.2.fill") }
                GuardioesView()
                    .tabItem { Label("Guardiões", systemImage: "shield.fill") }
                ProtejoView()
                    .tabItem { Label("Protejo", systemImage: "heart.fill") }
                AjustesView()
                    .tabItem { Label("Ajustes", systemImage: "gearshape.fill") }
            }
            .task { if app.configurado { app.registrarLead() } }
        } else {
            CadastroView()
        }
    }
}

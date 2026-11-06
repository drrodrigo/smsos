import SwiftUI

/// As abas do app (o checklist da tela Início pode trocar de aba por aqui).
enum Aba: Hashable {
    case inicio, contatos, guardioes, protejo, ajustes
}

/// Primeira vez → cadastro. Depois → as abas do app (igual ao Android).
struct ContentView: View {
    @EnvironmentObject var app: AppState
    @State private var aba: Aba = .inicio

    var body: some View {
        if app.configurado {
            TabView(selection: $aba) {
                InicioView(irPara: { aba = $0 })
                    .tabItem { Label("Início", systemImage: "house.fill") }
                    .tag(Aba.inicio)
                ContatosView()
                    .tabItem { Label("Contatos", systemImage: "person.2.fill") }
                    .tag(Aba.contatos)
                GuardioesView()
                    .tabItem { Label("Guardiões", systemImage: "shield.fill") }
                    .tag(Aba.guardioes)
                ProtejoView()
                    .tabItem { Label("Protejo", systemImage: "heart.fill") }
                    .tag(Aba.protejo)
                AjustesView()
                    .tabItem { Label("Ajustes", systemImage: "gearshape.fill") }
                    .tag(Aba.ajustes)
            }
            .task { if app.configurado { app.registrarLead() } }
        } else {
            CadastroView()
        }
    }
}

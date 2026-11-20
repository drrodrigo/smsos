import SwiftUI

/// O GuardiõesSOS no Apple Watch: abrir o app (ícone, complicação, Botão de Ação) já é o SOS.
/// No modo discreto (ligado de fábrica) a tela fica preta, conta 3 s e dispara sozinho; tocar
/// cancela. A Siri ("Socorro no GuardiõesSOS") dispara sem abrir a tela (AtalhoDoSOS.swift).
@main
struct GuardioesRelogioApp: App {
    @StateObject private var estado = EstadoDoRelogio.unico

    init() {
        PonteComOIPhone.unica.ligar()
    }

    var body: some Scene {
        WindowGroup {
            RaizView()
                .environmentObject(estado)
        }
    }
}

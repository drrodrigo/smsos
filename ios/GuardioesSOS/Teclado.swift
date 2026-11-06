import SwiftUI
import UIKit

/// Fecha o teclado (resign first responder) — o teclado de número não tem botão de fechar sozinho.
func esconderTeclado() {
    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
}

extension View {
    /// Dá ao teclado um botão "Concluído" e fecha ao rolar — pros campos de telefone não prenderem o usuário.
    func tecladoFechavel() -> some View {
        self
            .scrollDismissesKeyboard(.immediately)
            .toolbar {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Concluído") { esconderTeclado() }
                }
            }
    }
}

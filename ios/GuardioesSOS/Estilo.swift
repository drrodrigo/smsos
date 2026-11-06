import SwiftUI

/// Fundo azul do app (a cara da marca).
struct Fundo: View {
    var body: some View {
        LinearGradient(
            colors: [Color(red: 0.11, green: 0.20, blue: 0.40), Color(red: 0.04, green: 0.11, blue: 0.21)],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}

import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.11, green: 0.20, blue: 0.40), Color(red: 0.04, green: 0.11, blue: 0.21)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 18) {
                Image(systemName: "shield.lefthalf.filled")
                    .font(.system(size: 92))
                    .foregroundStyle(.white)
                Text("GuardiõesSOS")
                    .font(.system(size: 34, weight: .heavy))
                    .foregroundStyle(.white)
                Text("Botão de pânico e SOS")
                    .font(.title3)
                    .foregroundStyle(Color(white: 0.82))
                Text("Em breve no seu iPhone.")
                    .font(.callout)
                    .foregroundStyle(Color(white: 0.65))
                    .padding(.top, 10)
            }
            .padding()
        }
    }
}

#Preview {
    ContentView()
}

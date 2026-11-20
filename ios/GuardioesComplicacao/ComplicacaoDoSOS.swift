import WidgetKit
import SwiftUI

/// A complicação "SOS" do mostrador do Apple Watch (WidgetKit, watchOS 9). Tocada, abre o app do
/// relógio, que já começa no modo discreto, contando. Não tem nada para atualizar: a linha do tempo
/// tem UMA entrada e nunca pede outra (nada de gastar bateria).
@main
struct ComplicacaoDoSOS: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "br.com.guardioessos.sos", provider: ProvedorDoSOS()) { _ in
            VistaDoSOS()
                .fundoDaComplicacao()
                .widgetURL(URL(string: "guardioessos-relogio://sos"))
        }
        .configurationDisplayName("SOS do Guardião")
        .description("Um toque abre o GuardiõesSOS já contando para avisar seus guardiões.")
        .supportedFamilies([.accessoryCircular, .accessoryCorner, .accessoryInline])
    }
}

struct EntradaDoSOS: TimelineEntry {
    let date: Date
}

struct ProvedorDoSOS: TimelineProvider {
    func placeholder(in context: Context) -> EntradaDoSOS {
        EntradaDoSOS(date: Date())
    }

    func getSnapshot(in context: Context, completion: @escaping (EntradaDoSOS) -> Void) {
        completion(EntradaDoSOS(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<EntradaDoSOS>) -> Void) {
        completion(Timeline(entries: [EntradaDoSOS(date: Date())], policy: .never))
    }
}

struct VistaDoSOS: View {
    @Environment(\.widgetFamily) private var familia

    var body: some View {
        switch familia {
        case .accessoryCorner:
            Image(systemName: "shield.fill")
                .font(.system(size: 20, weight: .semibold))
                .widgetAccentable()
                .widgetLabel("SOS")
                .accessibilityLabel("SOS do Guardião")
        case .accessoryInline:
            Label("SOS Guardião", systemImage: "shield.fill")
        default:
            ZStack {
                AccessoryWidgetBackground()
                Text("SOS")
                    .font(.system(size: 17, weight: .heavy, design: .rounded))
                    .minimumScaleFactor(0.6)
                    .widgetAccentable()
            }
            .accessibilityLabel("SOS do Guardião")
        }
    }
}

extension View {
    /// No watchOS 10 em diante o sistema exige o fundo do widget declarado; no 9 não existe.
    @ViewBuilder
    func fundoDaComplicacao() -> some View {
        if #available(watchOS 10.0, *) {
            containerBackground(for: .widget) { Color.clear }
        } else {
            self
        }
    }
}

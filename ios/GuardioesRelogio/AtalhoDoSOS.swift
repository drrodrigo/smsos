import AppIntents

/// "Disparar o SOS do Guardião": o SOS SEM ABRIR A TELA, para a Siri ("Socorro no GuardiõesSOS")
/// e para o Botão de Ação do Apple Watch Ultra (pelo app Atalhos). Vai pelo mesmo caminho da tela:
/// o iPhone por perto, ou a internet do relógio; no modo teste, nada é enviado.
struct DispararSOSIntent: AppIntent {
    static var title: LocalizedStringResource = "Disparar o SOS do Guardião"
    static var description = IntentDescription(
        "Avisa os seus guardiões com a sua localização, sem abrir a tela. Sem o iPhone por perto, sai pela internet do relógio."
    )
    static var openAppWhenRun: Bool = false

    @Parameter(title: "Tipo", description: "Segurança ou Saúde. Em branco: o tipo do SOS rápido do relógio.")
    var tipo: TipoDoSOS?

    @MainActor
    func perform() async throws -> some IntentResult {
        let estado = EstadoDoRelogio.unico
        let perfil = tipo?.perfil ?? estado.tipoRapido
        let r = await estado.dispararSemTela(perfil)
        guard r.ok else {
            throw FalhaDoSOS(motivo: r.motivo ?? "Tente de novo pelo app.")
        }
        // Sem frase falada: a resposta é a vibração (um botão de pânico não anuncia nada em voz alta).
        return .result()
    }
}

/// Segurança ou Saúde (o mesmo Perfil do iPhone).
enum TipoDoSOS: String, AppEnum {
    case seguranca
    case saude

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Tipo do SOS"
    static var caseDisplayRepresentations: [TipoDoSOS: DisplayRepresentation] = [
        .seguranca: "Segurança",
        .saude: "Saúde",
    ]

    var perfil: Perfil { self == .saude ? .saude : .seguranca }
}

/// Quando não sai, a Siri/o Atalhos precisa dizer (senão a pessoa acha que saiu).
struct FalhaDoSOS: Error, CustomLocalizedStringResourceConvertible {
    let motivo: String
    var localizedStringResource: LocalizedStringResource { "O SOS não saiu. \(motivo)" }
}

/// As frases da Siri. O "\(.applicationName)" é o nome do app (GuardiõesSOS).
struct AtalhosDoGuardiao: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: DispararSOSIntent(),
            phrases: [
                "Socorro no \(.applicationName)",
                "SOS no \(.applicationName)",
                "Disparar o \(.applicationName)",
            ]
        )
    }
}

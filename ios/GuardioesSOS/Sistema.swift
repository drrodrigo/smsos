import SwiftUI
import UIKit
import MessageUI
import ContactsUI
import AVFoundation
import AudioToolbox

/// A tela de cima de tudo (para apresentar as telas do sistema: agenda e SMS).
enum Tela {
    static func topo() -> UIViewController? {
        let cenas = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let janelas = cenas.flatMap { $0.windows }
        var vc = (janelas.first { $0.isKeyWindow } ?? janelas.first)?.rootViewController
        while let acima = vc?.presentedViewController { vc = acima }
        return vc
    }

    static func abrir(_ endereco: String) {
        if let url = URL(string: endereco) { UIApplication.shared.open(url) }
    }

    /// Codifica um texto para ir num endereço (só letras sem acento, números e - . _ ~ ficam como estão).
    static func codificar(_ texto: String) -> String {
        let permitidos = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~")
        return texto.addingPercentEncoding(withAllowedCharacters: permitidos) ?? ""
    }

    /// Abre a página do GuardiõesSOS nos Ajustes do iPhone (para liberar a localização).
    static func abrirAjustesDoIPhone() {
        abrir(UIApplication.openSettingsURLString)
    }
}

// MARK: - Plano B: o SMS do próprio celular

/// Abre o app de Mensagens já com os números e o texto (quando a nossa estrutura não conseguiu enviar).
final class SmsDoCelular: NSObject, MFMessageComposeViewControllerDelegate {
    private static var atual: SmsDoCelular?

    static func abrir(telefones: [String], texto: String) {
        if MFMessageComposeViewController.canSendText(), let topo = Tela.topo() {
            let tela = MFMessageComposeViewController()
            tela.recipients = telefones
            tela.body = texto
            let delegado = SmsDoCelular()
            atual = delegado
            tela.messageComposeDelegate = delegado
            topo.present(tela, animated: true)
        } else {
            Tela.abrir("sms:/open?addresses=\(telefones.joined(separator: ","))&body=\(Tela.codificar(texto))")
        }
    }

    func messageComposeViewController(_ controller: MFMessageComposeViewController, didFinishWith result: MessageComposeResult) {
        controller.dismiss(animated: true)
        SmsDoCelular.atual = nil
    }
}

// MARK: - Da agenda

/// Escolher um número da agenda do iPhone. Não precisa de permissão para ler os contatos
/// (a própria tela do sistema entrega só o número que a pessoa tocou).
final class Agenda: NSObject, CNContactPickerDelegate {
    private static var atual: Agenda?
    private let aoEscolher: (String, String) -> Void

    init(aoEscolher: @escaping (String, String) -> Void) {
        self.aoEscolher = aoEscolher
    }

    static func abrir(aoEscolher: @escaping (String, String) -> Void) {
        guard let topo = Tela.topo() else { return }
        let agenda = Agenda(aoEscolher: aoEscolher)
        atual = agenda
        let tela = CNContactPickerViewController()
        tela.delegate = agenda
        tela.displayedPropertyKeys = [CNContactPhoneNumbersKey]
        tela.predicateForEnablingContact = NSPredicate(format: "phoneNumbers.@count > 0")
        tela.predicateForSelectionOfContact = NSPredicate(format: "phoneNumbers.@count == 1")
        topo.present(tela, animated: true)
    }

    func contactPicker(_ picker: CNContactPickerViewController, didSelect contact: CNContact) {
        let nome = CNContactFormatter.string(from: contact, style: .fullName) ?? ""
        let numero = contact.phoneNumbers.first?.value.stringValue ?? ""
        aoEscolher(nome, numero)
        Agenda.atual = nil
    }

    func contactPicker(_ picker: CNContactPickerViewController, didSelect contactProperty: CNContactProperty) {
        let nome = CNContactFormatter.string(from: contactProperty.contact, style: .fullName) ?? ""
        let numero = (contactProperty.value as? CNPhoneNumber)?.stringValue ?? ""
        aoEscolher(nome, numero)
        Agenda.atual = nil
    }

    func contactPickerDidCancel(_ picker: CNContactPickerViewController) {
        Agenda.atual = nil
    }
}

// MARK: - A sirene (demonstração da tela do guardião)

/// Toca uma sirene (som que sobe e desce, como a do Android) e vibra. Para sozinha em 36 s.
final class Sirene {
    private static var tocador: AVAudioPlayer?
    private static var tarefa: Task<Void, Never>?

    static func tocar() {
        parar()
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
        try? AVAudioSession.sharedInstance().setActive(true)
        if let p = try? AVAudioPlayer(data: somDaSirene()) {
            p.numberOfLoops = -1
            p.volume = 1
            p.play()
            tocador = p
        }
        tarefa = Task { @MainActor in
            for _ in 0..<40 {
                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                try? await Task.sleep(nanoseconds: 900_000_000)
                if Task.isCancelled { return }
            }
            Sirene.parar()
        }
    }

    static func parar() {
        tarefa?.cancel()
        tarefa = nil
        tocador?.stop()
        tocador = nil
    }

    /// 0,9 s de som (dente de serra de 700 Hz a 1300 Hz e de volta), em WAV, para repetir.
    private static func somDaSirene() -> Data {
        let taxa = 22_050
        let duracao = 0.9
        let n = Int(Double(taxa) * duracao)
        var amostras = [Int16](repeating: 0, count: n)
        var fase = 0.0
        for i in 0..<n {
            let t = Double(i) / Double(taxa)
            let metade = duracao / 2
            let f = t < metade ? 700 + 600 * (t / metade) : 1300 - 600 * ((t - metade) / metade)
            fase += f / Double(taxa)
            fase -= fase.rounded(.down)
            amostras[i] = Int16((2 * fase - 1) * 0.35 * 32_767)
        }

        var d = Data()
        func texto(_ s: String) { d.append(contentsOf: Array(s.utf8)) }
        func u32(_ v: UInt32) { withUnsafeBytes(of: v.littleEndian) { d.append(contentsOf: $0) } }
        func u16(_ v: UInt16) { withUnsafeBytes(of: v.littleEndian) { d.append(contentsOf: $0) } }
        let bytes = UInt32(n * 2)
        texto("RIFF"); u32(36 + bytes); texto("WAVE")
        texto("fmt "); u32(16); u16(1); u16(1); u32(UInt32(taxa)); u32(UInt32(taxa * 2)); u16(2); u16(16)
        texto("data"); u32(bytes)
        for a in amostras { withUnsafeBytes(of: a.littleEndian) { d.append(contentsOf: $0) } }
        return d
    }
}

/// A tela que o guardião vê quando chega um alerta (aqui, só a demonstração): vermelha, com a
/// sirene tocando, "Ver no mapa (silencia)" e "Silenciar"/"Fechar". Igual à AlertaRecebidoActivity.
struct TelaDoGuardiao: View {
    let corpo: String
    let fechar: () -> Void
    @State private var silenciado = false
    @Environment(\.openURL) private var openURL

    init(corpo: String, fechar: @escaping () -> Void) {
        self.corpo = corpo
        self.fechar = fechar
    }

    private var mapa: URL? {
        guard let r = corpo.range(of: "https://maps.google.com/?q=") else { return nil }
        let resto = corpo[r.lowerBound...]
        let fim = resto.firstIndex(of: " ") ?? resto.endIndex
        return URL(string: String(resto[..<fim]))
    }

    var body: some View {
        ZStack {
            Cor.alertaRecebido.ignoresSafeArea()
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                Text("⚠️").font(.system(size: 72))
                Spacer().frame(height: 14)
                Text("TESTE — ⚠️ Alerta de emergência")
                    .font(.system(size: 26, weight: .bold))
                    .multilineTextAlignment(.center)
                Spacer().frame(height: 16)
                Text(corpo)
                    .font(.system(size: 17))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer().frame(height: 34)
                if let mapa {
                    Button {
                        Sirene.parar()
                        silenciado = true
                        openURL(mapa)
                    } label: {
                        Text("Ver no mapa (silencia)")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(Cor.alertaRecebido)
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .background(Color.white, in: Capsule())
                    }
                    .buttonStyle(.plain)
                    Spacer().frame(height: 12)
                }
                Button {
                    if silenciado {
                        fechar()
                    } else {
                        Sirene.parar()
                        silenciado = true
                    }
                } label: {
                    Text(silenciado ? "Fechar" : "Silenciar")
                        .font(.system(size: 16, weight: .medium))
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .overlay(Capsule().stroke(Color.white, lineWidth: 1))
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                Spacer(minLength: 0)
            }
            .foregroundStyle(.white)
            .padding(28)
        }
        .onAppear {
            if !Demo.ativo { Sirene.tocar() }
        }
        .onDisappear { Sirene.parar() }
    }
}

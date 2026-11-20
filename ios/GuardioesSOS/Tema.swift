import SwiftUI
import UIKit

// A cara do app Android (Material 3), copiada para o iPhone: as mesmas cores, os mesmos cantos e os
// mesmos componentes (cartão, cartão que abre e fecha, botões em pílula, chave, chip, campo com borda).
// Tudo segue o claro/escuro do sistema.

extension Color {
    /// Cor fixa, em hexadecimal (0xRRGGBB).
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    /// Cor que muda com o tema do sistema: uma no claro, outra no escuro.
    init(claro: UInt32, escuro: UInt32) {
        self.init(UIColor { t in
            let hex = t.userInterfaceStyle == .dark ? escuro : claro
            return UIColor(
                red: CGFloat((hex >> 16) & 0xFF) / 255,
                green: CGFloat((hex >> 8) & 0xFF) / 255,
                blue: CGFloat(hex & 0xFF) / 255,
                alpha: 1
            )
        })
    }
}

/// As cores do tema do Android (ui/Theme.kt + os padrões do Material 3).
enum Cor {
    static let primary = Color(claro: 0xB71C1C, escuro: 0xFFB4AB)
    static let onPrimary = Color(claro: 0xFFFFFF, escuro: 0x690005)
    static let primaryContainer = Color(claro: 0xFFDAD6, escuro: 0x93000A)
    static let onPrimaryContainer = Color(claro: 0x410002, escuro: 0xFFDAD6)
    static let secondaryContainer = Color(claro: 0xD9E4EA, escuro: 0x334148)
    static let onSecondaryContainer = Color(claro: 0x101D23, escuro: 0xD9E4EA)

    static let fundo = Color(claro: 0xFEF7FF, escuro: 0x141218)
    static let cartao = Color(claro: 0xF3EDF7, escuro: 0x211F26)
    static let barra = Color(claro: 0xF3EDF7, escuro: 0x211F26)
    static let dialogo = Color(claro: 0xECE6F0, escuro: 0x2B2930)
    static let trilho = Color(claro: 0xE6E0E9, escuro: 0x36343B)
    static let surfaceVariant = Color(claro: 0xE7E0EC, escuro: 0x49454F)

    static let onSurface = Color(claro: 0x1D1B20, escuro: 0xE6E0E9)
    static let onSurfaceVariant = Color(claro: 0x49454F, escuro: 0xCAC4D0)
    static let outline = Color(claro: 0x79747E, escuro: 0x938F99)
    static let outlineVariant = Color(claro: 0xCAC4D0, escuro: 0x49454F)
    // Botão em relevo (o secundário, que era só contorno): fundo claro em degradê + borda fina + sombra.
    static let relevoTopo = Color(claro: 0xFFFFFF, escuro: 0x423F49)
    static let relevoBase = Color(claro: 0xF3EBEF, escuro: 0x302D36)
    static let relevoBorda = Color(claro: 0xE2D8DF, escuro: 0x55515C)

    static let ok = Color(claro: 0x2E7D32, escuro: 0x81C784)
    static let warning = Color(claro: 0xE65100, escuro: 0xFFB74D)
    static let error = Color(claro: 0xB3261E, escuro: 0xF2B8B5)

    // Botões de SOS: cores fortes e iguais nos dois temas (botão de emergência não fica pastel).
    static let seguranca = Color(hex: 0xD32F2F)
    static let saude = Color(hex: 0x1E66D0)
    static let segurancaTeste = Color(hex: 0x616161)
    static let saudeTeste = Color(hex: 0x9E9E9E)

    /// O vermelho da tela do guardião (alerta recebido).
    static let alertaRecebido = Color(hex: 0xB00020)
}

// MARK: - Textos

/// Título da tela (headlineSmall do Android).
struct TituloTela: View {
    let texto: String
    init(_ texto: String) { self.texto = texto }
    var body: some View {
        Text(texto)
            .font(.system(size: 24))
            .foregroundStyle(Cor.onSurface)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Texto de ajuda, menor e mais claro (HelpText do Android).
struct TextoAjuda: View {
    let texto: String
    init(_ texto: String) { self.texto = texto }
    var body: some View {
        Text(texto)
            .font(.system(size: 13))
            .foregroundStyle(Cor.onSurfaceVariant)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Cartões

/// Cartão com título (SectionCard do Android): cantos de 12, fundo do cartão.
struct Cartao<Conteudo: View>: View {
    let titulo: String?
    let conteudo: Conteudo

    init(_ titulo: String? = nil, @ViewBuilder conteudo: () -> Conteudo) {
        self.titulo = titulo
        self.conteudo = conteudo()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let titulo {
                Text(titulo)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Cor.onSurface)
            }
            conteudo
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Cor.cartao, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Cartão que abre e fecha tocando no título (CollapsibleSectionCard). Fechado, mostra um resumo.
struct CartaoRecolhivel<Conteudo: View>: View {
    let titulo: String
    let resumo: String
    @Binding var aberto: Bool
    let conteudo: Conteudo

    init(_ titulo: String, resumo: String, aberto: Binding<Bool>, @ViewBuilder conteudo: () -> Conteudo) {
        self.titulo = titulo
        self.resumo = resumo
        self._aberto = aberto
        self.conteudo = conteudo()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) { aberto.toggle() }
            } label: {
                HStack(spacing: 8) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(titulo)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(Cor.onSurface)
                        if !aberto {
                            Text(resumo)
                                .font(.system(size: 13))
                                .foregroundStyle(Cor.onSurfaceVariant)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    Spacer(minLength: 0)
                    Image(systemName: aberto ? "chevron.up" : "chevron.down")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Cor.onSurface)
                        .frame(width: 24, height: 24)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(aberto ? "Fechar \(titulo)" : "Abrir \(titulo)")

            if aberto {
                VStack(alignment: .leading, spacing: 10) { conteudo }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Cor.cartao, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Linha divisória fina, entre itens de uma lista.
struct Divisor: View {
    var body: some View {
        Rectangle().fill(Cor.outlineVariant).frame(height: 1)
    }
}

// MARK: - Botões (pílula de 40 pt, como no Android)

enum TipoBotao { case cheio, tonal, contorno, texto }

struct EstiloM3: ButtonStyle {
    let tipo: TipoBotao
    let largo: Bool
    let alto: CGFloat
    let fonte: CGFloat
    @Environment(\.isEnabled) private var ativo

    init(tipo: TipoBotao, largo: Bool = false, alto: CGFloat = 40, fonte: CGFloat = 14) {
        self.tipo = tipo
        self.largo = largo
        self.alto = alto
        self.fonte = fonte
    }

    private var frente: Color {
        switch tipo {
        case .cheio: return Cor.onPrimary
        case .tonal: return Cor.onSecondaryContainer
        case .contorno, .texto: return Cor.primary
        }
    }

    private var fundo: AnyShapeStyle {
        switch tipo {
        case .cheio: return AnyShapeStyle(Cor.primary)
        case .tonal: return AnyShapeStyle(Cor.secondaryContainer)
        // 20/11, o Rodrigo: o botão só de contorno "não ficou parecendo um botão mesmo". Agora é em relevo
        // (degradê claro + borda fina + sombra embaixo), igual ao BotaoRelevo do Android e ao /app.
        case .contorno: return AnyShapeStyle(LinearGradient(colors: [Cor.relevoTopo, Cor.relevoBase], startPoint: .top, endPoint: .bottom))
        case .texto: return AnyShapeStyle(Color.clear)
        }
    }

    /// Todo botão que não é só texto tem cara de botão: sombra embaixo e "afunda" ao apertar.
    private var temRelevo: Bool { tipo != .texto }

    func makeBody(configuration: Configuration) -> some View {
        let apertado = configuration.isPressed && ativo
        return configuration.label
            .font(.system(size: fonte, weight: tipo == .contorno ? .semibold : .medium))
            .lineLimit(1)
            .minimumScaleFactor(0.75)
            .padding(.horizontal, tipo == .texto ? 12 : 24)
            .frame(minHeight: alto)
            .frame(maxWidth: largo ? .infinity : nil)
            .foregroundStyle(frente)
            .background(fundo, in: Capsule())
            .overlay(Capsule().stroke(tipo == .contorno ? Cor.relevoBorda : Color.clear, lineWidth: 1))
            .contentShape(Capsule())
            .shadow(color: Color.black.opacity(temRelevo && ativo ? (apertado ? 0.12 : 0.26) : 0),
                    radius: apertado ? 1 : 3, x: 0, y: apertado ? 1 : 2.5)
            .offset(y: temRelevo && apertado ? 1.5 : 0)
            .opacity(ativo ? (apertado && !temRelevo ? 0.8 : 1) : 0.38)
            .animation(.easeOut(duration: 0.08), value: apertado)
    }
}

/// Botão em pílula, com ícone opcional à esquerda.
struct BotaoM3: View {
    let titulo: String
    var icone: String? = nil
    var tipo: TipoBotao = .cheio
    var largo = false
    var alto: CGFloat = 40
    var fonte: CGFloat = 14
    let acao: () -> Void

    init(_ titulo: String, icone: String? = nil, tipo: TipoBotao = .cheio, largo: Bool = false,
         alto: CGFloat = 40, fonte: CGFloat = 14, acao: @escaping () -> Void) {
        self.titulo = titulo
        self.icone = icone
        self.tipo = tipo
        self.largo = largo
        self.alto = alto
        self.fonte = fonte
        self.acao = acao
    }

    var body: some View {
        Button(action: acao) {
            HStack(spacing: 8) {
                if let icone {
                    Image(systemName: icone).font(.system(size: 15, weight: .semibold))
                }
                Text(titulo)
            }
        }
        .buttonStyle(EstiloM3(tipo: tipo, largo: largo, alto: alto, fonte: fonte))
    }
}

/// Botão só com ícone (IconButton do Android), 40 pt.
struct BotaoIcone: View {
    let icone: String
    let rotulo: String
    var cor: Color = Cor.onSurfaceVariant
    let acao: () -> Void

    init(_ icone: String, rotulo: String, cor: Color = Cor.onSurfaceVariant, acao: @escaping () -> Void) {
        self.icone = icone
        self.rotulo = rotulo
        self.cor = cor
        self.acao = acao
    }

    var body: some View {
        Button(action: acao) {
            Image(systemName: icone)
                .font(.system(size: 18))
                .foregroundStyle(cor)
                .frame(width: 40, height: 40)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(rotulo)
    }
}

// MARK: - Chave (Switch do Material 3)

struct ChaveM3: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        let ligado = configuration.isOn
        return HStack(spacing: 12) {
            configuration.label
                .frame(maxWidth: .infinity, alignment: .leading)
            ZStack(alignment: ligado ? .trailing : .leading) {
                Capsule().fill(ligado ? Cor.primary : Cor.trilho)
                Capsule().stroke(ligado ? Cor.primary : Cor.outline, lineWidth: 2)
                Circle()
                    .fill(ligado ? Cor.onPrimary : Cor.outline)
                    .frame(width: ligado ? 24 : 16, height: ligado ? 24 : 16)
                    .padding(.horizontal, ligado ? 4 : 8)
            }
            .frame(width: 52, height: 32)
            .contentShape(Capsule())
            .onTapGesture {
                withAnimation(.easeInOut(duration: 0.15)) { configuration.isOn.toggle() }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityValue(ligado ? "ligado" : "desligado")
        .accessibilityAction { configuration.isOn.toggle() }
    }
}

/// Uma linha com texto e a chave à direita (SwitchRow do Android).
struct LinhaComChave: View {
    let titulo: String
    @Binding var ligado: Bool

    init(_ titulo: String, ligado: Binding<Bool>) {
        self.titulo = titulo
        self._ligado = ligado
    }

    var body: some View {
        Toggle(isOn: $ligado) {
            Text(titulo).font(.system(size: 16)).foregroundStyle(Cor.onSurface)
        }
        .toggleStyle(ChaveM3())
    }
}

// MARK: - Chip (FilterChip do Material 3)

struct ChipM3: View {
    let titulo: String
    let selecionado: Bool
    let acao: () -> Void

    init(_ titulo: String, selecionado: Bool, acao: @escaping () -> Void) {
        self.titulo = titulo
        self.selecionado = selecionado
        self.acao = acao
    }

    var body: some View {
        Button(action: acao) {
            HStack(spacing: 6) {
                if selecionado {
                    Image(systemName: "checkmark").font(.system(size: 12, weight: .bold))
                }
                Text(titulo).font(.system(size: 14, weight: .medium)).lineLimit(1)
            }
            .padding(.leading, selecionado ? 10 : 16)
            .padding(.trailing, 16)
            .frame(height: 32)
            .foregroundStyle(selecionado ? Cor.onSecondaryContainer : Cor.onSurfaceVariant)
            .background(selecionado ? Cor.secondaryContainer : Color.clear,
                        in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(selecionado ? Color.clear : Cor.outline, lineWidth: 1)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selecionado ? .isSelected : [])
    }
}

// MARK: - Campo de texto com borda (OutlinedTextField)

struct CampoM3: View {
    let rotulo: String
    @Binding var texto: String
    let placeholder: String
    let teclado: UIKeyboardType
    let palavras: Bool
    let maiusculas: Bool
    let erro: String?
    let autoFoco: Bool
    @FocusState private var focado: Bool

    init(_ rotulo: String, texto: Binding<String>, placeholder: String = "", teclado: UIKeyboardType = .default,
         palavras: Bool = false, maiusculas: Bool = false, erro: String? = nil, autoFoco: Bool = false) {
        self.rotulo = rotulo
        self._texto = texto
        self.placeholder = placeholder
        self.teclado = teclado
        self.palavras = palavras
        self.maiusculas = maiusculas
        self.erro = erro
        self.autoFoco = autoFoco
    }

    private var corDaBorda: Color {
        if erro != nil { return Cor.error }
        return focado ? Cor.primary : Cor.outline
    }

    private var capitalizacao: TextInputAutocapitalization {
        if maiusculas { return .characters }
        if palavras { return .words }
        return .sentences
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(rotulo)
                .font(.system(size: 12))
                .foregroundStyle(erro != nil ? Cor.error : (focado ? Cor.primary : Cor.onSurfaceVariant))
            TextField("", text: $texto, prompt: Text(placeholder).foregroundColor(Cor.onSurfaceVariant.opacity(0.55)))
                .font(.system(size: 16))
                .foregroundStyle(Cor.onSurface)
                .keyboardType(teclado)
                .textInputAutocapitalization(capitalizacao)
                .autocorrectionDisabled(teclado != .default || maiusculas)
                .focused($focado)
                .padding(.horizontal, 14)
                .frame(height: 52)
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(corDaBorda, lineWidth: (focado || erro != nil) ? 2 : 1)
                )
            if let erro {
                Text(erro)
                    .font(.system(size: 12))
                    .foregroundStyle(Cor.error)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .task {
            guard autoFoco else { return }
            try? await Task.sleep(nanoseconds: 350_000_000)
            focado = true
        }
    }
}

// MARK: - Balão de mensagem (como o SMS chega)

struct FormaBalao: Shape {
    func path(in r: CGRect) -> Path {
        let raio: CGFloat = 14
        var p = Path()
        p.move(to: CGPoint(x: r.minX, y: r.minY))
        p.addLine(to: CGPoint(x: r.maxX - raio, y: r.minY))
        p.addArc(tangent1End: CGPoint(x: r.maxX, y: r.minY), tangent2End: CGPoint(x: r.maxX, y: r.maxY), radius: raio)
        p.addArc(tangent1End: CGPoint(x: r.maxX, y: r.maxY), tangent2End: CGPoint(x: r.minX, y: r.maxY), radius: raio)
        p.addArc(tangent1End: CGPoint(x: r.minX, y: r.maxY), tangent2End: CGPoint(x: r.minX, y: r.minY), radius: raio)
        p.closeSubpath()
        return p
    }
}

struct BalaoMensagem: View {
    let rotulo: String?
    let texto: String

    init(_ rotulo: String?, _ texto: String) {
        self.rotulo = rotulo
        self.texto = texto
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let rotulo {
                Text(rotulo).font(.system(size: 14, weight: .bold)).foregroundStyle(Cor.onSurface)
            }
            Text(texto)
                .font(.system(size: 14))
                .foregroundStyle(Cor.onSurface)
                .fixedSize(horizontal: false, vertical: true)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Cor.surfaceVariant, in: FormaBalao())
        }
    }
}

// MARK: - Rodapé

enum Versao {
    static var numero: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
    }
    static var build: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
    }
}

struct Rodape: View {
    var versao = false

    var body: some View {
        VStack(spacing: 4) {
            if versao {
                Text("GuardiõesSOS · versão \(Versao.numero) (\(Versao.build))")
            }
            Text("Desenvolvido por Soluções da Web · QR Único Ltda · CNPJ 43.115.644/0001-38")
            if let url = URL(string: "https://guardioessos.com.br/privacidade") {
                Link(destination: url) { Text("Privacidade").underline() }
            }
        }
        .font(.system(size: 12))
        .foregroundStyle(Cor.onSurfaceVariant)
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
        .padding(.bottom, 4)
    }
}

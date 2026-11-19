import SwiftUI
import CoreLocation

/// Aba Lugares, igual à do Android: os lugares onde você costuma estar. Quando o SOS sai de
/// dentro da área de um lugar, as pessoas dele também são avisadas (os vizinhos, os colegas...).
struct LugaresView: View {
    @EnvironmentObject var app: AppState
    @Binding var aberto: String?

    init(aberto: Binding<String?>) {
        self._aberto = aberto
    }

    var body: some View {
        if let id = aberto, app.lugares.contains(where: { $0.id == id }) {
            EditorDeLugar(id: id, fechar: { aberto = nil })
        } else {
            lista
        }
    }

    struct Sugestao: Hashable {
        let rotulo: String
        let nome: String
    }

    /// Os atalhos "🏠 Casa" e "💼 Trabalho" (somem quando o lugar já existe).
    private var sugestoes: [Sugestao] {
        let todas = [Sugestao(rotulo: "🏠 Casa", nome: "Casa"), Sugestao(rotulo: "💼 Trabalho", nome: "Trabalho")]
        return todas.filter { s in
            !app.lugares.contains { $0.nome.compare(s.nome, options: .caseInsensitive) == .orderedSame }
        }
    }

    private var lista: some View {
        ScrollView {
            VStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    TituloTela("Lugares")
                    TextoAjuda("Cadastre os lugares onde você costuma ficar: casa, escritório, garagem... Quando o alerta sair de dentro de um lugar, as pessoas dele (por exemplo, os vizinhos) também recebem a mensagem.")
                }
                if !sugestoes.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        TextoAjuda("Comece pelos mais comuns:")
                        HStack(spacing: 8) {
                            ForEach(sugestoes, id: \.self) { s in
                                BotaoM3(s.rotulo, tipo: .contorno, largo: true) { novo(s.nome) }
                            }
                        }
                    }
                }
                ForEach(app.lugares) { l in
                    cartao(l)
                }
                HStack {
                    BotaoM3("Adicionar lugar", icone: "plus", tipo: .cheio) { novo("Novo lugar") }
                    Spacer()
                }
                Rodape()
            }
            .padding(16)
        }
    }

    private func cartao(_ l: Lugar) -> some View {
        Button {
            aberto = l.id
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.system(size: 22))
                    .foregroundStyle(Cor.primary)
                    .frame(width: 24)
                VStack(alignment: .leading, spacing: 2) {
                    Text(l.nome.isEmpty ? "Sem nome" : l.nome)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(Cor.onSurface)
                    Text("Área de \(l.raio) m · \(l.contatos.count) contato(s)")
                        .font(.system(size: 13))
                        .foregroundStyle(Cor.onSurfaceVariant)
                    if !l.temLocal {
                        Text("Falta marcar a localização")
                            .font(.system(size: 13))
                            .foregroundStyle(Cor.warning)
                    }
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Cor.onSurfaceVariant)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Cor.cartao, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func novo(_ nome: String) {
        let l = Lugar(nome: nome)
        app.lugares.append(l)
        aberto = l.id
    }
}

/// O editor de um lugar: nome, onde fica (a localização atual ou coordenadas coladas), o tamanho
/// da área (50 a 1000 m) e quem avisar neste lugar.
struct EditorDeLugar: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var camada: Camada
    @Environment(\.openURL) private var openURL
    let id: String
    let fechar: () -> Void
    @State private var procurando = false
    @State private var coordenadas = ""

    init(id: String, fechar: @escaping () -> Void) {
        self.id = id
        self.fechar = fechar
    }

    private var lugar: Lugar {
        app.lugares.first { $0.id == id } ?? Lugar(nome: "")
    }

    /// Muda o lugar guardado.
    private func mudar(_ f: (inout Lugar) -> Void) {
        guard let i = app.lugares.firstIndex(where: { $0.id == id }) else { return }
        f(&app.lugares[i])
    }

    private var nome: Binding<String> {
        Binding(get: { lugar.nome }, set: { novo in mudar { $0.nome = novo } })
    }

    private var raio: Binding<Double> {
        Binding(get: { Double(lugar.raio) }, set: { novo in mudar { $0.raio = Int(novo) } })
    }

    private var contatos: Binding<[Contato]> {
        Binding(get: { lugar.contatos }, set: { novo in mudar { $0.contatos = novo } })
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                HStack(spacing: 4) {
                    Button(action: fechar) {
                        Image(systemName: "arrow.left")
                            .font(.system(size: 20, weight: .medium))
                            .foregroundStyle(Cor.onSurface)
                            .frame(width: 40, height: 40)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Voltar para a lista de lugares")
                    TituloTela("Editar lugar")
                }

                CampoM3("Nome do lugar (ex.: Casa, Escritório)", texto: nome, palavras: true)

                Cartao("Onde fica") {
                    if let lat = lugar.latitude, let lng = lugar.longitude {
                        Text(String(format: "%.5f, %.5f", locale: Locale(identifier: "en_US_POSIX"), lat, lng))
                            .font(.system(size: 16))
                            .foregroundStyle(Cor.onSurface)
                        if let p = lugar.precisao {
                            TextoAjuda("Marcado ✓ (precisão de uns \(p) m)")
                        }
                        if let url = URL(string: Mensagem.linkMapa(lat, lng)) {
                            BotaoM3("Ver no mapa", tipo: .texto) { openURL(url) }
                                .padding(.leading, -12)
                        }
                    } else {
                        Text("Localização ainda não marcada")
                            .font(.system(size: 16))
                            .foregroundStyle(Cor.warning)
                    }
                    BotaoM3(procurando ? "Procurando o GPS..." : "Usar minha localização atual", tipo: .cheio, largo: true) {
                        Task { await marcarAqui() }
                    }
                    .disabled(procurando)
                    TextoAjuda("Faça isso estando no lugar (por exemplo, dentro de casa).")
                    CampoM3("Ou cole as coordenadas", texto: $coordenadas, placeholder: "-19.46830, -42.53670",
                            teclado: .numbersAndPunctuation)
                    TextoAjuda("No Google Maps, segure o dedo em cima do lugar e copie os números que aparecem.")
                    BotaoM3("Usar estas coordenadas", tipo: .contorno) { usarCoordenadas() }
                        .disabled(coordenadas.trimmingCharacters(in: .whitespaces).isEmpty)
                }

                Cartao("Tamanho da área") {
                    Text("Área de \(lugar.raio) m")
                        .font(.system(size: 16))
                        .foregroundStyle(Cor.onSurface)
                    Slider(value: raio, in: 50...1000, step: 50)
                    TextoAjuda("Se o alerta sair a até esta distância do ponto marcado, as pessoas deste lugar são avisadas.")
                }

                Cartao("Quem avisar neste lugar") {
                    ListaDeContatos(
                        contatos: contatos,
                        vazio: "Nenhum contato. Adicione seus vizinhos ou quem estiver perto deste lugar."
                    )
                }

                BotaoM3("Excluir este lugar", tipo: .contorno, largo: true) { confirmarExcluir() }
                Rodape()
            }
            .padding(16)
        }
    }

    private func marcarAqui() async {
        if app.local.negado {
            camada.dialogo(
                "Localização",
                "A localização do GuardiõesSOS está desligada. Toque em \"Abrir Ajustes\", depois em \"Localização\", e escolha \"Durante o Uso do App\".",
                acoes: [
                    AcaoDialogo("Agora não"),
                    AcaoDialogo("Abrir Ajustes") { Tela.abrirAjustesDoIPhone() },
                ]
            )
            return
        }
        procurando = true
        let p = await app.local.pegar(timeout: 30)
        procurando = false
        guard let p else {
            camada.toast("Não consegui pegar a localização. Confira se a Localização está ligada.")
            return
        }
        let precisao = Int(p.horizontalAccuracy.rounded())
        mudar {
            $0.latitude = p.coordinate.latitude
            $0.longitude = p.coordinate.longitude
            $0.precisao = precisao
        }
        app.atualizarLocal()
        camada.toast("Localização salva (precisão de \(precisao) m)")
    }

    private func usarCoordenadas() {
        let partes = coordenadas
            .replacingOccurrences(of: ";", with: ",")
            .split(whereSeparator: { $0 == "," || $0 == " " })
            .compactMap { Double($0) }
        guard partes.count == 2, abs(partes[0]) <= 90, abs(partes[1]) <= 180 else {
            camada.toast("Não entendi essas coordenadas")
            return
        }
        mudar {
            $0.latitude = partes[0]
            $0.longitude = partes[1]
            $0.precisao = nil
        }
        coordenadas = ""
        esconderTeclado()
        camada.toast("Localização salva")
    }

    private func confirmarExcluir() {
        let nome = lugar.nome.isEmpty ? "este lugar" : lugar.nome
        camada.dialogo(
            "Excluir \"\(nome)\"?",
            "Os contatos deste lugar também serão apagados.",
            acoes: [
                AcaoDialogo("Cancelar"),
                AcaoDialogo("Excluir") {
                    app.lugares.removeAll { $0.id == id }
                    fechar()
                },
            ]
        )
    }
}

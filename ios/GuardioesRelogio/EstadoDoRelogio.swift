import Foundation
import CoreLocation
import WatchKit

/// O estado do app do relógio: em que tela está (contando, enviando, resultado, menu), os ajustes da
/// pessoa e o que o iPhone sincronizou. Um só, para a tela e a Siri/Botão de Ação mexerem no mesmo.
@MainActor
final class EstadoDoRelogio: ObservableObject {
    static let unico = EstadoDoRelogio()

    enum Fase: Equatable {
        /// Contando para disparar sozinho (restante em segundos). Tocar cancela.
        case contando(Perfil, Int)
        /// O pedido está saindo (já não dá para cancelar).
        case enviando(Perfil)
        /// Saiu (ou, no modo teste, o teste funcionou).
        case saiu(Perfil, Disparo.Resultado)
        /// Não saiu: mostra "Não saiu" + "Tentar de novo".
        case naoSaiu(Perfil, Disparo.Resultado)
        /// Os dois botões grandes (Segurança e Saúde) e os ajustes.
        case menu
    }

    @Published private(set) var fase: Fase
    @Published var contagem: Int { didSet { Guardado.contagem = contagem } }
    @Published var discreto: Bool { didSet { Guardado.discreto = discreto } }
    @Published var tipoRapido: Perfil { didSet { Guardado.tipoRapido = tipoRapido } }
    @Published private(set) var contexto: ContextoDoRelogio?
    @Published private(set) var contextoEm: Date?
    @Published private(set) var ultimo: UltimoAlerta?

    let local = Localizacao()

    private var tarefa: Task<Void, Never>?
    private var posicao: Task<CLLocation?, Never>?
    private var vivo: MantemVivo?

    private init() {
        if Demo.ativo {
            contagem = Demo.contagem
            discreto = !Demo.visivel
            tipoRapido = Demo.saude ? .saude : .seguranca
            contexto = Demo.contexto
            contextoEm = Demo.contexto == nil ? nil : Date().addingTimeInterval(-12 * 60)
            ultimo = nil
            fase = EstadoDoRelogio.faseDemo(Demo.tela, Demo.saude ? .saude : .seguranca, contexto: Demo.contexto)
            return
        }
        let ctx = Guardado.contexto
        contagem = Guardado.contagem
        discreto = Guardado.discreto
        tipoRapido = Guardado.tipoRapido ?? ctx?.perfil ?? .seguranca
        contexto = ctx
        contextoEm = Guardado.contextoEm
        ultimo = Guardado.ultimo
        fase = .menu

        PonteComOIPhone.unica.aoReceberContexto = { [weak self] novo in
            guard let self else { return }
            self.contexto = novo
            self.contextoEm = Date()
            // Ainda não escolheu o tipo no relógio: acompanha o perfil do iPhone (sem gravar como
            // escolha do relógio).
            if Guardado.tipoRapido == nil {
                self.tipoRapido = novo.perfil
                Guardado.tipoRapido = nil
            }
        }
    }

    // MARK: - Abrir o app = começar a contar

    /// Abriu o app (ícone, complicação, Siri com a tela, Botão de Ação): já começa contando.
    /// Se já está contando ou enviando, deixa como está.
    func aoAbrir() {
        guard !Demo.ativo else { return }
        switch fase {
        case .contando, .enviando: return
        default: comecar(tipoRapido)
        }
    }

    /// Começa a contagem (ou dispara na hora, com 0 s). A localização já começa a ser buscada aqui,
    /// para estar pronta quando a contagem acabar.
    func comecar(_ perfil: Perfil) {
        guard !Demo.ativo else { return }
        tarefa?.cancel()
        Haptico.toque()
        // Mantém o app vivo até o fim, mesmo se a pessoa baixar o braço ou apertar a coroa.
        vivo?.soltar()
        vivo = MantemVivo("SOS do GuardiõesSOS")
        posicao = buscarPosicao()

        let total = contagem
        if total <= 0 {
            enviar(perfil)
            return
        }
        fase = .contando(perfil, total)
        tarefa = Task { [weak self] in
            var restante = total
            while restante > 0 {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard let self, !Task.isCancelled else { return }
                restante -= 1
                if restante > 0 { self.fase = .contando(perfil, restante) }
            }
            guard let self, !Task.isCancelled else { return }
            self.enviar(perfil)
        }
    }

    /// Tocou na tela durante a contagem: cancela e vai para o menu.
    func cancelar() {
        guard case .contando = fase else { return }
        tarefa?.cancel()
        tarefa = nil
        posicao = nil
        vivo?.soltar()
        vivo = nil
        fase = .menu
    }

    /// "Tentar de novo" (depois do "Não saiu"): dispara na hora, sem contar de novo.
    func tentarDeNovo() {
        guard case let .naoSaiu(perfil, _) = fase else { return }
        Haptico.toque()
        vivo?.soltar()
        vivo = MantemVivo("SOS do GuardiõesSOS")
        posicao = buscarPosicao()
        enviar(perfil)
    }

    func irParaOMenu() {
        switch fase {
        case .contando, .enviando: return
        default: fase = .menu
        }
    }

    private func enviar(_ perfil: Perfil) {
        fase = .enviando(perfil)
        let busca = posicao ?? buscarPosicao()
        posicao = nil
        tarefa = Task { [weak self] in
            let p = await busca.value
            let r = await Disparo.executar(perfil: perfil, posicao: p)
            guard let self else { return }
            self.guardarUltimo(r, perfil)
            if r.ok {
                Haptico.sucesso()
                self.fase = .saiu(perfil, r)
            } else {
                Haptico.falha()
                self.fase = .naoSaiu(perfil, r)
            }
            self.vivo?.soltar()
            self.vivo = nil
        }
    }

    // MARK: - Siri e Botão de Ação (sem abrir a tela)

    /// O SOS sem tela (App Intent): mesmo caminho, sem contagem.
    func dispararSemTela(_ perfil: Perfil) async -> Disparo.Resultado {
        let p = await buscarPosicao(timeout: 6).value
        let r = await Disparo.executar(perfil: perfil, posicao: p)
        guardarUltimo(r, perfil)
        if r.ok { Haptico.sucesso() } else { Haptico.falha() }
        return r
    }

    // MARK: - Localização

    /// Pede a permissão da localização (só num momento calmo: no menu, nunca no meio do SOS).
    func pedirLocalizacaoSeNuncaPediu() {
        guard !Demo.ativo, local.naoPerguntou else { return }
        local.pedirPermissao()
    }

    /// UMA leitura do GPS, com tempo limite. Sem permissão, nem tenta (e nunca pergunta aqui:
    /// a pergunta do sistema acenderia a tela no meio do modo discreto).
    private func buscarPosicao(timeout: TimeInterval = 8) -> Task<CLLocation?, Never> {
        let local = self.local
        return Task { @MainActor in
            guard !Demo.ativo, local.autorizado else { return nil }
            return await local.pegar(timeout: timeout)
        }
    }

    private func guardarUltimo(_ r: Disparo.Resultado, _ perfil: Perfil) {
        let u = UltimoAlerta(em: Date(), perfil: perfil, ok: r.ok, teste: r.teste, contatos: r.contatos, caminho: r.caminho)
        ultimo = u
        Guardado.ultimo = u
    }

    // MARK: - Demonstração

    private static func faseDemo(_ tela: String, _ perfil: Perfil, contexto: ContextoDoRelogio?) -> Fase {
        let teste = contexto?.emTeste ?? false
        switch tela {
        case "contando":
            return .contando(perfil, Demo.contagem == 0 ? 3 : Demo.contagem)
        case "enviando":
            return .enviando(perfil)
        case "saiu":
            return .saiu(perfil, Disparo.Resultado(ok: true, teste: teste, contatos: 4, caminho: .iphone))
        case "saiu-internet":
            return .saiu(perfil, Disparo.Resultado(ok: true, teste: teste, contatos: 4, caminho: .internet))
        case "nao-saiu":
            return .naoSaiu(perfil, Disparo.Resultado(ok: false, teste: false, contatos: 0, caminho: .nenhum,
                                                      motivo: "Sem internet no relógio e o iPhone longe."))
        default:
            return .menu
        }
    }
}

/// A vibração do relógio (o "taptic"): é por ela que a pessoa sabe o que aconteceu sem olhar.
enum Haptico {
    /// Toque curto: abriu e começou a contar.
    static func toque() { tocar(.click) }
    /// O pedido saiu.
    static func sucesso() { tocar(.success) }
    /// Não saiu.
    static func falha() { tocar(.failure) }

    private static func tocar(_ tipo: WKHapticType) {
        guard !Demo.ativo else { return }
        WKInterfaceDevice.current().play(tipo)
    }
}

/// Pede ao watchOS para não suspender o app enquanto o SOS não termina (a pessoa pode baixar o
/// braço ou apertar a coroa no meio). Solta sozinho no fim, ou quando o sistema exigir.
final class MantemVivo {
    private let sinal = DispatchSemaphore(value: 0)

    init(_ motivo: String, limite: TimeInterval = 90) {
        let sinal = self.sinal
        ProcessInfo.processInfo.performExpiringActivity(withReason: motivo) { expirando in
            if expirando {
                // O sistema vai suspender: libera a espera da primeira chamada.
                sinal.signal()
                return
            }
            _ = sinal.wait(timeout: .now() + limite)
        }
    }

    func soltar() {
        sinal.signal()
    }
}

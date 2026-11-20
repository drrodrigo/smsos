import SwiftUI
import CoreLocation
import UIKit
import AudioToolbox

/// O SOS de verdade: pega a localização, escolhe quem recebe (guardiões + pessoas do lugar),
/// manda pela nossa estrutura e guarda no histórico. Igual ao AlertManager do Android e ao
/// disparar() da versão no navegador. O Apple Watch pede o SOS pelo mesmo executar() (PonteComORelogio).
enum Alerta {
    /// Quem recebe e o lugar (a regra mora em Comum/QuemRecebe.swift, que o relógio também usa).
    typealias Destino = QuemRecebe.Destino

    static func destinatarios(guardioes: [Contato], lugares: [Lugar], posicao: CLLocation?) -> Destino {
        QuemRecebe.calcular(guardioes: guardioes, lugares: lugares, posicao: posicao)
    }

    /// O que aconteceu no SOS, sem tela nenhuma.
    struct Resultado {
        let teste: Bool
        let destino: Destino
        let mensagem: String
        /// Os números (11 dígitos) que a nossa estrutura aceitou.
        let aceitos: Set<String>

        var total: Int { destino.contatos.count }
        var enviados: Int { aceitos.count }
    }

    /// O SOS de verdade, SEM TELA: pega a localização, escolhe quem recebe, monta o texto e, fora do
    /// modo teste, manda pela nossa estrutura; guarda no histórico e marca o alerta ativo.
    /// É o MESMO caminho do botão SOS do iPhone (disparar, logo abaixo) e do pedido do Apple Watch.
    /// - posicaoDada: a posição que veio de fora (a do relógio); sem ela, pega a do iPhone.
    /// - etapa: avisa a tela quando passa de "pegando a localização" para "enviando".
    @MainActor
    static func executar(_ perfil: Perfil, app: AppState, posicaoDada: CLLocation? = nil,
                         etapa: ((String) -> Void)? = nil) async -> Resultado {
        let posicao: CLLocation?
        if let posicaoDada {
            posicao = posicaoDada
        } else {
            posicao = Demo.ativo ? nil : await app.local.pegar(timeout: 8)
        }
        let destino = destinatarios(guardioes: app.guardioes, lugares: app.lugares, posicao: posicao)
        let lat = posicao?.coordinate.latitude
        let lng = posicao?.coordinate.longitude
        let msg = Mensagem.alerta(nome: app.nome, perfil: perfil, latitude: lat, longitude: lng)
        let total = destino.contatos.count

        if app.emTeste {
            app.registrar(RegistroAlerta(em: Date(), teste: true, perfil: perfil, total: total, enviados: 0,
                                         lugar: destino.lugar, latitude: lat, longitude: lng))
            return Resultado(teste: true, destino: destino, mensagem: msg, aceitos: [])
        }

        if total == 0 {
            return Resultado(teste: false, destino: destino, mensagem: msg, aceitos: [])
        }

        etapa?("Enviando o alerta…")
        let aceitos = await Gateway.enviar(telefones: destino.contatos.map { $0.telefone }, texto: msg)
        app.registrar(RegistroAlerta(em: Date(), teste: false, perfil: perfil, total: total, enviados: aceitos.count,
                                     lugar: destino.lugar, latitude: lat, longitude: lng))
        if !aceitos.isEmpty {
            app.ativo = AlertaAtivo(em: Date(), telefones: Array(aceitos))
        }
        return Resultado(teste: false, destino: destino, mensagem: msg, aceitos: aceitos)
    }

    /// O botão SOS do iPhone: o mesmo executar(), com a espera, o diálogo do resultado e o plano B.
    @MainActor
    static func disparar(_ perfil: Perfil, app: AppState, camada: Camada, irPara: @escaping (Aba) -> Void) async {
        vibrar()
        camada.esperando("Pegando sua localização…")
        let r = await executar(perfil, app: app) { camada.esperando($0) }
        let total = r.total
        let msg = r.mensagem

        if r.teste {
            let texto: String
            if total == 0 {
                texto = "Nenhum contato receberia o alerta. Cadastre contatos."
            } else {
                let nomes = r.destino.contatos.map { $0.nome }.joined(separator: ", ")
                texto = "Seria enviado para \(total) contato(s): \(nomes).\nLugar: \(r.destino.lugar ?? "nenhum lugar cadastrado").\n\n\(msg)"
            }
            camada.dialogo("Teste: o alerta funcionou (nenhum SMS enviado)", texto)
            return
        }

        if total == 0 {
            camada.fechar()
            Demonstracao.semGuardioes(perfil, app: app, camada: camada, irPara: irPara)
            return
        }

        let enviados = r.enviados
        if enviados > 0 {
            vibrar()
            let faltou = r.destino.contatos.filter { !r.aceitos.contains($0.telefone) }
            if !faltou.isEmpty {
                planoB("Alerta enviado para \(enviados) de \(total)",
                       "\(faltou.count) contato(s) não receberam pelo nosso envio. Mande para eles pelo SMS do seu celular:",
                       contatos: faltou, msg: msg, camada: camada)
                return
            }
            camada.dialogo("Alerta enviado ✓", "\(enviados) contato(s) avisado(s) com a sua localização.")
            return
        }

        planoB("O alerta não saiu",
               "Sem internet ou o envio falhou. Mande agora pelo SMS do seu celular (abre já com o texto e os números):",
               contatos: r.destino.contatos, msg: msg, camada: camada)
    }

    /// Plano B: o alerta não saiu pela nossa estrutura → abrir o SMS do próprio celular (ou ligar 190).
    @MainActor
    static func planoB(_ titulo: String, _ texto: String, contatos: [Contato], msg: String, camada: Camada) {
        let numeros = contatos.map { "+55" + Telefone.nacional($0.telefone) }
        camada.dialogo(titulo, texto, acoes: [
            AcaoDialogo("Ligar 190") {
                if let url = URL(string: "tel:190") { UIApplication.shared.open(url) }
            },
            AcaoDialogo("Mandar pelo SMS do celular") {
                SmsDoCelular.abrir(telefones: numeros, texto: msg)
            },
        ])
    }

    /// "Estou bem — avisar todos": manda o SMS de tudo bem para quem recebeu o alerta.
    @MainActor
    static func avisarTudoBem(app: AppState, camada: Camada) async {
        guard let ativo = app.ativo else { return }
        camada.esperando("Enviando…")
        let aceitos = await Gateway.enviar(telefones: ativo.telefones, texto: Mensagem.tudoBem(nome: app.nome))
        camada.fechar()
        if aceitos.isEmpty {
            camada.toast("Não deu para enviar agora. Tente de novo.")
        } else {
            app.ativo = nil
            camada.toast("Aviso de \"tudo bem\" enviado ✓")
        }
    }

    /// Vibra (o "pronto, disparou" do Android).
    static func vibrar() {
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }
}

/// O cartão "Demonstração e teste" e os diálogos que ele abre. Nada aqui envia SMS.
enum Demonstracao {
    static func textos(nome: String) -> (seguranca: String, saude: String) {
        let n = nome.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Maria" : nome
        let seg = Mensagem.alerta(nome: n, perfil: .seguranca,
                                  latitude: Mensagem.exemploLatitude, longitude: Mensagem.exemploLongitude)
        let sau = Mensagem.alerta(nome: n, perfil: .saude,
                                  latitude: Mensagem.exemploLatitude, longitude: Mensagem.exemploLongitude)
        return (seg, sau)
    }

    @MainActor
    static func comoChega(app: AppState, camada: Camada) {
        let t = textos(nome: app.nome)
        camada.abrir {
            CaixaDialogo("Como a mensagem chega", rolavel: true, acoes: [AcaoDialogo("Fechar")]) {
                Text("É assim que o seu guardião recebe (exemplo — nada foi enviado):")
                    .font(.system(size: 14))
                    .foregroundStyle(Cor.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
                BalaoMensagem("🛡️ Segurança", t.seguranca)
                BalaoMensagem("❤️ Saúde", t.saude)
            }
        }
    }

    @MainActor
    static func telaDoGuardiao(app: AppState, camada: Camada) {
        camada.guardiao = "Isto é um TESTE. " + textos(nome: app.nome).seguranca
    }

    @MainActor
    static func semGuardioes(_ perfil: Perfil, app: AppState, camada: Camada, irPara: @escaping (Aba) -> Void) {
        let dois = app.dualSos
        camada.abrir {
            CaixaDialogo(perfil == .saude ? "SOS de Saúde" : "SOS de Segurança", acoes: [
                AcaoDialogo("Agora não"),
                AcaoDialogo("Cadastrar guardiões") { irPara(.guardioes) },
            ]) {
                Text("Pra esse botão enviar o alerta, cadastre seus guardiões — as pessoas que recebem o aviso com a sua localização.")
                    .font(.system(size: 14))
                    .foregroundStyle(Cor.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
                if dois {
                    TextoAjuda("Os dois SOS (Segurança e Saúde) ficam ativos. Se quiser usar só um, você desliga em Ajustes.")
                }
            }
        }
    }
}

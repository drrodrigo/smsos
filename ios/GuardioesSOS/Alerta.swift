import SwiftUI
import CoreLocation
import UIKit
import AudioToolbox

/// O SOS de verdade: pega a localização, escolhe quem recebe (guardiões + pessoas do lugar),
/// manda pela nossa estrutura e guarda no histórico. Igual ao AlertManager do Android e ao
/// disparar() da versão no navegador.
enum Alerta {
    struct Destino {
        let contatos: [Contato]
        let lugar: String?
    }

    /// Quem recebe (igual ao destinatarios() do app.js / RecipientResolver.kt): os guardiões sempre;
    /// as pessoas do lugar onde você está; e, sem GPS, as pessoas de TODOS os lugares.
    static func destinatarios(guardioes: [Contato], lugares: [Lugar], posicao: CLLocation?) -> Destino {
        var lista: [Contato] = []
        var vistos = Set<String>()
        var lugar: String? = nil

        func incluir(_ c: Contato) {
            let t = Telefone.nacional(c.telefone)
            guard Telefone.celularValido(t), !vistos.contains(t) else { return }
            vistos.insert(t)
            lista.append(Contato(id: c.id, nome: c.exibicao, telefone: t))
        }

        for c in guardioes { incluir(c) }
        for l in lugares {
            let dentro: Bool
            if let p = posicao, let lat = l.latitude, let lng = l.longitude {
                dentro = p.distance(from: CLLocation(latitude: lat, longitude: lng)) <= Double(l.raio)
            } else {
                dentro = posicao == nil
            }
            if dentro {
                if posicao != nil && lugar == nil { lugar = l.nome }
                for c in l.contatos { incluir(c) }
            }
        }
        return Destino(contatos: lista, lugar: lugar)
    }

    @MainActor
    static func disparar(_ perfil: Perfil, app: AppState, camada: Camada, irPara: @escaping (Aba) -> Void) async {
        vibrar()
        camada.esperando("Pegando sua localização…")
        let posicao: CLLocation? = Demo.ativo ? nil : await app.local.pegar(timeout: 8)
        let destino = destinatarios(guardioes: app.guardioes, lugares: app.lugares, posicao: posicao)
        let lat = posicao?.coordinate.latitude
        let lng = posicao?.coordinate.longitude
        let msg = Mensagem.alerta(nome: app.nome, perfil: perfil, latitude: lat, longitude: lng)
        let total = destino.contatos.count

        if app.emTeste {
            app.registrar(RegistroAlerta(em: Date(), teste: true, perfil: perfil, total: total, enviados: 0,
                                         lugar: destino.lugar, latitude: lat, longitude: lng))
            let texto: String
            if total == 0 {
                texto = "Nenhum contato receberia o alerta. Cadastre contatos."
            } else {
                let nomes = destino.contatos.map { $0.nome }.joined(separator: ", ")
                texto = "Seria enviado para \(total) contato(s): \(nomes).\nLugar: \(destino.lugar ?? "nenhum lugar cadastrado").\n\n\(msg)"
            }
            camada.dialogo("Teste: o alerta funcionou (nenhum SMS enviado)", texto)
            return
        }

        if total == 0 {
            camada.fechar()
            Demonstracao.semGuardioes(perfil, app: app, camada: camada, irPara: irPara)
            return
        }

        camada.esperando("Enviando o alerta…")
        let aceitos = await Gateway.enviar(telefones: destino.contatos.map { $0.telefone }, texto: msg)
        let enviados = aceitos.count
        app.registrar(RegistroAlerta(em: Date(), teste: false, perfil: perfil, total: total, enviados: enviados,
                                     lugar: destino.lugar, latitude: lat, longitude: lng))

        if enviados > 0 {
            app.ativo = AlertaAtivo(em: Date(), telefones: Array(aceitos))
            vibrar()
            let faltou = destino.contatos.filter { !aceitos.contains($0.telefone) }
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
               contatos: destino.contatos, msg: msg, camada: camada)
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

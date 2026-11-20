import CoreLocation

/// Quem recebe o SOS (igual ao destinatarios() do app.js / RecipientResolver.kt): os guardiões sempre;
/// as pessoas do lugar onde você está; e, sem GPS, as pessoas de TODOS os lugares.
/// Entra nos dois apps: o iPhone e o relógio (quando ele dispara sozinho) escolhem igual.
enum QuemRecebe {
    struct Destino {
        let contatos: [Contato]
        let lugar: String?
    }

    static func calcular(guardioes: [Contato], lugares: [Lugar], posicao: CLLocation?) -> Destino {
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
}

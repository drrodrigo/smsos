# App Store (iOS) — GuardiõesSOS

## ⚠️ SITUAÇÃO REAL — ler primeiro

Este app é **nativo Android** (Kotlin/Compose). **NÃO existe versão iOS ainda.**

Diferente do **PDV Único** (que é um app **web** e por isso consegue "subir sozinho" por um
wrapper/pipeline), o GuardiõesSOS precisa de um **build iOS próprio**. Isso exige:

- um **Mac com Xcode** (não dá pra gerar o `.ipa` no Linux desta sessão);
- a **conta Apple Developer** (você já tem);
- e um **app iOS de verdade**: ou reescrito em Swift/SwiftUI, ou o app refeito num framework
  **multiplataforma** (Flutter / React Native / Kotlin Multiplatform) que gera Android **e** iOS
  do mesmo código.

Ou seja: **não é o mesmo upload do PDV.** Os textos abaixo já ficam prontos pra quando o build
iOS existir — mas o build em si é um passo de desenvolvimento, não uma configuração na loja.

## Caminhos pra chegar no iOS (a gente decide isso)

- **A. Reescrever em SwiftUI** — melhor experiência no iPhone, porém o dobro de manutenção (duas
  bases de código).
- **B. Refazer multiplataforma (Flutter é o mais indicado)** — uma base de código só para Android
  **e** iOS. Reaproveita a lógica (gateway de SMS, push, regras do número) e refaz a tela. É o
  caminho que normalmente compensa quando o app ainda é novo. **Recomendado.**
- **C. Capacitor/PWA** (provavelmente o que o PDV usa) — só serve se houver **versão web** do app.
  Hoje não há, e para um app de pânico (som alto, tela cheia, push com o app fechado) o wrapper web
  não entrega bem. **Não recomendado.**

## ⚠️ Pegadinha técnica do iOS (importante pro produto)

O recurso **"toca como uma ligação"** é fácil no Android e **difícil no iOS**:

- Notificação comum no iOS **não toca alto** no silencioso.
- Pra tocar como chamada: **CallKit/PushKit** (fluxo de VoIP) ou **Critical Alerts**, que exige um
  **pedido especial de aprovação à Apple** (uso justificado: segurança/saúde).
- Vale planejar isso **antes**, porque é o coração do alerta.

## Ficha da App Store (textos prontos)

- **Nome** (até 30): `GuardiõesSOS`
- **Subtítulo** (até 30): `Botão de pânico e SOS`
- **Categoria:** Estilo de Vida (alt.: Utilitários)
- **Classificação etária:** 4+
- **URL de suporte:** https://www.guardioessos.com.br
- **Política de privacidade:** https://www.guardioessos.com.br/privacidade
- **Exclusão de conta:** https://www.guardioessos.com.br/excluir

### Texto promocional (até 170)
```
Um toque e seus guardiões recebem seu SOS com a sua localização. Um botão de pânico de verdade, sempre com você — pra sua segurança e de quem você ama.
```

### Palavras-chave (até 100, separadas por vírgula, sem espaços)
```
pânico,SOS,emergência,segurança,botão,alerta,guardião,socorro,localização,família,proteção,ajuda
```

### Descrição
Reaproveitar a **descrição completa** de `docs/play-store.md` (seção 3) — a mensagem é a mesma.

### Privacidade (App Privacy / "nutrition label")
- **Localização (precisa):** funcionalidade do app, vinculada ao usuário, **sem rastreamento**.
- **Telefone / Nome:** funcionalidade, vinculada ao usuário, sem rastreamento.
- **Identificadores (token de push):** funcionalidade, sem rastreamento.
- Nada é usado para anúncios. **Não há Advertising ID.**

### Capturas (gerar nos tamanhos do iPhone)
iPhone 6.7" (1290x2796) e 6.5" (1242x2688). As **mesmas 4 telas** do Android servem de base
(`docs/loja/store-shots.html`), só mudando a proporção.

## Próximo passo (decisão sua)
Escolher entre **A (SwiftUI)** e **B (Flutter, recomendado)**. Depois disso eu monto o esqueleto do
projeto e a lista de tarefas. O build/assinatura/upload final roda **no seu Mac** (ou num serviço de
build na nuvem com macOS), porque esta sessão é Linux.

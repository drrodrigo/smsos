# Guardiões SOS — Passagem 08/11/2026

Chat: **Guardiões SOS**. Repositório do app: `drrodrigo/smsos` (namespace interno
`br.com.alertasilencioso`). Site: `drrodrigo/guardioessos` (guardioessos.com.br).

## Onde está (resumo rápido)

- **Android:** v1.0 **no ar na Play** (1º round aprovado).
- **iOS:** v1.0 **REJEITADO** pela Apple — **Guideline 2.1 (Information Needed)**. NÃO é erro de
  código: a Apple só quer informações + um vídeo. Resposta pronta pra colar e reenviar em
  **`docs/apple-review-notes.md`** (combinado: reenviar em 08/11).
- **Todo o "2º round" está na branch `feat/captura-sos`** (NÃO mesclado em beta/main). A produção
  da Play é um build anterior. 🔴 Os recursos sensíveis (câmera/mic/acessibilidade/localização 2º
  plano) estão **só no build de teste** — NÃO subir pra loja até a decisão (iOS em revisão).

## Canal de teste fora das lojas (Firebase App Distribution)

- App de teste **`br.com.guardioessos.teste`** registrado no projeto Firebase **qrbio**
  (`qrbio-1743174702100`). Testadores recebem atualização sozinhos (app "App Tester").
- Registrar o `.teste` no Firebase fez **o push/sirene do guardião funcionar no app de teste** também.
- Link direto (vai no `.teste` certo, sem os "dois GuardiõesSOS"):
  `https://console.firebase.google.com/project/qrbio-1743174702100/appdistribution/app/android:br.com.guardioessos.teste/releases`
- Já distribuído: v0.29.3 pra 3 testadores (por e-mail). Falta criar **grupo + "Link de convite"**
  pra divulgar sem adicionar e-mail um a um.

## O que foi feito nesta conversa (branch `feat/captura-sos`)

- **Botão lateral (o "coração") LIGADO só no build de teste** via `BuildConfig.DEBUG`
  (`core/Features.kt`): acessibilidade ligada no debug, `MonitorService` desligado; **release
  intocado**. O `<service>` da acessibilidade e `ACCESS_BACKGROUND_LOCATION` moram só em
  `app/src/debug/AndroidManifest.xml`.
  - **Tela LIGADA: FUNCIONA** no M54 e no S25. 🎉 Correção: o M54 não entrega `ACTION_UP` pela
    acessibilidade; agora cada toque solta o aperto anterior preso (coberto por
    `TriggerControllerTest`). Diagnóstico: contador "Tela ligada" no cartão "Testar sem enviar SMS".
  - **Tela APAGADA: ainda não** no M54/S25 (contador "Tela apagada" = 0), MAS o Rodrigo viu
    funcionar "num momento" → adicionado o **truque do som mudo** (`res/raw/silencio.wav` em loop,
    volume 0, em `ScreenOffVolumeListener`) pra manter a sessão de mídia ativa (**v0.31.0**).
    🔴 **AGUARDANDO o Rodrigo testar** se a tela apagada destravou.
- **Vídeo no SOS** (10s, câmera frontal, com áudio), opt-in, salva na galeria.
- **Cartão "Demonstração e teste"** (Início): "Ver como a mensagem chega" + "Testar a sirene e a
  tela do guardião" (local, não envia nada).
- **Botão "Convidar"** em cada guardião já cadastrado (um toque, sem redigitar).
- **Padrão de atualização de localização 3 → 1** (cada disparo mandava alerta + 3 "ATUALIZAÇÃO").
- **Cobrança (Billing):** a TELA dos planos existe; o **gating por plano NÃO** foi feito; billing
  não ligado.

## Decisões de negócio travadas (ver `docs/planos.md`)

- Preço **POR ANO**, barato (lógica Bill Gates). **Grátis = só PUSH** (sem SMS).
- **Proteção+ US$5/ano:** GPS ao vivo + "achar um ao outro" (grupo) + criar mais.
- **Completo US$10/ano:** + vídeo + microfone + casa/trabalho.
- **SMS = crédito pré-pago comprado no SITE** (R$0,30/SMS, mínimo R$10). O pânico por push NUNCA
  trava por pagamento.
- Specs novas: `docs/achar-um-ao-outro.md` (localização ao vivo em grupo, no plano de US$5),
  `docs/auto-update-apk.md` (escolhido Firebase App Distribution), `docs/demonstracao-e-autoteste.md`,
  `docs/apple-review-notes.md`. Ideia anotada: limite anti-abuso no grátis (com a ressalva da regra
  de ouro — socorro real nunca bloqueia).

## Versões de teste entregues (pasta `entregas/`)

0.29.3 (push) → 0.30.0 / 0.30.1 (botão lateral + correção dos toques) → **0.31.0** (som mudo p/
tela apagada). **Última = 0.31.0.**

## Pendências (o que falta)

1. 🔴 Rodrigo testar o **0.31.0**: a **tela apagada** destravou no Samsung? (contador "Tela apagada").
2. Construir a opção **"Os dois"** em "Como acionar" (segurar OU apertar — os dois disparam). PEDIDO dele.
3. **iOS:** reenviar com as notas + vídeo (`docs/apple-review-notes.md`).
4. Criar página privada **`guardioessos.com.br/painel`** com atalhos (App Distribution `.teste` +
   Play Console + App Store Connect) — proposto, aguardando OK dele.
5. Criar o **"Link de convite"** do grupo no App Distribution.
6. **Billing:** gating por plano + IDs anuais (hoje `_mensal`). Não ligar com iOS em revisão.
7. **Bate-bola do guardião** (`docs/onboarding-guardiao-bate-bola.md`).
8. **i18n inglês/espanhol** (vender pro mundo) — refactor grande.
9. **Sessão ao vivo** (mapa + áudio contínuo) — `docs/sessao-ao-vivo.md`.
10. QUANDO a Apple aprovar a v1.0: mesclar/subir o 2º round como **ATUALIZAÇÃO**.

## Cuidados que já custaram tempo

- Cada atualização do APK de teste **DESLIGA a acessibilidade** (Android) → **religar**. No
  Samsung: desligar o **"Bloqueador automático"** + **"Permitir configurações restritas"** no app.
- `app/google-services.json` (com o `.teste`) é local/gitignored; o `.teste` está registrado no
  Firebase qrbio.
- Build do APK de teste que ENVIA de verdade: `./gradlew :app:assembleDebug -Preal`
  (sem `-Preal` o gateway sai vazio e não envia — é o APK "inerte" de desenvolvimento).

# CLAUDE.md — GuardiõesSOS

App Android de **botão de pânico silencioso**. A pessoa segura o botão de volume (ou o botão
na tela) e, sem ninguém perceber, dispara um SMS com a localização pros seus **guardiões**.
Dois tipos de SOS: **Segurança** (perigo/assalto) e **Saúde** (passou mal/caiu).

Spec e decisões completas: [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Arte e imagem: eu NÃO gero aqui — quem gera é o GPT do Rodrigo (regra fixa)

> O Rodrigo paga o ChatGPT. **Não gaste os tokens dele com "fazer arte".** Qualquer imagem que
> o produto precise — logo, ícone, ilustração, arte de loja, banner, screenshot promocional —
> eu **não** desenho/gero aqui.

O que eu faço: entrego um **prompt/briefing pronto pra colar no ChatGPT**, dizendo exatamente o
que a imagem tem que ser (ou, quando é editar uma imagem que ele já tem, a instrução de edição).
Ele gera lá, me manda o arquivo, e eu só **integro** no app. Assim a gente ganha tempo e não
gasta token à toa.

**Exceção (isso é trabalho de dev, eu faço aqui):** processar um arquivo que o Rodrigo já mandou
— recortar, redimensionar, tirar fundo/deixar transparente, gerar as densidades do Android,
montar o ícone adaptativo, otimizar. Gerar a imagem nova é dele (via GPT); tratar a imagem
existente é meu.

Dica que já valeu: pra manter o logo **idêntico**, peça ao GPT pra **editar a imagem existente**
(não gerar do zero, senão o desenho muda), e sempre peça **PNG alta resolução com fundo
transparente**. O GPT não entrega SVG vetorial de verdade — e tudo bem, o app não precisa.

## Stack e build

- Kotlin + Jetpack Compose (Material 3), minSdk 26.
- `applicationId = br.com.guardioessos` (identidade da loja, PERMANENTE depois de publicar —
  era `br.com.smsos`, trocado antes da 1ª publicação); namespace interno `br.com.alertasilencioso`.
- Build/teste: `./gradlew test assembleDebug bundleRelease`. Rode **antes de qualquer PR**.
- APK de teste e AAB de loja saem em `entregas/` (nome `GuardioesSOS-teste-vX.Y.Z.apk` / `-loja-`).
- Segredos (chave do gateway, keystore de release) ficam **fora do git**: `gateway.properties` e
  `keystore/release.properties` estão no `.gitignore`. Nunca commite chave.

- 🔴 **SEM ACESSIBILIDADE em nenhum build (0.35.0, 24/11):** com o serviço de Acessibilidade no APK de teste, o
  **Nubank bloqueou a conta do Rodrigo** ("o aplicativo GuardiõesSOS teste possui características suspeitas"): app de
  fora da Play + Acessibilidade é o perfil do golpe da "mão fantasma", e o banco olha os apps que TÊM o serviço,
  ligado ou não. E no Samsung ela nem resolvia o volume com a tela apagada. `Features.ACESSIBILIDADE = false`; o
  `<service>` guardado em `docs/acessibilidade-service.xml.txt`. **Não leve a Acessibilidade para a loja**: o banco
  do cliente bloquearia e a pessoa desinstalaria o GuardiõesSOS. Tela apagada = SOS rápido + relógio.
- **SOS rápido pelo BOTÃO LATERAL (0.33.0, 21/11):** o Samsung NÃO entrega o botão de volume a app nenhum com a
  tela apagada (testado no aparelho do Rodrigo). Saída: o 2º ícone "SOS rápido" (`ui/SosRapidoActivity`, regras em
  `alert/SosRapido.kt`), SEMPRE LIGADO desde a 0.35.2 (Rodrigo, 24/11: "se tiver a opção de desligar, ele desliga,
  aí não consegue fazer pelo celular"; não traga a chave de volta). Escolhido no
  "Pressionar duas vezes" da Tecla lateral, abre por cima da tela bloqueada: tela preta, 3 s, dispara
  (`TriggerSource.BOTAO_LATERAL`); SEGURAR o dedo ou voltar cancela (toque leve não: o bolso cancelava, 0.33.1).
  **Contra curiosos (0.35.2):** pelo ÍCONE com o celular DESBLOQUEADO, só dispara no **3º toque seguido no ícone**
  (em 3 s); o 1º e o 2º fecham sem desenhar nada (tema transparente). SEMPRE, sem chave. (A 0.35.1 pedia 2 toques
  na tela preta; ele viu o furo: o curioso toca na tela preta "para testar" e dispara.) BLOQUEADO (botão lateral)
  dispara direto. Ajustes tem "Pôr o SOS rápido na tela inicial" (atalho fixado).
  **Botão lateral:** a lista do Samsung mostra APPS, não ícones, e abre a "porta de entrada" do pacote (escolher o
  GuardiõesSOS abria a configuração). A engrenagem ⚙️ de funções (0.36.1, atalho estático `res/xml/atalhos.xml`)
  NÃO aparece para app de outra empresa no Samsung dele. Desde a 0.36.2 a porta do pacote é o SOS rápido (filtro
  MAIN + INFO, que o Android escolhe antes do LAUNCHER): bloqueado = dispara; pela porta com o celular DESBLOQUEADO
  ("Abrir" da Play, informações do app) abre o MainActivity, nunca dispara (`SosRapido.comoAbrir`). O atalho
  estático continua (menu do ícone, `ORIGEM=atalho` = dispara direto). A 0.36.2 NÃO resolveu (o Samsung não usa a
  porta INFO): desde a 0.36.3 o SosRapidoActivity vem ANTES do MainActivity no manifesto (o Samsung guarda a 1ª
  tela "de abrir" do pacote); não troque a ordem (`SosRapidoTest` trava). Travas: `SosRapidoTest`.
- **App do RELÓGIO (Wear OS 3+, módulo `relogio/`, 20/11):** abriu = conta (3 s, tela preta no modo discreto) e
  dispara pelo celular (`OuvinteDoRelogio` → `AlertManager.trigger(..., TriggerSource.RELOGIO, ...)`). A ponte do
  Wear OS só liga app com o MESMO pacote e a MESMA chave: o relógio usa `br.com.guardioessos` (`.teste` no
  debug) e os MESMOS signingConfigs do celular; `versionCode` = o do celular **+100000** e `versionName` com o
  sufixo **`-relogio`** (subiu a versão do celular, suba a do relógio). O contrato (`/guardioes/sos` e
  `/guardioes/sos/resultado`, chaves do JSON) mora em `relogio/.../Contrato.kt` e `app/.../relogio/ContratoDoRelogio.kt`;
  o `ContratoDoRelogioTest` trava os dois lados. Detalhes e como instalar: `docs/relogio-sos.md`.

## Envio do alerta (gateway)

O alerta sai pela **nossa estrutura de SMS**, não pelo chip (o chip falhava: Bloqueador
automático da Samsung, número sem o 9/DDD). App → Edge Function `smsos-alerta` → `bipe-sms` →
SMS Market. Projeto Supabase `ymszgqpgxtprkqxjhtwb` (SoluçõesdaWeb). Captura de lead: schema
`guardioes` + função `guardioes-registrar` (nome + telefone + idioma, sem senha).

**O envio pelo chip foi REMOVIDO (v0.23.0).** A permissão `SEND_SMS` era o que fazia o Play
Protect bloquear a instalação (susto da Fernanda) e seria o maior risco na revisão da Play
Store. Como o envio pela nossa estrutura está provado, tiramos o chip: o app agora é
**só gateway** (precisa de internet). Se um dia a reserva offline fizer falta, sobe-se uma
versão depois. Não traga `SEND_SMS` de volta sem decisão explícita do Rodrigo.

**Push (FCM):** função `guardioes-push` (projeto SoluçõesdaWeb) mint o token OAuth com o
segredo `FCM_SERVICE_ACCOUNT` (service account do Firebase do QRBio) e dispara o FCM HTTP v1.
Manda DATA message (o app recebe sempre, mesmo fechado) com `tipo`: `alerta` faz o app TOCAR a
sirene; outro valor = aviso comum. Autentica com `x-smsos-app`.

**Fluxo real do SOS (0.28.0):** ao acionar, o app chama `guardioes-alertar` com o `dispositivo`
de quem acionou; a função usa a RPC `public.guardioes_tokens_dos_guardioes` (SECURITY DEFINER,
schema `guardioes` não exposto) pra achar os tokens dos GUARDIÕES que aceitaram e dispara a
sirene em cada um. Isso é EXTRA ao SMS do gateway (o SMS nunca depende do push). Helper no app:
`alert/Alertar.kt`. O alarme (som/vibra/lanterna/tela cheia) mora em `alert/Alarme.kt` +
`ui/AlertaRecebidoActivity.kt` + `push/MessagingService.kt` (canais `guardioes_alertas` e
`guardioes_sos`, criados no `App.onCreate`/na hora). Sirene embutida: `res/raw/sirene.wav`.
Pendente: limpar token morto quando o FCM devolve `UNREGISTERED` (404).

## Segurança do número = segurança da pessoa

Número errado = alerta não chega. Tudo é padronizado em `core/PhoneNumbers.kt`: tira
`+55`/traço/parênteses/espaço, **acrescenta o 9** do celular quando falta, guarda só os 11
dígitos e mostra `(31) 99999-8888`. Mexeu em contato? Padronize na entrada e avise em vermelho
quando faltar dígito.

## Dados sensíveis (LGPD)

O app guarda telefone, localização e histórico de alerta de gente real. O histórico fica **só no
aparelho** (dá pra apagar — proteção contra coação) e some sozinho depois de uns dias. Nunca
mande esses dados pra serviço que não seja o nosso gateway. Marketing/clube é **opt-in separado**
e nunca usa contato, localização ou histórico.

> **Regra de ouro: o alerta NUNCA pode ser bloqueado por falta de pagamento.** O que é pago é o
> extra (histórico na nuvem, mais guardiões, etc.), nunca o socorro.

## Comunicação

- Responda sempre em **português do Brasil**.
- Quando o Rodrigo tiver que fazer algo: **👉 + passo a passo numerado + o link direto**. Quando
  publicar algo, mande o **link da página/app**, não link de GitHub/PR/commit.

## Git

- Nunca commite funcionalidade direto na branch principal — branch + Pull Request.
- `./gradlew test assembleDebug bundleRelease` tem que passar antes do PR.

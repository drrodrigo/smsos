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
Recebe `tokens`/`token` + `titulo`/`corpo`; autentica com `x-smsos-app`. O canal
`guardioes_alertas` é criado no `App.onCreate` (senão push com app fechado não aparece).

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

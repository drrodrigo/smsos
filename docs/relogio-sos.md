# SOS pelo relógio (Galaxy Watch / Wear OS)

Ideia anotada em 20/11/2026 (pergunta do Rodrigo: dá para disparar o SOS do Guardião pelos botões do
relógio?) e **o 1º passo pronto no mesmo dia**: o app do relógio que dispara o SOS pelo celular.

## O que ficou pronto (20/11, `0.32.0-relogio`)

**No relógio** (módulo `relogio/`, Wear OS 3 em diante = Galaxy Watch 4, 5, 6, 7, Ultra e os outros Wear OS):

- **Abriu = já está contando.** Pelo ícone, pelo "pressionar duas vezes" do botão de cima ou pela complicação
  do mostrador. Ninguém precisa apertar mais nada: **dispara sozinho**.
- **Modo discreto (ligado de fábrica)**, pedido do Rodrigo: a tela fica **PRETA** (parece apagada, brilho no
  mínimo), sem número, sem a palavra SOS, sem vermelho. Só um "toque para cancelar" pequeno, cinza-escuro.
  Uma vibração **curta ao abrir** e uma **ao disparar** (não vibra a cada segundo).
- **Contagem de 3 s** (de fábrica). Dá para escolher **Na hora (0 s)**, 3 s ou 5 s.
- **Cancelar:** tocar em **qualquer lugar** da tela ou apertar o **botão de baixo** (voltar).
- **Resultado discreto** (a tela continua preta):
  - **vibração dupla curta** = o pedido chegou ao celular e os guardiões estão sendo avisados;
  - **vibração longa** = **NÃO saiu**. Aí aparece, em letra pequena, "Não saiu: celular longe" e o botão
    "Tentar de novo" (manda na hora, sem contar de novo). Também vibra longo e avisa "Cadastre guardiões no
    celular" quando o celular não tem nenhum guardião cadastrado.
  - Celular em **modo teste**: vibração dupla e "modo teste: nada foi enviado".
- **Modo discreto desligado** (a tela "visível"): número grande 3→0, "SOS de Segurança", botão enorme vermelho
  **CANCELAR**, botão pequeno "Trocar para Saúde", vibração curta a cada segundo; depois "Enviando…" e
  "Pedido enviado ✓ — seus guardiões estão sendo avisados" (ou "MODO TESTE: nada foi enviado", ou "Cadastre
  guardiões no celular", ou "O alerta NÃO saiu: celular longe ou desligado" + "Tentar de novo").
- **Depois de cancelar:** dois botões grandes **🛡️ SEGURANÇA** (vermelho) e **❤️ SAÚDE** (azul) — cada um abre
  a contagem de novo para aquele tipo — e **Ajustes**:
  - Contagem: Na hora (0 s) / 3 s / 5 s;
  - Modo discreto: ligado / desligado;
  - Tipo do SOS rápido: Segurança / Saúde (o que o botão, o ícone e a complicação abrem).
  Os ajustes ficam guardados no próprio relógio.
- **Complicação "SOS"** para o mostrador (texto curto "SOS" com o escudo, ou só o escudo): o toque abre o app
  já contando. Não se atualiza sozinha (não gasta bateria).
- **Bateria:** a tela fica acesa (e o relógio acordado) **só durante a contagem e o envio**. Depois o relógio
  apaga quando quiser.

**No celular** (`app/.../relogio/OuvinteDoRelogio.kt`): recebe o pedido do relógio, mesmo com o app fechado, e
dispara o **mesmo SOS do botão do app** (`AlertManager.trigger`, com o modo teste, o envio pelo nosso servidor,
o histórico e a sirene nos guardiões). No histórico aparece **"Acionado pelo relógio"**. Responde ao relógio se
deu certo, se está em modo teste e quantos guardiões receberiam.

**O contrato** (os dois lados falam a mesma língua): pedido no caminho `/guardioes/sos`
(`{"perfil":"seguranca"|"saude","id":"..."}`), resposta em `/guardioes/sos/resultado`
(`{"id","ok","teste","contatos","erro"}`). Mora em `relogio/.../Contrato.kt` e `app/.../relogio/ContratoDoRelogio.kt`;
o `ContratoDoRelogioTest` lê os dois arquivos e reprova se não baterem (e também se o pacote, a chave de teste
ou a versão do relógio saírem do combinado).

## Apertar 5 vezes o botão NÃO dá (nem segurar)

O Galaxy Watch **reserva os botões**: apertar o de cima várias vezes é o **SOS da própria Samsung** (avisa os
contatos de emergência da Samsung, não os guardiões do GuardiõesSOS), segurar o de cima liga/desliga ou chama
a assistente, e o de baixo é o "voltar". **Nenhum app de fora recebe esses apertos** — e botão de pânico que
"às vezes funciona" não serve. Os dois SOS convivem.

**O mais rápido possível é: apertar 2 vezes o botão de cima** → o GuardiõesSOS abre com a **tela preta** e
**dispara sozinho em 3 s** (ou na hora, se a contagem estiver em "Na hora"). Sem olhar e sem tocar em mais nada.

## 👉 Pôr o GuardiõesSOS no "pressionar duas vezes" (Galaxy Watch)

1. No relógio, abra **Configurações**.
2. Toque em **Recursos avançados**.
3. Toque em **Personalizar teclas**.
4. Toque em **Pressionar duas vezes** (na tecla Início, a de cima).
5. Escolha **GuardiõesSOS** (no APK de teste: **GuardiõesSOS teste**).

Pronto: dois apertos no botão de cima abrem o SOS já contando. Teste com o celular em **modo teste** primeiro.

## 👉 Pôr a complicação "SOS" no mostrador

1. No relógio, **toque e segure** o mostrador.
2. Toque em **Personalizar**.
3. Deslize até a tela das **Complicações** e toque num dos espaços.
4. Escolha **GuardiõesSOS** (ou "GuardiõesSOS teste") → **SOS**.
5. Aperte o botão de cima para salvar.

(Nem todo mostrador aceita complicações; os da Samsung, quase todos aceitam.)

## 👉 Instalar no relógio para testar (APK de teste, pelo Wi-Fi do relógio)

O relógio só conversa com o **GuardiõesSOS teste** do celular se os dois vierem da mesma fábrica (mesmo pacote
`br.com.guardioessos.teste` e mesma chave). Os dois APKs saem juntos no mesmo pacote da fábrica do GitHub
(Actions → "fábrica do Android" → a execução → Artifacts): `GuardioesSOS-teste-vX.Y.Z.apk` (celular) e
`GuardioesSOS-relogio-teste-vX.Y.Z.apk` (relógio).

1. **No celular:** instale (ou atualize) o **GuardiõesSOS teste** com o APK do celular, do mesmo pacote.
2. **No relógio, ligue o modo desenvolvedor:** Configurações → **Sobre o relógio** → **Informações do
   software** → toque umas 7 vezes em **Versão do software**, até aparecer "Modo de desenvolvedor ativado".
3. **No relógio, ligue a depuração pelo Wi-Fi:** Configurações → **Opções do desenvolvedor** → ligue
   **Depuração ADB** e **Depurar por Wi-Fi** (o relógio tem de estar no MESMO Wi-Fi do computador). Ele mostra
   um endereço do tipo `192.168.0.23:5555`. Nos relógios mais novos aparece também **Parear novo dispositivo**,
   com um código de 6 números e outro endereço (o de parear).
4. **No computador:** baixe as "SDK Platform-Tools" do Android
   (https://developer.android.com/tools/releases/platform-tools), descompacte, e ponha o APK do relógio na
   mesma pasta. Abra o terminal (no Windows: "Prompt de Comando") nessa pasta.
5. Se o relógio mostrou o código de parear: `adb pair ENDERECO-DE-PAREAR` e digite o código.
6. `adb connect ENDERECO-DO-RELOGIO` (o do passo 3) e aceite no relógio, se ele perguntar.
7. `adb install GuardioesSOS-relogio-teste-v0.32.0.apk` → aparece "Success" e o app surge na lista do relógio.
8. **Depois de instalar, desligue a Depuração ADB** no relógio (gasta bateria).

Sem computador: há apps na Play do celular que fazem esse mesmo passo pelo Wi-Fi (por exemplo, o
"Wear Installer 2"); os passos 2 e 3 são os mesmos.

**Teste sem gastar SMS:** ligue o **modo teste** no GuardiõesSOS do celular, abra o app no relógio e espere os
3 s: tem de vibrar duplo e, no celular, aparecer o aviso do teste e o registro "Acionado pelo relógio".

## A outra forma (decisão do Rodrigo): a faixa "Teste interno" da Play, no formato Wear OS

Em vez do ADB, o relógio baixa o app pela própria Play, como um testador:

1. Montar o pacote da loja do relógio com a chave da loja: `./gradlew :relogio:bundleRelease` (com o
   `keystore/release.properties`, fora do git) → `relogio/build/outputs/bundle/release/relogio-release.aab`
   (pacote `br.com.guardioessos`, código `100052`, versão `0.32.0-relogio`).
2. Play Console → GuardiõesSOS → **Configuração avançada → Formatos** → adicionar **Wear OS**.
3. **Teste → Teste interno** → escolher a faixa **"Somente no Wear OS"** → enviar o `relogio-release.aab`.
   (🔴 O do relógio vai SÓ na faixa do Wear OS; o do celular, na de celulares. O sufixo `-relogio` no nome
   da versão existe para não trocar as faixas.)
4. O celular da Play também precisa de uma versão com a ponte do relógio (esta, 0.32.0 em diante).

Isso passa pela revisão do Google para Wear OS e é decisão do Rodrigo (nada foi enviado à Play).

## Como o alerta sai

O Guardião **não manda SMS pelo chip** (removido na v0.23.0 — ver CLAUDE.md). O alerta sai pelo **nosso
servidor**, pela internet (`smsos-alerta` → `bipe-sms` → SMS Market, + sirene nos guardiões pela
`guardioes-alertar`). Por isso:

- **Relógio perto do celular (Bluetooth): PRONTO.** O relógio manda o pedido ao app do celular, que dispara
  como sempre. A localização vem do celular.
- **Relógio com Wi-Fi ou chip próprio (LTE), sem o celular:** ainda NÃO (próximo passo). O relógio chamaria o
  servidor direto, com o GPS do próprio relógio.
- Sem celular e sem internet no relógio: não sai (não há chip para SMS direto no Wear OS por app).

**Bom saber:**
- Com o app do celular fechado, a localização depende da permissão "Permitir o tempo todo" (o APK de teste
  pede; a versão da Play ainda não tem a localização em 2º plano). Sem ela, o alerta sai igual, só que sem o
  mapa e para todos os guardiões (é a regra de quando o GPS não responde).
- O celular espera 30 s entre um alerta e outro (5 s no modo teste). Disparar de novo nesse intervalo responde
  "chegou" (o alerta que já saiu continua valendo) e não manda outro.

## O que falta (próximos passos)

1. **Testar num Galaxy Watch de verdade** (até aqui tudo foi montado e testado só no computador).
2. **Envio direto pelo relógio com Wi-Fi/LTE** (sem o celular), com o GPS do relógio.
3. Fábrica da loja para o relógio (montar o `.aab` assinado no GitHub), se o Rodrigo decidir publicar.

## ✅ Testado de verdade (20/11/2026, 23h04) — Galaxy Watch do Rodrigo (SM-L315F)

Dois toques no botão de cima → tela preta → 3 s → o relógio mostrou **"modo teste: nada foi enviado"**:
o pedido foi pelo Bluetooth, o **GuardiõesSOS teste** do celular rodou o SOS em modo teste e respondeu.
O caminho inteiro (relógio → ponte do Wear OS → `OuvinteDoRelogio` → `AlertManager` → resposta) funciona.

**Envio REAL também (20/11, ~23h15):** com o modo teste desligado, dois toques no relógio → o SMS chegou à
Fernanda (guardiã) com a localização. Na 1ª vez o celular dela estava no silencioso e o SMS chegou calado (SMS
obedece o silencioso de quem recebe); na 2ª, fora do silencioso, apitou. Para tocar MESMO no silencioso, o
guardião precisa do app (aceitar o convite em Protejo): a sirene usa USAGE_ALARM no volume máximo (`Alarme.kt`).
Pendente de conferir: o histórico mostrou 2 disparos no mesmo minuto (23:05) no teste em modo teste — ver se o
relógio repete o pedido quando a resposta demora (o `id` do pedido existe para isso).

### Os pulos do gato da instalação (o que travou e como passou)
- **"Depurar por Wi-Fi" dizia "ligue o Wi-Fi" com o Wi-Fi ligado:** com o Bluetooth do relógio ligado ao
  celular, o Galaxy Watch não usa o Wi-Fi de verdade. **Desligue o Bluetooth do relógio** só durante a
  instalação (e religue depois: o SOS precisa dele).
- **Wear OS 4+ pede pareamento:** no Wear Installer 2, ⋮ → **Pair with watch** → **ENABLE** e digite
  **código ESPAÇO porta** (ex.: `697211 40787`), com a tela "Parear novo aparelho" do relógio ABERTA (o código
  muda quando fecha). Depois, na tela inicial, a porta de CONEXÃO é a da tela principal da Depuração sem fio.
- **O teclado do celular sem espaço:** desmarque "Use phone keypad for codes" (o teclado do app tem espaço).
- **"Could not connect to watch":** a porta no celular voltou para 5555; ponha a do relógio.
- **O arquivo vindo pelo WhatsApp não aparece** no Meus Arquivos nem no Wear Installer (pasta escondida).
  Baixe pelo navegador do celular (link direto) → vai para **Download**.
- 🔴 **NUNCA toque no APK do relógio no celular:** ele tem o MESMO pacote do app de teste do celular e
  versionCode maior, então o Android oferece **"Atualizar este app? GuardiõesSOS teste"** — aceitar trocaria
  o app do celular pelo do relógio. O APK do relógio só se abre pelo Wear Installer.
- No Wear Installer, INSTALL sem escolher o arquivo deu "There was a problem installing the apk": escolha o
  APK do relógio na lista de arquivos antes.

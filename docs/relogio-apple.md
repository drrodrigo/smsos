# GuardiõesSOS no Apple Watch

O app do relógio vai **dentro do app do iPhone**: quem instala o GuardiõesSOS no iPhone (TestFlight
ou App Store) ganha o do relógio junto. Ele faz a mesma coisa que o do Galaxy Watch: **abrir o app já
é o SOS**, de um jeito que não chama a atenção de ninguém.

Código: `ios/GuardioesRelogio/` (o app), `ios/GuardioesComplicacao/` (a complicação do mostrador),
`ios/Comum/` (o que o iPhone e o relógio dividem) e, no iPhone, `ios/GuardioesSOS/PonteComORelogio.swift`.

## O que ficou pronto

1. **Abrir o app = SOS discreto.** Pelo ícone, pela complicação do mostrador ou pelo Botão de Ação
   do Ultra: a tela fica **preta** (parece apagada), sem número, sem "SOS" e sem vermelho; só um texto
   pequeno, cinza-escuro, "toque para cancelar". Um toque curto de vibração ao abrir. Conta **3 s** e
   **dispara sozinho**; tocar em qualquer lugar cancela. Com o braço baixado (tela esmaecida), nem o
   texto pequeno aparece.
2. **O resultado vem pela vibração:** a de sucesso = o pedido saiu; a de falha = não saiu, e aí aparece
   em letra pequena "Não saiu" + **Tentar de novo** (que dispara na hora, sem contar de novo).
3. **Depois de cancelar:** os dois botões grandes, **🛡️ SEGURANÇA** (vermelho #D32F2F) e **❤️ SAÚDE**
   (azul #1E66D0), uma linha de status (pronto / modo teste / falta guardião / último SOS) e os
   **Ajustes**, guardados no relógio:
   - **Contagem:** Na hora (0 s) / 3 s / 5 s;
   - **Modo discreto:** ligado (de fábrica) ou desligado (tela acesa, número grande e CANCELAR vermelho);
   - **Tipo do SOS rápido:** Segurança ou Saúde (enquanto a pessoa não escolhe no relógio, vale o que
     está escolhido no iPhone);
   - o que veio do iPhone: o nome que vai na mensagem, quantos contatos, se está no modo teste.
4. **Como o alerta sai** (sempre pelo NOSSO servidor, `smsos-alerta`, nunca SMS do relógio):
   - **Com o iPhone por perto** (Bluetooth ou a mesma rede): o relógio pede ao iPhone
     (WatchConnectivity), o iPhone acorda em segundo plano, dispara pelo **mesmo caminho do botão SOS
     de lá** (respeitando o modo teste, com histórico e "Estou bem — avisar todos") e responde se saiu.
     O relógio manda junto a posição dele, porque o iPhone acordado em segundo plano não consegue pegar
     a dele (a permissão é "durante o uso").
   - **Sem o iPhone, com internet no relógio** (Wi-Fi ou celular): o relógio chama o `smsos-alerta`
     direto, com a mesma chave e o mesmo texto do SMS, os guardiões e lugares que o iPhone sincronizou e
     a localização do próprio relógio. Modo teste ligado no iPhone = o relógio também não envia. Depois,
     quando o iPhone aparece, o relógio conta o que fez (vai para o histórico do iPhone e liga o
     "Estou bem — avisar todos").
   - **Se o iPhone tentou e não saiu** (sem internet lá), o relógio tenta pela internet dele.
5. **Siri:** "E aí Siri, **socorro no GuardiõesSOS**" (também "SOS no GuardiõesSOS" e "Disparar o
   GuardiõesSOS") dispara **sem abrir a tela**. A ação "**Disparar o SOS do Guardião**" (App Intent)
   também aparece no app Atalhos, com a escolha opcional Segurança/Saúde. Quando sai, a Siri não fala
   nada de confirmação (o retorno é a vibração); quando não sai, ela diz "O SOS não saiu…".
6. **Complicação "SOS"** (circular, de canto e em linha): um toque abre o app já no modo discreto,
   contando.
7. **Bateria:** nada fica aceso ou ligado à toa. A tela é preta, a contagem é um relógio simples (sem
   animação), o GPS faz UMA leitura só quando um SOS começa (com tempo limite de 8 s) e a complicação
   não se atualiza nunca.

## Limites (o que é da Apple, não nosso)

- **O botão lateral e a coroa são da Apple.** Nenhum app consegue usá-los para disparar alguma coisa.
- **Segurar o botão lateral é o SOS de Emergência da própria Apple**: liga para a emergência e avisa os
  contatos de emergência do app Saúde. Ele **convive** com o nosso; não avisa os guardiões do
  GuardiõesSOS.
- **O Botão de Ação só existe no Apple Watch Ultra.** Ele roda um atalho escolhido pela pessoa (abaixo).
- **iPhone 8 → watchOS 9.** O iPhone 8 para no iOS 16, e o relógio emparelhado com ele fica no
  watchOS 9. Por isso o app do relógio pede **watchOS 9.0** (e o do iPhone continua no iOS 16).
- **Sem iPhone e sem internet no relógio, não sai**: app de relógio não manda SMS. Aparece "Não saiu"
  e a vibração de falha.
- **A localização do relógio** precisa da permissão no relógio. Ela é pedida na primeira vez que o menu
  aparece (depois do primeiro "toque para cancelar"), nunca no meio do SOS (a pergunta acenderia a
  tela). Sem permissão, o alerta vai com "localizacao indisponivel" e avisa as pessoas de todos os
  lugares (igual ao iPhone sem GPS).
- **Pode sair duas vezes**, raramente: se o iPhone recebeu o pedido mas a resposta se perdeu no
  caminho, o relógio tenta pela internet dele. Num botão de pânico, repetido é melhor do que nenhum.
- **Abrir o app sempre começa a contar** — inclusive para mudar os ajustes (abra, toque para cancelar,
  vá em Ajustes). Baixar e levantar o braço com o app aberto **não** conta de novo.
- **A complicação mostra "SOS" no mostrador**: ela é o atalho rápido, não é escondida.
- **A hora fica no canto de cima**: o watchOS mostra a hora em todo app, e nenhum app consegue tirar.
  No modo discreto a tela é preta só com a hora (e o "toque para cancelar" bem apagado): parece um
  mostrador simples.

## Como a pessoa usa

**Primeira vez**
1. Instale o GuardiõesSOS no iPhone. No app **Watch** do iPhone: **Meu Relógio** → GuardiõesSOS →
   **Instalar** (se "Instalar apps automaticamente" estiver ligado, ele já vem).
2. Abra o app no **iPhone** uma vez (é ele que manda o nome e os guardiões para o relógio).
3. Abra o app no **relógio**, toque para cancelar e toque em **Permitir** na localização.

**Complicação (o jeito mais rápido)**
1. No relógio, toque e segure o mostrador → **Editar** → deslize até as complicações.
2. Toque num espaço e escolha **GuardiõesSOS — SOS do Guardião**.
3. Um toque nela abre o app já contando.

**Siri**
- Levante o pulso e diga: "**E aí Siri, socorro no GuardiõesSOS**". Dispara sem abrir a tela.

**Botão de Ação (só Apple Watch Ultra)**
1. No relógio: **Ajustes** → **Botão de Ação** → **Ação**: **Atalho**.
2. Escolha o atalho do GuardiõesSOS ("Socorro no GuardiõesSOS"). Se ele não aparecer na lista, crie no
   app **Atalhos** do iPhone um atalho com a ação **Disparar o SOS do Guardião** (do GuardiõesSOS),
   ligue **Mostrar no Apple Watch** e escolha esse atalho no passo 2.
3. Outra opção: **Botão de Ação** → **Ação: Abrir app** → GuardiõesSOS (abre já contando, no discreto).

## Como testar

1. **TestFlight instala o app do relógio junto com o do iPhone** (o mesmo build). Depois de instalar o
   do iPhone pelo TestFlight, no app **Watch** do iPhone aparece o GuardiõesSOS em "Apps disponíveis":
   toque em **Instalar**.
2. **Teste seguro: deixe o modo teste ligado no iPhone** (vem ligado). O relógio faz tudo (conta, pega a
   localização, escolhe quem recebe, vibra o sucesso), o iPhone guarda "teste" no histórico e **nenhum
   SMS sai**.
3. Roteiro:
   1. iPhone perto, modo teste: abra o app no relógio, espere 3 s, sinta a vibração de sucesso; confira
      o histórico no iPhone.
   2. Abra de novo e toque para cancelar: os dois botões aparecem. Em **Ajustes**, desligue o modo
      discreto e dispare pelo botão SEGURANÇA: aparece o número grande e, no fim, "pelo iPhone".
   3. Sem o iPhone: Bluetooth do iPhone desligado e o relógio no Wi-Fi. Dispare: no modo visível aparece
      "pela internet do relógio".
   4. Sem iPhone e sem Wi-Fi (modo avião no relógio): "Não saiu" + "Tentar de novo".
   5. Siri e complicação, como acima.
4. **Os prints do simulador** saem a cada push na branch `feat/relogio-apple`, no workflow
   "iPhone compila (cara do Android)": artifact **telas-relogio** (os do iPhone continuam em
   **telas-iphone**). O modo `-demo` do relógio mostra cada tela sem contar, vibrar nem enviar nada.

**O que NÃO dá para testar no simulador:** a vibração, a conversa com o iPhone (o simulador do relógio
dos prints não está emparelhado), a Siri, o Botão de Ação, a complicação no mostrador e o envio de
verdade. Tudo isso só num relógio de verdade.

## A assinatura do relógio (feita em 22/11)

O `guardioes-iphone.yml` assina os 3 pacotes:

1. **Os dois App IDs novos**, `br.com.guardioessos.watchkitapp` (o app do relógio) e
   `br.com.guardioessos.watchkitapp.complicacao` (a complicação), o `ios/ci/assinatura.py` registra na
   Apple se faltarem (plataforma IOS, sem capacidade especial: WatchConnectivity e WidgetKit não pedem).
   O `br.com.guardioessos` continua sendo registrado pelo "preparar" da porta `guardioes-apple-envio`.
2. **Um perfil "App Store" por pacote** (os 3), com o MESMO certificado de distribuição. Cada um vai como
   `<uuid>.mobileprovision` e a rotina copia todos para o Mac. Saídas do passo: `perfil`,
   `perfil_relogio`, `perfil_complicacao`.
3. **O perfil por alvo no archive:** cada alvo do `project.yml` tem
   `PROVISIONING_PROFILE_SPECIFIER: "$(PERFIL_APP)"` / `"$(PERFIL_RELOGIO)"` / `"$(PERFIL_COMPLICACAO)"`,
   e a rotina passa as três variáveis na linha de comando (vazias no resto, que compila sem assinar).
4. **`ExportOptions.plist`** com os 3 pacotes em `provisioningProfiles`.
5. A versão e o número do build do relógio saem iguais aos do iPhone (a Apple exige): o mesmo
   `MARKETING_VERSION` do `project.yml` e o `CURRENT_PROJECT_VERSION` da linha de comando.
6. Na App Store Connect, o relógio não é um app separado: vai no mesmo registro do GuardiõesSOS. Para
   **publicar na loja** (não para o TestFlight), a Apple pede **prints do Apple Watch**; os do
   simulador estão no artifact `telas-relogio`.

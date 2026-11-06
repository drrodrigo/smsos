# Segundo round: a teclinha (o botão lateral) — o que dá, SEM arriscar a loja

> Resposta à sua dúvida do almoço: *"queima o cartucho logo e tenta habilitar a tecla, ou
> ajusta mais coisas?"* — li o motor inteiro do gesto no código. Tem um terceiro caminho,
> melhor que os dois.

## A notícia boa (em uma linha)

A alma do produto — **pegar o telefone e segurar pra disparar calado** — PODE sair no Android
**sem a Acessibilidade** (o maior motivo de recusa do Google). O motor já está pronto no nosso
código. Ou seja: **não precisa "queimar o cartucho".**

## Por que (sem enrolação)

O gesto do botão de volume tem DOIS caminhos no nosso app:

- **Tela LIGADA** → precisa da Acessibilidade. *(essa é a arriscada na Play)*
- **Tela APAGADA** → usa uma **"sessão de mídia"**: o telefone trata o app como um controle de
  música (tipo o de um Chromecast) e manda cada aperto do volume pro app, em vez de mudar o
  volume. **Isso NÃO é Acessibilidade.** O Google não revisa com vídeo nem recusa como recusa a
  Acessibilidade.

E o seu cenário é **exatamente** o de tela apagada: telefone na mão ou no bolso, parado, você
pega e segura. É aí que esse caminho funciona.

Hoje esse motor de tela apagada está **preso dentro do serviço de Acessibilidade**. Quando
desligamos a Acessibilidade pra passar na Play, ele desligou junto, de carona — não porque ele
precisasse. O trabalho do segundo round é **soltar esse motor** e botar pra rodar sozinho num
**serviço de primeiro plano** (aquela notificaçãozinha fixa: *"GuardiõesSOS protegendo você"*).

## O que você ganha

- Pega-e-segura, com o tempo que você escolher (5s, 10s…), **sem Acessibilidade**.
- Os **dois SOS já existem no código**: Volume − = Segurança, Volume + = Saúde.
- **Sem** a permissão de localização em 2º plano (a outra que o Google revisa com vídeo): o
  alerta sai com a última localização conhecida quando o app está fechado, e com o mapa certinho
  quando está aberto.

## Os limites honestos (pra não te vender ilusão)

1. **Só com a tela apagada.** Se a tela estiver LIGADA e o app fechado, o volume não dispara
   (isso só a Acessibilidade faz). Mas o seu cenário discreto é tela apagada — então tudo bem.
2. **Alguns fabricantes bloqueiam** esse truque da sessão de mídia. Não dá pra saber sem
   **testar no aparelho de verdade** — por isso o app já tem "modo teste".
3. **Se estiver tocando música**, o volume controla a música (o app cede). Caso de borda.
4. Precisa da **notificação fixa** do serviço (regra do Google — e até ajuda: mostra que o app
   está te protegendo).

## iPhone: a verdade

No iPhone **não dá** pra pegar o botão lateral com o app fechado — regra da Apple, não tem
truque. O que a Apple deixa (tudo por um "atalho" nosso dentro do app):

- **Toque Atrás** (seu iPhone 8 tem): toca 2 ou 3× nas costas do telefone → dispara o SOS.
  Funciona até com o telefone bloqueado.
- **Botão de Ação** (iPhone 15 Pro pra cima): segura o botão → mesmo SOS.
- **Siri**: *"E aí Siri, socorro"* → dispara sem tocar na tela.

É menos discreto que o botão lateral (você mesmo falou) — mas é o que existe no iPhone.

## A jogada esperta (minha recomendação)

Não queime o cartucho. Em vez disso:

1. Eu construo o caminho **sem Acessibilidade** numa branch — **não publico nada**.
2. Você **instala no SEU telefone** direto (de graça, sem passar pela Play) e testa o
   pega-e-segurar.
3. Funcionou bem → a gente lança como **atualização**. O app já está aprovado; atualização
   recusada **não derruba** o que está no ar.
4. O seu telefone bloqueou o truque → a gente aprendeu **de graça**, sem gastar cartucho nenhum
   na Play.

Assim você vê a alma do produto **funcionando antes** de arriscar qualquer coisa na loja.

---

## Parte técnica (pra mim, quando você der o "vai")

- Criar `MonitorService` (foreground service, `foregroundServiceType="specialUse"` com
  justificativa no manifesto; `mediaPlayback` como alternativa, já que hospeda uma MediaSession).
  Hospedar o `ScreenOffVolumeListener` ali (ele já recebe um `Context`, não depende de ser
  AccessibilityService — por isso a mudança é limpa).
- `BOOT_COMPLETED` receiver pra religar o serviço depois de reiniciar o celular.
- `Features.ACESSIBILIDADE` continua `false`. O caminho de tela ligada (`onAccessibilityKey`)
  fica dormindo — só volta se um dia a gente topar a Acessibilidade.
- Localização no disparo com app fechado: `lastLocation` (sem background location) ou tipo de
  FGS `location`. Decidir conforme o que o Google aceita hoje sem vídeo.
- iOS (branch separada, NUNCA no build que vai pra revisão agora): `AppIntent` + `AppShortcut`
  "SOS" que chama o mesmo `Gateway.enviar`. Testar com Toque Atrás no seu iPhone 8.
- Testes: o `PressPatternDetectorTest` já cobre o gesto (segurar/toques); manter verde.
  O truque da MediaSession só prova no aparelho real.

# Publicar o GuardiõesSOS na Google Play

Checklist completo pra primeira publicação. O que é **texto/declaração** já está pronto aqui
pra colar. O que é **imagem** vai como prompt pro ChatGPT (regra do projeto). O que é **técnico**
está no fim.

> ⚠️ **Leia primeiro a seção "3 pontos que o Google revisa com lupa".** Dois deles (SMS e
> Acessibilidade) podem fazer o app ser **recusado** se a gente não tratar antes de enviar.
> É melhor saber disso agora do que levar um "não" depois de montar tudo.

---

## 1. Conta e técnico

- [ ] **Conta Google Play Console** — taxa única de **US$ 25** (uma vez, não é mensal).
      Crie como conta de **desenvolvedor pessoal** (CPF) ou **empresa** (CNPJ, se tiver).
- [ ] **Arquivo do app**: o **.aab** (Android App Bundle), não o .apk. Já geramos:
      `entregas/GuardioesSOS-loja-vX.Y.Z.aab`.
- [ ] **Assinatura**: ative o **Play App Signing** (o Google guarda a chave). Você envia com a
      sua *upload key* (o `keystore/release.properties`). **Nunca perca esse keystore.**
- [ ] **Nome do pacote**: `br.com.smsos` — é **permanente**, não muda depois de publicado.
- [ ] **Versão de lançamento**: subir o `versionName` para **"1.0.0"** (hoje está 0.9.x).
      O `versionCode` é o número que sempre aumenta a cada envio.
- [ ] **Target API**: hoje `targetSdk = 36` — ok (o Google exige 34+ para apps novos).

---

## 2. Ficha da loja — TEXTOS (prontos pra colar)

### Nome do app (máx. 30 caracteres)
```
GuardiõesSOS
```

### Descrição curta (máx. 80 caracteres)
```
SOS silencioso: um toque avisa seus guardiões com a sua localização.
```

### Descrição completa (máx. 4000 caracteres)
```
GuardiõesSOS é o jeito mais rápido e discreto de pedir ajuda.

Em uma emergência, quem precisa de socorro nem sempre pode pegar o telefone, abrir um
aplicativo e digitar. Com o GuardiõesSOS, basta segurar o botão de volume — mesmo com a
tela apagada e o celular no bolso — para avisar na hora as pessoas que você escolheu, com
a sua localização no mapa.

COMO FUNCIONA
• Você cadastra seus "guardiões": as pessoas de confiança que recebem o alerta.
• Em perigo, segura o botão (de volume ou na tela) pelos segundos que você configurar.
• Pronto: cada guardião recebe uma mensagem com o seu nome e um link do mapa mostrando
  onde você está — e atualizações da sua localização, se você quiser.

DOIS TIPOS DE SOS
• Segurança: para risco, assalto, perseguição.
• Saúde: para quando você passa mal ou cai e precisa de alguém por perto.

DISCRETO DE VERDADE
Nada de alarme, sirene ou tela chamativa. O pedido de ajuda sai em silêncio. Uma vibração
curta só para você confirma que foi enviado.

VOCÊ NO CONTROLE
• Modo teste para treinar sem enviar nada de verdade.
• Aviso de "estou bem" para tranquilizar todo mundo com um toque.
• O histórico fica só no seu aparelho e você apaga quando quiser.

PARA QUEM É
Para quem anda sozinho à noite, para a mulher que quer se sentir mais segura, para o idoso
que mora só, para o filho que quer cuidar dos pais à distância, para quem trabalha na rua.
Uma família inteira pode se proteger entre si.

GuardiõesSOS. Seus guardiões a um toque de distância.
```

> Depois do lançamento a gente pode fazer versões em outros idiomas (o app já nasce pensando
> em ser global).

### Categoria
- **Primária sugerida:** Ferramentas (ou "Estilo de vida").
- **E-mail de contato** (aparece na loja): use um e-mail de suporte do produto
  (ex.: `contato@guardioessos.com.br`) em vez do pessoal.
- **Site:** `https://guardioessos.com.br` (quando estiver no ar).

---

## 3. Ficha da loja — IMAGENS (prompts pro seu GPT)

Tamanhos que a Play **exige**:

| Imagem | Tamanho | Obrigatória? |
|---|---|---|
| Ícone do app | 512 × 512 px, PNG | Sim |
| Gráfico de destaque (capa) | 1024 × 500 px, PNG/JPG | Sim |
| Prints do celular | mín. 2, até 8 — 9:16 (ex. 1080×1920) | Sim (mín. 2) |

### Ícone do app (512×512)
**Não precisa de prompt** — eu faço a partir do **escudo** que você já mandou (aquele sem o
texto). Só preciso que você **me reenvie o arquivo** (veja o fim). Eu gero o ícone 512 e as
versões internas do Android.

### Gráfico de destaque / capa (1024×500) — 👉 cola no GPT
```
Crie uma arte de capa (feature graphic) para a loja de aplicativos, 1024x500 pixels,
horizontal. Fundo azul-marinho escuro (#0B1B36) com um leve degradê. À esquerda, coloque o
logotipo GuardiõesSOS que eu enviei (o escudo com o pin vermelho e a pessoa). À direita, em
tipografia forte e branca, o texto "Seus guardiões a um toque de distância". Deixe respiro
nas bordas, sem cortar nada. Estilo limpo, moderno, confiável. PNG.
```

### Prints do celular (screenshots)
Esses são **fotos reais do app** (não é arte de GPT). O jeito certo:
1. Abra o app no seu celular.
2. Tire print (ligar/volume) das telas: **Início** (o botão grande), **Guardiões**,
   **Ajustes** e o **modo teste** funcionando.
3. Me manda os prints — eu posso **emoldurar** eles com uma faixa e uma frase por cima pra
   ficarem bonitos na loja (isso é tratamento, eu faço).

---

## 4. Classificação de conteúdo (questionário IARC)

É um questionário no Console. Respostas para este app (resultado esperado: **Livre / L**):

- Violência, sexo, linguagem imprópria, drogas, jogos de azar, apostas → **Não** para tudo.
- O app permite **compartilhar a localização** do usuário? → **Sim** (é o coração do app).
- O app permite usuários se comunicarem / conteúdo gerado por usuário? → a mensagem é enviada
  para contatos escolhidos; responda conforme o questionário descrever (não é rede social).

---

## 5. Público-alvo e conteúdo

- **Faixa etária:** adultos (18+). **Não** marque faixas infantis — isso evita cair nas
  regras pesadas de "apps para crianças" (Families Policy).
- O app **não** é dirigido a menores de 13 anos.

---

## 6. Segurança dos dados (formulário "Data Safety") — IMPORTANTE

Tem que bater **exatamente** com a Política de Privacidade (`docs/POLITICA-PRIVACIDADE.md`).
O que declarar:

| Dado | Coletado? | Compartilhado? | Para quê | Obrigatório? |
|---|---|---|---|---|
| Localização precisa | Sim | Sim (nosso servidor, só para entregar o alerta) | Função do app | Sim |
| Nome | Sim | Sim (vai na mensagem do alerta) | Função do app | Opcional |
| Telefone (do usuário) | Sim | Sim (cadastro/entrega) | Função do app, conta | Opcional |
| Contatos (os guardiões escolhidos) | Sim | Sim (para enviar o alerta a eles) | Função do app | Sim |
| ID do dispositivo | Sim | Não | Notificações/antifraude | Opcional |

Outras respostas:
- **Dados criptografados em trânsito?** Sim (HTTPS até o nosso servidor). *Obs.: o SMS em si
  trafega pela operadora, como qualquer SMS.*
- **O usuário pode pedir a exclusão dos dados?** Sim — apaga o histórico no app e pode pedir
  exclusão da conta por e-mail. (O Google **exige** um caminho de exclusão; a gente deixa um
  e-mail e, em breve, um botão "excluir minha conta".)
- **Dados usados para anúncios?** Não. **Vendidos a terceiros?** Não.

---

## 7. Política de privacidade (OBRIGATÓRIA)

App que pede **SMS e localização NÃO é publicado sem** uma política de privacidade num link
público. Já escrevi o texto em **`docs/POLITICA-PRIVACIDADE.md`**.

👉 Falta só **hospedar** num endereço público, ex.: `https://guardioessos.com.br/privacidade`
(dá pra subir uma página simples na Vercel, como nos seus outros sites). Me avisa que eu monto
a página.

---

## 8. ⚠️ 3 pontos que o Google revisa com lupa (podem recusar o app)

Vou ser direto: do jeito que o app está, **três permissões** disparam revisão manual e podem
dar recusa. Dá pra resolver, mas é decisão sua e é melhor decidir ANTES de enviar.

### a) Envio de SMS (`SEND_SMS`) — permissão restrita, a mais difícil
O Google trata SMS como permissão de alto risco. Hoje o nosso alerta sai pela **nossa
estrutura (internet)**, e o SMS pelo chip ficou só como **reserva**. Como existe a alternativa
(a internet), o Google tende a **recusar** o pedido de SMS.
**Minha recomendação:** tirar a permissão `SEND_SMS` do app e, quando não tiver internet,
**abrir o app de mensagens do celular com o texto já pronto** (a pessoa só toca em enviar) —
isso **não precisa de permissão** e some com o problema. Perde um pouco do "100% silencioso
sem internet", mas destrava a publicação. **Decisão sua.**

### b) Localização em segundo plano (`ACCESS_BACKGROUND_LOCATION`)
Para pegar o GPS com o app fechado/tela apagada. O Google aceita, mas exige: aviso claro na
tela + formulário de justificativa + **um vídeo curto** mostrando o uso. Dá trabalho, mas é
justificável (é emergência). A gente prepara o texto e o roteiro do vídeo.

### c) Serviço de Acessibilidade (o que "ouve" o botão de volume)
Esse é o mais delicado. A política do Google diz que o serviço de Acessibilidade é para
**ajudar pessoas com deficiência**. Usar para "ouvir o botão de volume" **não** é esse uso, e
já derrubou apps parecidos.
**Caminhos:** (1) enviar com uma justificativa bem feita de acessibilidade/segurança e torcer;
(2) manter o gatilho pelo botão como recurso **fora da Play** (instalação direta) e, na Play,
publicar com o **botão na tela** + outros gatilhos que não dependem de Acessibilidade; (3)
repensar o gatilho. Precisamos decidir a estratégia pra essa parte.

> Resumo honesto: montar a ficha (textos, imagens, declarações) é tranquilo. O que decide se o
> app **entra** na Play são esses 3 pontos. Vale a gente fechar isso antes de clicar em enviar.

---

## 9. Ordem sugerida

1. Decidir os 3 pontos da seção 8 (principalmente SMS e Acessibilidade).
2. Hospedar a política de privacidade.
3. Gerar ícone (eu) + capa (GPT) + prints (você).
4. Subir o .aab como teste **interno** primeiro (libera na hora, só pra você e convidados).
5. Testar, depois promover para **produção**.

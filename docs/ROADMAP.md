# SMSOS — Rumo do produto (roadmap)

Botão de pânico silencioso. A pessoa aciona (segurando o botão de volume, por voz, ou pelo
botão do app) e o SMSOS avisa, em silêncio, a rede de confiança dela, com a localização.

Público: começa por mulheres (segurança) e por idosos / quem tem problema de saúde.
Não substitui 190 (polícia), 192 (SAMU) nem 180 (Central de Atendimento à Mulher).

---

## Fase 1 — no ar (gratuita, Android)   ✅ pronta para a loja
- Acionar segurando o botão de volume (tela ligada e, na maioria dos aparelhos, apagada).
- SMS de emergência pelo chip, com link do mapa.
- Rede por GPS: contatos "sempre avisar" (família) + contatos de cada lugar (vizinhos de
  casa, do trabalho). O app escolhe os certos pela localização.
- Atualizações de localização e aviso de "estou bem".
- Perfil Segurança / Saúde (muda o texto da mensagem).
- Teto de 10 contatos por lista.
- Modo teste (sem enviar SMS).
- Passo a passo guiado de configuração.

## Fase 2 — conta + site (o cofre e a monetização)
- **Site com login e senha** guarda histórico, provas e créditos. O app fica só com o
  básico, de propósito: quem pega o celular NÃO vê para quem a pessoa mandou mensagem
  (proteção contra coação — tomarem o celular e mandarem abrir o app).
  - **Histórico:** o completo mora no **site** (vê e apaga lá, junto com créditos/venda).
    No app, a lista fica **enxuta** e tem **"apagar deste aparelho"** (feito na v0.8.2),
    pra não deixar rastro se tomarem o celular. (Depois: agrupar por dia e esconder/
    minimizar ainda mais no app; o detalhe fica no site.)
- **Créditos / planos vendidos no site, no PIX** (como nos produtos QR). O app só mostra
  "plano ativo / X créditos". Regra das lojas: dentro do app, principalmente no iPhone,
  NÃO pode ter botão de comprar, preço nem link de pagamento — a compra é no site, por fora.
- **Envio por servidor (SMS Market via disparador do BipeChat)**, deixando o chip do
  celular como reserva (sem internet).
- **Comando de voz** pelo assistente ("Ok Google / E aí Siri, [frase]") — Android e iPhone.
- **Modo escuta** (palavra-chave secreta ouvida pelo próprio app, no aparelho, sem mandar
  áudio para fora). Só Android. Ligado só nos momentos de risco, para poupar bateria.

## Fase 3 — vídeo e saldo (o mais caro)
- **Gravar vídeo + áudio** ao acionar (prova), guardado por 30 dias, apagado depois.
  Guardar é barato (centavos). Recomendado: Cloudflare R2 (download grátis).
- **Transmissão ao vivo** para a família (GPS, câmera e microfone em tempo real).
  Custo por emergência baixo (centavos a ~R$2); o peso é o servidor fixo.
- **Saldo pré-pago (carteira):** abrir vídeo R$1, +R$1 por 30 min. Debita do saldo do dono
  da conta. REGRA DE OURO: o alerta de socorro (SMS) NUNCA é bloqueado por falta de saldo.
- **Câmera IP de casa** (nível 1: app puxa a câmera no Wi-Fi; nível 2: aparelho sempre
  ligado em casa; nível 3: central de monitoramento — isso é empresa de segurança
  regulada, Lei 7.102 / Polícia Federal).

---

## Ideias a amadurecer

### Situações de mensagem (presets)
Além do perfil Segurança/Saúde, a pessoa cadastra **mensagens prontas por situação** e
escolhe rápido qual disparar, cada uma com seu texto e, se quiser, seu próprio grupo:
- Assalto / abordagem.
- **Violência doméstica** — mensagem para um grupo de amigas de confiança. Este caso pede
  cuidado extra:
  - **Disfarce é essencial** (o agressor costuma ter acesso ao celular): nome/ícone
    discretos, tela "disfarçada", nada aparecendo. Combina com o histórico ficar no site.
  - **Discrição total** no envio (sem som, sem vibração, sem notificação visível).
  - Oferecer os canais oficiais no app: **180** (Central de Atendimento à Mulher), **190**.
  - Pensar no contexto de **medida protetiva** (quando o agressor se aproxima).
- "Estou saindo / estou na rua", "cheguei", "a caminho de casa".
  (O GPS já diz onde a pessoa está; a situação muda o TEXTO e, às vezes, o grupo.)

### Dois botões, dois alertas (volume − = SOS, volume + = Saúde)
Usar os **dois botões de volume** para **dois tipos** de alerta, sem olhar a tela:
- **Volume −** → **SOS / segurança** (texto e, se quiser, grupo de segurança).
- **Volume +** → **Saúde** (texto e grupo de saúde — família, cuidador).
- A MESMA pessoa pode ter os dois: segura o − se for perigo, o + se passar mal.
- O app já detecta a direção do botão, então é natural ligar cada uma a um perfil/preset.
- **Manter simples por padrão:** começa com UM perfil e **qualquer** botão (como é hoje);
  "dois botões, dois tipos" é uma **opção** que a pessoa liga. É a porta de entrada das
  "situações de mensagem" acima (cada gesto → um preset).
- Segurar os 5s continua valendo em cada botão (evita disparo sem querer).

### Disfarce do app
Ícone/nome que não entregam a função; abrir normal e acionar escondido.

### iPhone
Botão de Ação (15 Pro e mais novos), Toque nas Costas, Siri, botão no app. Não dá para
segurar o volume em segundo plano (regra da Apple). App precisa ser construído à parte
(conta Apple US$99/ano, Mac). Envio sempre por servidor.

### Marca e domínios
Marca **SMSOS**. Domínios registrados: smsos.com.br e alertasilencioso.com.br
(este último atrai quem pesquisa "alerta silencioso" e aponta para o site).

### Modelo de planos (ponto de partida)
- Básico (~R$20/ano, até 3 contatos) — alerta por SMS + pacote de SMS.
- Família (até 7 contatos) — rede de vizinhos por lugar.
- Completo (até 10 contatos) — vídeo + ao vivo.
- SMS pré-pago (recarga). Créditos de vídeo por uso. Venda no site (PIX).

### Convite e aceite (consentimento + crescimento + captação de leads)
Cadastrar um contato vira um **convite**: "Fulano te convidou para ser contato de
emergência dele no SMSOS — como filho / cônjuge / amigo / vizinho". A pessoa **aceita**
receber. Isso resolve TRÊS coisas de uma vez:
1. **Consentimento (LGPD + saúde da conta de SMS).** Só quem aceitou recebe. Reduz
   reclamação de spam, que é o que pode fazer a operadora/SMS Market bloquear o pacote.
2. **Crescimento viral.** Cada contato convidado é um **novo usuário em potencial**: o
   convite é a porta pra ele **baixar o app** e virar dono da própria rede.
3. **Captação de lead.** No aceite dá pra pegar **nome, relação e WhatsApp** — e o
   trabalho do lead é feito **no site** (não no app), onde a gente capta e nutre.
- **Regra de ouro (segurança não pode travar):** o alerta por SMS deve sair **mesmo sem o
  aceite**. O aceite NÃO é um bloqueio do socorro — ele **destrava o push** e confirma o
  consentimento. Sem aceite, manda SMS do mesmo jeito (interesse vital numa emergência);
  o convidado pode se descadastrar.
- Convite sai por SMS/WhatsApp/link; aceite no site ou no app.

### Notificação push (canal grátis, além do SMS)
Se o contato **tem o app instalado**, mandar **push** em vez de (ou além de) SMS:
- **De graça** (não gasta o pacote de SMS), **instantâneo** e **rico** (link do mapa,
  botão "estou bem", abrir o alerta no app).
- **Desenho:** o servidor (a mesma função `smsos-alerta`) decide por contato — **tem app
  registrado? manda push; não tem? manda SMS.** Melhor dos dois: barato e alcança todo
  mundo.
- Técnico: FCM (Firebase Cloud Messaging), grátis. O app registra o token do aparelho no
  nosso backend; o alerta dispara o push pra esses tokens. Encaixa na mesma espinha dorsal
  de contas/rede do site.
- **A assimetria é o pulo do gato:** quem ACIONA faz tudo em silêncio (pro agressor não
  perceber), mas quem RECEBE tem que ser avisado **da forma mais barulhenta possível** —
  senão o socorro passa batido. O push do app pode **apitar alto, com som próprio, e furar
  o silencioso / Não Perturbe** (como um alarme/emergência). Um SMS comum não faz isso:
  chega como texto e é fácil não ver. Então o push ganha do SMS em **duas** frentes:
  **custo (grátis)** e, principalmente, **chamar a atenção de verdade** (acorda a pessoa).
- **Som configurável pelo quem recebe:** escolher um toque mais alto, vibração forte,
  repetir até abrir. O recebedor manda no som; o acionador, não (ele só dispara).
- **Camadas de entrega e preço** (o servidor decide por contato):
  1. Contato **com o app** → **push grátis** (barato e mais seguro). É o que puxa o
     convidado a baixar: "para funcionar de verdade, instala o app".
  2. Contato **sem o app** → **SMS** (pago, do pacote). Funciona pra todo mundo, mas custa.
  Isso vira o discurso de venda: o app é barato e mais seguro; o SMS é o reforço que a
  pessoa paga quando o contato não tem o app.

### Clube de benefícios (a base de usuários vale ouro)
O SMSOS é barato **de propósito**: assinatura baixa → **milhares de cadastros** → uma
**base de dados** (com nome) que alimenta o **clube de benefícios** (um dos focos
principais do Rodrigo). Dois ganhos no mesmo produto:
1. **Assinatura** barata, mas com **margem quase cheia** no modo push (push é grátis):
   ~R$20–25/ano já paga. Volume vira dinheiro de verdade; o SMS pago é extra em cima.
2. **Base qualificada** — e o público casa: quem confia num **app de segurança** é público
   perfeito pro clube; a confiança do produto de segurança **transfere** pro resto.
- **Preço de partida:** ~R$25/ano (≈ US$5), preço de app. Mesmo **só no push** já vale.
- **Cuidado que PROTEGE a marca (e é LGPD):**
  - **Consentimento separado** pro clube: opt-in próprio no cadastro ("quero receber
    benefícios"), nunca embutido na assinatura de segurança.
  - **Nunca** usar os dados **sensíveis** (contatos de emergência, localização, histórico
    de alertas) pra marketing. O clube usa só **nome/contato + opt-in**. Misturar "sei quem
    são seus contatos de emergência" com propaganda **quebra a confiança** — e num app de
    segurança, confiança é o produto.
  - Quando as **contas** entrarem (Fase 2), **atualizar a política de privacidade** e o
    Data Safety: hoje dizem "não coletamos nada" (porque é tudo no aparelho); com conta e
    nome isso muda e tem que estar declarado certinho.

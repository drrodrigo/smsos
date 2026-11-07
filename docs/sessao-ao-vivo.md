# Sessão de socorro ao vivo (segundo round) — spec

Ideia do Rodrigo (07/11): o SOS deixa de ser "um alerta e acabou" e vira uma **sessão ao vivo**
que o guardião acompanha até a situação se resolver.

## A visão

Ao acionar o SOS, abre uma sessão que o guardião acompanha por um link:
- 🎥 **Vídeo curto** (10s, ajustável) — o visual do momento. **(já feito)**
- 🎤 **Áudio contínuo** — o microfone fica gravando **até parar**; o **guardião pode desligar**.
- 📍 **Localização ao vivo no mapa** — a posição atualiza **a cada 30s** (ou contínua).
- O **guardião abre o link**, vê o mapa se mexendo, ouve o áudio, e pode **encerrar a escuta**.

## As peças (e onde cada uma mora)

1. **App do protegido:** grava áudio contínuo (serviço de primeiro plano), manda a localização a cada
   30s pro servidor, e inclui o **link da sessão** no alerta.
2. **Servidor (Supabase, schema `guardioes`):** tabela da sessão (código, posições, status do áudio),
   função pra **receber** (do app) e **ler** (o mapa), e o canal de **"encerrar"**.
3. **Página do mapa ao vivo** (`guardioessos.com.br/ao-vivo/CODIGO`): mapa que atualiza a posição a
   cada 30s + player do áudio + botão **"Encerrar escuta"** (pro guardião que abre pelo link).
4. **App do guardião:** o mesmo botão de encerrar dentro do app (pra quem já tem o app).

## Ordem de construção (do mais visível ao mais complexo)

1. **Mapa ao vivo (página web)** — dá pra VER funcionando rápido (começo por aqui).
2. **Localização a cada 30s** — o app manda a posição pro servidor durante o alerta.
3. **Áudio contínuo** — o app grava até parar (serviço de primeiro plano), com limite máximo.
4. **Guardião encerra** — o canal de volta (botão → sinal → o app para).

## Cuidados honestos (não dá pra varrer pra baixo do tapete)

- 🔴 **Mexe no BANCO de produção** (tabela nova + funções no schema `guardioes`). Faço **com cuidado**:
  migration no repositório, teste no banco de mentira, teste de RLS (um guardião não vê a sessão de
  outro). **Não é coisa pra fazer às pressas de madrugada** — é a parte que merece calma.
- **Bateria e internet:** áudio e localização contínuos gastam. Tem que ter **limite máximo** e o
  protegido poder parar pelo botão **"Estou bem"** que já existe.
- **Permissões sensíveis** (câmera, microfone, localização contínua/2º plano): entram como
  **atualização depois da aprovação** nas lojas — o iOS está EM REVISÃO agora, não encosto.
- **Privacidade (LGPD):** áudio e posição ao vivo são dados sensíveis. Só pros **guardiões escolhidos**,
  por um **link com código**, e **apaga depois** de um tempo. A sessão é temporária, não um rastreador.

## O que já está pronto

- 🎥 Vídeo de 10s (câmera frontal + áudio), salvo na galeria. APK de teste entregue.
- 📍 O app já pega a localização no acionamento e já manda atualizações (hoje por SMS, a cada X min).
  A novidade é mandar pro **mapa ao vivo** a cada 30s.

## Status

Spec registrada. Construção **em andamento**, por partes, começando pelo mapa ao vivo.

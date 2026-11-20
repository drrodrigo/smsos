# SOS pelo relógio (Galaxy Watch / Wear OS) — ideia anotada em 20/11/2026

Pergunta do Rodrigo: dá para disparar o SOS do Guardião **segurando um dos botões do relógio**
(o dele, Samsung, tem dois)?

## Resposta curta
**Segurar o botão, não.** A Samsung reserva os dois botões: segurar o de cima desliga/chama a
assistente, o de baixo é o "voltar". Nenhum app de fora toma esses botões com garantia — e botão
de pânico que "às vezes funciona" não serve.

**Mas dá algo muito perto:**
1. **Dois toques no botão de cima abrem o Guardião** — o próprio relógio deixa escolher o app do
   "pressionar duas vezes" (Configurações → Recursos avançados → Personalizar teclas).
2. O Guardião do relógio **já abre contando**: vibra (sem som) e **dispara sozinho em 5 s**, com um
   botão grande "Cancelar" para quando foi sem querer.
3. **Complicação / bloco no mostrador** com o SOS: um toque e dispara, sem procurar o app.
4. Os dois SOS (Segurança e Saúde) como no celular.

## Como o alerta sai (CORREÇÃO do que veio do chat do App Funcional)
O Guardião **não manda mais SMS pelo chip** (removido na v0.23.0 — ver CLAUDE.md). O alerta sai pelo
**nosso servidor**, pela internet (`smsos-alerta` → `bipe-sms` → SMS Market, + sirene nos guardiões
pela `guardioes-alertar`). Por isso:
- **Relógio perto do celular (Bluetooth):** o relógio manda a ordem para o app do celular, que dispara
  como hoje (mesma receita de comunicação que o App Funcional, `drrodrigo/appfuncional`, já usa).
- **Relógio com Wi-Fi ou chip próprio (LTE):** o relógio pode chamar o servidor **direto**, sem o
  celular. Nesse caso a localização vem do GPS do próprio relógio.
- Sem celular e sem internet no relógio: não sai (não há chip para SMS direto no Wear OS por app).

## Bom saber
- O relógio da Samsung já tem um **SOS próprio** (apertar o botão de cima várias vezes). Ele avisa os
  contatos de emergência da Samsung, não os guardiões do GuardiõesSOS. Os dois convivem.
- Wear OS é Android: o app do relógio entra no mesmo repositório (`drrodrigo/smsos`, novo módulo
  `relogio/`) e reaproveita o motor (`core/`: mensagem, telefones, quem recebe).
- Montar não custa minutos do GitHub (repositório público). Publicar na Play (o app do relógio vai
  junto do app do celular, como "app para relógio") é decisão do Rodrigo, e passa pela revisão do Google.

## Quando
Depois do 2º round (botão lateral + iPhone com a cara do Android). Ordem sugerida: 1) app do relógio
com o SOS que dispara pelo celular; 2) complicação no mostrador; 3) envio direto pelo relógio com
Wi-Fi/LTE.

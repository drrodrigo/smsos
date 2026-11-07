# Demonstração ("ver como chega") + autoteste da sirene/tela (pedido do Rodrigo, 07/11)

## O pedido

Rodrigo, testando/mostrando pra clientes: quer um botão **"testar o funcionamento"** que
**mostre como a mensagem chega SEM disparar nada** — pra mostrar pro cliente "ó, a mensagem vem
assim". E, junto, poder **conferir que a sirene toca e a tela acende** (a experiência do guardião).

> "Botar um botão aqui, tipo testando o funcionamento... mostrar pro cliente, ó, a mensagem vai
> chegar assim, aí eu aperto lá e ele mostra como é que a mensagem chega, sem disparar nada.
> Isso agora inicialmente pra mim seria importante, depois pode ficar na configuração... testar
> pra ver que está tocando, que a tela vai ligar."

## O que JÁ existe (dá pra usar hoje, no APK atual)

- **Modo teste** (tela Início → "Testar sem enviar SMS" → liga o interruptor). Com ele ligado,
  segurar o botão faz tudo **menos enviar**: vibra e mostra uma **notificação** com o **texto
  exato** que o guardião receberia (`Feedback.showTestResult` → `record.message`).
- Então "ver como a mensagem chega" **já funciona** — só que aparece como notificação (tem que
  puxar a barra) e depende de ter contato cadastrado pra não dizer "nenhum contato receberia".

## O que falta (o polimento pedido)

1. **Botão dedicado "Ver como a mensagem chega"** (demonstração):
   - Mostra o texto do alerta **na própria tela**, num balão estilo SMS recebido (não numa
     notificação) — melhor pra mostrar pro cliente.
   - **Não precisa de contato cadastrado**: usa um exemplo ("Fulano", local de exemplo). É só
     vitrine, não manda nada, não toca no backup nem no gateway.
   - Mostra o alerta de Segurança e o de Saúde (os dois textos).
2. **Botão "Testar a sirene e a tela do guardião"** (autoteste):
   - Toca a sirene (`Alarme.tocar`) e abre a tela cheia de alerta recebido
     (`AlertaRecebidoActivity`) em **modo demonstração** (com um "isto é um teste"), pra ver que
     som/vibração/lanterna/tela acendendo funcionam **neste aparelho**.
   - Hoje isso só acontece quando chega um push de verdade; não há autoteste local.
3. **Lugar:** inicialmente visível/fácil de achar (pra demonstração agora); depois pode morar na
   **Configuração**, como o Rodrigo disse.

## Cuidados

- 🔴 **Não envia nada, não gasta SMS, não mexe no backend.** É demonstração/local puro — risco zero.
- O autoteste da sirene deve ter um **"Parar"** bem na cara (a tela cheia já tem o botão de parar).
- Deixar claro na tela que é **teste/demonstração**, pra ninguém confundir com alerta real.

## Status

Registrado. **Não implementado ainda.** Entra no próximo APK de teste junto com os outros
ajustes. Risco zero (local), então pode ir sem esperar a aprovação das lojas.

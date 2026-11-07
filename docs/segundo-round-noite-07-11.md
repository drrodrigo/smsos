# Segundo round — o que fiz essa noite (relatório honesto)

Bom dia! 🌅 Trabalhei no segundo round. Aqui está **a verdade** do que ficou pronto, o que você
precisa testar, e o que ainda falta — sem enrolação.

## ✅ O que FICOU PRONTO (Android): captura no SOS

Ao acionar o SOS, o app agora faz, **em paralelo** (sem atrasar o alerta):
- 📍 **GPS** (localização) — já existia.
- 📷 **Foto** da cena, com a **câmera frontal**, **sem abrir a tela**.
- 🎤 **Áudio** do ambiente (uns 5 segundos).

Tudo isso vira **prova** pra mandar aos guardiões. É **opt-in** (desligado por padrão) e
**experimental**.

**Status técnico (honesto):** compila limpo, os testes automáticos passaram, o APK está pronto.
🔴 **NÃO testei em aparelho de verdade** — câmera e microfone só dá pra provar no seu celular.
É por isso que te mandei o APK: pra VOCÊ ver funcionando.

## 👉 COMO TESTAR (no seu Samsung M54)

1. **Instale** o APK `GuardioesSOS-teste-captura` que te mandei (instala por cima do app de teste
   anterior, sem perder nada — continua sendo o "GuardiõesSOS teste", separado do app da Play).
2. Abra o app → **Ajustes** → bloco **"Capturar foto e áudio no SOS (experimental)"** → **ligue** a
   chave. Ele vai **pedir permissão de câmera e microfone** → **Permitir** nas duas.
3. Na tela **Início**, ligue o **Modo teste** (pra não enviar nada de verdade).
4. 👉 Aperte o **botão SOS** na tela (segura até disparar).
5. Espere uns **5 segundos**. Vai chegar uma **notificação**: *"SOS capturou a cena — 📷 Foto
   capturada · 🎤 Áudio 5s"*.
6. **Toque na notificação** → abre a **foto** (ela também fica salva na sua **galeria**, na pasta
   **GuardiõesSOS**).

**Deu certo se:** a foto apareceu (câmera ✓) e a notificação disse "Áudio 5s" (microfone ✓).
Se algo falhar, a notificação mostra o motivo (ex.: "Câmera indisponível: ...") — me manda print.

## ⚠️ Limites honestos (pra não te iludir)

- **Testei no teste guiado (app aberto, botão na tela).** Capturar com o **app fechado / tela
  apagada** (pelo gesto) tem restrição extra de câmera/microfone em segundo plano no Android — é
  o próximo passo, depois que a captura "básica" estiver provada no seu aparelho.
- **Câmera/microfone são as permissões mais fiscalizadas** da Play e da Apple. Por isso a captura
  entra como **ATUALIZAÇÃO depois da aprovação** — nunca agora (o iOS está EM REVISÃO e o Android
  está no ar; não encostei em nenhum dos dois). Tudo está em **branch separada**
  (`feat/captura-sos`), **nada publicado**.
- O envio da foto/áudio aos guardiões (subir pra nuvem + link no alerta) é o **próximo passo** —
  por enquanto a prova é LOCAL (salva na galeria), que é o que precisa pra você ver funcionando.

## 🍎 iOS

O iOS está **EM REVISÃO na Apple** — **não encostei** no que foi enviado (a 1.0 segue intacta).

**Já escrevi o código da captura no iPhone** (AVFoundation: foto frontal + áudio) na branch
`feat/captura-ios`, com as permissões no Info.plist. 🔴 **MAS:** não dá pra compilar/testar aqui
(não existe toolchain Swift no meu ambiente de dev), então esse código **ainda não está provado**
e **ainda não está ligado na tela**. O próximo passo do iOS é: ligar na tela + um build de
**TestFlight** pra você testar no seu iPhone (um build novo de TestFlight **não atrapalha** a
revisão da 1.0). Fiz o Android primeiro porque é o que eu consigo **compilar e te entregar
testável** daqui.

## 🔑 Recap do botão lateral (gesto)

Do teste de ontem: no seu Samsung, o gesto **sem Acessibilidade** não pega a tecla com a tela
apagada (contador ficou em 0). A conclusão continua: o botão lateral **confiável** precisa da
**Acessibilidade**, que entra como **atualização** no segundo round, com o vídeo de justificativa
(está tudo no `docs/botao-lateral-plano-acessibilidade.md`).

## 👉 O que eu preciso de você

1. **Testar a captura** no seu celular (passos acima) e me dizer: a foto saiu? o áudio marcou 5s?
2. Me dizer se quer que eu siga com: (a) **enviar a foto/áudio aos guardiões** (nuvem), (b) a
   **captura pelo gesto** (app fechado), ou (c) a **versão iOS** da captura.

Descansa que o trabalho rendeu. 💪

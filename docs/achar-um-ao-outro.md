# "Achar um ao outro" — localização em tempo real entre pessoas (ideia do Rodrigo, 07/11)

## A ideia (do amigo dele)

Duas pessoas que **se aceitam mutuamente** veem a localização uma da outra, ao vivo, no mapa.
Exemplo do Rodrigo: estão num shopping, não querem ficar ligando "onde você está?" — abre o mapa
e um acha o outro. Cada um **libera a sua** e vê a do outro.

É diferente do SOS: aqui é no **dia a dia** e nos **dois sentidos** (mútuo e contínuo), não é só
na emergência (protegido → guardiões).

## Dá pra fazer? Sim.

Usa a **mesma engrenagem** do "mapa ao vivo" que já estamos montando pro socorro
(`docs/sessao-ao-vivo.md`): app manda a posição → servidor → mapa atualiza. A diferença é a UX:
- **Mútuo:** os dois compartilham e os dois veem.
- **Contínuo / do dia a dia**, com liga/desliga a qualquer momento.

## É aprovável nas lojas? Sim, COM regras (a parte sensível é a localização em 2º plano)

Apps de compartilhar localização são aprovados (Buscar/Find My, Life360, Google Maps, WhatsApp).
O que as lojas exigem — e o que **proíbem**:

- 🔴 **Consentimento dos DOIS.** Você só vê quem **aceitou** te mostrar. Nada de ver alguém sem a
  pessoa saber. As duas lojas **banem rastreio escondido (stalkerware)** — então tem que ser
  mútuo, **visível** ("você está compartilhando com Fulano") e **desligável** na hora.
- **Google Play:** localização em **segundo plano** precisa de uma **declaração** no console e, em
  geral, de um **vídeo** mostrando pra que serve (igual à acessibilidade). A revisão demora mais.
- **Apple:** precisa da justificativa de "Always Location" (`NSLocationAlwaysAndWhenInUse...`) e a
  Apple mostra lembretes periódicos ao usuário de que o app usa a localização.
- **LGPD/privacidade:** dado sensível. Compartilhamento **temporário ou com controle fácil**,
  apagável, e um indicador claro de que está ligado. Nunca liga sozinho.

## Plano: vai no de US$5 (plano 1, "Proteção+") — DECIDIDO por ele (07/11)

Estrutura: **Grátis** · **plano 1 = US$5** · **plano 2 = US$10**. Este recurso entra no **US$5**.

## Como funciona (confirmado por ele, 07/11)

- **Em GRUPO / equipe, não só de dois.** Ex.: 4 pessoas num show → os 4 veem os 4 no mapa.
  Pode criar um **grupo** e todos do grupo se enxergam.
- **Você convida a pessoa** pra compartilhar (entra no grupo).
- **Quem compartilha pode desligar** a qualquer momento — e aí os outros **deixam de ver** a sua
  posição. Você **só vê no mapa quem está compartilhando** naquele momento (aceitou E ligou).
- Mútuo e simétrico: cada um controla a própria partilha; ninguém aparece sem ter ligado.

### Pra que serve (as falas dele)

- Saiu com os amigos / "todo mundo meio alto" → ligam pra **não se perder** um do outro.
- Num **evento / show** → liga e o grupo se acha sem ficar telefonando "onde você está?".

## Por que é forte no produto

- **Recurso PAGO do US$5** com apelo largo (não é só segurança — é social, pra sair/eventos).
- **Dá motivo pra manter o app** (uso no dia a dia, não só em emergência) — e app instalado e
  ativo = mais proteção de verdade na hora que precisa.
- Reforça a justificativa de localização em 2º plano que o SOS ao vivo também vai pedir — as duas
  (achar um ao outro + seguir na emergência) são usos legítimos que ajudam na aprovação.
- **Grupo temporário ajuda na privacidade/aprovação:** o grupo do show acaba/some depois, não é
  rastreador permanente.

## Monetização: experimenta grátis, depois paga (ideia dele, 07/11)

- **Isca:** a primeira vez é **grátis** (baixa, liga a localização, usa no evento). **Da próxima,
  paga.** As duas lojas já têm "período grátis" na assinatura — é o jeito nativo e aprovável.
- **Preço que ele falou:** **R$30 por ANO**. Reação do amigo: "usei, quanto custa? R$30/ano? compro".
  A psicologia é boa — depois de provar de graça, R$30/ano é um "sim" por impulso.

### 🔴 Atenção (decisão dele): anual barato × mensal

Isto precisa ser decidido sem misturar, porque muda o faturamento:
- O **plano 1** a gente tinha posto em **US$5 por MÊS** (~R$30/**mês**).
- Agora ele falou **R$30 por ANO** — isso é **12x mais barato** que o mensal.
- Os dois funcionam e as lojas aceitam os dois (mensal E anual, com desconto no anual). São
  estratégias diferentes:
  - **US$5/mês:** ganha mais por pessoa; público mais "segurança".
  - **R$30/ano:** isca de impulso; público "só pro rolê/show"; converte muita gente barato.
- Caminho possível: oferecer **mensal + anual** no mesmo plano, com o anual com desconto; a
  "primeira vez grátis" como isca. **Falta ele dizer qual é o preço/âncora principal.**

## Status

Ideia registrada. **Não implementada.** Entra junto com o **mapa ao vivo**, na atualização depois
da v1.0 aprovada nas lojas (localização em 2º plano NÃO encosta agora: iOS em revisão). É uma
frente de produto separada do botão de pânico.

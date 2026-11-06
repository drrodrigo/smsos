# Botão lateral: decisão e plano da Acessibilidade (06/11)

## O que o teste provou (Samsung Galaxy M54)

- O motor do gesto **roda SEM Acessibilidade** (serviço de primeiro plano + sessão de mídia):
  a notificação "protegendo você" fica ativa, "Motor do gesto: ligado ✓".
- **MAS** o Samsung **não entrega a tecla** pro app com a tela apagada: o contador "apertos
  percebidos" ficou em **0**.
- O único jeito de forçar isso **sem Acessibilidade** seria o app ficar "tocando um som mudo" o
  tempo todo (pra ser a sessão de mídia ativa) — o que atrapalharia música/ligações. Não presta
  pro dia a dia.
- Conclusão: **o botão lateral confiável (inclusive no Samsung, maioria no Brasil) precisa da
  Acessibilidade.** O experimento sem acessibilidade foi feito **de graça, sem risco na loja** —
  e deu a resposta.

## A decisão (do Rodrigo)

1. **Primeiro round:** finalizar o app de pânico na versão **segura** (botão na tela, SEM tecla,
   SEM acessibilidade) e deixar **os dois aprovados** — Play (já no ar) e App Store.
2. **Segundo round:** religar o botão lateral **via Acessibilidade**, lançado como
   **ATUALIZAÇÃO** (update recusado **não derruba** o app que está no ar), com **vídeo** e
   justificativa pro Google.

## A justificativa (argumento do Rodrigo — usar no vídeo e no formulário)

> Acessibilidade **não é só** pra quem é cego ou não tem uma perna. É pra **qualquer pessoa usar
> o aparelho** do jeito dela. Uma pessoa **em perigo** usa a acessibilidade dela: **apertar o
> botão lateral pra pedir socorro**, sem precisar desbloquear nem abrir o app. É esse uso que o
> app atende.

## Roteiro do vídeo pra revisão (quando for a hora)

1. Mostrar o app aberto e a tela de ativação do serviço (o que o usuário vê e consente).
2. Bloquear a tela do celular.
3. Segurar o botão de volume por ~5s — o celular vibra (disparo discreto, sem abrir a tela).
4. Mostrar o **outro aparelho** (o guardião) recebendo o alerta com a localização.
5. Narrar a justificativa acima: o gesto é a "acessibilidade" da pessoa em perigo.

## O que fazer no código (quando o Rodrigo mandar, no 2º round)

- Religar o `GuardianService` (serviço de Acessibilidade): colar de volta o `<service>` de
  `docs/acessibilidade-service.xml.txt` no Manifest e `Features.ACESSIBILIDADE = true`.
- Recolocar `ACCESS_BACKGROUND_LOCATION` (a localização com o app fechado), que o Google também
  revisa com vídeo.
- O `MonitorService` (sem acessibilidade) pode continuar como reforço pros aparelhos que aceitam
  a sessão de mídia — mas a Acessibilidade é o que garante o Samsung.

## Nota honesta (pra não vender ilusão)

A política do Google sobre AccessibilityService é **rígida** (pensada pra deficiência; exige
disclosure e, às vezes, o formulário de declaração de permissão). O vídeo + a justificativa
**melhoram muito** as chances, mas **não garantem**. Por isso é **atualização**, não lançamento:
se recusarem, o app aprovado continua no ar e a gente ajusta.

# Planos do GuardiõesSOS — tabela travada (07/11)

Lógica dele (Bill Gates): **software barato pra milhões**, não caro pra poucos. Por isso o preço é
**por ANO** e baixo. R$30 por MÊS é caro; a âncora é **US$5 por ANO**.

## A tabela

| Plano | Preço | O que tem |
|---|---|---|
| **Grátis** | R$0 | **Pânico da família**: botão de SOS → SMS pros contatos + sirene/aviso pros guardiões com o app. O socorro básico é sempre de graça. |
| **Plano 1 — Proteção+** | **US$5/ano** (~R$29,90/ano) | Tudo do grátis **+ GPS ao vivo no mapa** (seguir na emergência) **+ "achar um ao outro"** (grupo de localização ao vivo pra sair/eventos) **+ criar mais** guardiões/lugares. |
| **Plano 2 — Completo** | **US$10/ano** (~R$59,90/ano) | Tudo do Plano 1 **+ vídeo + microfone** no SOS **+ casa/trabalho (lugares)** **+ criar ainda mais**. |

## Regras de preço

- 🔴 **É por ANO, não por mês.** (Decisão dele, 07/11: "não é por mês, é por ano; R$30/mês é caro".)
- **Âncora = US$5** (Plano 1). Volume > margem.
- **Preço base em dólar; a loja mostra a moeda de cada país** (R$, £, €, US$...) e já cobra o
  imposto local. Cobre o "vender pro mundo todo" sem a gente fazer preço país por país.
- **Isca: experimenta grátis, depois paga.** Usa o "período grátis" nativo da assinatura das lojas
  (a pessoa prova no primeiro evento; na renovação, cobra).
- **🔴 O socorro (pânico + SMS/push) NUNCA é bloqueado por pagamento** — regra de ouro do produto.
  O que é pago é o EXTRA (GPS ao vivo, grupo, vídeo/áudio, mais guardiões).

## Pendências técnicas (quando formos LIGAR a cobrança, não agora)

- Os IDs no código estão como `guardioes_protecao_mensal` / `guardioes_completo_mensal`
  (`billing/Billing.kt`). Como é **anual**, criar na loja como **assinatura ANUAL** e, de
  preferência, renomear os IDs pra `..._anual` (o ID é permanente depois de criado — definir certo
  na 1ª vez). O app já mostra o preço que vier da loja (localizado), então o valor não fica
  "chumbado" no código.
- **Gating por plano ainda NÃO existe** — hoje só a TELA do plano existe; falta a lógica de liberar
  cada recurso conforme o plano assinado. Entra quando a cobrança for ligada.

## Status

Tabela **travada** por ele em 07/11. Cobrança **não está ligada** (iOS em revisão; billing entra na
atualização, depois da v1.0 aprovada).

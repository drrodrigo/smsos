# Onboarding: guardião por último + o "bate-bola" (ideia do Rodrigo, 06/11)

## A ideia

- No cadastro, **não** pedir guardião no começo — trava a ativação (a pessoa empaca em "quem
  vou botar? deixa eu achar na agenda…" e abandona).
- Guardião é o **último item** da configuração, nunca o primeiro.
- Para concluir, a pessoa faz um **teste guiado obrigatório**: ela **manda** um alerta pro
  guardião e o guardião **confirma de volta** ("recebi / ok").
- Esse ida‑e‑volta é o **"bate‑bola"**: disparou → chegou no guardião → guardião confirma →
  a pessoa **vê** que funciona **e** sabe que o guardião aceitou e entendeu o papel dele.

## Por que é forte

- **Ativa rápido** (sem atrito no começo do cadastro).
- **Prova o caminho inteiro ANTES da emergência** — não se descobre na hora do perigo que não
  funcionava.
- **Onboarda o guardião junto** (ele recebe, entende e aceita) → é o laço viral do produto.
- **Dá confiança**: "meu guardião está pronto".

## O que já existe (dá pra reaproveitar)

- Convite/aceite de guardião: Edge Function `guardioes-convite` + aba **Protejo**.
- Sirene/aviso pro guardião: `guardioes-alertar` / `guardioes-push`.
- Então o bate‑bola ≈ o teste manda convite + alerta → guardião recebe → toca "Aceitar/Recebi"
  → volta pro protegido como "✓ pronto".

## Os cuidados (decisões a tomar)

1. 🔴 **O SOS de verdade NUNCA espera o bate‑bola.** Numa emergência o alerta dispara sempre,
   mesmo que o guardião nunca tenha confirmado. O bate‑bola é para **ativação/confiança**, não é
   tranca do socorro. (Regra de ouro do produto: o socorro não pode ser bloqueado por nada.)
2. **Guardião sem o app:** o convite/confirmação precisa funcionar por um **link** (SMS/WhatsApp
   → página que explica + botão "Confirmar que recebi"), senão a pessoa fica travada esperando o
   guardião instalar. Depois ele pode virar usuário — o laço viral.
3. **É assíncrono:** o guardião pode confirmar minutos/horas depois. O onboarding diz "mandei o
   convite pro seu guardião; assim que ele confirmar, te aviso", e o "✓ pronto" acende quando a
   confirmação chega (push/consulta), não necessariamente na mesma sessão.

## Fluxo proposto

1. Cadastro **rápido** (nome + número) → entra no app.
2. Checklist de configuração com o **guardião em ÚLTIMO** (pendente).
3. Toca em "Adicionar guardião" (da agenda ou digitando) → escolhe.
4. "Vamos fazer o teste (bate‑bola)": o app manda o **convite + um alerta de teste**.
5. Guardião recebe → toca **"Recebi / Aceito"**.
6. O protegido vê **"✓ [Nome] recebeu e está pronto — você está protegido"** → checklist fica
   verde. Só aqui o setup é considerado concluído.

## Status

Ideia registrada. **Não implementado ainda** (mexe no onboarding/SetupWizard e no fluxo de
convite). É uma frente separada do gesto do botão. Fazer quando o Rodrigo mandar.

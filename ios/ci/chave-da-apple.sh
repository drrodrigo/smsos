#!/usr/bin/env bash
# A CHAVE DA APPLE para a rotina do iPhone do GuardiõesSOS.
#
# A chave `claude-gestao` (Admin) mora só nos segredos do Supabase (os MESMOS do PDV, no mesmo
# projeto). A rotina pede ao GitHub um token de identidade (OIDC, público "guardioes-apple") e o
# entrega à Edge Function guardioes-apple-envio, que confere repositório, rotina e ramo antes de
# responder. Nada aqui imprime a chave. Espelha pdv/ios/ci/chave-da-apple.sh do PDV.
#
# Uso: chave-da-apple.sh <pasta>
#   -> <pasta>/AuthKey_<id>.p8 e as saídas do passo: tem, chave, key_id, issuer, app_existe, app_id
# Precisa de: permissions: id-token: write; curl; jq.
set -euo pipefail

PASTA="${1:?diga a pasta}"
FUNCAO="https://ymszgqpgxtprkqxjhtwb.supabase.co/functions/v1/guardioes-apple-envio"

if [ -z "${ACTIONS_ID_TOKEN_REQUEST_URL:-}" ]; then
  echo "::notice::Sem o token de identidade do GitHub: o app só é montado, sem assinatura nem envio."
  echo "tem=nao" >> "$GITHUB_OUTPUT"
  exit 0
fi

mkdir -p "$PASTA"
chmod 700 "$PASTA"
OIDC="$(curl -sS -H "Authorization: bearer $ACTIONS_ID_TOKEN_REQUEST_TOKEN" \
  "$ACTIONS_ID_TOKEN_REQUEST_URL&audience=guardioes-apple" | jq -r .value)"
echo "::add-mask::$OIDC"

pedir() { # pedir <acao> <arquivo de saída> -> o código HTTP
  curl -sS -o "$2" -w '%{http_code}' -X POST "$FUNCAO" \
    -H 'Content-Type: application/json' -H "x-github-oidc: $OIDC" -d "{\"acao\":\"$1\"}"
}

CODIGO="$(pedir preparar "$PASTA/preparar.json")"
if [ "$CODIGO" != 200 ]; then
  echo "::error::guardioes-apple-envio (preparar) respondeu $CODIGO: $(cat "$PASTA/preparar.json")"
  exit 1
fi
echo "A Apple, pela porta guardioes-apple-envio:"
jq . "$PASTA/preparar.json"

CODIGO="$(pedir chave "$PASTA/chave.json")"
if [ "$CODIGO" != 200 ]; then
  # Resposta de erro não traz a chave: dá para mostrar.
  echo "::error::guardioes-apple-envio (chave) respondeu $CODIGO: $(cat "$PASTA/chave.json")"
  exit 1
fi
KEY_ID="$(jq -r .key_id "$PASTA/chave.json")"
ISSUER="$(jq -r .issuer "$PASTA/chave.json")"
CHAVE="$PASTA/AuthKey_$KEY_ID.p8"
jq -r .p8 "$PASTA/chave.json" > "$CHAVE"
rm -f "$PASTA/chave.json"
chmod 600 "$CHAVE"
grep -q 'BEGIN PRIVATE KEY' "$CHAVE" || { echo "::error::A chave chegou num formato que não é .p8."; exit 1; }

{
  echo "tem=sim"
  echo "chave=$CHAVE"
  echo "key_id=$KEY_ID"
  echo "issuer=$ISSUER"
  echo "app_existe=$(jq -r 'if .app_existe then "sim" else "nao" end' "$PASTA/preparar.json")"
  echo "app_id=$(jq -r '.app_id // ""' "$PASTA/preparar.json")"
} >> "$GITHUB_OUTPUT"

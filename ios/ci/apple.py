"""Conversa com a App Store Connect pela chave `claude-gestao` (a mesma do painel de aplicativos).

A chave chega ao executor do GitHub pela porta `guardioes-apple-envio` do Supabase
(chave-da-apple.sh) e mora num arquivo do executor só durante a execução. Nada aqui imprime a
chave nem o token. Espelha pdv/ios/ci/apple.py do PDV (drrodrigo/catologoonline).
"""

import json
import os
import time
import urllib.error
import urllib.request

import jwt

API = "https://api.appstoreconnect.apple.com"
PACOTE = "br.com.guardioessos"


class ErroDaApple(Exception):
    def __init__(self, metodo, caminho, status, corpo):
        super().__init__(f"{metodo} {caminho}: {status} {corpo[:600]}")
        self.status = status
        self.corpo = corpo


class Apple:
    def __init__(self):
        self.key_id = os.environ["APPLE_KEY_ID"]
        self.issuer = os.environ["APPLE_ISSUER_ID"]
        with open(os.environ["APPLE_CHAVE"], encoding="utf-8") as f:
            self._p8 = f.read()
        self._token = None
        self._vence = 0

    def _autorizacao(self):
        agora = int(time.time())
        if not self._token or agora > self._vence - 60:
            self._vence = agora + 1100
            self._token = jwt.encode(
                {"iss": self.issuer, "iat": agora, "exp": self._vence, "aud": "appstoreconnect-v1"},
                self._p8,
                algorithm="ES256",
                headers={"kid": self.key_id, "typ": "JWT"},
            )
        return f"Bearer {self._token}"

    def pedir(self, metodo, caminho, corpo=None):
        dados = json.dumps(corpo).encode() if corpo is not None else None
        pedido = urllib.request.Request(API + caminho, data=dados, method=metodo)
        pedido.add_header("Authorization", self._autorizacao())
        if dados is not None:
            pedido.add_header("Content-Type", "application/json")
        try:
            with urllib.request.urlopen(pedido, timeout=60) as r:
                texto = r.read().decode()
                return json.loads(texto) if texto else {}
        except urllib.error.HTTPError as e:
            raise ErroDaApple(metodo, caminho, e.code, e.read().decode(errors="replace")) from None

    def get(self, caminho):
        return self.pedir("GET", caminho)

    def post(self, caminho, corpo):
        return self.pedir("POST", caminho, corpo)

    def patch(self, caminho, corpo):
        return self.pedir("PATCH", caminho, corpo)

    def delete(self, caminho):
        return self.pedir("DELETE", caminho)

    def app(self):
        for a in self.get(f"/v1/apps?filter[bundleId]={PACOTE}").get("data", []):
            if a["attributes"].get("bundleId") == PACOTE:
                return a
        return None

    def pacote(self):
        for b in self.get(f"/v1/bundleIds?filter[identifier]={PACOTE}&limit=200").get("data", []):
            if b["attributes"].get("identifier") == PACOTE:
                return b
        return None


def saida(nome, valor):
    """Uma saída do passo do GitHub (GITHUB_OUTPUT)."""
    caminho = os.environ.get("GITHUB_OUTPUT")
    if caminho:
        with open(caminho, "a", encoding="utf-8") as f:
            f.write(f"{nome}={valor}\n")


def resumo(texto):
    """Uma linha no resumo da execução do GitHub."""
    caminho = os.environ.get("GITHUB_STEP_SUMMARY")
    if caminho:
        with open(caminho, "a", encoding="utf-8") as f:
            f.write(texto + "\n")
    print(texto)

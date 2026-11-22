"""O GUARDIÕESSOS NA APP STORE: por que o app aprovado não aparece na loja? (só LEITURA)

Pedido do Rodrigo (22/11): "como fazemos pra liberar o aplicativo na apple". A versão 1.0 foi aprovada
(e-mail "Ready for Distribution" de 19/11), mas em 22/11 a App Store não tem a página dele (nem no
Brasil nem nos EUA). Ele não consegue entrar na App Store Connect, então a conferência é pela chave da
rotina (a mesma do TestFlight).

Mostra, sem mudar nada: as versões e o estado de cada uma, a disponibilidade por país (com o motivo
que a Apple dá quando NÃO está à venda, ex.: BRAZIL_REQUIRED_TAX_ID, MISSING_RATING) e o preço.

22/11, o resultado: versão 1.0 READY_FOR_SALE, preço com base no Brasil, e NENHUM país escolhido (a
Apple responde 404 para a disponibilidade). Por isso a página da loja não existe.

`python loja.py --liberar` (só pela escolha "liberar no Brasil" do botão, com o OK dele): marca o
app como disponível SÓ no Brasil (o SMS sai pela nossa estrutura, para números do Brasil; o app é em
português) e sem entrar sozinho em países novos. Depois mostra o relatório de novo.
"""

import collections
import sys

from apple import PACOTE, Apple, ErroDaApple


def tentar(rotulo, funcao):
    try:
        return funcao()
    except ErroDaApple as e:
        print(f"  [{rotulo}] a Apple respondeu {e.status}: {e.corpo[:400]}")
        return None


def todas(a, caminho):
    """Junta as páginas de uma lista da API."""
    dados, incluidos = [], []
    while caminho:
        r = a.pedir("GET", caminho)
        dados += r.get("data", [])
        incluidos += r.get("included", [])
        prox = (r.get("links") or {}).get("next")
        caminho = prox.replace("https://api.appstoreconnect.apple.com", "") if prox else None
    return dados, incluidos


def main():
    a = Apple()
    apps = a.pedir("GET", f"/v1/apps?filter[bundleId]={PACOTE}&fields[apps]=name,bundleId,sku,primaryLocale,contentRightsDeclaration")
    if not apps.get("data"):
        print(f"O app {PACOTE} NÃO existe na App Store Connect.")
        return
    app = apps["data"][0]
    app_id = app["id"]
    print(f"App: {app['attributes'].get('name')} | id {app_id} | {PACOTE}")
    print(f"Página que deveria existir: https://apps.apple.com/br/app/id{app_id}")
    print(f"Direitos de conteúdo: {app['attributes'].get('contentRightsDeclaration')}")

    print("\n== Versões (iOS) ==")
    versoes = tentar("versões", lambda: a.pedir(
        "GET",
        f"/v1/apps/{app_id}/appStoreVersions?filter[platform]=IOS&limit=10"
        "&fields[appStoreVersions]=versionString,appStoreState,appVersionState,releaseType,earliestReleaseDate,createdDate",
    ))
    for v in (versoes or {}).get("data", []):
        at = v["attributes"]
        print(f"  {at.get('versionString')}: appVersionState={at.get('appVersionState')} "
              f"appStoreState={at.get('appStoreState')} releaseType={at.get('releaseType')} "
              f"earliestReleaseDate={at.get('earliestReleaseDate')} criada={at.get('createdDate')}")

    print("\n== Disponibilidade por país ==")
    disp = tentar("disponibilidade", lambda: a.pedir("GET", f"/v1/apps/{app_id}/appAvailabilityV2"))
    if disp and disp.get("data"):
        disp_id = disp["data"]["id"]
        print(f"  disponível em países novos automaticamente: {disp['data']['attributes'].get('availableInNewTerritories')}")
        territorios = tentar("países", lambda: todas(
            a,
            f"/v2/appAvailabilities/{disp_id}/territoryAvailabilities?limit=200"
            "&fields[territoryAvailabilities]=available,releaseDate,contentStatuses,territory&include=territory",
        ))
        if territorios:
            dados, _ = territorios
            disponiveis = 0
            motivos = collections.Counter()
            for t in dados:
                at = t["attributes"]
                pais = ((t.get("relationships") or {}).get("territory") or {}).get("data", {}).get("id", "?")
                if at.get("available"):
                    disponiveis += 1
                for s in at.get("contentStatuses") or []:
                    motivos[s] += 1
                if pais in ("BRA", "USA", "PRT"):
                    print(f"  {pais}: available={at.get('available')} releaseDate={at.get('releaseDate')} "
                          f"contentStatuses={at.get('contentStatuses')}")
            print(f"  países marcados como disponíveis: {disponiveis} de {len(dados)}")
            print(f"  situação (contentStatuses) em todos os países: {dict(motivos)}")
    else:
        print("  SEM disponibilidade configurada (nenhum país escolhido).")

    print("\n== Preço ==")
    preco = tentar("preço", lambda: a.pedir("GET", f"/v1/apps/{app_id}/appPriceSchedule?include=baseTerritory,manualPrices"))
    if preco:
        base = ((preco.get("data") or {}).get("relationships") or {}).get("baseTerritory", {}).get("data")
        print(f"  país base do preço: {base}")
        if not preco.get("included"):
            print("  (sem preço manual listado)")
    # O VALOR do preço (o ponto de preço do país base): tem que ser 0,00, o app é gratuito.
    valores = tentar("valor do preço", lambda: a.pedir(
        "GET",
        f"/v1/appPriceSchedules/{app_id}/manualPrices?include=appPricePoint,territory"
        "&fields[appPricePoints]=customerPrice,proceeds",
    ))
    for inc in (valores or {}).get("included", []):
        if inc.get("type") == "appPricePoints":
            print(f"  valor ao cliente: {inc['attributes'].get('customerPrice')} (repasse {inc['attributes'].get('proceeds')})")


def precos_ao_cliente(a, app_id):
    """Os valores ao cliente dos preços manuais (ex.: ["0.0"]). Lista vazia = não deu para saber."""
    r = a.pedir(
        "GET",
        f"/v1/appPriceSchedules/{app_id}/manualPrices?include=appPricePoint&fields[appPricePoints]=customerPrice",
    )
    return [inc["attributes"].get("customerPrice") for inc in r.get("included", []) if inc.get("type") == "appPricePoints"]


def liberar_no_brasil():
    """Disponível SÓ no Brasil. Só roda com o OK dele (escolha "liberar no Brasil" no botão)."""
    a = Apple()
    apps = a.pedir("GET", f"/v1/apps?filter[bundleId]={PACOTE}&fields[apps]=bundleId")
    app_id = next(x["id"] for x in apps["data"] if x["attributes"].get("bundleId") == PACOTE)
    # Trava: o app é GRATUITO. Se o preço não for 0,00 (ou não der para saber), NÃO libera.
    precos = tentar("valor do preço", lambda: precos_ao_cliente(a, app_id)) or []
    if not precos or any(float(p or "1") != 0 for p in precos):
        print(f"NÃO liberei: o preço não está confirmado como gratuito ({precos}). Nada foi mudado.")
        return
    existente = tentar("disponibilidade", lambda: a.pedir("GET", f"/v1/apps/{app_id}/appAvailabilityV2"))
    if existente and existente.get("data"):
        # Já existe: só garante o Brasil ligado (os outros países ficam como estão).
        disp_id = existente["data"]["id"]
        dados, _ = todas(a, f"/v2/appAvailabilities/{disp_id}/territoryAvailabilities?limit=200&include=territory")
        for t in dados:
            pais = ((t.get("relationships") or {}).get("territory") or {}).get("data", {}).get("id")
            if pais == "BRA" and not t["attributes"].get("available"):
                a.pedir("PATCH", f"/v1/territoryAvailabilities/{t['id']}", {
                    "data": {"type": "territoryAvailabilities", "id": t["id"], "attributes": {"available": True}},
                })
                print("Brasil ligado na disponibilidade que já existia.")
        return
    a.pedir("POST", "/v2/appAvailabilities", {
        "data": {
            "type": "appAvailabilities",
            "attributes": {"availableInNewTerritories": False},
            "relationships": {
                "app": {"data": {"type": "apps", "id": app_id}},
                "territoryAvailabilities": {"data": [{"type": "territoryAvailabilities", "id": "${bra}"}]},
            },
        },
        "included": [{
            "type": "territoryAvailabilities",
            "id": "${bra}",
            "attributes": {"available": True},
            "relationships": {"territory": {"data": {"type": "territories", "id": "BRA"}}},
        }],
    })
    print("Disponibilidade criada: SÓ no Brasil (sem países novos automáticos).")


if __name__ == "__main__":
    if "--liberar" in sys.argv:
        print("== LIBERANDO no Brasil ==")
        liberar_no_brasil()
        print()
    main()

"""O GUARDIÕESSOS NA APP STORE: por que o app aprovado não aparece na loja? (só LEITURA)

Pedido do Rodrigo (22/11): "como fazemos pra liberar o aplicativo na apple". A versão 1.0 foi aprovada
(e-mail "Ready for Distribution" de 19/11), mas em 22/11 a App Store não tem a página dele (nem no
Brasil nem nos EUA). Ele não consegue entrar na App Store Connect, então a conferência é pela chave da
rotina (a mesma do TestFlight).

Mostra, sem mudar nada: as versões e o estado de cada uma, a disponibilidade por país (com o motivo
que a Apple dá quando NÃO está à venda, ex.: BRAZIL_REQUIRED_TAX_ID, MISSING_RATING) e o preço.
"""

import collections

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
        for inc in preco.get("included", []):
            if inc.get("type") == "appPrices":
                print(f"  preço manual: {inc.get('attributes')}")
        if not preco.get("included"):
            print("  (sem preço manual listado)")


if __name__ == "__main__":
    main()

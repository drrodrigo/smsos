"""O TESTFLIGHT do GuardiõesSOS: depois do envio, deixa o build pronto para instalar.

Espelha pdv/ios/ci/testflight.py do PDV (drrodrigo/catologoonline).

1. Espera a Apple processar o build (de 5 a 30 minutos).
2. Grupo INTERNO "Equipe" (o dono da conta): instala NA HORA, sem a revisão da Apple, com todos
   os builds. É por aqui que o Rodrigo instala a versão de teste no iPhone dele.
3. Grupo externo "Guardiões", com link público, para quando o app estiver pronto (a primeira
   versão de cada número passa pela revisão do TestFlight; um app mínimo pode ser recusado aí, e
   tudo bem: o grupo interno não depende disso).

Uso: testflight.py <versao> <build>   (ex.: 0.1.0 57)
"""

import sys
import time
import urllib.parse

from apple import Apple, ErroDaApple, resumo, saida

GRUPO = "Guardiões"
GRUPO_INTERNO = "Equipe"
POLITICA = "https://www.guardioessos.com.br/privacidade"
SITE = "https://www.guardioessos.com.br"
DESCRICAO = (
    "GuardiõesSOS é um botão de pânico pessoal: numa emergência, um toque envia um SOS com a sua "
    "localização para seus guardiões (as pessoas de confiança que você escolhe). Dois tipos de "
    "alerta: Segurança e Saúde. Esta é uma versão de teste."
)
NOVIDADES = "Versão de teste do GuardiõesSOS no iPhone. Conte o que estranhar."
# O revisor da Apple lê inglês.
NOTAS_DA_REVISAO = (
    "GuardiõesSOS is a personal safety / panic button app. One tap sends an SOS alert with the "
    "user's location to their trusted guardians. This TestFlight build is an early version for "
    "the developer's own device."
)


def esperar_build(apple, app_id, versao, build):
    caminho = (
        f"/v1/builds?filter[app]={app_id}&filter[version]={build}"
        f"&filter[preReleaseVersion.version]={versao}&limit=1"
    )
    for volta in range(90):  # até 45 minutos
        dados = apple.get(caminho).get("data", [])
        if dados:
            estado = dados[0]["attributes"].get("processingState")
            print(f"build {versao} ({build}): {estado}")
            if estado == "VALID":
                return dados[0]
            if estado in ("FAILED", "INVALID"):
                resumo(f"::error::A Apple recusou o build {versao} ({build}): {estado}.")
                sys.exit(1)
        else:
            print(f"build {versao} ({build}): ainda não apareceu na App Store Connect")
        time.sleep(30)
    resumo("::error::A Apple não terminou de processar o build em 45 minutos.")
    sys.exit(1)


def contatos_dos_outros_apps(apple, app_id):
    """Os contatos da revisão e o e-mail de retorno que ele já deu à Apple nos outros apps."""
    contato, retorno = None, None
    for outro in apple.get("/v1/apps?limit=200").get("data", []):
        if outro["id"] == app_id:
            continue
        try:
            d = apple.get(f"/v1/apps/{outro['id']}/betaAppReviewDetail")["data"]["attributes"]
            if not contato and d.get("contactEmail") and d.get("contactPhone"):
                contato = d
            for loc in apple.get(f"/v1/apps/{outro['id']}/betaAppLocalizations").get("data", []):
                if not retorno and loc["attributes"].get("feedbackEmail"):
                    retorno = loc["attributes"]["feedbackEmail"]
            if not contato:
                for v in apple.get(f"/v1/apps/{outro['id']}/appStoreVersions?limit=3").get("data", []):
                    rd = apple.get(f"/v1/appStoreVersions/{v['id']}/appStoreReviewDetail").get("data")
                    a = (rd or {}).get("attributes") or {}
                    if a.get("contactEmail") and a.get("contactPhone"):
                        contato = a
                        break
        except ErroDaApple as e:
            print(f"(contatos de {outro['attributes'].get('name')}: {e.status})")
    return contato, retorno or (contato or {}).get("contactEmail")


def texto_do_teste(apple, app_id, retorno):
    atributos = {"description": DESCRICAO, "feedbackEmail": retorno, "marketingUrl": SITE, "privacyPolicyUrl": POLITICA}
    for loc in apple.get(f"/v1/apps/{app_id}/betaAppLocalizations").get("data", []):
        if loc["attributes"].get("locale") == "pt-BR":
            apple.patch(f"/v1/betaAppLocalizations/{loc['id']}", {"data": {"type": "betaAppLocalizations", "id": loc["id"], "attributes": atributos}})
            return
    apple.post(
        "/v1/betaAppLocalizations",
        {
            "data": {
                "type": "betaAppLocalizations",
                "attributes": {"locale": "pt-BR", **atributos},
                "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
            }
        },
    )


def detalhes_da_revisao(apple, app_id, contato):
    atributos = {
        "contactFirstName": contato.get("contactFirstName"),
        "contactLastName": contato.get("contactLastName"),
        "contactPhone": contato.get("contactPhone"),
        "contactEmail": contato.get("contactEmail"),
        "demoAccountRequired": False,
        "notes": NOTAS_DA_REVISAO,
    }
    apple.patch(f"/v1/betaAppReviewDetails/{app_id}", {"data": {"type": "betaAppReviewDetails", "id": app_id, "attributes": atributos}})


def novidades_do_build(apple, build_id):
    for loc in apple.get(f"/v1/builds/{build_id}/betaBuildLocalizations").get("data", []):
        if loc["attributes"].get("locale") == "pt-BR":
            apple.patch(f"/v1/betaBuildLocalizations/{loc['id']}", {"data": {"type": "betaBuildLocalizations", "id": loc["id"], "attributes": {"whatsNew": NOVIDADES}}})
            return
    apple.post(
        "/v1/betaBuildLocalizations",
        {
            "data": {
                "type": "betaBuildLocalizations",
                "attributes": {"locale": "pt-BR", "whatsNew": NOVIDADES},
                "relationships": {"build": {"data": {"type": "builds", "id": build_id}}},
            }
        },
    )


def grupo(apple, app_id):
    for g in apple.get(f"/v1/apps/{app_id}/betaGroups?limit=200").get("data", []):
        if g["attributes"].get("name") == GRUPO and not g["attributes"].get("isInternalGroup"):
            return g
    return apple.post(
        "/v1/betaGroups",
        {
            "data": {
                "type": "betaGroups",
                "attributes": {"name": GRUPO, "feedbackEnabled": True},
                "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
            }
        },
    )["data"]


def equipe_interna(apple, app_id):
    """O grupo INTERNO "Equipe": quem já é usuário da App Store Connect testa NA HORA, sem a
    revisão da Apple, com todos os builds. Entra o dono da conta (ACCOUNT_HOLDER); ele recebe o
    convite da Apple por e-mail e instala pelo app TestFlight."""
    grupos = apple.get(f"/v1/apps/{app_id}/betaGroups?limit=200").get("data", [])
    g = next((x for x in grupos if x["attributes"].get("name") == GRUPO_INTERNO and x["attributes"].get("isInternalGroup")), None)
    if not g:
        g = apple.post(
            "/v1/betaGroups",
            {
                "data": {
                    "type": "betaGroups",
                    "attributes": {"name": GRUPO_INTERNO, "isInternalGroup": True, "hasAccessToAllBuilds": True},
                    "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
                }
            },
        )["data"]
    for u in apple.get("/v1/users?limit=200").get("data", []):
        a = u["attributes"]
        if "ACCOUNT_HOLDER" not in (a.get("roles") or []):
            continue
        try:
            apple.post(
                "/v1/betaTesters",
                {
                    "data": {
                        "type": "betaTesters",
                        "attributes": {"email": a.get("username"), "firstName": a.get("firstName"), "lastName": a.get("lastName")},
                        "relationships": {"betaGroups": {"data": [{"type": "betaGroups", "id": g["id"]}]}},
                    }
                },
            )
            resumo(f"Testador interno convidado: {a.get('firstName')} (e-mail da Apple a caminho).")
        except ErroDaApple as e:
            if e.status != 409:
                raise
            # Já é testador (de outro app da conta): liga o cadastro que existe ao grupo deste.
            email = urllib.parse.quote(a.get("username") or "")
            for t in apple.get(f"/v1/betaTesters?filter[email]={email}&limit=10").get("data", []):
                apple.post(f"/v1/betaGroups/{g['id']}/relationships/betaTesters", {"data": [{"type": "betaTesters", "id": t["id"]}]})
                resumo(f"Testador interno ligado ao grupo {GRUPO_INTERNO}: {a.get('firstName')}.")


def main():
    versao, build = sys.argv[1], sys.argv[2]
    apple = Apple()
    app = apple.app()
    if not app:
        resumo("::error::O app br.com.guardioessos não existe na App Store Connect.")
        sys.exit(1)
    app_id = app["id"]

    b = esperar_build(apple, app_id, versao, build)
    try:
        equipe_interna(apple, app_id)
    except ErroDaApple as e:
        resumo(f"::warning::Grupo interno: a Apple respondeu {e.status} ({e.corpo[:300]}).")
    build_id = b["id"]
    if b["attributes"].get("usesNonExemptEncryption") is None:
        apple.patch(f"/v1/builds/{build_id}", {"data": {"type": "builds", "id": build_id, "attributes": {"usesNonExemptEncryption": False}}})

    contato, retorno = contatos_dos_outros_apps(apple, app_id)
    if not contato or not retorno:
        resumo(
            "::warning::Não achei os contatos da revisão nos outros apps da conta. O build está na "
            "App Store Connect e no grupo interno (Equipe); o grupo público ficou para depois."
        )
        return
    texto_do_teste(apple, app_id, retorno)
    detalhes_da_revisao(apple, app_id, contato)
    novidades_do_build(apple, build_id)

    g = grupo(apple, app_id)
    try:
        apple.post(f"/v1/betaGroups/{g['id']}/relationships/builds", {"data": [{"type": "builds", "id": build_id}]})
    except ErroDaApple as e:
        print(f"(build no grupo: {e.status}, segue)")

    try:
        apple.post(
            "/v1/betaAppReviewSubmissions",
            {"data": {"type": "betaAppReviewSubmissions", "relationships": {"build": {"data": {"type": "builds", "id": build_id}}}}},
        )
        resumo(f"Build {versao} ({build}) enviado para a revisão do TestFlight.")
    except ErroDaApple as e:
        # Já pedido; outro build da mesma versão ainda em revisão; versão já aprovada; ou app mínimo
        # ainda sem funcionalidade para a revisão externa (segue valendo no grupo interno).
        resumo(f"Revisão do TestFlight: a Apple respondeu {e.status} ({e.corpo[:300]}).")

    link = g["attributes"].get("publicLink")
    if not g["attributes"].get("publicLinkEnabled"):
        try:
            g = apple.patch(
                f"/v1/betaGroups/{g['id']}",
                {"data": {"type": "betaGroups", "id": g["id"], "attributes": {"publicLinkEnabled": True, "publicLinkLimitEnabled": False}}},
            )["data"]
            link = g["attributes"].get("publicLink")
        except ErroDaApple as e:
            resumo(f"Link público: a Apple respondeu {e.status} ({e.corpo[:300]}). Ele liga depois da primeira aprovação.")
    if link:
        resumo(f"Link público do TestFlight: {link}")
        saida("link", link)


if __name__ == "__main__":
    main()

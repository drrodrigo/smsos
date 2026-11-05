"""O CERTIFICADO DE DISTRIBUIÇÃO e o PERFIL DE LOJA do GuardiõesSOS no iPhone.

Espelha pdv/ios/ci/assinatura.py do PDV (drrodrigo/catologoonline). O certificado nasce UMA vez,
com a chave privada gerada aqui, e fica cifrado (AES-256, pbkdf2 com 600 mil voltas, a senha
GUARDIOES_SENHA_DA_CHAVE) na release `guardioes-certificado-apple` deste repositório, que é
privado. As próximas execuções usam o guardado: a Apple limita os certificados de distribuição da
conta, e cada certificado novo vira e-mail para ele. Vencendo (dura um ano) ou revogado no site da
Apple, nasce outro sozinho.

O perfil (o "App Store" que liga o certificado ao pacote br.com.guardioessos) é refeito a cada
execução: não é segredo e não tem limite.

Por que não a assinatura automática do Xcode: ela arquiva com o perfil de DESENVOLVIMENTO, que
exige aparelho cadastrado na conta, e cria um certificado novo a cada execução (o Mac do GitHub
nasce vazio).

Uso: assinatura.py <pasta>
  -> <pasta>/certificado.p12, <pasta>/perfil.mobileprovision, <pasta>/ExportOptions.plist
  -> saídas do passo: time, perfil, uuid
"""

import base64
import datetime
import os
import plistlib
import subprocess
import sys
import urllib.parse

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

from apple import PACOTE, Apple, ErroDaApple, saida

RELEASE = "guardioes-certificado-apple"
CIFRADO = "certificado-apple.p12.cifrado"
NOME_DO_PERFIL = "GuardioesSOS App Store (GitHub)"


def senha() -> bytes:
    s = os.environ.get("GUARDIOES_SENHA_DA_CHAVE", "")
    if len(s) < 12:
        print("::error::Sem a senha GUARDIOES_SENHA_DA_CHAVE (12 letras ou mais). Crie o segredo no repositório.")
        sys.exit(1)
    return s.encode()


# O formato do `openssl enc -aes-256-cbc -pbkdf2 -iter 600000`, feito aqui: o openssl do Mac do
# GitHub pode ser o LibreSSL, que não tem todas essas opções.
def _chave_e_iv(sal: bytes):
    kdf = PBKDF2HMAC(algorithm=hashes.SHA256(), length=48, salt=sal, iterations=600000)
    k = kdf.derive(senha())
    return k[:32], k[32:]


def cifrar(dados: bytes) -> bytes:
    sal = os.urandom(8)
    chave, iv = _chave_e_iv(sal)
    enchimento = padding.PKCS7(128).padder()
    cifra = Cipher(algorithms.AES(chave), modes.CBC(iv)).encryptor()
    return b"Salted__" + sal + cifra.update(enchimento.update(dados) + enchimento.finalize()) + cifra.finalize()


def decifrar(dados: bytes):
    if dados[:8] != b"Salted__":
        return None
    chave, iv = _chave_e_iv(dados[8:16])
    try:
        decifra = Cipher(algorithms.AES(chave), modes.CBC(iv)).decryptor()
        cheio = decifra.update(dados[16:]) + decifra.finalize()
        tira = padding.PKCS7(128).unpadder()
        return tira.update(cheio) + tira.finalize()
    except ValueError:
        return None


def p12_do_mac(chave, cert) -> bytes:
    # O `security import` do Mac não abre o PKCS#12 moderno (AES): vai no formato antigo, 3DES.
    cifra = (
        serialization.PrivateFormat.PKCS12.encryption_builder()
        .kdf_rounds(50000)
        .key_cert_algorithm(pkcs12.PBES.PBESv1SHA1And3KeyTripleDESCBC)
        .hmac_hash(hashes.SHA1())
        .build(senha())
    )
    return pkcs12.serialize_key_and_certificates(b"GuardioesSOS", chave, cert, None, cifra)


def serial(texto: str) -> str:
    return texto.upper().lstrip("0")


def ativos(apple: Apple) -> dict:
    """Os certificados de distribuição ativos na Apple, pelo número de série."""
    lista = apple.get("/v1/certificates?filter[certificateType]=DISTRIBUTION&limit=200").get("data", [])
    return {serial(c["attributes"].get("serialNumber", "")): c["id"] for c in lista}


def guardado(apple: Apple, pasta: str):
    repo = os.environ["REPO"]
    r = subprocess.run(
        ["gh", "release", "download", RELEASE, "--repo", repo, "--pattern", CIFRADO, "--dir", pasta, "--clobber"],
        capture_output=True,
    )
    if r.returncode:
        print("Certificado: nenhum guardado ainda.")
        return None
    with open(os.path.join(pasta, CIFRADO), "rb") as f:
        aberto = decifrar(f.read())
    os.remove(os.path.join(pasta, CIFRADO))
    if aberto is None:
        print(f"::error::A senha GUARDIOES_SENHA_DA_CHAVE não abre o certificado guardado na release {RELEASE}. Ela foi trocada?")
        sys.exit(1)
    chave, cert, _ = pkcs12.load_key_and_certificates(aberto, senha())
    if cert.not_valid_after_utc < datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=15):
        print("Certificado: o guardado vence em menos de 15 dias; nasce outro.")
        return None
    cert_id = ativos(apple).get(serial(format(cert.serial_number, "X")))
    if not cert_id:
        print("Certificado: o guardado não está mais ativo na Apple (revogado?); nasce outro.")
        return None
    print(f"Certificado: o guardado (vence em {cert.not_valid_after_utc:%d/%m/%Y}).")
    return chave, cert, cert_id


def novo(apple: Apple, pasta: str):
    chave = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    pedido = (
        x509.CertificateSigningRequestBuilder()
        .subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "GuardioesSOS (GitHub)")]))
        .sign(chave, hashes.SHA256())
    )
    try:
        r = apple.post(
            "/v1/certificates",
            {
                "data": {
                    "type": "certificates",
                    "attributes": {
                        "certificateType": "DISTRIBUTION",
                        "csrContent": pedido.public_bytes(serialization.Encoding.PEM).decode(),
                    },
                }
            },
        )
    except ErroDaApple as e:
        print(
            "::error::A Apple recusou criar o certificado de distribuição. Se a conta chegou ao limite de "
            f"certificados, é preciso revogar um antigo em developer.apple.com. Resposta: {e}"
        )
        sys.exit(1)
    cert = x509.load_der_x509_certificate(base64.b64decode(r["data"]["attributes"]["certificateContent"]))
    with open(os.path.join(pasta, CIFRADO), "wb") as f:
        f.write(cifrar(p12_do_mac(chave, cert)))

    repo = os.environ["REPO"]
    if subprocess.run(["gh", "release", "view", RELEASE, "--repo", repo], capture_output=True).returncode:
        subprocess.run(
            [
                "gh", "release", "create", RELEASE, "--repo", repo, "--prerelease",
                "--title", "GuardiõesSOS no iPhone: certificado de distribuição da Apple (NÃO APAGUE)",
                "--notes",
                "O certificado que assina o GuardiõesSOS para a Apple, cifrado. Só abre com o segredo "
                "GUARDIOES_SENHA_DA_CHAVE. Apagado, a rotina cria outro na próxima vez (e a Apple manda e-mail).",
            ],
            check=True,
            capture_output=True,
        )
    subprocess.run(
        ["gh", "release", "upload", RELEASE, "--repo", repo, "--clobber", os.path.join(pasta, CIFRADO)],
        check=True,
        capture_output=True,
    )
    os.remove(os.path.join(pasta, CIFRADO))
    print(f"Certificado: NASCEU agora (vence em {cert.not_valid_after_utc:%d/%m/%Y}) e foi guardado cifrado na release {RELEASE}.")
    return chave, cert, r["data"]["id"]


def perfil(apple: Apple, cert_id: str):
    pacote = apple.pacote()
    if not pacote:
        print(f"::error::O pacote {PACOTE} não está registrado na Apple (a porta guardioes-apple-envio registra no 'preparar').")
        sys.exit(1)
    nome = urllib.parse.quote(NOME_DO_PERFIL)
    for p in apple.get(f"/v1/profiles?filter[name]={nome}&limit=200").get("data", []):
        if p["attributes"].get("name") == NOME_DO_PERFIL:
            apple.delete(f"/v1/profiles/{p['id']}")
    r = apple.post(
        "/v1/profiles",
        {
            "data": {
                "type": "profiles",
                "attributes": {"name": NOME_DO_PERFIL, "profileType": "IOS_APP_STORE"},
                "relationships": {
                    "bundleId": {"data": {"type": "bundleIds", "id": pacote["id"]}},
                    "certificates": {"data": [{"type": "certificates", "id": cert_id}]},
                },
            }
        },
    )
    a = r["data"]["attributes"]
    return base64.b64decode(a["profileContent"]), a["uuid"]


def main():
    pasta = sys.argv[1]
    os.makedirs(pasta, exist_ok=True)
    apple = Apple()

    chave, cert, cert_id = guardado(apple, pasta) or novo(apple, pasta)
    # O código do time sai do próprio certificado (o "OU"), não de palpite.
    time = cert.subject.get_attributes_for_oid(NameOID.ORGANIZATIONAL_UNIT_NAME)[0].value

    with open(os.path.join(pasta, "certificado.p12"), "wb") as f:
        f.write(p12_do_mac(chave, cert))
    conteudo, uuid = perfil(apple, cert_id)
    with open(os.path.join(pasta, "perfil.mobileprovision"), "wb") as f:
        f.write(conteudo)
    with open(os.path.join(pasta, "ExportOptions.plist"), "wb") as f:
        plistlib.dump(
            {
                "method": "app-store-connect",
                "destination": "upload",
                "signingStyle": "manual",
                "teamID": time,
                "signingCertificate": "Apple Distribution",
                "provisioningProfiles": {PACOTE: NOME_DO_PERFIL},
                "uploadSymbols": True,
                "manageAppVersionAndBuildNumber": False,
            },
            f,
        )
    saida("time", time)
    saida("perfil", NOME_DO_PERFIL)
    saida("uuid", uuid)
    print(f"Perfil de loja pronto ({uuid}), time {time}.")


if __name__ == "__main__":
    main()

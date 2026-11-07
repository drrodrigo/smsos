# Auto-update do APK pelo nosso servidor (pergunta do Rodrigo, 07/11)

## A pergunta

Dá pra ter um APK que **se atualiza sozinho pelo nosso servidor**, sem o testador reinstalar na mão
a cada versão nova?

## Resposta: sim, no ANDROID (fora da loja). Como funciona

- Hospedamos um arquivo pequeno (ex. **`versao.json`**: versionCode, versionName, URL do APK, texto
  do "o que mudou") + o **APK** no nosso servidor (Supabase Storage, de graça — já temos o projeto;
  ou no domínio). Mesma ideia do `versao.json` dos outros sistemas dele.
- O app, ao abrir, **lê o `versao.json`**. Se o versionCode de lá for maior que o instalado, mostra
  **"Nova versão — Atualizar"**, **baixa o APK** e abre o instalador. O testador só toca "Atualizar".
- Precisa da permissão **`REQUEST_INSTALL_PACKAGES`** (e o testador libera "instalar apps
  desconhecidos" uma vez). Download por **HTTPS**.

## 🔴 Limites honestos

- **Só vale pro APK de TESTE (sideload).** O app **da Play Store NÃO pode se auto-atualizar por
  fora** — é regra do Google (app da loja atualiza pela loja). Então o auto-updater entra **só no
  build de teste** (debug), nunca no build de release da loja; a permissão `REQUEST_INSTALL_PACKAGES`
  também fica **fora** do build de loja (senão vira risco na revisão).
- **iPhone: não dá por servidor próprio.** Pra iOS, o auto-update fora da loja é o **TestFlight**
  (que já atualiza sozinho e avisa os testadores). Servidor próprio é só Android.
- Hospedar APK custa quase nada (um arquivo), mas é bom ter uma pastinha/bucket só disso.

## O que é preciso construir

1. Bucket/arquivo no servidor (Supabase Storage: `guardioes-apk/versao.json` + o `.apk`).
2. Tela/aviso de atualização no app (checar versão → baixar → instalar), só no build de teste.
3. Um passo no meu fluxo: a cada APK de teste novo, subir o `.apk` + atualizar o `versao.json`.

Alternativa pronta (sem construir): **Firebase App Distribution** (oficial do Google pra builds de
teste, avisa e atualiza os testadores) — mais simples de ligar, um pouco menos "nosso".

## Status

Pergunta respondida. **Não construído.** Aguardando ele dizer se quer (e se prefere "nosso servidor"
ou Firebase App Distribution). Vale pra Android; iOS segue por TestFlight.

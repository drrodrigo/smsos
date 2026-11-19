# Resposta à rejeição da Apple (Guideline 2.1 — Information Needed)

> **19/11:** vídeo gravado pelo Rodrigo no iPhone dele (app já configurado, Modo teste ligado, passa pelas abas e segura o SOS); texto abaixo é o que foi colado na Apple.
>
> **Corrigido em 18/11:** a versão anterior descrevia o app de **Android** (aba "Guardiões" para
> cadastrar contato, notificação no teste). O build enviado à Apple é o **iPhone build 11**
> (`feat/ios-fabrica`, commit `914e95a`): lá o contato é cadastrado na aba **Contatos** e o teste
> mostra o resultado **na tela** (não em notificação). Se o revisor seguir um passo que não existe,
> reprova de novo — por isso o texto abaixo segue o app do iPhone, botão por botão.

A Apple NÃO reprovou por defeito no app. Ela pediu **informações + um vídeo**. Não precisa mudar código
nem mandar build novo: responde a mensagem e reenvia o mesmo build 11.

## 1) Texto pra colar (em inglês — quem lê é o revisor da Apple)

```
GuardiõesSOS — App Review Notes

1) DEMO VIDEO
A screen recording made on a physical iPhone is attached. It shows the Home screen, the "Contatos" (contacts), "Guardiões", "Protejo" and "Ajustes" tabs, and then holding the SOS button in Test Mode, with the result screen showing who would receive the alert and the message.

2) PURPOSE AND TARGET AUDIENCE
GuardiõesSOS is a free personal-safety panic button for individuals and families in Brazil. When a person is in danger (for example, a robbery) or has a medical emergency, they press and hold the SOS button and the app sends an SMS with their location (a map link) to the emergency contacts they chose.

3) HOW TO TEST (no login and no password)
- First launch: the "Vamos configurar" screen asks for your name, your phone number and one emergency contact (name + phone). Then tap "Continuar". In Test Mode nothing is sent, so any number in the Brazilian format can be used, e.g. 11 91234-5678.
- Home tab ("Início"): turn ON "Modo teste (não envia de verdade)" (Test Mode: nothing is sent).
- Press and hold the big round "SOS" button for about 2 seconds. A screen opens showing who would receive the alert and the exact message, confirming that nothing was sent.
- More contacts: "Contatos" tab > "Adicionar contato" (up to 10).
- Real alert: turn Test Mode OFF and hold the SOS button. An SMS with the location is sent to the contacts. For this, please use a phone number you own as the contact.
- Other tabs: "Guardiões" creates an invitation code to share with a guardian; "Protejo" is where a guardian enters that code; "Ajustes" has your name, phone and alert type (Segurança = danger, Saúde = health).
- Location permission is used only to include the map link in the alert.

4) EXTERNAL SERVICES
- Supabase: our backend (database and server functions that register the user and route the alert).
- SMS Market: SMS gateway in Brazil that delivers the alert SMS.
- Firebase Cloud Messaging: used by our server to notify guardians who use our Android app.
No AI services. No payments and no in-app purchases.

5) REGIONAL DIFFERENCES
The app is for Brazil, in Brazilian Portuguese, and sends SMS to Brazilian numbers. All users get the same features.

6) REGULATED INDUSTRY / THIRD-PARTY CONTENT
Not a regulated industry. It is a consumer safety app for the general public, not an official emergency service, and it does not replace calling 190 (police) or 192 (ambulance). No protected third-party content.

ACCOUNT AND DATA
There is no password-based account, only name and phone number. Users can request deletion of their data at guardioessos.com.br.
```

### O que o texto diz (tradução, pra você saber o que está mandando)

1. **Vídeo:** vai anexado um vídeo gravado num iPhone de verdade mostrando o app funcionando.
2. **Para que serve:** botão de pânico grátis pra pessoas e famílias no Brasil; segurando o SOS, o app
   manda SMS com a localização pros contatos escolhidos.
3. **Como testar (sem login/senha):** na 1ª abertura põe nome, telefone e um contato → "Continuar";
   na aba Início liga o "Modo teste"; segura o botão SOS ~2 segundos → abre a tela mostrando quem
   receberia e a mensagem, sem enviar nada. Mais contatos na aba "Contatos". Alerta real: desliga o
   Modo teste (usar o próprio número). Explica as abas Guardiões, Protejo e Ajustes.
4. **Serviços usados:** Supabase (nosso servidor), SMS Market (envio do SMS), Firebase (aviso pros
   guardiões no Android). Sem IA, sem pagamento.
5. **Região:** só Brasil, em português.
6. **Área regulada:** não é; não é serviço oficial de emergência e não substitui o 190/192.
   **Conta:** não tem senha; os dados podem ser apagados pelo site.

## 2) Vídeo (no iPhone do Rodrigo, que já tem o app instalado e configurado — ~1 minuto)

Gravar a tela do iPhone (Central de Controle → botão de gravar) mostrando, nesta ordem:
1. Aba **Início** (checklist "Tudo pronto").
2. Aba **Contatos** → adicionar um contato (nome + telefone) → **Adicionar**.
3. Volta no **Início** → ligar **"Modo teste"** (ANTES de segurar o SOS, senão envia SMS de verdade).
4. Segurar o botão **SOS** → aparece a tela com "Para:" e "Mensagem:" → **Fechar**.
5. Passar pelas abas **Guardiões**, **Protejo** e **Ajustes**.

## 3) Onde colar e reenviar (App Store Connect)

1. Página do envio rejeitado → **NÃO** clicar em "Remover" (tira a versão do envio).
2. Em **Mensagens**, embaixo da mensagem da Apple → **Responder** → colar o texto da parte 1 → anexar o
   vídeo → **Enviar**.
3. Topo da página → **Reenviar para Revisão do app**.
4. Se o botão continuar cinza: na linha "iOS App 1.0" → **Editar** → seção **Informações da revisão do
   app** → colar o mesmo texto em **Notas** e o vídeo em **Anexo** → **Salvar** → voltar e **Reenviar**.

## Ponto de atenção

Apple pede **exclusão de conta DENTRO do app** quando há criação de conta (5.1.1(v)). O GuardiõesSOS não
tem login/senha, só nome+telefone — por isso a nota explica e aponta o site. Se a Apple insistir, adiciona-se
um botão "Apagar meus dados" no app do iPhone e manda um build novo.

# Resposta à rejeição da Apple (Guideline 2.1 — Information Needed) — pronto pra amanhã

A Apple NÃO reprovou o app por qualidade. É a rejeição mais comum da 1ª submissão: ela só quer
**informações + um vídeo**. Resolve sem mexer no código — preenche o campo de Notas e reenvia.

## 1) Texto pra colar no campo "Notas" (App Review Information) — em inglês, é o revisor que lê

```
GuardiõesSOS — App Review Notes

2) PURPOSE AND TARGET AUDIENCE
GuardiõesSOS is a personal-safety "silent panic button" for individuals and families (not for businesses or fleets). If a person is in danger (e.g., assault) or has a medical emergency, they discreetly trigger an alert that sends an SMS with their GPS location to their chosen emergency contacts ("guardians"), plus a push notification to guardians who also have the app. It helps people get help quickly and discreetly.

3) HOW TO SET UP AND USE (NO login/password required)
- The app has no password-based account. On first launch the user enters only their name and phone number (used to identify them in the alert).
- Add a guardian: open the "Guardiões" tab and add a name + phone number.
- SAFE TEST (nothing is sent): on the Home screen turn ON "Modo teste" (Test mode), then press and hold the "SEGURANÇA" or "SAÚDE" button for a few seconds. The app vibrates and shows a notification with exactly what WOULD be sent — but no SMS is sent.
- REAL ALERT: with Test mode OFF, hold the button; an SMS with the location is sent to the contacts.
- To verify real SMS delivery, add your own phone number as a contact and trigger a real alert.
No demo login credentials are needed because the app has no login.

4) EXTERNAL SERVICES USED FOR CORE FUNCTIONALITY
- Supabase — database, storage and serverless (Edge) functions that store the registration and route the alerts.
- SMS Market — SMS gateway that delivers the alert SMS to the emergency contacts.
- Firebase Cloud Messaging — push notifications to guardians who have the app installed.
- No AI services. No payment processing and no in-app purchases in version 1.0 (the app is free).

5) REGIONAL DIFFERENCES
The app currently targets Brazil and is in Brazilian Portuguese; SMS delivery is for Brazilian phone numbers. It behaves consistently for users in this region; there are no region-locked features.

6) REGULATED INDUSTRY / THIRD-PARTY MATERIAL
GuardiõesSOS is a consumer personal-safety app for the general public. It is not part of a regulated industry and contains no protected third-party material. It is not restricted to specific businesses, organizations or employees.

ACCOUNT / DATA DELETION
The app does not create password-based accounts. Data stored on the device (alert history) can be cleared inside the app. The registration data (name, phone, contacts) can be deleted on request through our website (guardioessos.com.br).
```

## 2) Vídeo (grava no iPhone, ~1–2 min)

Central de Controle → gravar tela. Mostrar nesta ordem:
1. Abrir o app (começa pela abertura).
2. Tela inicial (nome/telefone se pedir).
3. Aba Guardiões → adicionar contato (nome + número).
4. Início → ligar Modo teste.
5. Segurar o botão SOS → mostrar a notificação "o alerta funcionou (nenhum SMS enviado)".
6. (Se puder) Modo teste OFF → acionar pro próprio número → mostrar o SMS chegando.

## 3) Onde colar e reenviar

App Store Connect → GuardiõesSOS → versão 1.0 → seção "Informações da Revisão do App":
- Campo **Notas**: colar o texto da parte 1.
- Campo **Anexo**: enviar o vídeo da parte 2.
- Clicar em **Adicionar para Revisão** / **Enviar para Revisão**.

## Ponto a confirmar

Apple pede **exclusão de conta DENTRO do app** quando há criação de conta. O GuardiõesSOS não tem
login/senha, só nome+telefone — por isso a nota diz que não é conta com senha e aponta a exclusão
pelo site. Se a Apple insistir, a gente adiciona um botão "Apagar meus dados" dentro do app.

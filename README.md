# Alerta Silencioso (nome provisório)

Botão de pânico silencioso para Android. A pessoa segura o botão de volume por alguns segundos
(ou aperta várias vezes) e o app manda, sem som e sem acender a tela, um SMS com a localização para:

- os contatos de **"Sempre avisar"** (ex.: o filho), em qualquer lugar;
- as pessoas do **lugar** onde ela está (ex.: os vizinhos de casa ou do escritório), escolhidas pelo GPS.

Se o GPS não responder, todos os contatos são avisados. Depois do alerta, o app pode mandar a localização
atualizada algumas vezes e, quando a pessoa estiver bem, um SMS de "está tudo bem".

## Como o app está organizado

O motor do pânico foi separado das telas, para poder ser levado para outros apps (BipChat, QR Care...).

| Pasta | O que faz |
|---|---|
| `core/` | Regras puras, sem Android: reconhecer o gesto, escolher os contatos, montar o SMS. Tem testes. |
| `trigger/` | Percebe os botões de volume: serviço de acessibilidade (tela ligada) e sessão de mídia (tela apagada). |
| `alert/` | Executa o alerta: GPS, envio de SMS pelo chip, vibração, atualizações de localização. |
| `data/` | Salva contatos, lugares, ajustes e histórico no celular. |
| `ui/` | Telas (Jetpack Compose). |

## Como gerar o APK

Precisa do JDK 17+ e do Android SDK (plataforma 37).

```
./gradlew testDebugUnitTest   # testes
./gradlew assembleDebug       # APK em app/build/outputs/apk/debug/
```

As versões de teste são assinadas com `keystore/teste.keystore`, que fica no projeto de propósito: assim
cada APK novo instala por cima do anterior sem apagar os dados. Para publicar na Play Store, crie uma
chave própria e guarde fora do projeto.

## Limitações conhecidas

- **Tela apagada**: o Android não entrega os botões ao serviço de acessibilidade com a tela apagada. O app
  se apresenta como um "player remoto" para receber os apertos; funciona em muitos aparelhos, mas alguns
  fabricantes bloqueiam. O modo teste mostra se funciona no aparelho.
- **Limite de SMS**: o Android pede confirmação na tela acima de 30 SMS em 30 minutos. Por isso as
  atualizações de localização vão, por padrão, só para "Sempre avisar".
- **Mensagens enviadas** ficam no app de Mensagens do celular (é assim que o Android funciona para SMS pelo chip).
- **Play Store**: SMS, localização em segundo plano e acessibilidade exigem justificativa no Google Play.
  O envio por um serviço de SMS pela internet (planejado) reduz essas exigências.

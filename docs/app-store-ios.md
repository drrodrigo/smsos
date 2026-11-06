# GuardiõesSOS — Submissão na App Store (iOS)

Tudo pronto para você preencher na **App Store Connect** → app **GuardiõesSOS** (br.com.guardioessos)
e enviar para revisão. Copie e cole os textos abaixo.

---

## 1. Textos da loja (App Store Connect → "Informações da versão")

**Nome** (máx. 30):
```
GuardiõesSOS
```

**Subtítulo** (máx. 30):
```
Botão de pânico e SOS
```

**Texto promocional** (máx. 170, dá pra mudar sem nova revisão):
```
Seu botão de emergência. Segure e avise na hora quem você escolheu, com a sua localização no mapa. Sua rede de guardiões a um toque.
```

**Descrição** (máx. 4000):
```
O GuardiõesSOS é o seu botão de emergência. Em uma situação de perigo ou de saúde, você segura o botão SOS e avisa na hora as pessoas que você escolheu — com a sua localização no mapa.

COMO FUNCIONA
• Cadastre seus contatos de confiança (família, vizinhos, amigos).
• Em uma emergência, segure o botão SOS por alguns segundos.
• Eles recebem na hora uma mensagem com o seu nome e o link do mapa mostrando onde você está.

REDE DE GUARDIÕES
Convide pessoas para serem seus guardiões — elas recebem os seus alertas. E você pode ser guardião de quem confia em você: aceite um convite pelo código e proteja quem você ama.

DOIS TIPOS DE ALERTA
• Segurança: quando há risco (um assalto, uma perseguição, um perigo).
• Saúde: quando você passa mal (um mal súbito, uma queda).

FEITO PARA A VIDA REAL
Rápido, direto e no seu controle. Ideal para quem anda sozinho(a) e para a família cuidar de perto, mesmo à distância.

SUA PRIVACIDADE É SUA
Seus contatos e a sua localização são usados apenas para enviar o seu alerta, para as pessoas que você escolheu. Nada é compartilhado com terceiros.
```

**Palavras-chave** (máx. 100, separadas por vírgula):
```
sos,emergencia,panico,seguranca,socorro,guardiao,protecao,localizacao,alerta,familia,botao,ajuda
```

**URL de suporte:**
```
https://www.guardioessos.com.br
```

**URL de marketing** (opcional):
```
https://www.guardioessos.com.br
```

**URL da política de privacidade** (App Store Connect → "Informações do app"):
```
https://www.guardioessos.com.br/privacidade
```

---

## 2. Categoria
- **Primária:** Estilo de vida (Lifestyle)
- **Secundária:** Utilitários (Utilities)

*(Safety não existe como categoria na Apple; Lifestyle é o encaixe comum para apps de segurança pessoal. Se preferir Saúde e Fitness pela parte de Saúde, também vale.)*

## 3. Classificação etária
Responda o questionário como **4+** (sem conteúdo impróprio). O app trata de emergências, mas
não tem violência, apostas, etc. Se o questionário perguntar sobre "contato irrestrito com a web",
responda **não**.

## 4. Privacidade do app ("nutrition label" — App Store Connect → "Privacidade do app")
Declare que o app **coleta** (ligado ao usuário, para funcionamento do app — NÃO para rastreamento):
- **Localização aproximada/precisa** → finalidade: **Funcionalidade do app** (mandar o link do mapa no alerta).
- **Contatos** → os números que o próprio usuário cadastra como quem recebe o SOS → **Funcionalidade do app**.
- **Informações de contato: número de telefone / nome** → identidade na rede de guardiões → **Funcionalidade do app**.
- **Rastreamento:** **NÃO** (não usamos para publicidade nem compartilhamos com terceiros).

## 5. Criptografia (quando perguntar no envio)
"Seu app usa criptografia?" → responda conforme a configuração: o app já declara
`ITSAppUsesNonExemptEncryption = NO`, então pode responder **Não** (ou "só HTTPS padrão, isento").

## 6. Notas para a revisão (campo "Notas" / "App Review Information")
```
App de segurança pessoal (botão de pânico / SOS).

Como testar:
1. Abra o app e faça o cadastro rápido (nome + telefone). Não há login.
2. Na aba "Contatos", adicione um contato com um número de telefone de teste.
3. Na tela Início, segure o botão SOS. Por padrão o app está em "Modo teste":
   ele mostra a mensagem que seria enviada (com a localização), SEM enviar de verdade.
4. Para enviar de verdade (um SMS com o nome e o link do mapa), desligue o "Modo teste"
   em Ajustes e segure o botão.

A localização é usada só para montar o link do mapa no alerta. O envio é feito pela nossa
estrutura, pela internet (o app não usa o chip para SMS).
```

## 7. Captura de telas (screenshots)
Geradas automaticamente pela fábrica (iPhone 6.9"). Baixe o arquivo **prints-appstore** em:
Actions → "prints do iPhone (App Store)" → artifact. Suba em App Store Connect → a versão → Capturas (6.9").

## 8. Build
Selecione o build mais novo do GuardiõesSOS (o que está no TestFlight) na seção **Build** da versão.

---

## Passo a passo para ENVIAR (resumo)
1. App Store Connect → **Apps** → **GuardiõesSOS** → aba **Distribuição** → versão **1.0** (iOS).
2. Cole os textos (seções 1), escolha a **Categoria** (2), suba as **capturas** (7), escolha o **Build** (8).
3. Preencha **Privacidade do app** (4) e a **Classificação etária** (3) se ainda não estiverem.
4. **Informações de revisão** (6): cole as notas. Marque "Entrar em contato" com seu e-mail/telefone.
5. Clique em **"Adicionar para revisão"** → **"Enviar para revisão"**.
6. Pronto. A Apple costuma responder em ~1 dia.
```

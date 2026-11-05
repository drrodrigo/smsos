# Play Store — GuardiõesSOS (textos e formulários prontos)

Tudo em pt-BR, pronto pra colar no Google Play Console. Vai por partes.

---

## 1. Identidade do app

- **Nome do app** (máx. 30 caracteres): `GuardiõesSOS: botão de pânico`
  (alternativas: `GuardiõesSOS — SOS pessoal`, `GuardiõesSOS`)
- **Pacote (applicationId):** `br.com.guardioessos` (já definido, PERMANENTE depois de publicar)
- **Categoria:** Estilo de vida (alternativa: Ferramentas)
- **E-mail de contato:** contato@qrunico.com.br
- **Site:** https://www.guardioessos.com.br
- **Política de privacidade:** https://www.guardioessos.com.br/privacidade

---

## 2. Descrição curta (máx. 80 caracteres)

```
Botão de pânico e SOS: um toque avisa seus guardiões com a sua localização.
```

## 3. Descrição completa (máx. 4000 caracteres)

```
GuardiõesSOS — botão de pânico e alerta de emergência

Um botão de pânico sempre com você. Em uma emergência — um assalto, uma perseguição, um mal súbito — nem sempre dá pra pegar o telefone e ligar. Com o GuardiõesSOS, um toque envia um SOS com a sua localização para seus guardiões: as pessoas de confiança que você escolhe.

É segurança pessoal de verdade — pra você e pra quem você ama.

COMO FUNCIONA
• Toque no botão de SOS (Segurança ou Saúde) — rápido e direto.
• Seus guardiões recebem na hora um alerta de emergência com a sua localização no mapa.
• No celular do guardião, o aviso TOCA como uma chamada: sirene alta, vibração e uma tela de emergência — mesmo no silencioso ou com o telefone no bolso.

DOIS TIPOS DE SOS
• Segurança — perigo, assalto, perseguição, situação de risco.
• Saúde — se você passar mal, cair ou precisar de socorro.

POR QUE USAR O GUARDIÕESSOS
• Botão de emergência que funciona com o app fechado e a tela apagada.
• O pedido de socorro sai pela nossa estrutura — não depende do seu saldo de SMS.
• Monte sua rede de proteção: escolha seus guardiões e seja guardião de quem você ama.
• Avise que está tudo bem com um toque.

PRA QUEM É
• Segurança pessoal no dia a dia: ao sair à noite, ao voltar pra casa, ao andar sozinho.
• Famílias que se cuidam: o pai, a mãe, o filho, a filha na faculdade.
• Qualquer pessoa que queira um botão de socorro sempre à mão.

PRIVACIDADE
• Seus dados são usados só pra criar sua conta e enviar o alerta. Sem spam.
• O histórico de alertas fica no seu aparelho e pode ser apagado quando você quiser.
• Política de privacidade: https://www.guardioessos.com.br/privacidade

Baixe o GuardiõesSOS e tenha um botão de pânico sempre com você. Proteja quem você ama.
```

---

## 4. Imagens (você gera as artes no GPT; os prints você tira do app)

- **Ícone do app:** 512x512 PNG (já temos o escudo).
- **Imagem de destaque (feature graphic):** 1024x500 PNG. (Prompt pro GPT depois.)
- **Prints do celular:** no mínimo 2, recomendo 4–6. Telas boas pra capturar:
  1. Tela Início com "Proteção ATIVA" e os botões Segurança/Saúde.
  2. A tela do alerta recebido (vermelha, "acionou o SOS").
  3. A aba Guardiões (quem recebe).
  4. A Configuração (tudo ✓).
  Tirar com o modo teste DESLIGADO e sem dados pessoais reais na tela.

---

## 5. Data Safety (Segurança dos dados) — o que declarar

O app COLETA e usa (para funcionar, não para anúncios):
- **Localização aproximada e precisa** — enviada aos guardiões no alerta. Compartilhada com os guardiões. Não vendida.
- **Telefone / número** — identifica a conta e os guardiões. Não vendido.
- **Nome** — aparece na mensagem de alerta.
- **ID do dispositivo / token de push** — para entregar o alerta.

Para cada item: "Coletado" = sim, "Compartilhado" = só com os guardiões que a própria pessoa escolhe, "Finalidade" = Funcionalidade do app / Segurança. Dados criptografados em trânsito: sim. Dá pra pedir exclusão: sim (contato@qrunico.com.br).

---

## 6. Declarações sensíveis (onde o Google pega no pé) — LER COM ATENÇÃO

### 6.1 Localização em segundo plano
A Play vai pedir uma justificativa (e às vezes um vídeo curto). Justificativa:
```
O GuardiõesSOS é um app de segurança pessoal / botão de pânico. A localização em segundo
plano é necessária para enviar a localização do usuário aos seus guardiões no momento de uma
emergência, mesmo com o app fechado ou a tela apagada — que é exatamente quando a pessoa não
consegue abrir o telefone. Sem isso, o socorro não saberia onde a pessoa está.
```

### 6.2 Serviço de Acessibilidade (ESTE é o maior risco)
O app usa AccessibilityService só para perceber quando o usuário SEGURA o botão de volume
(o gesto do pânico), com o app fechado. Não lê a tela, não coleta conteúdo. A Play tem política
dura sobre acessibilidade — pode pedir justificativa ou recusar. Preparar:
- Declaração de uso de acessibilidade no Console, texto sugerido:
```
O GuardiõesSOS usa a API de Acessibilidade exclusivamente para detectar um gesto físico
(segurar o botão de volume) que dispara um alerta de emergência silencioso, com o app fechado
ou a tela apagada. O serviço NÃO lê o conteúdo da tela, NÃO captura o que o usuário digita e
NÃO coleta nenhum dado de outros apps. É um recurso de segurança pessoal que exige acionamento
discreto; não há outra API no Android que permita capturar esse gesto de forma confiável em
segundo plano. O usuário é informado disso ao ativar o recurso.
```
- **Plano B se recusarem:** o app funciona SÓ com o botão na tela (sem acessibilidade). Dá pra
  publicar uma versão sem o AccessibilityService e manter o gesto de volume como "ative por sua
  conta" (sideload/beta), separando o risco.

### 6.3 Notificações em tela cheia (USE_FULL_SCREEN_INTENT)
A Play pede declaração no Android 14+. Justificativa: é um app de alerta/emergência; a tela do
alerta precisa aparecer por cima de tudo, como uma chamada, para o guardião não perder o socorro.

---

## 7. Classificação de conteúdo (questionário)
App sem violência, sexo, drogas, jogos de azar. Respostas "não" para quase tudo. Deve sair
"Livre" / "L". Mencionar que lida com localização/segurança se perguntarem.

---

## 8. Caminho recomendado: TESTE FECHADO primeiro
Não publicar direto pra produção. Subir em **Teste fechado** (closed testing), adicionar os
e-mails dos testadores (Google exige um nº mínimo de testadores por um período para apps novos
de conta nova). Assim a galera instala PELA LOJA (sem Play Protect, sem configuração restrita) e
a gente valida antes de abrir pro público.

Pré-requisito: **conta de desenvolvedor Google Play** (taxa única de US$ 25).

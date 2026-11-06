# Testar o gesto (botão de volume, tela apagada) — SEM Acessibilidade

Este é o **APK de teste** do segundo round. Ele instala **do lado** do seu app da Play (tem nome
"**GuardiõesSOS teste**" e é um app separado) — **não mexe** no app de verdade nem nos seus dados.

> O que este APK testa: o **pega-e-segurar com a tela apagada, sem Acessibilidade**.
> O que ele NÃO faz: push/sirene de guardião (isso só no app da Play). Aqui o teste é em
> **Modo teste**: o gesto vibra e mostra o que seria enviado, sem mandar SMS.

## 1. Instalar

👉 Toque no arquivo `.apk` que eu te mandei e instale.

1. O Android vai perguntar se pode **instalar apps desta fonte** → **Permitir / Configurações → ligar**.
2. Toque em **Instalar**.
3. Se aparecer um aviso do **Play Protect** ("app desconhecido"), toque em **Instalar mesmo assim**
   (é porque o APK não veio da loja — é o nosso de teste).
4. Vai aparecer um app novo na tela: **GuardiõesSOS teste**. É esse que você abre (o seu app normal
   continua lá, intacto).

## 2. Ligar o gesto

1. Abra o **GuardiõesSOS teste** e passe pelo cadastro rápido (nome + telefone).
2. Deixe a permissão de **localização** e a de **notificação** quando ele pedir (a notificação é a
   plaquinha fixa "protegendo você" — é ela que deixa o gesto funcionar com o app fechado).
3. Vá em **Ajustes** → bloco **"Botão com a tela apagada (experimental)"** → confirme que está
   **ligado** (deve aparecer *"Motor do gesto: ligado ✓"*).
4. Na tela **Início**, ligue o **Modo teste** (pra não enviar nada de verdade).

## 3. O teste de verdade

👉 **Bloqueie a tela** (aperte o power uma vez, tela apagada) e **segure o botão de volume por 5
segundos**.

- Deu certo: o celular **vibra** ao completar os 5s. 🎉
- Confira: desbloqueie, abra o app, vá em **Ajustes** → o número *"Apertos percebidos com a tela
  apagada"* tem que ter **subido**.

## 4. Me conta

- **Vibrou?** (sim/não)
- O **contador subiu**? (de quanto pra quanto)
- Qual o **modelo do seu celular** (ex.: Samsung A54, Motorola...).

Se vibrou e o contador subiu → **a alma do produto funciona sem Acessibilidade** e a gente lança
como atualização. Se ficou em 0 → seu aparelho bloqueia o truque (alguns bloqueiam), e aí a gente
decide o próximo caminho. De qualquer jeito, **não gastamos nada na Play** pra descobrir.

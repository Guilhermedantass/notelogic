# Roadmap do NoteLogic

O que vem pela frente, as decisões de produto já tomadas e as ideias que ainda estão sendo pensadas.
O que o app **já faz** está no [README](../README.md).

## 1. Próximas features

Em ordem de prioridade, por bloco. Cada bloco é quebrado em partes pequenas, cada uma com banco, API e tela.

**Feito:** tarefas recorrentes (out/2026).

### A. Agenda — **em modelagem** (ver [seção 4](#agenda-estilo-google-agenda))

| # | Feature | O que é |
|---|---|---|
| A1 | Evento | Prova, aula, treino, compromisso: data, horário ou dia inteiro |
| A2 | Eventos que se repetem | Reaproveita a regra das tarefas recorrentes; exceções para "só esta" |
| A3 | Tela Agenda | Mês, semana e dia, com eventos e tarefas com prazo |
| A4 | Hoje vira o resumo do dia | Agenda de hoje, próximas datas importantes (30 dias) e as tarefas |
| A5 | *Depois* | Lembretes (depende do P2), pausar rotina (férias), prova cria tarefa de revisão, pomodoros na semana, `.ics` |

### D. Deploy e CI (ver [seção 2](#2-deploy))

| # | Feature | O que é |
|---|---|---|
| D1 | Imagem Docker única | Dockerfile em estágios; o Spring entrega o Angular junto com a `/api` |
| D2 | Rota de fallback | F5 em `/painel` devolve o `index.html` |
| D3 | Configuração de produção | JWT e banco por variáveis de ambiente |
| D4 | GitHub Actions | Testes a cada push e publicação quando a `main` muda |
| D5 | Hospedagem | PaaS (Render, Railway, Fly) + Postgres gerenciado (Neon, Supabase) |
| D6 | *Depois* | VPS com Docker Compose + Caddy |

### K. Workspace / Kanban (ver [seção 4](#workspace-com-quadros-kanban))

| # | Feature | O que é |
|---|---|---|
| K1 | Quadro | O workspace é a lista de quadros da pessoa |
| K2 | Colunas | Nome livre, ordem e papel (normal, em andamento, conclusão) |
| K3 | Cartão | Título, descrição, prazo e matéria opcionais; fora da lista de tarefas |
| K4 | Arrastar | Cartões entre colunas e na ordem, e também reordenar tarefas |
| K5 | Modelos de quadro | Fixos no código, para começar |
| K6 | Ligar cartão ↔ tarefa | "Mandar para Hoje", sincronizados |
| K7 | *Em análise* | Pomodoro move o cartão para "em andamento" |

### Q. Banco de questões (ver [seção 4](#banco-de-questões))

| # | Feature | O que é |
|---|---|---|
| Q1 | Questão | Enunciado, alternativas, gabarito, explicação; matéria e assunto |
| Q2 | Tipos | Múltipla escolha, V/F, discursiva com autoavaliação (definir quais primeiro) |
| Q3 | Praticar | Responder uma por uma e ver se acertou |
| Q4 | Errou → revisão | A questão entra na revisão espaçada (SM-2), a confirmar |
| Q5 | Importar texto Aiken | Colar, pré-visualizar com erros por linha, confirmar |
| Q6 | Importar CSV | Mesmo fluxo, com modelo para baixar |
| Q7 | *Em aberto* | Simulado com tempo |
| Q8 | *Depois* | IA estrutura uma prova antiga colada (custo por uso) |

### F. Flashcards

| # | Feature | O que é |
|---|---|---|
| F1 | Importar CSV | Frente e verso por linha, mesma pré-visualização do Q5 |
| F2 | Importar `.apkg` do Anki | Zip com SQLite dentro; mais trabalhoso |

### M. Metas

| # | Feature | O que é |
|---|---|---|
| M1 | Meta semanal | "10 h de Grafos", barra de progresso no painel a partir das sessões de foco |

### P. PWA

| # | Feature | O que é |
|---|---|---|
| P1 | Instalar no celular | Manifest e service worker |
| P2 | Notificações | Pomodoro avisa com a aba fechada; base para os lembretes da agenda |

### T. Tarefas e conta

| # | Feature | O que é |
|---|---|---|
| T1 | Subtarefas | Checklist dentro da tarefa (serve também para os cartões) |
| T2 | Busca | Busca única em tarefas, cartões, eventos, flashcards e questões |
| C1 | Esqueci minha senha | Serviço de e-mail (Resend, Brevo…) e token que expira |

### Dependências

- **D antes de C1:** o link de redefinir senha precisa de um endereço público.
- **P2 antes de A5:** lembretes usam as notificações.
- **Q5 e F1** usam a mesma tela de importação (pré-visualizar → confirmar).
- **K4 e T1:** tarefas e cartões compartilham a mesma ideia de ordem e de checklist.
- **T2 por último:** a busca só faz sentido quando tudo o que ela procura existir.

## 2. Deploy

### A ideia

Juntar API e front numa **imagem Docker só**: o Angular é compilado e o Spring entrega os arquivos dele junto com a
`/api`. Fica um endereço só e continua sem CORS, como no `ng serve` em desenvolvimento.

### Onde hospedar

| | **PaaS** (Render, Railway, Fly.io) + Postgres gerenciado (Neon, Supabase) | **VPS** (Hetzner, Oracle Free Tier…) com Docker Compose + Caddy |
|---|---|---|
| Esforço | Baixo: conecta o GitHub e define as variáveis | Médio: Linux, firewall e backups por sua conta |
| Custo | Tem plano grátis, mas a API "dorme" sem uso e leva ~30 s para acordar | Alguns dólares/euros por mês, sempre ligado |
| O que se aprende | Deploy e variáveis de ambiente | Tudo isso, mais servidor, HTTPS (o Caddy faz sozinho) e domínio |

**Recomendação:** começar pelo PaaS, que é o caminho mais rápido para ter um link público. A VPS fica como
próximo passo de aprendizado.

### O que muda no código (vale para os dois)

1. **Dockerfile em estágios:** compila o Angular, compila o Java e gera uma imagem enxuta só com o necessário.
2. **Rota de fallback no Spring:** um F5 em `/painel` precisa devolver o `index.html`, para o roteamento do Angular
   assumir.
3. **Variáveis de ambiente:** `NOTELOGIC_JWT_SEGREDO` (32+ caracteres aleatórios) e as do banco.
4. **GitHub Actions:** roda os testes da API e do front a cada push e publica quando a `main` muda.

## 3. Decisões de produto já tomadas

### Agenda — A1: Evento (30/10/2026)

- **Tarefa ou evento:** o que se *conclui* é tarefa (entregar lista dia 12) e aparece na agenda pelo prazo. O que só
  *acontece* num horário é evento (prova, aula, treino).
- **Categorias da pessoa:** cada uma tem nome e cor (Treino, Aula, Trabalho…), e o app já cria algumas prontas.
- **Vários dias:** o evento tem data de início e de fim (semana de provas, viagem).
- **Hora de fim opcional:** "prova às 14h" pode ficar sem fim; na semana o bloco aparece com duração padrão.
- **Cor do evento:** a da categoria; sem categoria, a da matéria; sem nenhuma, o violeta do app.
- **Categorias prontas:** Prova, Aula, Treino e Compromisso, criadas no cadastro (e por migração para as contas que
  já existem). A pessoa pode editar e apagar; apagar deixa os eventos sem categoria.
- **Campos:** título, descrição, local, categoria, matéria, data de início e de fim, dia inteiro ou hora de início
  (obrigatória) e de fim (opcional). Data e hora sem fuso: "treinar 9h" é 9h no relógio.
- **Regras de validade também no banco** (`check`): fim nunca antes do início.
- **Categorias** são criadas e editadas na própria tela da Agenda, sem item novo no menu.
- **Data importante é marcada na categoria** (`destaque`): Prova já vem marcada; Treino, Aula e Compromisso não.

### Agenda — A2: Eventos que se repetem (30/10/2026)

- **A regra de repetição vira uma peça só** (`RegraDeRepeticao`, um `@Embeddable` com o cálculo das datas), usada
  pelas tarefas recorrentes e pelos eventos. Os tipos e o fim (nunca, até uma data, depois de N vezes) são os mesmos.
- **As ocorrências de eventos são só calculadas** para exibir, nunca salvas: não há nada para concluir.
- **Só esta:** dá para cancelar ou mudar data, horário e local. Fica gravado como exceção (`evento_excecao`).
- **Esta e as próximas:** encerra a série na véspera e cria um evento novo a partir dali (como o Google Agenda).
- **Só eventos de 1 dia se repetem.**
- **Pausar a rotina num período** (férias, recesso) fica para o A5.

### Agenda — A3: Tela Agenda (30/10/2026)

- **Abre na semana**, que é onde a rotina aparece com horários; mês e dia a um clique.
- **FullCalendar** (pacotes MIT: daygrid, timegrid, interaction), com o visual do app por CSS.
- **Arrastar já no A3:** muda dia e horário; num evento que se repete, pergunta "só esta" ou "esta e as próximas".
  Arrastar uma tarefa muda o prazo dela.
- **Tarefas com prazo** aparecem todas na faixa de dia inteiro, com visual próprio e check para concluir ali.

### Agenda — A4: Hoje como resumo do dia (30/10/2026)

- **Prova não vai para a lista de tarefas:** é um compromisso, fica na agenda. A tela Hoje junta os dois.
- **Três blocos:** agenda de hoje (eventos em ordem de horário), próximas datas importantes e as tarefas
  (atrasadas e de hoje, como já é).
- **Próximas datas importantes:** os próximos **30 dias**, com contagem ("em 4 dias"). Entram os eventos de
  categorias com destaque e as tarefas de prioridade **alta** com prazo no período.

### Tarefas recorrentes (29/10/2026)

- **Modelo:** a *regra* de repetição é guardada, e a tarefa de cada dia (a *ocorrência*) é criada sob demanda,
  quando a lista é pedida. Cada ocorrência é uma tarefa comum, então concluir, editar e o histórico do painel
  continuam funcionando sem mudanças.
- **Quando não é feita no dia:** cada tarefa escolhe. O padrão é **expirar** (some da lista e conta como não feita);
  a outra opção é **acumular** como atrasada.
- **Editar e apagar uma ocorrência** perguntam "só esta" ou "esta e as próximas".
- **Constância** por tarefa recorrente: "fez 18 de 20".
- **⚠️ Provisória — mostrar só a próxima ocorrência** de cada regra. Pode mudar (por exemplo, para mostrar a
  semana inteira). Para mudar, é só alterar a constante `OCORRENCIAS_PENDENTES` em
  `api/src/main/java/dev/guilherme/notelogic/recorrencia/GeradorDeOcorrencias.java`.

### Visual (29/10/2026)

- Barra lateral neutra; o violeta (`#543EFE`) aparece só nos destaques: item ativo, botões, checks.
- Tema claro como padrão, escuro com fundo roxo bem escuro, e a opção de seguir o sistema.
- Marca: quadrado em gradiente violeta com um ✓ (no lugar do "NL" da v1).

## 4. Ideias em aberto

### Banco de questões

**A ideia:** questões de prova com gabarito, organizadas por matéria e assunto, para praticar e fazer simulados. A
ligação com o resto do app: **a questão que você erra entra na revisão espaçada**, como um flashcard.

**Tipos de questão possíveis:** múltipla escolha, verdadeiro ou falso, e discursiva (você escreve, compara com a
resposta esperada e se autoavalia).

**Importação fácil — três caminhos, do mais simples ao mais poderoso:**

1. **Colar texto no formato Aiken** (o padrão simples do Moodle), estendido com `MATERIA:` e `EXPLICACAO:`:
   ```
   Qual camada do modelo TCP/IP contém o TCP?
   A) Rede
   B) Transporte
   C) Aplicação
   D) Enlace
   ANSWER: B
   ```
2. **Planilha CSV** com um modelo para baixar: uma linha por questão (enunciado, alternativas, gabarito, matéria,
   assunto).
3. **Para depois:** colar o texto de uma prova antiga (PDF) e uma IA estruturar as questões. Funciona bem, mas tem
   custo por uso de API.

Em todos os casos, a importação acontece em **duas etapas**: primeiro uma pré-visualização com os erros apontados
por linha ("questão 7: falta o ANSWER"), depois a confirmação, que salva tudo numa transação só.

**Perguntas em aberto:**

- [ ] De onde vêm as questões? (escritas por você, provas antigas, ENEM, concursos, listas de professor)
- [ ] Quais tipos de questão entram primeiro?
- [ ] Simulado com tempo ("30 questões em 60 minutos") ou só praticar uma por uma?
- [ ] Confirmar: questão errada entra na revisão espaçada?

### Agenda (estilo Google Agenda)

**A ideia:** um calendário (mês, semana e dia) com duas coisas juntas:

1. **Prazos:** "entregar a lista dia 12", "prova dia 20".
2. **Rotina:** compromissos com horário que se repetem, como "treinar segunda 9h" e "treinar terça 18h".

**Como encaixa no que já existe:**

- **Prazos são tarefas:** a tarefa já tem `prazo`, então "entregar lista" aparece no dia certo sem mudar nada.
- **Prova e rotina são *eventos*, não tarefas:** têm horário e não são algo para "concluir". Isso pede uma entidade
  nova, `Evento`: título, data, hora de início e de fim (ou "dia inteiro"), matéria opcional e uma regra de
  repetição opcional.
- **A repetição reaproveita a regra das tarefas recorrentes** (diária, dias úteis, semanal, a cada N dias, mensal).
  Para eventos, as ocorrências são só *calculadas* para exibir, sem salvar no banco, porque não há nada para marcar
  como feito.
- **Rotina com horário diferente por dia** ("segunda 9h, terça 18h") vira um evento por horário. É o que o Google
  Agenda faz, e mantém a regra simples.
- **As sessões de pomodoro** já guardam início e fim e podem aparecer na semana como blocos do que foi estudado
  de fato.

**Perguntas em aberto:**

- [ ] Uma prova deveria gerar sozinha uma tarefa de estudo antes ("revisar Grafos 3 dias antes")?
- [ ] Lembretes ("avisar 1 h antes")? Depende do PWA (P2) para funcionar com a aba fechada.
- [x] Arrastar um evento ou tarefa para outro dia ou horário muda a data dele? **Sim** (ver decisões do A3).
- [ ] Integrar com o Google Agenda de verdade (exportar `.ics` ou sincronizar) ou só o visual?

### Workspace com quadros Kanban

**A ideia:** uma área de *workspace* onde a pessoa cria os próprios quadros ("TCC", "Estágio", "Mudança"), cada um
com as colunas que quiser. Para não começar do zero, há **modelos** prontos (por exemplo "A fazer / Fazendo / Feito"
ou "Ideias / Escrevendo / Revisão / Entregue").

**Como encaixa no que já existe:**

- **O cartão é uma coisa própria, não uma tarefa:** um cartão vive só no quadro e não aparece na lista de Hoje.
  Quando a pessoa quiser, ele pode ser **ligado a uma tarefa** ("mandar para Hoje"). Aí os dois ficam sincronizados:
  concluir a tarefa move o cartão para a coluna de conclusão, e vice-versa. Desligar não apaga nenhum dos dois.
- **Colunas são livres, mas cada uma tem um papel:** *normal*, *em andamento* ou *conclusão*. É o papel que diz
  para onde o cartão vai quando a tarefa ligada é concluída, sem depender do nome que a pessoa deu à coluna.
- **Ordem dentro da coluna:** um campo `posicao`. É o mesmo que o item 10 ("arrastar para reordenar") precisa, então
  os dois saem juntos, e o Angular CDK (drag and drop) resolve o arrasto nos dois.
- **Modelos** podem começar fixos no código; modelos criados pela pessoa ficam para depois.

**Perguntas em aberto:**

- [ ] O cartão tem prazo, matéria e prioridade próprios, ou isso só existe quando ele está ligado a uma tarefa?
      (sugestão: o cartão tem só título, descrição e, opcionalmente, prazo e matéria; ao ligar, a tarefa herda esses dados)
- [ ] Dá para trazer uma tarefa que já existe para um quadro (o caminho inverso)?
- [ ] Uma tarefa pode estar ligada a mais de um cartão? (sugestão: não, no máximo um)
- [ ] Pomodoro em cartão sem tarefa ligada? A sessão de foco hoje se liga a tarefa e matéria; ligar a cartão também
      seria um campo a mais.
- [ ] **Em análise:** iniciar um pomodoro num cartão move ele para a coluna *em andamento* do quadro.

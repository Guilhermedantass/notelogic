# Roadmap do NoteLogic

O que vem pela frente, as decisões de produto já tomadas e as ideias que ainda estão sendo pensadas.
O que o app **já faz** está no [README](../README.md).

## 1. Próximas features

Em ordem de prioridade.

| # | Feature | Estado |
|---|---|---|
| 1 | Tarefas recorrentes | ✅ Feito (out/2026) |
| 2 | Deploy + CI (ver [seção 2](#2-deploy)) | Próximo |
| 3 | Banco de questões (ver [seção 4](#4-ideias-em-aberto)) | Em discussão |
| 4 | Importar baralhos: CSV e `.apkg` do Anki | Planejado |
| 5 | Metas semanais ("10 h de Grafos") com barra de progresso no painel | Planejado |
| 6 | PWA: instalar no celular, e o pomodoro avisar mesmo com a aba fechada | Planejado |
| 7 | Esqueci minha senha (precisa de envio de e-mail) | Planejado |
| 8 | Subtarefas | Planejado |
| 9 | Busca | Planejado |
| 10 | Arrastar para reordenar tarefas | Planejado |

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

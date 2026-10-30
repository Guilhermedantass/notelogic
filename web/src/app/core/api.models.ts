// Espelho em TypeScript dos DTOs da API (api/src/main/java/.../*Response e *Request).
// Datas chegam como string ISO: "2026-11-05" (LocalDate) ou "2026-10-29T18:49:18Z" (Instant).

export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA';

export interface Materia {
  id: number;
  nome: string;
  cor: string;
}

export interface MateriaRequest {
  nome: string;
  cor: string;
}

export interface Tarefa {
  id: number;
  titulo: string;
  descricao: string | null;
  concluida: boolean;
  criadaEm: string;
  concluidaEm: string | null;
  materia: Materia | null;
  prazo: string | null;
  prioridade: Prioridade;
  /** Preenchido quando a tarefa é a ocorrência de uma tarefa que se repete. */
  recorrencia: { id: number; descricao: string; acumula: boolean } | null;
  dataOcorrencia: string | null;
}

export interface TarefaRequest {
  titulo: string;
  descricao?: string | null;
  materiaId?: number | null;
  prazo?: string | null;
  prioridade?: Prioridade;
}

/** Formato de erro da API (RFC 9457), montado pelo TratadorDeErros. */
export interface ProblemDetail {
  status: number;
  title: string;
  detail: string;
  erros?: Record<string, string>;
}

// ---- Flashcards ----

export type Resposta = 'ERREI' | 'DIFICIL' | 'BOM' | 'FACIL';

export interface Baralho {
  id: number;
  nome: string;
  descricao: string | null;
  materia: Materia | null;
  totalCartoes: number;
  paraRevisarHoje: number;
}

export interface BaralhoRequest {
  nome: string;
  descricao?: string | null;
  materiaId?: number | null;
}

export interface Cartao {
  id: number;
  frente: string;
  verso: string;
  proximaRevisao: string;
  intervaloDias: number;
  repeticoes: number;
  facilidade: number;
}

export interface CartaoRequest {
  frente: string;
  verso: string;
}

// ---- Pomodoro ----

export type TipoSessao = 'FOCO' | 'PAUSA_CURTA' | 'PAUSA_LONGA';
export type StatusSessao = 'EM_ANDAMENTO' | 'CONCLUIDA' | 'INTERROMPIDA';

export interface Sessao {
  id: number;
  tipo: TipoSessao;
  status: StatusSessao;
  duracaoPlanejadaMin: number;
  iniciadaEm: string;
  fimPrevisto: string;
  finalizadaEm: string | null;
  segundosFocados: number | null;
  tarefa: { id: number; titulo: string } | null;
  materia: Materia | null;
}

export interface IniciarSessaoRequest {
  tipo: TipoSessao;
  duracaoMin?: number;
  tarefaId?: number | null;
  materiaId?: number | null;
}

// ---- Estatísticas (GET /api/estatisticas?dias=N) ----

export interface DiaEstatistica {
  data: string;
  minutosFocados: number;
  cartoesRevisados: number;
  tarefasConcluidas: number;
}

export interface Estatisticas {
  hoje: {
    minutosFocados: number;
    cartoesRevisados: number;
    tarefasConcluidas: number;
    cartoesParaRevisar: number;
  };
  sequencia: number;
  taxaDeAcerto: number | null;
  dias: DiaEstatistica[];
  focoPorMateria: { materia: Materia; minutos: number }[];
}

// ---- Tarefas recorrentes ----

export type TipoRecorrencia = 'DIARIA' | 'DIAS_UTEIS' | 'SEMANAL' | 'A_CADA_N_DIAS' | 'MENSAL';
export type DiaDaSemana = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface Regra {
  tipo: TipoRecorrencia;
  diasSemana?: DiaDaSemana[];
  intervaloDias?: number | null;
  diaDoMes?: number | null;
  inicio?: string | null;
  fim?: string | null;
  maxOcorrencias?: number | null;
}

export interface RecorrenciaRequest {
  titulo: string;
  descricao?: string | null;
  materiaId?: number | null;
  prioridade?: Prioridade;
  regra: Regra;
  acumula: boolean;
}

export interface Recorrencia {
  id: number;
  titulo: string;
  descricao: string | null;
  materia: Materia | null;
  prioridade: Prioridade;
  regra: Regra;
  resumo: string;
  acumula: boolean;
  encerrada: boolean;
  feitas: number;
  esperadas: number;
}

/** "Só esta" ou "esta e as próximas", nas ações de uma tarefa que se repete. */
export type Escopo = 'so-esta' | 'proximas';

// ---- Agenda ----

export interface CategoriaEvento {
  id: number;
  nome: string;
  cor: string;
  /** Os eventos desta categoria aparecem em "próximas datas importantes". */
  destaque: boolean;
}

export interface CategoriaRequest {
  nome: string;
  cor: string;
  destaque: boolean;
}

/**
 * Um evento como aparece na agenda. Num evento que se repete, cada ocorrência vem com dataOcorrencia (o dia
 * original pela regra, que identifica a ocorrência para "só esta"); dataInicio/horas já são as do dia.
 * Horas chegam como "14:00:00".
 */
export interface Evento {
  id: number;
  titulo: string;
  descricao: string | null;
  local: string | null;
  categoria: CategoriaEvento | null;
  materia: Materia | null;
  dataInicio: string;
  dataFim: string;
  diaInteiro: boolean;
  horaInicio: string | null;
  horaFim: string | null;
  /** A da categoria, senão a da matéria, senão o violeta do app. */
  cor: string;
  repeticao: Regra | null;
  repeticaoResumo: string | null;
  dataOcorrencia: string | null;
  alterada: boolean;
}

export interface EventoRequest {
  titulo: string;
  descricao?: string | null;
  local?: string | null;
  categoriaId?: number | null;
  materiaId?: number | null;
  dataInicio: string;
  dataFim?: string | null;
  diaInteiro: boolean;
  horaInicio?: string | null;
  horaFim?: string | null;
  /** O inicio da regra é ignorado: vale dataInicio. */
  repeticao?: Regra | null;
}

/** "Só esta": a ocorrência vai para outro dia, horário ou local. */
export interface OcorrenciaRequest {
  data: string;
  horaInicio?: string | null;
  horaFim?: string | null;
  local?: string | null;
}

/** GET /api/agenda/hoje: a parte da agenda na tela Hoje. */
export interface ResumoDoDia {
  hoje: string;
  agenda: Evento[];
  importantes: DataImportante[];
}

export interface DataImportante {
  tipo: 'EVENTO' | 'TAREFA';
  id: number;
  titulo: string;
  /** O nome da categoria do evento, ou "Tarefa". */
  rotulo: string;
  data: string;
  hora: string | null;
  cor: string;
  /** 0 = hoje (ou um evento de vários dias que já começou). */
  diasAte: number;
}

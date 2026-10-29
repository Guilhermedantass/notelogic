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

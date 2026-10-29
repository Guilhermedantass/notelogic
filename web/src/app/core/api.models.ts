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

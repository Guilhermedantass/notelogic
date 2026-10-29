import { Resposta } from './api.models';

// Cópia do AgendadorSm2.java, só para PREVER o intervalo de cada botão na tela de estudo.
// Quem decide de verdade continua sendo a API; os testes em sm2.spec.ts usam os mesmos casos do Java.

export interface EstadoSm2 {
  repeticoes: number;
  intervaloDias: number;
  facilidade: number;
}

const QUALIDADE: Record<Resposta, number> = { ERREI: 1, DIFICIL: 3, BOM: 4, FACIL: 5 };
export const FACILIDADE_MINIMA = 1.3;

export function agendar(atual: EstadoSm2, resposta: Resposta): EstadoSm2 {
  const q = QUALIDADE[resposta];
  if (q < 3) {
    return { repeticoes: 0, intervaloDias: 0, facilidade: atual.facilidade };
  }

  const facilidade = Math.max(
    FACILIDADE_MINIMA,
    atual.facilidade + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)),
  );

  let intervaloDias: number;
  if (atual.repeticoes === 0) intervaloDias = 1;
  else if (atual.repeticoes === 1) intervaloDias = 6;
  else intervaloDias = Math.round(atual.intervaloDias * facilidade);

  return { repeticoes: atual.repeticoes + 1, intervaloDias, facilidade };
}

/** "de novo hoje", "1 dia", "6 dias", "~2 meses", "~1 ano". */
export function formatarIntervalo(dias: number): string {
  if (dias === 0) return 'de novo hoje';
  if (dias === 1) return '1 dia';
  if (dias < 30) return `${dias} dias`;
  if (dias < 365) {
    const meses = Math.round(dias / 30);
    return meses === 1 ? '~1 mês' : `~${meses} meses`;
  }
  const anos = Math.round(dias / 365);
  return anos === 1 ? '~1 ano' : `~${anos} anos`;
}

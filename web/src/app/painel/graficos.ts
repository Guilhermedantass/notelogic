import { DiaEstatistica } from '../core/api.models';
import { formatarDia } from '../core/datas';

// Contas dos gráficos do painel, sem nada de Angular: entram dados, saem números. Testadas em graficos.spec.ts.

/** Uma revisão de cartão conta como ~1 minuto de estudo para o heatmap. */
export function atividade(dia: DiaEstatistica): number {
  return dia.minutosFocados + dia.cartoesRevisados;
}

/** Limiares fixos (e não relativos ao seu melhor dia): 1h30 de estudo sempre parece "muito", em qualquer período. */
const LIMIARES = [1, 15, 45, 90];

/** 0 = nada; 1 a 4 = intensidade crescente. */
export function nivel(valor: number): number {
  return LIMIARES.filter((limiar) => valor >= limiar).length;
}

/**
 * Organiza os dias em colunas de semana (domingo no topo), como o gráfico de contribuições do GitHub.
 * A primeira semana é completada com null antes do primeiro dia, para os dias da semana alinharem.
 */
export function semanas(dias: DiaEstatistica[]): (DiaEstatistica | null)[][] {
  if (dias.length === 0) return [];
  const [a, m, d] = dias[0].data.split('-').map(Number);
  const diaDaSemana = new Date(a, m - 1, d).getDay(); // 0 = domingo

  const celulas: (DiaEstatistica | null)[] = [...Array(diaDaSemana).fill(null), ...dias];
  const resultado: (DiaEstatistica | null)[][] = [];
  for (let i = 0; i < celulas.length; i += 7) {
    resultado.push(celulas.slice(i, i + 7));
  }
  return resultado;
}

/** Nome do mês em cima da coluna em que ele começa. */
export function rotulosDeMes(colunas: (DiaEstatistica | null)[][]): { coluna: number; rotulo: string }[] {
  const rotulos: { coluna: number; rotulo: string }[] = [];
  let mesAnterior = '';
  colunas.forEach((semana, coluna) => {
    const primeiro = semana.find((d) => d !== null);
    if (!primeiro) return;
    const mes = primeiro.data.slice(0, 7);
    if (mes !== mesAnterior) {
      rotulos.push({ coluna, rotulo: formatarDia(primeiro.data, { month: 'short' }).replace('.', '') });
      mesAnterior = mes;
    }
  });
  // um rótulo colado no seguinte (mês que começou no fim da 1ª coluna) fica ilegível: descarta
  return rotulos.filter((r, i) => i === rotulos.length - 1 || rotulos[i + 1].coluna - r.coluna >= 3);
}

/** Topo "redondo" para o eixo: 37 → 45, 130 → 150. Os ticks dividem em três partes iguais. */
export function topoDoEixo(maximo: number): number {
  const candidatos = [15, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360, 480, 600];
  return candidatos.find((c) => c >= maximo) ?? Math.ceil(maximo / 60) * 60;
}

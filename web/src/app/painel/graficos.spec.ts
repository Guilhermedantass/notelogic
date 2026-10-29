import { DiaEstatistica } from '../core/api.models';
import { nivel, rotulosDeMes, semanas, topoDoEixo } from './graficos';

function dia(data: string, minutos = 0): DiaEstatistica {
  return { data, minutosFocados: minutos, cartoesRevisados: 0, tarefasConcluidas: 0 };
}

describe('gráficos do painel', () => {
  it('classifica a atividade em níveis', () => {
    expect(nivel(0)).toBe(0);
    expect(nivel(5)).toBe(1);
    expect(nivel(25)).toBe(2);
    expect(nivel(60)).toBe(3);
    expect(nivel(200)).toBe(4);
  });

  it('alinha a primeira semana pelo dia da semana', () => {
    // 2026-11-04 é quarta-feira: domingo, segunda e terça ficam vazios
    const cols = semanas([dia('2026-11-04'), dia('2026-11-05'), dia('2026-11-06'), dia('2026-11-07'), dia('2026-11-08')]);
    expect(cols.length).toBe(2);
    expect(cols[0].slice(0, 3)).toEqual([null, null, null]);
    expect(cols[0][3]?.data).toBe('2026-11-04');
    expect(cols[1][0]?.data).toBe('2026-11-08'); // domingo abre a 2ª coluna
  });

  it('marca o começo de cada mês', () => {
    const dias = Array.from({ length: 60 }, (_, i) => {
      const d = new Date(2026, 8, 1 + i); // de 1º de setembro em diante
      return dia(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
    });
    const rotulos = rotulosDeMes(semanas(dias)).map((r) => r.rotulo);
    expect(rotulos).toEqual(['set', 'out']);
  });

  it('arredonda o topo do eixo', () => {
    expect(topoDoEixo(0)).toBe(15);
    expect(topoDoEixo(37)).toBe(45);
    expect(topoDoEixo(130)).toBe(150);
    expect(topoDoEixo(700)).toBe(720);
  });
});

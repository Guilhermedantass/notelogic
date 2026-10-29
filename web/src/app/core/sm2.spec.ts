import { agendar, EstadoSm2, FACILIDADE_MINIMA, formatarIntervalo } from './sm2';

// Mesmos cenários do AgendadorSm2Test.java: se um mudar sem o outro, a previsão da tela mente.
describe('sm2', () => {
  const novo: EstadoSm2 = { repeticoes: 0, intervaloDias: 0, facilidade: 2.5 };

  it('segue a escada 1, 6, 15 com acertos seguidos', () => {
    let e = agendar(novo, 'BOM');
    expect(e.intervaloDias).toBe(1);
    e = agendar(e, 'BOM');
    expect(e.intervaloDias).toBe(6);
    e = agendar(e, 'BOM');
    expect(e.intervaloDias).toBe(15);
    expect(e.repeticoes).toBe(3);
  });

  it('difícil baixa e fácil sobe a facilidade', () => {
    expect(agendar(novo, 'DIFICIL').facilidade).toBeCloseTo(2.36, 9);
    expect(agendar(novo, 'FACIL').facilidade).toBeCloseTo(2.6, 9);
  });

  it('errar zera as repetições e mantém a facilidade', () => {
    const e = agendar({ repeticoes: 4, intervaloDias: 40, facilidade: 2.2 }, 'ERREI');
    expect(e).toEqual({ repeticoes: 0, intervaloDias: 0, facilidade: 2.2 });
  });

  it('a facilidade nunca passa do mínimo para baixo', () => {
    let e = novo;
    for (let i = 0; i < 20; i++) e = agendar(e, 'DIFICIL');
    expect(e.facilidade).toBe(FACILIDADE_MINIMA);
  });

  it('formata intervalos de forma amigável', () => {
    expect(formatarIntervalo(0)).toBe('de novo hoje');
    expect(formatarIntervalo(1)).toBe('1 dia');
    expect(formatarIntervalo(15)).toBe('15 dias');
    expect(formatarIntervalo(37)).toBe('~1 mês');
    expect(formatarIntervalo(400)).toBe('~1 ano');
  });
});

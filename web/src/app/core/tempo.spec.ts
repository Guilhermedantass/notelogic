import { formatarDuracao, formatarRelogio } from './tempo';

describe('tempo', () => {
  it('formata o relógio arredondando para cima', () => {
    expect(formatarRelogio(25 * 60_000)).toBe('25:00');
    expect(formatarRelogio(754_000)).toBe('12:34');
    expect(formatarRelogio(400)).toBe('00:01');
    expect(formatarRelogio(0)).toBe('00:00');
    expect(formatarRelogio(-5000)).toBe('00:00');
  });

  it('formata durações', () => {
    expect(formatarDuracao(1500)).toBe('25 min');
    expect(formatarDuracao(3600)).toBe('1h');
    expect(formatarDuracao(5400)).toBe('1h 30min');
  });
});

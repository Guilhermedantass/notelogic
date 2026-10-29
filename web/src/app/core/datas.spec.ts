import { grupoDoPrazo, hojeIso, rotuloDoPrazo, somarDias } from './datas';

describe('datas', () => {
  const hoje = '2026-11-02';

  it('formata hoje como AAAA-MM-DD no fuso local', () => {
    expect(hojeIso(new Date(2026, 0, 5, 23, 59))).toBe('2026-01-05');
  });

  it('soma dias atravessando o mês', () => {
    expect(somarDias('2026-10-31', 1)).toBe('2026-11-01');
    expect(somarDias('2026-11-01', -1)).toBe('2026-10-31');
  });

  it('agrupa pelo prazo', () => {
    expect(grupoDoPrazo('2026-11-01', hoje)).toBe('atrasadas');
    expect(grupoDoPrazo(hoje, hoje)).toBe('hoje');
    expect(grupoDoPrazo('2026-11-10', hoje)).toBe('proximas');
    expect(grupoDoPrazo(null, hoje)).toBe('sem-prazo');
  });

  it('usa nomes amigáveis perto de hoje', () => {
    expect(rotuloDoPrazo(hoje, hoje)).toBe('Hoje');
    expect(rotuloDoPrazo('2026-11-03', hoje)).toBe('Amanhã');
    expect(rotuloDoPrazo('2026-11-01', hoje)).toBe('Ontem');
    expect(rotuloDoPrazo('2026-11-05', hoje)).toContain('5');
  });
});

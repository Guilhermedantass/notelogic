import { Evento, Tarefa } from '../core/api.models';
import { paraCalendario, requestDe, tarefaParaCalendario } from './calendario';

function evento(mudancas: Partial<Evento> = {}): Evento {
  return {
    id: 1,
    titulo: 'Treino',
    descricao: null,
    local: 'Academia',
    categoria: null,
    materia: null,
    dataInicio: '2026-11-02',
    dataFim: '2026-11-02',
    diaInteiro: false,
    horaInicio: '09:00:00',
    horaFim: '10:00:00',
    cor: '#22A06B',
    repeticao: null,
    repeticaoResumo: null,
    dataOcorrencia: null,
    alterada: false,
    ...mudancas,
  };
}

describe('paraCalendario', () => {
  it('evento com horário vira início e fim com data e hora', () => {
    const e = paraCalendario(evento());
    expect(e.start).toBe('2026-11-02T09:00:00');
    expect(e.end).toBe('2026-11-02T10:00:00');
    expect(e.color).toBe('#22A06B');
  });

  it('dia inteiro: o fim do FullCalendar é exclusivo (o dia seguinte ao último)', () => {
    const e = paraCalendario(
      evento({ diaInteiro: true, dataFim: '2026-11-06', horaInicio: null, horaFim: null }),
    );
    expect(e.allDay).toBe(true);
    expect(e.start).toBe('2026-11-02');
    expect(e.end).toBe('2026-11-07');
  });

  it('sem hora de fim: um dia fica sem fim; vários dias vão até o fim do último', () => {
    expect(paraCalendario(evento({ horaFim: null })).end).toBeUndefined();
    expect(paraCalendario(evento({ horaFim: null, dataFim: '2026-11-04' })).end).toBe(
      '2026-11-04T23:59',
    );
  });

  it('cada ocorrência de um evento que se repete tem um id próprio', () => {
    const a = paraCalendario(evento({ dataOcorrencia: '2026-11-02' }));
    const b = paraCalendario(evento({ dataOcorrencia: '2026-11-04' }));
    expect(a.id).not.toBe(b.id);
  });
});

describe('tarefaParaCalendario', () => {
  it('tarefa vira um item de dia inteiro no prazo, marcado como concluída quando for', () => {
    const t = {
      id: 7,
      titulo: 'Lista 4',
      prazo: '2026-11-03',
      concluida: true,
      materia: null,
    } as Tarefa;
    const item = tarefaParaCalendario(t);
    expect(item.allDay).toBe(true);
    expect(item.start).toBe('2026-11-03');
    expect(item.className).toContain('concluida');
    expect(item.durationEditable).toBe(false);
  });
});

describe('requestDe', () => {
  it('monta o request do evento sem mudar nada, com os ids de categoria e matéria', () => {
    const r = requestDe(
      evento({ categoria: { id: 3, nome: 'Treino', cor: '#000000', destaque: false } }),
    );
    expect(r.categoriaId).toBe(3);
    expect(r.materiaId).toBeNull();
    expect(r.horaInicio).toBe('09:00:00');
  });
});

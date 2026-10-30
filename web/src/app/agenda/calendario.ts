import { EventInput } from '@fullcalendar/angular';

import { Evento, EventoRequest, Tarefa } from '../core/api.models';
import { somarDias } from '../core/datas';

// Tradução entre a API e o FullCalendar. Lógica pura, sem Angular: os testes cobrem sem montar a tela.

/** O que cada item do calendário carrega, para saber o que fazer ao clicar ou arrastar. */
export type Props = { tipo: 'evento'; evento: Evento } | { tipo: 'tarefa'; tarefa: Tarefa };

export function paraCalendario(e: Evento): EventInput {
  const base: EventInput = {
    id: `evento:${e.id}:${e.dataOcorrencia ?? ''}`,
    title: e.titulo,
    color: e.cor,
    extendedProps: { tipo: 'evento', evento: e } satisfies Props,
  };
  if (e.diaInteiro) {
    return { ...base, allDay: true, start: e.dataInicio, end: somarDias(e.dataFim, 1) }; // end exclusivo
  }
  const fim = e.horaFim
    ? `${e.dataFim}T${e.horaFim}`
    : e.dataFim !== e.dataInicio
      ? `${e.dataFim}T23:59` // vários dias sem hora de fim: vai até o fim do último dia
      : undefined; // sem fim: o FullCalendar desenha com 1 h
  return { ...base, start: `${e.dataInicio}T${e.horaInicio}`, end: fim };
}

export function tarefaParaCalendario(t: Tarefa): EventInput {
  return {
    id: `tarefa:${t.id}`,
    title: t.titulo,
    allDay: true,
    start: t.prazo!,
    durationEditable: false,
    className: t.concluida ? 'nl-tarefa concluida' : 'nl-tarefa',
    color: t.materia?.cor ?? 'var(--violeta)',
    contrastColor: 'var(--texto)', // o fundo é o da página (ver .nl-tarefa), então o texto é o normal
    extendedProps: { tipo: 'tarefa', tarefa: t } satisfies Props,
  };
}

/** O evento como request, sem mudar nada (a base para mudar só o "quando"). */
export function requestDe(e: Evento): EventoRequest {
  return {
    titulo: e.titulo,
    descricao: e.descricao,
    local: e.local,
    categoriaId: e.categoria?.id ?? null,
    materiaId: e.materia?.id ?? null,
    dataInicio: e.dataInicio,
    dataFim: e.dataFim,
    diaInteiro: e.diaInteiro,
    horaInicio: e.horaInicio,
    horaFim: e.horaFim,
    repeticao: e.repeticao,
  };
}

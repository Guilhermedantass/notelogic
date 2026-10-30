import { HttpClient, httpResource } from '@angular/common/http';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import {
  CalendarOptions,
  DateSelectInfo,
  EventClickInfo,
  EventDropInfo,
  EventInput,
  EventResizeDoneInfo,
  EventSourceFuncInfo,
} from '@fullcalendar/angular';
import { FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/angular/daygrid';
import interactionPlugin from '@fullcalendar/angular/interaction';
// o @fullcalendar/angular não traz os idiomas (o package.json aponta para arquivos que não existem): vêm do fullcalendar
import ptBrLocale from 'fullcalendar/locales/pt-br';
import classicTheme from '@fullcalendar/angular/themes/classic';
import timeGridPlugin from '@fullcalendar/angular/timegrid';
import { RouterLink } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';

import { AgendaService } from '../core/agenda.service';
import {
  CategoriaEvento,
  Evento,
  EventoRequest,
  Materia,
  OcorrenciaRequest,
  Tarefa,
} from '../core/api.models';
import { hojeIso, horaCurta, somarDias } from '../core/datas';
import { mensagemDeErro } from '../core/erros';
import { TarefaService } from '../core/tarefa.service';
import { Modal } from '../layout/modal';
import { TarefaItem } from '../tarefas/tarefa-item';
import { Categorias } from './categorias';
import { paraCalendario, Props, requestDe, tarefaParaCalendario } from './calendario';
import { EventoForm, EventoSalvo, RascunhoEvento } from './evento-form';

type Visao = 'timeGridWeek' | 'dayGridMonth' | 'timeGridDay';
type EscopoEvento = 'so-esta' | 'proximas' | 'todas';

/** Uma pergunta "só esta / esta e as próximas / todas" esperando resposta. */
interface Escolha {
  titulo: string;
  opcoes: EscopoEvento[];
  executar: (escopo: EscopoEvento) => void;
  /** Fechar sem escolher (ex.: desfazer o arrasto). */
  desistir?: () => void;
}

/** O formulário aberto: criando (com o dia clicado) ou editando um evento. */
type Formulario = { rascunho: RascunhoEvento; evento: null } | { rascunho: null; evento: Evento };

const ROTULOS_ESCOPO: Record<EscopoEvento, string> = {
  'so-esta': 'Só esta',
  proximas: 'Esta e as próximas',
  todas: 'Todas',
};

/**
 * A agenda: eventos (prova, aula, rotina) e as tarefas com prazo, em semana, mês ou dia.
 * O FullCalendar desenha; os dados vêm da API a cada período mostrado (events como função).
 */
@Component({
  selector: 'app-agenda',
  imports: [FullCalendarModule, EventoForm, Categorias, Modal, TarefaItem, RouterLink],
  templateUrl: './agenda.html',
  styleUrl: './agenda.scss',
})
export class Agenda {
  private readonly agenda = inject(AgendaService);
  private readonly tarefas = inject(TarefaService);
  private readonly http = inject(HttpClient);
  private readonly calendario = viewChild.required(FullCalendarComponent);

  protected readonly visoes: { valor: Visao; rotulo: string }[] = [
    { valor: 'timeGridDay', rotulo: 'Dia' },
    { valor: 'timeGridWeek', rotulo: 'Semana' },
    { valor: 'dayGridMonth', rotulo: 'Mês' },
  ];
  protected readonly rotulosEscopo = ROTULOS_ESCOPO;

  protected readonly visao = signal<Visao>('timeGridWeek');
  protected readonly periodo = signal('');
  protected readonly erro = signal<string | null>(null);
  protected readonly formulario = signal<Formulario | null>(null);
  protected readonly escolha = signal<Escolha | null>(null);
  protected readonly editandoCategorias = signal(false);
  /** A tarefa clicada no calendário, aberta para ver (e concluir). Editar fica na tela Tarefas. */
  protected readonly tarefaAberta = signal<Tarefa | null>(null);
  protected readonly hojeIso = hojeIso();

  protected readonly categorias = httpResource<CategoriaEvento[]>(() => '/api/agenda/categorias', {
    defaultValue: [],
  });
  protected readonly materias = httpResource<Materia[]>(() => '/api/materias', {
    defaultValue: [],
  });
  protected readonly listaCategorias = computed(() =>
    this.categorias.hasValue() ? this.categorias.value() : [],
  );
  protected readonly listaMaterias = computed(() =>
    this.materias.hasValue() ? this.materias.value() : [],
  );

  protected readonly opcoes: CalendarOptions = {
    plugins: [classicTheme, dayGridPlugin, timeGridPlugin, interactionPlugin],
    locale: ptBrLocale,
    initialView: 'timeGridWeek',
    headerToolbar: false, // a barra é nossa (no visual do app)
    firstDay: 0,
    height: '100%',
    nowIndicator: true,
    scrollTime: '07:00:00',
    allDayText: 'Dia todo',
    dayMaxEvents: true,
    editable: true,
    selectable: true,
    selectMirror: true,
    eventTimeFormat: { hour: '2-digit', minute: '2-digit' },
    events: (info, ok, falhou) => this.carregar(info, ok, falhou),
    datesSet: (info) => this.periodo.set(info.view.title),
    select: (info) => this.novoNoIntervalo(info),
    eventClick: (info) => this.clicar(info),
    eventDrop: (info) => this.mover(info),
    eventResize: (info) => this.mover(info),
  };

  // ---- navegação ----

  protected trocarVisao(visao: Visao): void {
    this.visao.set(visao);
    this.api().changeView(visao);
  }

  protected anterior(): void {
    this.api().prev();
  }

  protected proximo(): void {
    this.api().next();
  }

  protected hoje(): void {
    this.api().today();
  }

  protected novoEvento(): void {
    const dia = hojeIso();
    this.formulario.set({
      rascunho: {
        dataInicio: dia,
        dataFim: dia,
        diaInteiro: false,
        horaInicio: '09:00',
        horaFim: '10:00',
      },
      evento: null,
    });
  }

  protected categoriasMudaram(): void {
    this.categorias.reload();
    this.recarregar(); // as cores dos eventos vêm da categoria
  }

  // ---- dados ----

  /** O FullCalendar pede um período (end exclusivo); a API quer o último dia inclusive. */
  private carregar(
    info: EventSourceFuncInfo,
    ok: (eventos: EventInput[]) => void,
    falhou: (e: Error) => void,
  ): void {
    const de = info.startStr.slice(0, 10);
    const ate = somarDias(info.endStr.slice(0, 10), -1);
    forkJoin({
      eventos: this.agenda.listar(de, ate),
      tarefas: this.http.get<Tarefa[]>('/api/tarefas'),
    }).subscribe({
      next: ({ eventos, tarefas }) => {
        this.erro.set(null);
        const comPrazo = tarefas.filter((t) => t.prazo !== null && t.prazo >= de && t.prazo <= ate);
        ok([...eventos.map(paraCalendario), ...comPrazo.map(tarefaParaCalendario)]);
      },
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        falhou(e);
      },
    });
  }

  private recarregar(): void {
    this.api().refetchEvents();
  }

  private api() {
    return this.calendario().getApi();
  }

  // ---- interações no calendário ----

  /** Clicou (ou arrastou) num espaço vazio: abre o formulário com aquele dia/horário. */
  private novoNoIntervalo(info: DateSelectInfo): void {
    this.api().unselect();
    const dataInicio = hojeIso(info.start);
    if (info.allDay) {
      this.formulario.set({
        rascunho: {
          dataInicio,
          dataFim: somarDias(hojeIso(info.end), -1),
          diaInteiro: true,
          horaInicio: null,
          horaFim: null,
        },
        evento: null,
      });
      return;
    }
    const mesmoDia = hojeIso(info.end) === dataInicio;
    this.formulario.set({
      rascunho: {
        dataInicio,
        dataFim: dataInicio,
        diaInteiro: false,
        horaInicio: horaCurta(info.start),
        horaFim: mesmoDia ? horaCurta(info.end) : null,
      },
      evento: null,
    });
  }

  private clicar(info: EventClickInfo): void {
    const props = info.event.extendedProps as Props;
    if (props.tipo === 'evento') {
      this.formulario.set({ rascunho: null, evento: props.evento });
    } else {
      this.tarefaAberta.set(props.tarefa);
    }
  }

  /** Arrastou ou esticou. Se algo der errado (ou você desistir), o FullCalendar desfaz com revert(). */
  private mover(info: EventDropInfo | EventResizeDoneInfo): void {
    const props = info.event.extendedProps as Props;
    const inicio = info.event.start!;
    const fim = info.event.end;

    if (props.tipo === 'tarefa') {
      if (!info.event.allDay) {
        info.revert(); // tarefa não tem horário: só muda de dia
        return;
      }
      const t = props.tarefa;
      this.executar(
        this.tarefas.editar(t.id, {
          titulo: t.titulo,
          descricao: t.descricao,
          materiaId: t.materia?.id ?? null,
          prazo: hojeIso(inicio),
          prioridade: t.prioridade,
        }),
        info.revert,
      );
      return;
    }

    const e = props.evento;
    const diaInteiro = info.event.allDay;
    const dataInicio = hojeIso(inicio);
    const quando = diaInteiro
      ? {
          dataInicio,
          dataFim: fim ? somarDias(hojeIso(fim), -1) : dataInicio,
          diaInteiro,
          horaInicio: null,
          horaFim: null,
        }
      : {
          dataInicio,
          dataFim: fim ? hojeIso(fim) : dataInicio,
          diaInteiro,
          horaInicio: horaCurta(inicio),
          horaFim: fim ? horaCurta(fim) : null,
        };

    if (!e.repeticao) {
      this.executar(this.agenda.editar(e.id, { ...requestDe(e), ...quando }), info.revert);
      return;
    }
    if (diaInteiro !== e.diaInteiro) {
      info.revert();
      this.erro.set(
        'Para trocar entre dia inteiro e horário num evento que se repete, edite o evento.',
      );
      return;
    }
    const soEsta = (): void =>
      this.executar(
        this.agenda.alterarOcorrencia(e.id, e.dataOcorrencia!, {
          data: quando.dataInicio,
          horaInicio: quando.horaInicio,
          horaFim: quando.horaFim,
          local: e.local,
        }),
        info.revert,
      );
    if (dataInicio !== e.dataInicio) {
      soEsta(); // mudou de dia: só esta (mudar o dia de toda a série é pela edição da regra)
      return;
    }
    this.escolha.set({
      titulo: `Mudar o horário de "${e.titulo}"`,
      opcoes: ['so-esta', 'proximas', 'todas'],
      desistir: info.revert,
      executar: (escopo) => {
        if (escopo === 'so-esta') return soEsta();
        const dados = { ...requestDe(e), horaInicio: quando.horaInicio, horaFim: quando.horaFim };
        if (escopo === 'proximas') {
          this.executar(this.agenda.editar(e.id, dados, e.dataOcorrencia!), info.revert);
        } else {
          this.executar(
            this.agenda.editar(e.id, { ...dados, dataInicio: e.repeticao!.inicio! }),
            info.revert,
          );
        }
      },
    });
  }

  /** Concluir ou reabrir pela janela da tarefa: a janela continua aberta, já com o estado novo. */
  protected alternarTarefaAberta(t: Tarefa): void {
    const acao = t.concluida ? this.tarefas.reabrir(t.id) : this.tarefas.concluir(t.id);
    this.erro.set(null);
    acao.subscribe({
      next: (atualizada) => {
        this.tarefaAberta.set(atualizada);
        this.recarregar();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  /** O check da tarefa, ali mesmo na agenda. */
  protected alternarTarefa(t: Tarefa, evento: MouseEvent): void {
    evento.stopPropagation();
    this.executar(t.concluida ? this.tarefas.reabrir(t.id) : this.tarefas.concluir(t.id));
  }

  // ---- formulário ----

  protected salvar({ dados, ocorrencia, soQuandoMudou }: EventoSalvo): void {
    const e = this.formulario()?.evento;
    if (!e) {
      this.executar(this.agenda.criar(dados), undefined, true);
      return;
    }
    if (!e.repeticao) {
      this.executar(this.agenda.editar(e.id, dados), undefined, true);
      return;
    }
    this.escolha.set({
      titulo: `Salvar "${e.titulo}"`,
      // "só esta" guarda só data, horário e local: só aparece se foi só isso que mudou
      opcoes: soQuandoMudou ? ['so-esta', 'proximas', 'todas'] : ['proximas', 'todas'],
      executar: (escopo) => this.salvarComEscopo(e, dados, ocorrencia, escopo),
    });
  }

  private salvarComEscopo(
    e: Evento,
    dados: EventoRequest,
    ocorrencia: OcorrenciaRequest,
    escopo: EscopoEvento,
  ): void {
    if (escopo === 'so-esta') {
      this.executar(
        this.agenda.alterarOcorrencia(e.id, e.dataOcorrencia!, ocorrencia),
        undefined,
        true,
      );
    } else if (escopo === 'proximas') {
      this.executar(this.agenda.editar(e.id, dados, e.dataOcorrencia!), undefined, true);
    } else {
      // a série inteira continua começando onde começava (o dia no formulário é o da ocorrência clicada)
      this.executar(
        this.agenda.editar(e.id, { ...dados, dataInicio: e.repeticao!.inicio! }),
        undefined,
        true,
      );
    }
  }

  protected apagar(): void {
    const e = this.formulario()?.evento;
    if (!e) return;
    if (!e.repeticao) {
      if (confirm(`Apagar "${e.titulo}"?`))
        this.executar(this.agenda.remover(e.id), undefined, true);
      return;
    }
    this.escolha.set({
      titulo: `Apagar "${e.titulo}"`,
      opcoes: ['so-esta', 'proximas', 'todas'],
      executar: (escopo) => {
        const acao =
          escopo === 'so-esta'
            ? this.agenda.cancelarOcorrencia(e.id, e.dataOcorrencia!)
            : this.agenda.remover(e.id, escopo === 'proximas' ? e.dataOcorrencia! : undefined);
        this.executar(acao, undefined, true);
      },
    });
  }

  protected escolher(escopo: EscopoEvento): void {
    const escolha = this.escolha();
    this.escolha.set(null);
    escolha?.executar(escopo);
  }

  protected desistirDaEscolha(): void {
    const escolha = this.escolha();
    if (!escolha) return; // já respondida (o <dialog> também avisa "fechar" ao sair da tela)
    this.escolha.set(null);
    escolha.desistir?.();
  }

  /** Roda a ação; deu certo, recarrega a agenda (e fecha o formulário); deu errado, mostra e desfaz. */
  private executar(
    acao: Observable<unknown>,
    desfazer?: () => void,
    fecharFormulario = false,
  ): void {
    this.erro.set(null);
    acao.subscribe({
      next: () => {
        if (fecharFormulario) this.formulario.set(null);
        this.recarregar();
      },
      error: (e) => {
        desfazer?.();
        this.erro.set(mensagemDeErro(e));
      },
    });
  }
}

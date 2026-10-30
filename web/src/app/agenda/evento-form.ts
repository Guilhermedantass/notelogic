import { Component, computed, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  CategoriaEvento,
  DiaDaSemana,
  Evento,
  EventoRequest,
  Materia,
  OcorrenciaRequest,
  Regra,
  TipoRecorrencia,
} from '../core/api.models';
import { DIAS_DA_SEMANA, horaCurta, somarDias } from '../core/datas';
import { Modal } from '../layout/modal';

type Repetir = 'NAO' | TipoRecorrencia;
type Termina = 'NUNCA' | 'DATA' | 'VEZES';

/** O que já vem preenchido ao criar (o dia ou horário que você clicou na agenda). */
export interface RascunhoEvento {
  dataInicio: string;
  dataFim: string;
  diaInteiro: boolean;
  horaInicio: string | null;
  horaFim: string | null;
}

/**
 * O que o formulário devolve. {@code soQuandoMudou}: só data, horário ou local mudaram, o que permite
 * oferecer "só esta" num evento que se repete (a API só guarda isso numa exceção).
 */
export interface EventoSalvo {
  dados: EventoRequest;
  ocorrencia: OcorrenciaRequest;
  soQuandoMudou: boolean;
}

@Component({
  selector: 'app-evento-form',
  imports: [FormsModule, Modal],
  templateUrl: './evento-form.html',
  styleUrl: './evento-form.scss',
})
export class EventoForm implements OnInit {
  readonly categorias = input<CategoriaEvento[]>([]);
  readonly materias = input<Materia[]>([]);
  /** Editando: o evento (num que se repete, a ocorrência clicada). */
  readonly evento = input<Evento | null>(null);
  /** Criando: o que veio do clique na agenda. */
  readonly rascunho = input<RascunhoEvento | null>(null);

  readonly salvar = output<EventoSalvo>();
  readonly apagar = output<void>();
  readonly fechar = output<void>();

  protected readonly dias = DIAS_DA_SEMANA;

  protected titulo = '';
  protected categoriaId: number | null = null;
  protected materiaId: number | null = null;
  protected local = '';
  protected descricao = '';
  protected readonly diaInteiro = signal(false);
  protected dataInicio = '';
  protected readonly variosDias = signal(false);
  protected dataFim = '';
  protected horaInicio = '09:00';
  protected horaFim = '';

  protected readonly repetir = signal<Repetir>('NAO');
  protected readonly diasSemana = signal<DiaDaSemana[]>([]);
  protected intervaloDias = 2;
  protected diaDoMes = 1;
  protected readonly termina = signal<Termina>('NUNCA');
  protected fimRepeticao = '';
  protected maxOcorrencias = 10;

  protected readonly editando = computed(() => this.evento() !== null);
  protected readonly seRepete = computed(() => this.evento()?.repeticao != null);

  /** O que não é "quando": se nada disso mudar, dá para salvar como "só esta". */
  private original = '';

  ngOnInit(): void {
    const e = this.evento();
    const r = this.rascunho();
    if (e) {
      this.titulo = e.titulo;
      this.categoriaId = e.categoria?.id ?? null;
      this.materiaId = e.materia?.id ?? null;
      this.local = e.local ?? '';
      this.descricao = e.descricao ?? '';
      this.preencherQuando(e.dataInicio, e.dataFim, e.diaInteiro, e.horaInicio, e.horaFim);
      if (e.repeticao) this.preencherRegra(e.repeticao);
    } else if (r) {
      this.preencherQuando(r.dataInicio, r.dataFim, r.diaInteiro, r.horaInicio, r.horaFim);
    }
    const [, , d] = this.dataInicio.split('-').map(Number);
    this.diaDoMes = this.repetir() === 'MENSAL' ? this.diaDoMes : d;
    if (this.diasSemana().length === 0) this.diasSemana.set([this.diaDaSemanaDe(this.dataInicio)]);
    this.original = this.assinatura();
  }

  protected alternarDia(dia: DiaDaSemana): void {
    this.diasSemana.update((atual) =>
      atual.includes(dia) ? atual.filter((d) => d !== dia) : [...atual, dia],
    );
  }

  protected alternarVariosDias(valor: boolean): void {
    this.variosDias.set(valor);
    if (valor) {
      this.repetir.set('NAO'); // só eventos de um dia se repetem
      if (!this.dataFim || this.dataFim <= this.dataInicio)
        this.dataFim = somarDias(this.dataInicio, 1);
    }
  }

  protected enviar(): void {
    const titulo = this.titulo.trim();
    if (!titulo || !this.dataInicio) return;
    if (this.repetir() === 'SEMANAL' && this.diasSemana().length === 0) return;

    const diaInteiro = this.diaInteiro();
    const horaInicio = diaInteiro ? null : this.horaInicio || null;
    const horaFim = diaInteiro ? null : this.horaFim || null;
    const local = this.local.trim() || null;
    this.salvar.emit({
      dados: {
        titulo,
        descricao: this.descricao.trim() || null,
        local,
        categoriaId: this.categoriaId,
        materiaId: this.materiaId,
        dataInicio: this.dataInicio,
        dataFim: this.variosDias() ? this.dataFim : null,
        diaInteiro,
        horaInicio,
        horaFim,
        repeticao: this.regra(),
      },
      ocorrencia: { data: this.dataInicio, horaInicio, horaFim, local },
      soQuandoMudou: this.assinatura() === this.original,
    });
  }

  private regra(): Regra | null {
    const tipo = this.repetir();
    if (tipo === 'NAO' || this.variosDias()) return null;
    const termina = this.termina();
    return {
      tipo,
      diasSemana: tipo === 'SEMANAL' ? this.diasSemana() : undefined,
      intervaloDias: tipo === 'A_CADA_N_DIAS' ? this.intervaloDias : null,
      diaDoMes: tipo === 'MENSAL' ? this.diaDoMes : null,
      fim: termina === 'DATA' && this.fimRepeticao ? this.fimRepeticao : null,
      maxOcorrencias: termina === 'VEZES' ? this.maxOcorrencias : null,
    };
  }

  private preencherQuando(
    inicio: string,
    fim: string,
    diaInteiro: boolean,
    h1: string | null,
    h2: string | null,
  ) {
    this.dataInicio = inicio;
    this.dataFim = fim;
    this.variosDias.set(fim !== inicio);
    this.diaInteiro.set(diaInteiro);
    this.horaInicio = h1 ? horaCurta(h1) : '09:00';
    this.horaFim = h2 ? horaCurta(h2) : '';
  }

  private preencherRegra(regra: Regra): void {
    this.repetir.set(regra.tipo);
    this.diasSemana.set(regra.diasSemana ?? []);
    this.intervaloDias = regra.intervaloDias ?? 2;
    this.diaDoMes = regra.diaDoMes ?? 1;
    if (regra.maxOcorrencias) {
      this.termina.set('VEZES');
      this.maxOcorrencias = regra.maxOcorrencias;
    } else if (regra.fim) {
      this.termina.set('DATA');
      this.fimRepeticao = regra.fim;
    }
  }

  private assinatura(): string {
    return JSON.stringify([
      this.titulo.trim(),
      this.categoriaId,
      this.materiaId,
      this.descricao.trim(),
      this.diaInteiro(),
      this.variosDias(),
      this.regraParaComparar(),
    ]);
  }

  /** A regra sem o que é derivado da data (o dia do mês e o dia da semana que vêm do dia clicado). */
  private regraParaComparar(): unknown {
    const tipo = this.repetir();
    return tipo === 'NAO'
      ? null
      : [
          tipo,
          [...this.diasSemana()].sort(),
          this.intervaloDias,
          this.diaDoMes,
          this.termina(),
          this.fimRepeticao,
          this.maxOcorrencias,
        ];
  }

  private diaDaSemanaDe(iso: string): DiaDaSemana {
    const [a, m, d] = iso.split('-').map(Number);
    return DIAS_DA_SEMANA[new Date(a, m - 1, d).getDay()].valor;
  }
}

import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  DiaDaSemana,
  Materia,
  Prioridade,
  RecorrenciaRequest,
  TarefaRequest,
  TipoRecorrencia,
} from '../core/api.models';
import { DIAS_DA_SEMANA, hojeIso } from '../core/datas';

type Repetir = 'NAO' | TipoRecorrencia;

/**
 * O "Add task" da v1: um campo no fim da lista. Enter cria; os detalhes aparecem quando você começa a digitar.
 * Com "Repetir", em vez de uma tarefa cria uma REGRA (e a API gera a tarefa de cada dia).
 */
@Component({
  selector: 'app-nova-tarefa',
  imports: [FormsModule],
  templateUrl: './nova-tarefa.html',
  styleUrl: './nova-tarefa.scss',
})
export class NovaTarefa {
  readonly materias = input<Materia[]>([]);
  /** Se a lista está filtrada por uma matéria, a tarefa nova já nasce nela. */
  readonly materiaPadrao = input<number | null>(null);

  readonly criar = output<TarefaRequest>();
  readonly criarRecorrente = output<RecorrenciaRequest>();

  protected readonly dias = DIAS_DA_SEMANA;
  protected readonly titulo = signal('');
  protected readonly hoje = hojeIso();
  protected materiaId: number | null = null;
  protected prazo: string | null = null;
  protected prioridade: Prioridade = 'MEDIA';

  // ---- repetição ----
  protected readonly repetir = signal<Repetir>('NAO');
  protected readonly diasSemana = signal<DiaDaSemana[]>([
    DIAS_DA_SEMANA[new Date().getDay()].valor,
  ]);
  protected intervaloDias = 2;
  protected diaDoMes = new Date().getDate();
  protected acumula = false;

  protected alternarDia(dia: DiaDaSemana): void {
    this.diasSemana.update((atual) =>
      atual.includes(dia) ? atual.filter((d) => d !== dia) : [...atual, dia],
    );
  }

  protected enviar(): void {
    const titulo = this.titulo().trim();
    if (!titulo) return;
    const materiaId = this.materiaId ?? this.materiaPadrao();

    const tipo = this.repetir();
    if (tipo === 'NAO') {
      this.criar.emit({
        titulo,
        materiaId,
        prazo: this.prazo || null,
        prioridade: this.prioridade,
      });
    } else {
      if (tipo === 'SEMANAL' && this.diasSemana().length === 0) return;
      this.criarRecorrente.emit({
        titulo,
        materiaId,
        prioridade: this.prioridade,
        acumula: this.acumula,
        regra: {
          tipo,
          inicio: this.prazo || this.hoje, // com repetição, a data do campo vira "a partir de"
          diasSemana: tipo === 'SEMANAL' ? this.diasSemana() : undefined,
          intervaloDias: tipo === 'A_CADA_N_DIAS' ? this.intervaloDias : undefined,
          diaDoMes: tipo === 'MENSAL' ? this.diaDoMes : undefined,
        },
      });
    }
    this.limpar();
  }

  private limpar(): void {
    this.titulo.set('');
    this.materiaId = null;
    this.prazo = null;
    this.prioridade = 'MEDIA';
    this.repetir.set('NAO');
    this.acumula = false;
  }
}

import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { AgendaDeHoje, DatasImportantes } from '../agenda/resumo-do-dia';
import {
  Materia,
  RecorrenciaRequest,
  ResumoDoDia,
  Tarefa,
  TarefaRequest,
} from '../core/api.models';
import { formatarDia, grupoDoPrazo, hojeIso } from '../core/datas';
import { mensagemDeErro } from '../core/erros';
import { RecorrenciaService } from '../core/recorrencia.service';
import { TarefaService } from '../core/tarefa.service';
import { NovaTarefa } from '../tarefas/nova-tarefa';
import { TarefaItem } from '../tarefas/tarefa-item';

/**
 * A tela que abre depois do login: só o que é do dia. No centro, as tarefas atrasadas e de hoje (dá para concluir
 * e adicionar; editar e apagar ficam em Tarefas). Ao lado, a agenda de hoje; embaixo, as próximas datas importantes.
 */
@Component({
  selector: 'app-inicio',
  imports: [TarefaItem, NovaTarefa, AgendaDeHoje, DatasImportantes, RouterLink],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  private readonly tarefas = inject(TarefaService);
  private readonly regras = inject(RecorrenciaService);

  protected readonly hoje = hojeIso();
  protected readonly dataExtensa = formatarDia(this.hoje, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });

  protected readonly erro = signal<string | null>(null);

  protected readonly materias = httpResource<Materia[]>(() => '/api/materias', {
    defaultValue: [],
  });
  protected readonly pendentes = httpResource<Tarefa[]>(
    () => ({ url: '/api/tarefas', params: { concluida: false } }),
    { defaultValue: [] },
  );
  /** Agenda de hoje e datas importantes. Recarrega junto: entregas de prioridade alta entram nele. */
  protected readonly resumo = httpResource<ResumoDoDia>(() => '/api/agenda/hoje');

  protected readonly listaMaterias = computed(() =>
    this.materias.hasValue() ? this.materias.value() : [],
  );

  private readonly listaPendentes = computed(() =>
    this.pendentes.hasValue() ? this.pendentes.value() : [],
  );
  protected readonly atrasadas = computed(() =>
    this.listaPendentes().filter((t) => grupoDoPrazo(t.prazo, this.hoje) === 'atrasadas'),
  );
  protected readonly deHoje = computed(() =>
    this.listaPendentes().filter((t) => grupoDoPrazo(t.prazo, this.hoje) === 'hoje'),
  );

  protected readonly erroDeCarga = computed(() => {
    const e = this.pendentes.error() ?? this.resumo.error();
    return e ? mensagemDeErro(e) : null;
  });

  protected adicionar(dados: TarefaRequest): void {
    this.executar(this.tarefas.criar(dados));
  }

  protected adicionarRecorrente(dados: RecorrenciaRequest): void {
    this.executar(this.regras.criar(dados));
  }

  protected concluir(t: Tarefa): void {
    this.executar(this.tarefas.concluir(t.id));
  }

  protected reabrir(t: Tarefa): void {
    this.executar(this.tarefas.reabrir(t.id));
  }

  private executar(acao: Observable<unknown>): void {
    this.erro.set(null);
    acao.subscribe({
      next: () => {
        this.pendentes.reload();
        this.resumo.reload();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}

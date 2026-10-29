import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { Materia, Tarefa, TarefaRequest } from '../core/api.models';
import { formatarDia, GrupoPrazo, grupoDoPrazo, hojeIso } from '../core/datas';
import { mensagemDeErro } from '../core/erros';
import { TarefaService } from '../core/tarefa.service';
import { NovaTarefa } from './nova-tarefa';
import { TarefaItem } from './tarefa-item';

interface Grupo {
  chave: GrupoPrazo;
  titulo: string;
  tarefas: Tarefa[];
}

const TITULOS: Record<GrupoPrazo, string> = {
  atrasadas: 'Atrasadas',
  hoje: 'Para hoje',
  proximas: 'Próximas',
  'sem-prazo': 'Sem prazo',
};

/** A tela principal: tarefas pendentes agrupadas por prazo, filtro por matéria e as concluídas embaixo. */
@Component({
  selector: 'app-hoje',
  imports: [TarefaItem, NovaTarefa],
  templateUrl: './hoje.html',
  styleUrl: './hoje.scss',
})
export class Hoje {
  private readonly tarefas = inject(TarefaService);

  protected readonly hoje = hojeIso();
  protected readonly dataExtensa = formatarDia(this.hoje, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });

  protected readonly materiaFiltro = signal<number | null>(null);
  protected readonly mostrarConcluidas = signal(false);
  protected readonly erro = signal<string | null>(null);

  // httpResource refaz a requisição sozinho sempre que um signal lido dentro da função muda.
  // Trocar o filtro de matéria = nova busca, sem nenhum subscribe manual.
  protected readonly materias = httpResource<Materia[]>(() => '/api/materias', {
    defaultValue: [],
  });

  protected readonly pendentes = httpResource<Tarefa[]>(
    () => ({ url: '/api/tarefas', params: this.filtro(false) }),
    { defaultValue: [] },
  );

  // Devolver undefined deixa o resource parado: as concluídas só são buscadas quando você abre a seção.
  protected readonly concluidas = httpResource<Tarefa[]>(
    () =>
      this.mostrarConcluidas()
        ? { url: '/api/tarefas', params: this.filtro(true) }
        : undefined,
    { defaultValue: [] },
  );

  protected readonly listaMaterias = computed(() =>
    this.materias.hasValue() ? this.materias.value() : [],
  );

  protected readonly listaPendentes = computed(() =>
    this.pendentes.hasValue() ? this.pendentes.value() : [],
  );

  protected readonly listaConcluidas = computed(() =>
    this.concluidas.hasValue() ? this.concluidas.value() : [],
  );

  protected readonly grupos = computed<Grupo[]>(() => {
    const porGrupo = new Map<GrupoPrazo, Tarefa[]>();
    for (const t of this.listaPendentes()) {
      const chave = grupoDoPrazo(t.prazo, this.hoje);
      porGrupo.set(chave, [...(porGrupo.get(chave) ?? []), t]);
    }
    return (Object.keys(TITULOS) as GrupoPrazo[])
      .filter((chave) => porGrupo.has(chave))
      .map((chave) => ({ chave, titulo: TITULOS[chave], tarefas: porGrupo.get(chave)! }));
  });

  protected readonly erroDeCarga = computed(() => {
    const e = this.pendentes.error() ?? this.materias.error();
    return e ? mensagemDeErro(e) : null;
  });

  protected adicionar(dados: TarefaRequest): void {
    this.executar(this.tarefas.criar(dados));
  }

  protected concluir(t: Tarefa): void {
    this.executar(this.tarefas.concluir(t.id));
  }

  protected reabrir(t: Tarefa): void {
    this.executar(this.tarefas.reabrir(t.id));
  }

  protected remover(t: Tarefa): void {
    if (confirm(`Apagar "${t.titulo}"?`)) {
      this.executar(this.tarefas.remover(t.id));
    }
  }

  protected salvar({ tarefa, dados }: { tarefa: Tarefa; dados: TarefaRequest }): void {
    this.executar(this.tarefas.editar(tarefa.id, dados));
  }

  /** Toda escrita termina igual: recarrega as listas, ou mostra o erro que a API mandou. */
  private executar(acao: Observable<unknown>): void {
    this.erro.set(null);
    acao.subscribe({
      next: () => {
        this.pendentes.reload();
        this.concluidas.reload();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  private filtro(concluida: boolean): Record<string, string | number | boolean> {
    const materiaId = this.materiaFiltro();
    return materiaId === null ? { concluida } : { concluida, materiaId };
  }
}

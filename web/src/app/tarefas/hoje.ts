import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { ResumoDoDiaComponent } from '../agenda/resumo-do-dia';
import {
  Escopo,
  Materia,
  Recorrencia,
  RecorrenciaRequest,
  ResumoDoDia,
  Tarefa,
  TarefaRequest,
} from '../core/api.models';
import { formatarDia, GrupoPrazo, grupoDoPrazo, hojeIso } from '../core/datas';
import { mensagemDeErro } from '../core/erros';
import { RecorrenciaService } from '../core/recorrencia.service';
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

/**
 * A tela principal, o resumo do dia: tarefas pendentes agrupadas por prazo (com filtro por matéria e as concluídas
 * embaixo) e, ao lado, a agenda de hoje e as próximas datas importantes.
 */
@Component({
  selector: 'app-hoje',
  imports: [TarefaItem, NovaTarefa, ResumoDoDiaComponent],
  templateUrl: './hoje.html',
  styleUrl: './hoje.scss',
})
export class Hoje {
  private readonly tarefas = inject(TarefaService);
  private readonly regras = inject(RecorrenciaService);

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
      this.mostrarConcluidas() ? { url: '/api/tarefas', params: this.filtro(true) } : undefined,
    { defaultValue: [] },
  );

  /** As regras de repetição: dão a constância ("18/20") e o molde para "esta e as próximas". */
  protected readonly recorrencias = httpResource<Recorrencia[]>(() => '/api/recorrencias', {
    defaultValue: [],
  });
  private readonly recorrenciaPorId = computed(
    () =>
      new Map(
        (this.recorrencias.hasValue() ? this.recorrencias.value() : []).map((r) => [r.id, r]),
      ),
  );

  /** Agenda de hoje e datas importantes. Recarrega junto com as tarefas: entregas de prioridade alta entram nele. */
  protected readonly resumo = httpResource<ResumoDoDia>(() => '/api/agenda/hoje');

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

  protected adicionarRecorrente(dados: RecorrenciaRequest): void {
    this.executar(this.regras.criar(dados));
  }

  protected constanciaDe(t: Tarefa): { feitas: number; esperadas: number } | null {
    return t.recorrencia ? (this.recorrenciaPorId().get(t.recorrencia.id) ?? null) : null;
  }

  protected concluir(t: Tarefa): void {
    this.executar(this.tarefas.concluir(t.id));
  }

  protected reabrir(t: Tarefa): void {
    this.executar(this.tarefas.reabrir(t.id));
  }

  protected remover({ tarefa: t, escopo }: { tarefa: Tarefa; escopo: Escopo }): void {
    if (escopo === 'proximas' && t.recorrencia && t.dataOcorrencia) {
      // "esta e as próximas": a regra para de valer a partir do dia desta ocorrência
      this.executar(this.regras.encerrarAPartirDe(t.recorrencia.id, t.dataOcorrencia));
    } else if (t.recorrencia || confirm(`Apagar "${t.titulo}"?`)) {
      // tarefa que se repete já passou pela pergunta "só esta?"; a comum ainda pede confirmação
      this.executar(this.tarefas.remover(t.id));
    }
  }

  protected salvar({
    tarefa,
    dados,
    escopo,
  }: {
    tarefa: Tarefa;
    dados: TarefaRequest;
    escopo: Escopo;
  }): void {
    const regra = tarefa.recorrencia
      ? this.recorrenciaPorId().get(tarefa.recorrencia.id)
      : undefined;
    if (escopo === 'proximas' && regra && tarefa.dataOcorrencia) {
      // muda o molde da regra (mantendo o "quando") e refaz as ocorrências pendentes daqui para frente
      this.executar(
        this.regras.editarAPartirDe(regra.id, tarefa.dataOcorrencia, {
          titulo: dados.titulo,
          descricao: dados.descricao,
          materiaId: dados.materiaId,
          prioridade: dados.prioridade,
          regra: regra.regra,
          acumula: regra.acumula,
        }),
      );
    } else {
      this.executar(this.tarefas.editar(tarefa.id, dados));
    }
  }

  /** Toda escrita termina igual: recarrega as listas, ou mostra o erro que a API mandou. */
  private executar(acao: Observable<unknown>): void {
    this.erro.set(null);
    acao.subscribe({
      next: () => {
        this.pendentes.reload();
        this.concluidas.reload();
        this.recorrencias.reload();
        this.resumo.reload();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }

  private filtro(concluida: boolean): Record<string, string | number | boolean> {
    const materiaId = this.materiaFiltro();
    return materiaId === null ? { concluida } : { concluida, materiaId };
  }
}

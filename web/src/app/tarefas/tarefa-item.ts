import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Materia, Prioridade, Tarefa, TarefaRequest } from '../core/api.models';
import { grupoDoPrazo, rotuloDoPrazo } from '../core/datas';

/**
 * Uma linha da lista, no estilo da v1: check, título, prazo em verde e a etiqueta da matéria.
 * Componente "burro": não chama a API, só avisa o pai (outputs) do que o usuário fez.
 */
@Component({
  selector: 'app-tarefa-item',
  imports: [FormsModule],
  templateUrl: './tarefa-item.html',
  styleUrl: './tarefa-item.scss',
})
export class TarefaItem {
  readonly tarefa = input.required<Tarefa>();
  readonly hoje = input.required<string>();
  readonly materias = input<Materia[]>([]);

  readonly concluir = output<Tarefa>();
  readonly reabrir = output<Tarefa>();
  readonly remover = output<Tarefa>();
  readonly salvar = output<{ tarefa: Tarefa; dados: TarefaRequest }>();

  protected readonly editando = signal(false);
  protected rascunho: Required<TarefaRequest> = this.vazio();

  protected readonly prazo = computed(() => {
    const prazo = this.tarefa().prazo;
    if (!prazo) return null;
    return {
      rotulo: rotuloDoPrazo(prazo, this.hoje()),
      atrasada: !this.tarefa().concluida && grupoDoPrazo(prazo, this.hoje()) === 'atrasadas',
    };
  });

  protected readonly prioridades: { valor: Prioridade; rotulo: string }[] = [
    { valor: 'BAIXA', rotulo: 'Baixa' },
    { valor: 'MEDIA', rotulo: 'Média' },
    { valor: 'ALTA', rotulo: 'Alta' },
  ];

  protected alternar(): void {
    const t = this.tarefa();
    (t.concluida ? this.reabrir : this.concluir).emit(t);
  }

  protected abrirEdicao(): void {
    const t = this.tarefa();
    this.rascunho = {
      titulo: t.titulo,
      descricao: t.descricao,
      materiaId: t.materia?.id ?? null,
      prazo: t.prazo,
      prioridade: t.prioridade,
    };
    this.editando.set(true);
  }

  protected confirmarEdicao(): void {
    if (!this.rascunho.titulo.trim()) return;
    this.salvar.emit({
      tarefa: this.tarefa(),
      dados: { ...this.rascunho, prazo: this.rascunho.prazo || null },
    });
    this.editando.set(false);
  }

  private vazio(): Required<TarefaRequest> {
    return { titulo: '', descricao: null, materiaId: null, prazo: null, prioridade: 'MEDIA' };
  }
}

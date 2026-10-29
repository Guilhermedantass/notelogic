import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { Materia, MateriaRequest } from '../core/api.models';
import { mensagemDeErro } from '../core/erros';
import { MateriaService } from '../core/materia.service';

/** Cores sugeridas, começando pelo violeta da marca. */
const PALETA = ['#543EFE', '#E5484D', '#1F9D55', '#E08A00', '#0090FF', '#D6409F', '#12A594', '#8E4EC6'];

@Component({
  selector: 'app-materias',
  imports: [FormsModule],
  templateUrl: './materias.html',
  styleUrl: './materias.scss',
})
export class Materias {
  private readonly service = inject(MateriaService);

  protected readonly paleta = PALETA;
  protected readonly materias = httpResource<Materia[]>(() => '/api/materias', { defaultValue: [] });
  protected readonly lista = computed(() => (this.materias.hasValue() ? this.materias.value() : []));
  protected readonly erro = signal<string | null>(null);

  protected nova: MateriaRequest = { nome: '', cor: PALETA[0] };
  protected readonly editandoId = signal<number | null>(null);
  protected rascunho: MateriaRequest = { nome: '', cor: PALETA[0] };

  protected criar(): void {
    if (!this.nova.nome.trim()) return;
    this.executar(this.service.criar(this.nova), () => {
      // a próxima matéria já sugere uma cor diferente
      const proxima = PALETA[(PALETA.indexOf(this.nova.cor) + 1) % PALETA.length];
      this.nova = { nome: '', cor: proxima };
    });
  }

  protected editar(m: Materia): void {
    this.rascunho = { nome: m.nome, cor: m.cor };
    this.editandoId.set(m.id);
  }

  protected salvar(m: Materia): void {
    this.executar(this.service.editar(m.id, this.rascunho), () => this.editandoId.set(null));
  }

  protected remover(m: Materia): void {
    if (confirm(`Apagar "${m.nome}"? As tarefas dela continuam existindo, só ficam sem matéria.`)) {
      this.executar(this.service.remover(m.id));
    }
  }

  private executar(acao: Observable<unknown>, depois?: () => void): void {
    this.erro.set(null);
    acao.subscribe({
      next: () => {
        depois?.();
        this.materias.reload();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}

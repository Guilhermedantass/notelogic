import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Materia, Prioridade, TarefaRequest } from '../core/api.models';
import { hojeIso } from '../core/datas';

/** O "Add task" da v1: um campo no fim da lista. Enter cria; os detalhes aparecem quando você começa a digitar. */
@Component({
  selector: 'app-nova-tarefa',
  imports: [FormsModule],
  template: `
    <form class="nova" [class.aberta]="titulo().length > 0" (ngSubmit)="enviar()">
      <div class="principal">
        <span class="icone">add</span>
        <input
          name="titulo"
          placeholder="Adicionar tarefa"
          autocomplete="off"
          [ngModel]="titulo()"
          (ngModelChange)="titulo.set($event)"
        />
      </div>

      @if (titulo().length > 0) {
        <div class="detalhes">
          <select class="campo" name="materia" [(ngModel)]="materiaId">
            <option [ngValue]="null">Sem matéria</option>
            @for (m of materias(); track m.id) {
              <option [ngValue]="m.id">{{ m.nome }}</option>
            }
          </select>
          <input class="campo" type="date" name="prazo" [(ngModel)]="prazo" />
          <button type="button" class="campo" (click)="prazo = hoje">Hoje</button>
          <select class="campo" name="prioridade" [(ngModel)]="prioridade">
            <option ngValue="BAIXA">Prioridade baixa</option>
            <option ngValue="MEDIA">Prioridade média</option>
            <option ngValue="ALTA">Prioridade alta</option>
          </select>
          <button type="submit" class="botao">Adicionar</button>
        </div>
      }
    </form>
  `,
  styles: `
    .nova {
      border-bottom: 1px solid var(--borda);
      transition: background 0.15s;

      &.aberta {
        background: var(--superficie);
        border-radius: var(--raio);
        border-bottom-color: transparent;
        margin-top: 8px;
      }
    }

    .principal {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 4px;
      color: var(--violeta);

      input {
        flex: 1;
        border: none;
        outline: none;
        background: transparent;
        color: var(--texto);
        font-size: 1rem;

        &::placeholder {
          color: var(--texto-suave);
        }
      }
    }

    .aberta .principal {
      padding: 12px 14px;
    }

    .detalhes {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      padding: 0 14px 14px 46px;
    }
  `,
})
export class NovaTarefa {
  readonly materias = input<Materia[]>([]);
  /** Se a lista está filtrada por uma matéria, a tarefa nova já nasce nela. */
  readonly materiaPadrao = input<number | null>(null);

  readonly criar = output<TarefaRequest>();

  protected readonly titulo = signal('');
  protected readonly hoje = hojeIso();
  protected materiaId: number | null = null;
  protected prazo: string | null = null;
  protected prioridade: Prioridade = 'MEDIA';

  protected enviar(): void {
    const titulo = this.titulo().trim();
    if (!titulo) return;
    this.criar.emit({
      titulo,
      materiaId: this.materiaId ?? this.materiaPadrao(),
      prazo: this.prazo || null,
      prioridade: this.prioridade,
    });
    this.titulo.set('');
    this.materiaId = null;
    this.prazo = null;
    this.prioridade = 'MEDIA';
  }
}

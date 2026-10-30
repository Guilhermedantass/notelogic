import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { AgendaService } from '../core/agenda.service';
import { CategoriaEvento, CategoriaRequest } from '../core/api.models';
import { mensagemDeErro } from '../core/erros';
import { Modal } from '../layout/modal';

/**
 * As categorias da agenda, editadas ali mesmo (sem tela própria no menu). Cada mudança já é salva;
 * a estrela marca as categorias cujos eventos aparecem em "próximas datas importantes".
 */
@Component({
  selector: 'app-categorias',
  imports: [FormsModule, Modal],
  template: `
    <app-modal titulo="Categorias da agenda" (fechar)="fechar.emit()">
      @if (erro(); as mensagem) {
        <div class="aviso-erro" role="alert"><span class="icone">error</span>{{ mensagem }}</div>
      }
      <ul class="lista">
        @for (c of categorias(); track c.id) {
          <li>
            <input
              type="color"
              [value]="c.cor"
              (change)="salvar(c, { cor: cor($event) })"
              aria-label="Cor"
            />
            <input
              class="campo"
              [value]="c.nome"
              maxlength="50"
              aria-label="Nome"
              (change)="salvar(c, { nome: texto($event) })"
            />
            <button
              type="button"
              class="botao-icone estrela"
              [class.ativa]="c.destaque"
              [attr.aria-pressed]="c.destaque"
              [title]="
                c.destaque ? 'Aparece em datas importantes' : 'Não aparece em datas importantes'
              "
              (click)="salvar(c, { destaque: !c.destaque })"
            >
              <span class="icone">star</span>
            </button>
            <button type="button" class="botao-icone perigo" title="Apagar" (click)="remover(c)">
              <span class="icone">delete</span>
            </button>
          </li>
        } @empty {
          <li class="vazio">Nenhuma categoria ainda.</li>
        }
      </ul>

      <form class="nova" (ngSubmit)="criar()">
        <input type="color" name="cor" [(ngModel)]="novaCor" aria-label="Cor da nova categoria" />
        <input
          class="campo"
          name="nome"
          placeholder="Nova categoria"
          maxlength="50"
          [(ngModel)]="novoNome"
        />
        <button type="submit" class="botao">Adicionar</button>
      </form>
      <p class="dica">
        <span class="icone">star</span> = os eventos aparecem em "próximas datas importantes" no
        Hoje.
      </p>
    </app-modal>
  `,
  styles: `
    .lista {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 6px;
      margin-bottom: 16px;

      li {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .campo {
        flex: 1;
      }
    }

    .vazio {
      color: var(--texto-suave);
    }

    input[type='color'] {
      width: 32px;
      height: 32px;
      padding: 0;
      border: 1px solid var(--borda);
      border-radius: 8px;
      background: none;
      cursor: pointer;
    }

    .estrela.ativa {
      color: var(--laranja);

      .icone {
        font-variation-settings: 'FILL' 1;
      }
    }

    .nova {
      display: flex;
      gap: 8px;

      .campo {
        flex: 1;
      }
    }

    .dica {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 12px;
      font-size: 0.8rem;
      color: var(--texto-suave);

      .icone {
        font-size: 16px;
        color: var(--laranja);
        font-variation-settings: 'FILL' 1;
      }
    }
  `,
})
export class Categorias {
  private readonly agenda = inject(AgendaService);

  readonly categorias = input<CategoriaEvento[]>([]);
  /** Algo mudou: quem usa recarrega a lista (e a agenda, por causa das cores). */
  readonly mudou = output<void>();
  readonly fechar = output<void>();

  protected readonly erro = signal<string | null>(null);
  protected novoNome = '';
  protected novaCor = '#543efe';

  protected salvar(c: CategoriaEvento, mudanca: Partial<CategoriaRequest>): void {
    const dados = { nome: c.nome, cor: c.cor, destaque: c.destaque, ...mudanca };
    if (!dados.nome.trim()) return;
    this.executar(this.agenda.editarCategoria(c.id, { ...dados, nome: dados.nome.trim() }));
  }

  protected criar(): void {
    const nome = this.novoNome.trim();
    if (!nome) return;
    this.executar(this.agenda.criarCategoria({ nome, cor: this.novaCor, destaque: false }), () => {
      this.novoNome = '';
    });
  }

  protected remover(c: CategoriaEvento): void {
    if (confirm(`Apagar a categoria "${c.nome}"? Os eventos dela ficam sem categoria.`)) {
      this.executar(this.agenda.removerCategoria(c.id));
    }
  }

  protected cor(evento: Event): string {
    return (evento.target as HTMLInputElement).value;
  }

  protected texto(evento: Event): string {
    return (evento.target as HTMLInputElement).value;
  }

  private executar(acao: Observable<unknown>, depois?: () => void): void {
    this.erro.set(null);
    acao.subscribe({
      next: () => {
        depois?.();
        this.mudou.emit();
      },
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
  }
}

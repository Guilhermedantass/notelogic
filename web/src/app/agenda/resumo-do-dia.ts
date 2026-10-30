import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DataImportante, ResumoDoDia } from '../core/api.models';
import { formatarDia, horaCurta } from '../core/datas';

/**
 * A agenda dentro da tela Hoje: o que acontece hoje e as próximas datas importantes (provas e tarefas de
 * prioridade alta nos próximos 30 dias). Prova não vira tarefa: fica na agenda, e aparece aqui.
 */
@Component({
  selector: 'app-resumo-do-dia',
  imports: [RouterLink],
  template: `
    <section class="bloco">
      <header>
        <h2>Agenda de hoje</h2>
        <a routerLink="/agenda" class="ver">Ver agenda <span class="icone">arrow_forward</span></a>
      </header>
      <ul>
        @for (e of resumo().agenda; track e.id + (e.dataOcorrencia ?? '')) {
          <li [style.--cor]="e.cor">
            <span class="quando">{{ e.diaInteiro ? 'Dia todo' : hora(e.horaInicio!) }}</span>
            <span class="barra"></span>
            <span class="o-que">
              <strong>{{ e.titulo }}</strong>
              @if (e.local) {
                <span class="detalhe">{{ e.local }}</span>
              }
            </span>
          </li>
        } @empty {
          <li class="vazio">Nada marcado para hoje.</li>
        }
      </ul>
    </section>

    <section class="bloco">
      <header>
        <h2>Próximas datas importantes</h2>
      </header>
      <ul>
        @for (d of resumo().importantes; track d.tipo + d.id + d.data) {
          <li [style.--cor]="d.cor">
            <span class="quando data">
              <strong>{{ dia(d.data) }}</strong>
              <span>{{ diaDaSemana(d.data) }}</span>
            </span>
            <span class="barra"></span>
            <span class="o-que">
              <strong>{{ d.titulo }}</strong>
              <span class="detalhe">
                {{ d.rotulo }}
                @if (d.hora) {
                  · {{ hora(d.hora) }}
                }
              </span>
            </span>
            <span class="contagem" [class.perto]="d.diasAte <= 3">{{ contagem(d) }}</span>
          </li>
        } @empty {
          <li class="vazio">Nenhuma prova ou entrega importante nos próximos 30 dias.</li>
        }
      </ul>
    </section>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .bloco {
      padding: 18px 20px;
      border: 1px solid var(--borda);
      border-radius: var(--raio);
      background: var(--fundo);
    }

    header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      h2 {
        font-size: 0.8rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        color: var(--texto-suave);
      }
    }

    .ver {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--violeta-texto);

      .icone {
        font-size: 16px;
      }
    }

    ul {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    li {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .quando {
      flex-shrink: 0;
      width: 64px;
      font-weight: 700;
      font-size: 0.85rem;
      font-variant-numeric: tabular-nums;

      &.data {
        display: flex;
        flex-direction: column;
        width: 52px;

        span {
          font-size: 0.75rem;
          font-weight: 500;
          color: var(--texto-suave);
        }
      }
    }

    .barra {
      align-self: stretch;
      width: 4px;
      border-radius: 4px;
      background: var(--cor, var(--violeta));
    }

    .o-que {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;

      // até duas linhas: a coluna é estreita e o título de uma prova costuma ser comprido
      strong {
        font-weight: 600;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    }

    .detalhe {
      font-size: 0.8rem;
      color: var(--texto-suave);
    }

    .contagem {
      flex-shrink: 0;
      padding: 3px 10px;
      border-radius: 999px;
      background: var(--superficie);
      color: var(--texto-suave);
      font-size: 0.75rem;
      font-weight: 700;

      &.perto {
        background: color-mix(in srgb, var(--vermelho) 14%, transparent);
        color: var(--vermelho);
      }
    }

    .vazio {
      color: var(--texto-suave);
      font-size: 0.9rem;
    }
  `,
})
export class ResumoDoDiaComponent {
  readonly resumo = input.required<ResumoDoDia>();

  protected hora(h: string): string {
    return horaCurta(h);
  }

  /** "3 nov" */
  protected dia(iso: string): string {
    return formatarDia(iso, { day: 'numeric', month: 'short' })
      .replaceAll('.', '')
      .replace(' de ', ' ');
  }

  /** "seg" */
  protected diaDaSemana(iso: string): string {
    return formatarDia(iso, { weekday: 'short' }).replace('.', '');
  }

  protected contagem(d: DataImportante): string {
    if (d.diasAte === 0) return 'hoje';
    if (d.diasAte === 1) return 'amanhã';
    return `em ${d.diasAte} dias`;
  }
}

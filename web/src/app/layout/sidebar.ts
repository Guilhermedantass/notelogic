import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { Tema, TemaService } from '../core/tema.service';

/** A barra violeta da v1, agora na lateral. No celular ela vira uma barra de abas no rodapé. */
@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <a routerLink="/hoje" class="marca">
        <img src="img/logo.png" alt="" width="36" height="36" />
        <span>Note<strong>Logic</strong></span>
      </a>

      <nav>
        @for (item of itens; track item.rota) {
          <a [routerLink]="item.rota" routerLinkActive="ativo" class="item">
            <span class="icone">{{ item.icone }}</span>
            <span class="rotulo">{{ item.rotulo }}</span>
          </a>
        }
      </nav>

      <div class="rodape">
        <div class="seletor-tema" role="radiogroup" aria-label="Tema">
          @for (opcao of temas; track opcao.valor) {
            <button
              role="radio"
              [attr.aria-checked]="temaService.tema() === opcao.valor"
              [class.ativo]="temaService.tema() === opcao.valor"
              [title]="opcao.rotulo"
              (click)="temaService.tema.set(opcao.valor)"
            >
              <span class="icone">{{ opcao.icone }}</span>
            </button>
          }
        </div>
        <a class="credito" href="https://storyset.com" target="_blank" rel="noopener">
          Ilustrações: Storyset
        </a>
      </div>
    </aside>
  `,
  styles: `
    .sidebar {
      position: sticky;
      top: 0;
      height: 100vh;
      width: 240px;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      gap: 32px;
      padding: 28px 16px;
      background: var(--violeta);
      color: #fff;
    }

    .marca {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 0 8px;
      font-size: 1.3rem;
      font-weight: 500;

      img {
        border-radius: 8px;
      }

      strong {
        font-weight: 800;
      }
    }

    nav {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border-radius: 10px;
      font-weight: 500;
      color: rgb(255 255 255 / 0.75);
      transition: background 0.15s, color 0.15s;

      &:hover {
        color: #fff;
        background: rgb(255 255 255 / 0.1);
      }

      &.ativo {
        color: var(--violeta);
        background: #fff;
        font-weight: 700;

        .icone {
          font-variation-settings: 'FILL' 1;
        }
      }
    }

    .rodape {
      margin-top: auto;
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 0 8px;
    }

    .seletor-tema {
      display: flex;
      gap: 2px;
      padding: 3px;
      border-radius: 10px;
      background: rgb(255 255 255 / 0.12);

      button {
        flex: 1;
        display: grid;
        place-items: center;
        padding: 5px 0;
        border-radius: 8px;
        color: rgb(255 255 255 / 0.7);
        transition: background 0.15s, color 0.15s;

        .icone {
          font-size: 18px;
        }

        &:hover {
          color: #fff;
        }

        &.ativo {
          background: #fff;
          color: var(--violeta);
        }
      }
    }

    .credito {
      font-size: 0.7rem;
      color: rgb(255 255 255 / 0.5);
    }

    @media (max-width: 760px) {
      .sidebar {
        position: fixed;
        top: auto;
        bottom: 0;
        left: 0;
        right: 0;
        z-index: 10;
        height: auto;
        width: auto;
        padding: 8px;
      }

      .marca,
      .rodape {
        display: none;
      }

      nav {
        flex-direction: row;
        justify-content: space-around;
      }

      .item {
        flex-direction: column;
        gap: 2px;
        padding: 6px 10px;
        font-size: 0.7rem;
      }
    }
  `,
})
export class Sidebar {
  protected readonly temaService = inject(TemaService);

  protected readonly temas: { valor: Tema; icone: string; rotulo: string }[] = [
    { valor: 'claro', icone: 'light_mode', rotulo: 'Claro' },
    { valor: 'escuro', icone: 'dark_mode', rotulo: 'Escuro' },
    { valor: 'sistema', icone: 'contrast', rotulo: 'Seguir o sistema' },
  ];

  protected readonly itens = [
    { rota: '/hoje', icone: 'today', rotulo: 'Hoje' },
    { rota: '/flashcards', icone: 'style', rotulo: 'Flashcards' },
    { rota: '/pomodoro', icone: 'timer', rotulo: 'Pomodoro' },
    { rota: '/painel', icone: 'insights', rotulo: 'Painel' },
    { rota: '/materias', icone: 'palette', rotulo: 'Matérias' },
  ];
}

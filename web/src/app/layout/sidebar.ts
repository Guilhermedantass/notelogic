import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

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

      <a class="credito" href="https://storyset.com" target="_blank" rel="noopener">
        Ilustrações: Storyset
      </a>
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

    .credito {
      margin-top: auto;
      padding: 0 8px;
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
      .credito {
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
  protected readonly itens = [
    { rota: '/hoje', icone: 'today', rotulo: 'Hoje' },
    { rota: '/flashcards', icone: 'style', rotulo: 'Flashcards' },
    { rota: '/pomodoro', icone: 'timer', rotulo: 'Pomodoro' },
    { rota: '/painel', icone: 'insights', rotulo: 'Painel' },
    { rota: '/materias', icone: 'palette', rotulo: 'Matérias' },
  ];
}

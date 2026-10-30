import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../core/auth.service';
import { Tema, TemaService } from '../core/tema.service';
import { Logo } from './logo';

/** Barra lateral neutra; o violeta aparece só no item ativo. No celular vira uma barra de abas no rodapé. */
@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, Logo],
  template: `
    <aside class="sidebar">
      <a routerLink="/inicio" class="marca" aria-label="NoteLogic, ir para o início">
        <app-logo />
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
        @if (auth.usuario(); as u) {
          <div class="conta">
            <span class="avatar" aria-hidden="true">{{ u.nome.charAt(0).toUpperCase() }}</span>
            <div class="quem">
              <strong>{{ u.nome }}</strong>
              <span>{{ u.email }}</span>
            </div>
            <button class="botao-icone" (click)="auth.sair()" title="Sair" aria-label="Sair">
              <span class="icone">logout</span>
            </button>
          </div>
        }
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
      width: 236px;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      gap: 28px;
      padding: 24px 14px;
      background: var(--lateral);
      border-right: 1px solid var(--borda);
    }

    .marca {
      padding: 4px 10px;
    }

    nav {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 9px 12px;
      border-radius: 10px;
      font-weight: 500;
      color: var(--texto-suave);
      transition:
        background 0.15s,
        color 0.15s;

      &:hover {
        color: var(--texto);
        background: var(--superficie);
      }

      &.ativo {
        color: var(--violeta-texto);
        background: var(--violeta-suave);
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
      padding: 0 6px;
    }

    .conta {
      display: flex;
      align-items: center;
      gap: 10px;
      padding-bottom: 12px;
      border-bottom: 1px solid var(--borda);
    }

    .avatar {
      display: grid;
      place-items: center;
      flex-shrink: 0;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--violeta);
      color: #fff;
      font-weight: 700;
    }

    .quem {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      font-size: 0.8rem;

      strong,
      span {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      span {
        color: var(--texto-suave);
        font-size: 0.72rem;
      }
    }

    .seletor-tema {
      display: flex;
      gap: 2px;
      padding: 3px;
      border-radius: 10px;
      background: var(--superficie);
      border: 1px solid var(--borda);

      button {
        flex: 1;
        display: grid;
        place-items: center;
        padding: 5px 0;
        border-radius: 7px;
        color: var(--texto-suave);
        transition:
          background 0.15s,
          color 0.15s;

        .icone {
          font-size: 18px;
        }

        &:hover {
          color: var(--texto);
        }

        &.ativo {
          background: var(--fundo);
          color: var(--violeta-texto);
          box-shadow: 0 1px 3px rgb(0 0 0 / 0.12);
        }
      }
    }

    .credito {
      font-size: 0.7rem;
      color: var(--texto-suave);
      opacity: 0.7;
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
        padding: 6px;
        border-right: none;
        border-top: 1px solid var(--borda);
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
  protected readonly auth = inject(AuthService);

  protected readonly itens = [
    { rota: '/inicio', icone: 'home', rotulo: 'Início' },
    { rota: '/tarefas', icone: 'checklist', rotulo: 'Tarefas' },
    { rota: '/agenda', icone: 'calendar_month', rotulo: 'Agenda' },
    { rota: '/flashcards', icone: 'style', rotulo: 'Flashcards' },
    { rota: '/pomodoro', icone: 'timer', rotulo: 'Pomodoro' },
    { rota: '/painel', icone: 'insights', rotulo: 'Painel' },
    { rota: '/materias', icone: 'palette', rotulo: 'Matérias' },
  ];

  protected readonly temas: { valor: Tema; icone: string; rotulo: string }[] = [
    { valor: 'claro', icone: 'light_mode', rotulo: 'Claro' },
    { valor: 'escuro', icone: 'dark_mode', rotulo: 'Escuro' },
    { valor: 'sistema', icone: 'contrast', rotulo: 'Seguir o sistema' },
  ];
}

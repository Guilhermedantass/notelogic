import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Sidebar } from './layout/sidebar';

/** A "moldura" do app: barra lateral fixa e, ao lado, a tela da rota atual. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar],
  template: `
    <div class="layout">
      <app-sidebar />
      <main class="conteudo">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .layout {
      display: flex;
      min-height: 100vh;
    }

    .conteudo {
      flex: 1;
      min-width: 0;
      padding: 48px clamp(20px, 5vw, 72px);
    }

    @media (max-width: 760px) {
      .layout {
        flex-direction: column-reverse;
      }

      .conteudo {
        padding: 28px 20px 96px;
      }
    }
  `,
})
export class App {}

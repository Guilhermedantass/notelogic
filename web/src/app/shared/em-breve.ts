import { Component, input } from '@angular/core';

/** Tela provisória das partes que ainda vão ser feitas. Título e ícone vêm do "data" da rota. */
@Component({
  selector: 'app-em-breve',
  template: `
    <h1 class="titulo-pagina">{{ titulo() }}</h1>
    <div class="vazio">
      <span class="icone">{{ icone() }}</span>
      <p>Em construção: chega nos próximos blocos.</p>
    </div>
  `,
  styles: `
    .vazio {
      display: grid;
      place-items: center;
      gap: 12px;
      margin-top: 80px;
      color: var(--texto-suave);

      .icone {
        font-size: 64px;
        color: var(--violeta);
        opacity: 0.4;
      }
    }
  `,
})
export class EmBreve {
  // withComponentInputBinding() liga o "data" da rota a estes inputs
  readonly titulo = input('');
  readonly icone = input('construction');
}

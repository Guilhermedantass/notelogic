import { Component } from '@angular/core';

/**
 * A marca do NoteLogic: um quadrado arredondado em gradiente violeta com um "check" branco,
 * e o nome ao lado. É SVG, então fica nítido em qualquer tamanho e não depende de imagem.
 */
@Component({
  selector: 'app-logo',
  template: `
    <svg viewBox="0 0 32 32" width="32" height="32" aria-hidden="true">
      <defs>
        <linearGradient id="nl-gradiente" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#7a68ff" />
          <stop offset="1" stop-color="#4330e0" />
        </linearGradient>
      </defs>
      <rect width="32" height="32" rx="9" fill="url(#nl-gradiente)" />
      <path
        d="M9.5 16.5l4.5 4.5 8.5-9.5"
        fill="none"
        stroke="#fff"
        stroke-width="3.2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <span class="nome">note<strong>logic</strong></span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 10px;
    }

    .nome {
      font-size: 1.2rem;
      font-weight: 500;
      letter-spacing: -0.02em;
      color: var(--texto);

      strong {
        font-weight: 800;
        color: var(--violeta-texto);
      }
    }
  `,
})
export class Logo {}

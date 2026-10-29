import { effect, Injectable, signal } from '@angular/core';

export type Tema = 'claro' | 'escuro' | 'sistema';

const CHAVE = 'notelogic.tema';

/**
 * Guarda a escolha de tema no localStorage e a aplica como data-tema no <html>.
 * O CSS (styles.scss) faz o resto: cada tema é só um conjunto diferente de variáveis.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly tema = signal<Tema>(lerSalvo());

  constructor() {
    // effect: roda agora e de novo toda vez que o signal mudar
    effect(() => {
      const tema = this.tema();
      document.documentElement.dataset['tema'] = tema;
      localStorage.setItem(CHAVE, tema);
    });
  }
}

function lerSalvo(): Tema {
  const salvo = localStorage.getItem(CHAVE);
  return salvo === 'escuro' || salvo === 'sistema' ? salvo : 'claro';
}

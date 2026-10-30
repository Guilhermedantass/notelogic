import { afterNextRender, Component, ElementRef, input, output, viewChild } from '@angular/core';

/**
 * Uma janela por cima da tela, com o <dialog> nativo: o navegador já cuida do fundo escurecido, do Esc
 * e de prender o foco dentro dela. Quem usa mostra com @if e fecha tirando do @if (ouvindo "fechar").
 */
@Component({
  selector: 'app-modal',
  template: `
    <dialog
      #dialogo
      [attr.aria-label]="titulo()"
      (close)="fechar.emit()"
      (click)="cliqueNoFundo($event)"
    >
      <header>
        <h2>{{ titulo() }}</h2>
        <button type="button" class="botao-icone" (click)="dialogo.close()" aria-label="Fechar">
          <span class="icone">close</span>
        </button>
      </header>
      <ng-content />
    </dialog>
  `,
  styles: `
    dialog {
      margin: auto;
      width: min(var(--largura, 520px), calc(100vw - 32px));
      max-height: calc(100vh - 48px);
      padding: 20px 24px 24px;
      border: 1px solid var(--borda);
      border-radius: 16px;
      background: var(--fundo);
      color: var(--texto);
      box-shadow: var(--sombra);

      &::backdrop {
        background: rgb(15 10 40 / 0.45);
      }
    }

    header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      h2 {
        font-size: 1.15rem;
        font-weight: 700;
      }
    }
  `,
})
export class Modal {
  readonly titulo = input.required<string>();
  readonly fechar = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  constructor() {
    afterNextRender(() => this.dialogo().nativeElement.showModal());
  }

  /** O clique no fundo escurecido chega no próprio <dialog> (o conteúdo fica nos filhos). */
  protected cliqueNoFundo(evento: MouseEvent): void {
    if (evento.target === this.dialogo().nativeElement) {
      this.dialogo().nativeElement.close();
    }
  }
}

import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Raiz do app: só escolhe entre as telas de acesso (entrar/cadastro) e o app logado (Shell). */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class App {}

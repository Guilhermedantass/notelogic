import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Guard: roda antes de entrar na rota. Sem login, redireciona para /entrar (guardando para onde ia). */
export const exigeLogin: CanActivateFn = (_rota, estado) => {
  if (inject(AuthService).logado()) return true;
  return inject(Router).createUrlTree(['/entrar'], { queryParams: { voltar: estado.url } });
};

/** O contrário: quem já está logado não precisa ver a tela de entrar. */
export const soDeslogado: CanActivateFn = () =>
  inject(AuthService).logado() ? inject(Router).createUrlTree(['/hoje']) : true;

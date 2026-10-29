import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Interceptor: toda requisição do HttpClient (inclusive as dos httpResource) passa por aqui.
 * 1. Coloca o token no cabeçalho Authorization.
 * 2. Se a API responder 401 (token vencido ou inválido), desloga e volta para a tela de entrar.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const ehAuth = req.url.startsWith('/api/auth/login') || req.url.startsWith('/api/auth/cadastro');

  const comToken = token && !ehAuth ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(comToken).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse && erro.status === 401 && !ehAuth) {
        auth.sair();
      }
      return throwError(() => erro);
    }),
  );
};

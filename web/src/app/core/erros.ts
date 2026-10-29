import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from './api.models';

/** Transforma qualquer erro de requisição numa frase para mostrar na tela. */
export function mensagemDeErro(erro: unknown): string {
  if (!(erro instanceof HttpErrorResponse)) {
    return 'Algo deu errado.';
  }
  if (erro.status === 0) {
    return 'Não foi possível falar com a API. Ela está rodando?';
  }
  const problema = erro.error as Partial<ProblemDetail> | null;
  if (problema?.erros) {
    return Object.entries(problema.erros)
      .map(([campo, msg]) => `${campo}: ${msg}`)
      .join(' · ');
  }
  return problema?.detail ?? `Erro ${erro.status}`;
}

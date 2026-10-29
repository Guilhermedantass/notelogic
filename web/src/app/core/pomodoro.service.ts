import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { IniciarSessaoRequest, Sessao } from './api.models';

@Injectable({ providedIn: 'root' })
export class PomodoroService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/pomodoro/sessoes';

  /** A API responde 204 (sem corpo) quando não há sessão: o HttpClient entrega null. */
  atual(): Observable<Sessao | null> {
    return this.http.get<Sessao | null>(`${this.url}/atual`);
  }

  iniciar(dados: IniciarSessaoRequest): Observable<Sessao> {
    return this.http.post<Sessao>(this.url, dados);
  }

  concluir(id: number): Observable<Sessao> {
    return this.http.patch<Sessao>(`${this.url}/${id}/concluir`, null);
  }

  interromper(id: number): Observable<Sessao> {
    return this.http.patch<Sessao>(`${this.url}/${id}/interromper`, null);
  }
}

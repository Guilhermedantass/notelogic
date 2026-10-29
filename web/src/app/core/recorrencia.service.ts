import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Recorrencia, RecorrenciaRequest } from './api.models';

/** As regras de repetição. "Só esta" é editar a tarefa; "esta e as próximas" é mexer na regra a partir do dia. */
@Injectable({ providedIn: 'root' })
export class RecorrenciaService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/recorrencias';

  criar(dados: RecorrenciaRequest): Observable<Recorrencia> {
    return this.http.post<Recorrencia>(this.url, dados);
  }

  editarAPartirDe(id: number, aPartirDe: string, dados: RecorrenciaRequest): Observable<Recorrencia> {
    return this.http.put<Recorrencia>(`${this.url}/${id}`, dados, { params: { aPartirDe } });
  }

  encerrarAPartirDe(id: number, aPartirDe: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`, { params: { aPartirDe } });
  }
}

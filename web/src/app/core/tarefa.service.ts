import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Tarefa, TarefaRequest } from './api.models';

/** As escritas de tarefas. As leituras usam httpResource direto nas telas (reagem aos filtros sozinhas). */
@Injectable({ providedIn: 'root' })
export class TarefaService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/tarefas';

  criar(dados: TarefaRequest): Observable<Tarefa> {
    return this.http.post<Tarefa>(this.url, dados);
  }

  editar(id: number, dados: TarefaRequest): Observable<Tarefa> {
    return this.http.put<Tarefa>(`${this.url}/${id}`, dados);
  }

  concluir(id: number): Observable<Tarefa> {
    return this.http.patch<Tarefa>(`${this.url}/${id}/concluir`, null);
  }

  reabrir(id: number): Observable<Tarefa> {
    return this.http.patch<Tarefa>(`${this.url}/${id}/reabrir`, null);
  }

  remover(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

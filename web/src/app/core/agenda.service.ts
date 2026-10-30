import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CategoriaEvento,
  CategoriaRequest,
  Evento,
  EventoRequest,
  OcorrenciaRequest,
} from './api.models';

/**
 * Eventos e categorias da agenda. Num evento que se repete:
 * "só esta" = alterarOcorrencia/cancelarOcorrencia; "esta e as próximas" = editar/remover com aPartirDe;
 * sem aPartirDe = a série inteira.
 */
@Injectable({ providedIn: 'root' })
export class AgendaService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/eventos';
  private readonly urlCategorias = '/api/agenda/categorias';

  /** {@code ate} inclusive, como na API. */
  listar(de: string, ate: string): Observable<Evento[]> {
    return this.http.get<Evento[]>(this.url, { params: { de, ate } });
  }

  criar(dados: EventoRequest): Observable<Evento> {
    return this.http.post<Evento>(this.url, dados);
  }

  editar(id: number, dados: EventoRequest, aPartirDe?: string): Observable<Evento> {
    return this.http.put<Evento>(`${this.url}/${id}`, dados, {
      params: aPartirDe ? { aPartirDe } : {},
    });
  }

  remover(id: number, aPartirDe?: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`, { params: aPartirDe ? { aPartirDe } : {} });
  }

  alterarOcorrencia(id: number, dia: string, dados: OcorrenciaRequest): Observable<void> {
    return this.http.put<void>(`${this.url}/${id}/ocorrencias/${dia}`, dados);
  }

  cancelarOcorrencia(id: number, dia: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}/ocorrencias/${dia}`);
  }

  criarCategoria(dados: CategoriaRequest): Observable<CategoriaEvento> {
    return this.http.post<CategoriaEvento>(this.urlCategorias, dados);
  }

  editarCategoria(id: number, dados: CategoriaRequest): Observable<CategoriaEvento> {
    return this.http.put<CategoriaEvento>(`${this.urlCategorias}/${id}`, dados);
  }

  removerCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.urlCategorias}/${id}`);
  }
}
